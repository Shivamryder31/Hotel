package com.example.hotelbooking.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.example.hotelbooking.exception.ApiException;

/**
 * A stay is the half-open interval [checkIn, checkOut): the guest occupies the room on the
 * nights checkIn .. checkOut-1 and leaves on the morning of checkOut.
 */
public record StayPeriod(LocalDate checkIn, LocalDate checkOut) {

    public static final int MAX_NIGHTS = 30;

    /** Validates user-supplied dates against the booking rules. */
    public static StayPeriod of(LocalDate checkIn, LocalDate checkOut, LocalDate today) {
        if (checkIn == null || checkOut == null) {
            throw ApiException.invalidDates("Both check-in and check-out dates are required.");
        }
        if (checkIn.isBefore(today)) {
            throw ApiException.invalidDates("Check-in date cannot be in the past.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidDates("Check-out date must be after the check-in date.");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw ApiException.invalidDates("A single stay cannot be longer than " + MAX_NIGHTS + " nights.");
        }
        return new StayPeriod(checkIn, checkOut);
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Each occupied night, i.e. checkIn inclusive to checkOut exclusive. */
    public List<LocalDate> nightDates() {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            dates.add(d);
        }
        return dates;
    }

    public StayPeriod shiftedBy(int days) {
        return new StayPeriod(checkIn.plusDays(days), checkOut.plusDays(days));
    }
}
