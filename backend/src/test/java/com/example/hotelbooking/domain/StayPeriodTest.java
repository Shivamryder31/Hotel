package com.example.hotelbooking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.hotelbooking.exception.ApiException;

class StayPeriodTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);

    @Test
    void acceptsValidStayAndCountsNights() {
        StayPeriod stay = StayPeriod.of(TODAY, TODAY.plusDays(3), TODAY);
        assertEquals(3, stay.nights());
        assertEquals(3, stay.nightDates().size());
        // the check-out day is not an occupied night
        assertEquals(TODAY.plusDays(2), stay.nightDates().get(2));
    }

    @Test
    void rejectsCheckInInThePast() {
        ApiException ex = assertThrows(ApiException.class,
                () -> StayPeriod.of(TODAY.minusDays(1), TODAY.plusDays(2), TODAY));
        assertEquals(ApiException.INVALID_DATES, ex.getCode());
    }

    @Test
    void rejectsCheckOutNotAfterCheckIn() {
        assertThrows(ApiException.class, () -> StayPeriod.of(TODAY, TODAY, TODAY));
        assertThrows(ApiException.class, () -> StayPeriod.of(TODAY.plusDays(5), TODAY.plusDays(2), TODAY));
    }

    @Test
    void rejectsMissingDatesAndOverlongStays() {
        assertThrows(ApiException.class, () -> StayPeriod.of(null, TODAY.plusDays(2), TODAY));
        assertThrows(ApiException.class,
                () -> StayPeriod.of(TODAY, TODAY.plusDays(StayPeriod.MAX_NIGHTS + 1), TODAY));
    }
}
