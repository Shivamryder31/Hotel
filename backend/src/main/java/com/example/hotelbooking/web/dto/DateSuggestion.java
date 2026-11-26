package com.example.hotelbooking.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An alternative stay of the same length that still has a room (surprise feature). */
public record DateSuggestion(
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        int shiftDays,
        int availableRooms,
        BigDecimal totalAmount) {
}
