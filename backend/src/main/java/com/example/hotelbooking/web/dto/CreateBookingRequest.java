package com.example.hotelbooking.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotNull(message = "Hotel is required.")
        Long hotelId,

        @NotNull(message = "Check-in date is required.")
        LocalDate checkIn,

        @NotNull(message = "Check-out date is required.")
        LocalDate checkOut,

        @NotBlank(message = "Name is required.")
        @Size(max = 100, message = "Name must be at most 100 characters.")
        String guestName,

        @NotBlank(message = "Email is required.")
        @Email(message = "Please enter a valid email address.")
        @Size(max = 254, message = "Email must be at most 254 characters.")
        String guestEmail) {
}
