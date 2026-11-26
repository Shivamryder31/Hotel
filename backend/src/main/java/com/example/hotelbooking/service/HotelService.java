package com.example.hotelbooking.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.domain.StayPeriod;
import com.example.hotelbooking.exception.ApiException;
import com.example.hotelbooking.repository.HotelRepository;
import com.example.hotelbooking.web.dto.DateSuggestion;
import com.example.hotelbooking.web.dto.HotelResponse;
import com.example.hotelbooking.web.dto.QuoteResponse;

@Service
@Transactional(readOnly = true)
public class HotelService {

    private final HotelRepository hotelRepository;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    public HotelService(HotelRepository hotelRepository, AvailabilityService availabilityService, Clock clock) {
        this.hotelRepository = hotelRepository;
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    public List<HotelResponse> search(String location, LocalDate checkIn, LocalDate checkOut) {
        StayPeriod stay = optionalStay(checkIn, checkOut);
        List<Hotel> hotels = (location == null || location.isBlank())
                ? hotelRepository.findAllByOrderByNameAsc()
                : hotelRepository.findByLocationContainingIgnoreCaseOrderByNameAsc(location.trim());

        if (stay == null) {
            return hotels.stream().map(h -> HotelResponse.from(h, null)).toList();
        }
        Map<Long, Integer> available = availabilityService.availableRooms(hotels, stay);
        return hotels.stream().map(h -> HotelResponse.from(h, available.get(h.getId()))).toList();
    }

    public HotelResponse get(Long id, LocalDate checkIn, LocalDate checkOut) {
        Hotel hotel = findHotel(id);
        StayPeriod stay = optionalStay(checkIn, checkOut);
        Integer available = stay == null ? null : availabilityService.availableRooms(hotel, stay);
        return HotelResponse.from(hotel, available);
    }

    /** Price preview shown before the guest confirms. Computed here, never on the client. */
    public QuoteResponse quote(Long id, LocalDate checkIn, LocalDate checkOut) {
        Hotel hotel = findHotel(id);
        StayPeriod stay = StayPeriod.of(checkIn, checkOut, today());
        return new QuoteResponse(hotel.getId(), hotel.getName(), stay.checkIn(), stay.checkOut(), stay.nights(),
                hotel.getPricePerNight(), PriceCalculator.total(hotel.getPricePerNight(), stay.nights()),
                availabilityService.availableRooms(hotel, stay));
    }

    public List<DateSuggestion> alternatives(Long id, LocalDate checkIn, LocalDate checkOut) {
        Hotel hotel = findHotel(id);
        StayPeriod stay = StayPeriod.of(checkIn, checkOut, today());
        return availabilityService.suggestAlternatives(hotel, stay, today());
    }

    private Hotel findHotel(Long id) {
        return hotelRepository.findById(id).orElseThrow(() -> ApiException.notFound("Hotel not found."));
    }

    /** Both dates or neither; a half-specified range is a client error. */
    private StayPeriod optionalStay(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null && checkOut == null) {
            return null;
        }
        return StayPeriod.of(checkIn, checkOut, today());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
