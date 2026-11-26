package com.example.hotelbooking.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotelbooking.domain.Booking;
import com.example.hotelbooking.domain.BookingStatus;
import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.domain.RoomInventory;
import com.example.hotelbooking.domain.StayPeriod;
import com.example.hotelbooking.exception.ApiException;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.HotelRepository;
import com.example.hotelbooking.repository.RoomInventoryRepository;
import com.example.hotelbooking.web.dto.CreateBookingRequest;

/**
 * All state-changing booking logic. Every method that touches availability first takes a
 * pessimistic lock on the hotel row, so "check availability, then reserve" is atomic per hotel.
 */
@Service
public class BookingService {

    /** No 0/O/1/I so references are easy to read out over the phone. */
    private static final String REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REFERENCE_LENGTH = 8;

    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public BookingService(HotelRepository hotelRepository,
                          RoomInventoryRepository inventoryRepository,
                          BookingRepository bookingRepository,
                          Clock clock) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional
    public Booking createBooking(CreateBookingRequest request) {
        StayPeriod stay = StayPeriod.of(request.checkIn(), request.checkOut(), LocalDate.now(clock));

        // Serialise with every other booking/cancellation for this hotel.
        Hotel hotel = hotelRepository.findByIdForUpdate(request.hotelId())
                .orElseThrow(() -> ApiException.notFound("Hotel not found."));

        Map<LocalDate, RoomInventory> inventory = loadInventory(hotel, stay);

        for (LocalDate night : stay.nightDates()) {
            if (inventory.get(night).getBookedRooms() >= hotel.getTotalRooms()) {
                throw ApiException.roomUnavailable(
                        hotel.getName() + " is fully booked on " + night + ". Please choose different dates.");
            }
        }

        inventory.values().forEach(RoomInventory::reserve);
        inventoryRepository.saveAll(inventory.values());

        Booking booking = new Booking(
                newReference(),
                hotel,
                request.guestName().trim(),
                normaliseEmail(request.guestEmail()),
                stay.checkIn(),
                stay.checkOut(),
                stay.nights(),
                hotel.getPricePerNight(),
                PriceCalculator.total(hotel.getPricePerNight(), stay.nights()),
                Instant.now(clock));
        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public Booking findBooking(String reference, String email) {
        return bookingRepository
                .findByReferenceAndGuestEmail(normaliseReference(reference), normaliseEmail(email))
                .orElseThrow(BookingService::bookingNotFound);
    }

    @Transactional
    public Booking cancelBooking(String reference, String email) {
        Booking booking = bookingRepository
                .findForUpdate(normaliseReference(reference), normaliseEmail(email))
                .orElseThrow(BookingService::bookingNotFound);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.alreadyCancelled("This booking has already been cancelled.");
        }
        if (booking.getCheckIn().isBefore(LocalDate.now(clock))) {
            throw ApiException.cancellationNotAllowed("This stay has already started and can no longer be cancelled.");
        }

        Hotel hotel = hotelRepository.findByIdForUpdate(booking.getHotel().getId())
                .orElseThrow(() -> ApiException.notFound("Hotel not found."));

        StayPeriod stay = new StayPeriod(booking.getCheckIn(), booking.getCheckOut());
        List<RoomInventory> rows = inventoryRepository.findForHotel(hotel.getId(), stay.checkIn(), stay.checkOut());
        rows.forEach(RoomInventory::release);
        inventoryRepository.saveAll(rows);

        booking.cancel(Instant.now(clock));
        return bookingRepository.save(booking);
    }

    /** Existing rows for the stay, plus an empty (unsaved) row for every night that has none yet. */
    private Map<LocalDate, RoomInventory> loadInventory(Hotel hotel, StayPeriod stay) {
        Map<LocalDate, RoomInventory> byDate = new HashMap<>();
        for (RoomInventory row : inventoryRepository.findForHotel(hotel.getId(), stay.checkIn(), stay.checkOut())) {
            byDate.put(row.getStayDate(), row);
        }
        List<LocalDate> missing = new ArrayList<>();
        for (LocalDate night : stay.nightDates()) {
            if (!byDate.containsKey(night)) {
                missing.add(night);
            }
        }
        missing.forEach(night -> byDate.put(night, new RoomInventory(hotel.getId(), night)));
        return byDate;
    }

    private String newReference() {
        String reference;
        do {
            StringBuilder sb = new StringBuilder("HB-");
            for (int i = 0; i < REFERENCE_LENGTH; i++) {
                sb.append(REFERENCE_ALPHABET.charAt(random.nextInt(REFERENCE_ALPHABET.length())));
            }
            reference = sb.toString();
        } while (bookingRepository.existsByReference(reference));
        return reference;
    }

    private static String normaliseEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normaliseReference(String reference) {
        return reference == null ? "" : reference.trim().toUpperCase(Locale.ROOT);
    }

    /** Same message for "wrong reference" and "wrong email" so references cannot be probed. */
    private static ApiException bookingNotFound() {
        return ApiException.notFound("We couldn't find a booking with that reference and email address.");
    }
}
