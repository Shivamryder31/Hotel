package com.example.hotelbooking.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuoteResponse(
        Long hotelId,
        String hotelName,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal pricePerNight,
        BigDecimal totalAmount,
        int availableRooms) {
}
