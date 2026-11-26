package com.example.hotelbooking.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.service.BookingService;
import com.example.hotelbooking.web.dto.BookingResponse;
import com.example.hotelbooking.web.dto.CancelBookingRequest;
import com.example.hotelbooking.web.dto.CreateBookingRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookingService.createBooking(request));
    }

    @GetMapping("/{reference}")
    public BookingResponse get(@PathVariable String reference, @RequestParam String email) {
        return BookingResponse.from(bookingService.findBooking(reference, email));
    }

    @PostMapping("/{reference}/cancel")
    public BookingResponse cancel(@PathVariable String reference, @Valid @RequestBody CancelBookingRequest request) {
        return BookingResponse.from(bookingService.cancelBooking(reference, request.email()));
    }
}
