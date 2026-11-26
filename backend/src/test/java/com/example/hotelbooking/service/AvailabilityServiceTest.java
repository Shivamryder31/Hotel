package com.example.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.hotelbooking.domain.StayPeriod;

class AvailabilityServiceTest {

    private static final LocalDate D1 = LocalDate.of(2026, 7, 1);

    @Test
    void noInventoryRowsMeansAllRoomsFree() {
        StayPeriod stay = new StayPeriod(D1, D1.plusDays(3));
        assertEquals(4, AvailabilityService.computeAvailable(4, stay, Map.of()));
    }

    @Test
    void tightestNightDecidesAvailability() {
        StayPeriod stay = new StayPeriod(D1, D1.plusDays(3));
        Map<LocalDate, Integer> booked = Map.of(D1, 1, D1.plusDays(1), 3, D1.plusDays(2), 2);
        assertEquals(1, AvailabilityService.computeAvailable(4, stay, booked));
    }

    @Test
    void overlappingStayOnOneFullNightIsUnavailable() {
        StayPeriod stay = new StayPeriod(D1, D1.plusDays(4));
        Map<LocalDate, Integer> booked = Map.of(D1.plusDays(2), 2); // capacity 2, full on night 3
        assertEquals(0, AvailabilityService.computeAvailable(2, stay, booked));
    }

    @Test
    void checkOutDayDoesNotCountAsOccupied() {
        // Another guest is checking in on D1+2 (full that night); our stay ends that morning.
        StayPeriod stay = new StayPeriod(D1, D1.plusDays(2));
        Map<LocalDate, Integer> booked = Map.of(D1.plusDays(2), 1);
        assertEquals(1, AvailabilityService.computeAvailable(1, stay, booked));
    }
}
