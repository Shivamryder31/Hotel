package com.example.hotelbooking.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.domain.RoomInventory;
import com.example.hotelbooking.domain.StayPeriod;
import com.example.hotelbooking.repository.RoomInventoryRepository;
import com.example.hotelbooking.web.dto.DateSuggestion;

/** Read-only availability queries. Writes happen in {@link BookingService} under a hotel lock. */
@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    static final int SUGGESTION_WINDOW_DAYS = 7;
    static final int MAX_SUGGESTIONS = 3;

    private final RoomInventoryRepository inventoryRepository;

    public AvailabilityService(RoomInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Rooms that can still be booked for the whole stay: the tightest night decides.
     * A night without an inventory row has nothing booked.
     */
    public static int computeAvailable(int totalRooms, StayPeriod stay, Map<LocalDate, Integer> bookedByDate) {
        int available = totalRooms;
        for (LocalDate night : stay.nightDates()) {
            available = Math.min(available, totalRooms - bookedByDate.getOrDefault(night, 0));
        }
        return Math.max(available, 0);
    }

    public int availableRooms(Hotel hotel, StayPeriod stay) {
        Map<LocalDate, Integer> booked = toBookedMap(
                inventoryRepository.findForHotel(hotel.getId(), stay.checkIn(), stay.checkOut()));
        return computeAvailable(hotel.getTotalRooms(), stay, booked);
    }

    /** One query for all hotels, then grouped in memory. */
    public Map<Long, Integer> availableRooms(List<Hotel> hotels, StayPeriod stay) {
        Map<Long, Map<LocalDate, Integer>> bookedByHotel = new HashMap<>();
        for (RoomInventory row : inventoryRepository.findAllInRange(stay.checkIn(), stay.checkOut())) {
            bookedByHotel
                    .computeIfAbsent(row.getId().getHotelId(), k -> new HashMap<>())
                    .put(row.getStayDate(), row.getBookedRooms());
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Hotel hotel : hotels) {
            result.put(hotel.getId(), computeAvailable(hotel.getTotalRooms(), stay,
                    bookedByHotel.getOrDefault(hotel.getId(), Map.of())));
        }
        return result;
    }

    /**
     * Nearest stays of the same length (within +/- 7 days) that still have a room,
     * closest first; earlier dates win ties. Never suggests check-in dates in the past.
     */
    public List<DateSuggestion> suggestAlternatives(Hotel hotel, StayPeriod requested, LocalDate today) {
        LocalDate windowStart = requested.checkIn().minusDays(SUGGESTION_WINDOW_DAYS);
        LocalDate windowEnd = requested.checkOut().plusDays(SUGGESTION_WINDOW_DAYS);
        Map<LocalDate, Integer> booked = toBookedMap(
                inventoryRepository.findForHotel(hotel.getId(), windowStart, windowEnd));

        List<DateSuggestion> suggestions = new ArrayList<>();
        for (int distance = 1; distance <= SUGGESTION_WINDOW_DAYS && suggestions.size() < MAX_SUGGESTIONS; distance++) {
            for (int shift : new int[] {-distance, distance}) {
                StayPeriod candidate = requested.shiftedBy(shift);
                if (candidate.checkIn().isBefore(today)) {
                    continue;
                }
                int free = computeAvailable(hotel.getTotalRooms(), candidate, booked);
                if (free > 0 && suggestions.size() < MAX_SUGGESTIONS) {
                    suggestions.add(new DateSuggestion(candidate.checkIn(), candidate.checkOut(),
                            candidate.nights(), shift, free,
                            PriceCalculator.total(hotel.getPricePerNight(), candidate.nights())));
                }
            }
        }
        return suggestions;
    }

    private static Map<LocalDate, Integer> toBookedMap(List<RoomInventory> rows) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (RoomInventory row : rows) {
            map.put(row.getStayDate(), row.getBookedRooms());
        }
        return map;
    }
}
