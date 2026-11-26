package com.example.hotelbooking.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class InventoryId implements Serializable {

    @Column(name = "hotel_id")
    private Long hotelId;

    @Column(name = "stay_date")
    private LocalDate stayDate;

    protected InventoryId() {
    }

    public InventoryId(Long hotelId, LocalDate stayDate) {
        this.hotelId = hotelId;
        this.stayDate = stayDate;
    }

    public Long getHotelId() { return hotelId; }
    public LocalDate getStayDate() { return stayDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryId other)) return false;
        return Objects.equals(hotelId, other.hotelId) && Objects.equals(stayDate, other.stayDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hotelId, stayDate);
    }
}
