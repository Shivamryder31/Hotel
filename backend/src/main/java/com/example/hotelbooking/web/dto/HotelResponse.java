package com.example.hotelbooking.web.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.hotelbooking.domain.Hotel;

/** {@code availableRooms} is null when no dates were supplied. */
public record HotelResponse(
        Long id,
        String name,
        String location,
        String description,
        List<String> amenities,
        BigDecimal pricePerNight,
        Integer availableRooms) {

    public static HotelResponse from(Hotel h, Integer availableRooms) {
        return new HotelResponse(h.getId(), h.getName(), h.getLocation(), h.getDescription(),
                List.copyOf(h.getAmenities()), h.getPricePerNight(), availableRooms);
    }
}
