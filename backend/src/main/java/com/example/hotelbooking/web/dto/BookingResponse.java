package com.example.hotelbooking.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.example.hotelbooking.domain.Booking;
import com.example.hotelbooking.domain.BookingStatus;

public record BookingResponse(
        String reference,
        BookingStatus status,
        Long hotelId,
        String hotelName,
        String hotelLocation,
        String guestName,
        String guestEmail,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal pricePerNight,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant cancelledAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getReference(), b.getStatus(), b.getHotel().getId(),
                b.getHotel().getName(), b.getHotel().getLocation(), b.getGuestName(), b.getGuestEmail(),
                b.getCheckIn(), b.getCheckOut(), b.getNights(), b.getPricePerNight(), b.getTotalAmount(),
                b.getCreatedAt(), b.getCancelledAt());
    }
}
