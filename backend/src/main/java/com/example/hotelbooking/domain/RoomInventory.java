package com.example.hotelbooking.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Number of rooms already reserved at one hotel on one night. */
@Entity
@Table(name = "room_inventory")
public class RoomInventory {

    @EmbeddedId
    private InventoryId id;

    @Column(name = "booked_rooms", nullable = false)
    private int bookedRooms;

    protected RoomInventory() {
    }

    public RoomInventory(Long hotelId, LocalDate stayDate) {
        this.id = new InventoryId(hotelId, stayDate);
        this.bookedRooms = 0;
    }

    public InventoryId getId() { return id; }
    public LocalDate getStayDate() { return id.getStayDate(); }
    public int getBookedRooms() { return bookedRooms; }

    public void reserve() {
        bookedRooms++;
    }

    public void release() {
        if (bookedRooms > 0) {
            bookedRooms--;
        }
    }
}
