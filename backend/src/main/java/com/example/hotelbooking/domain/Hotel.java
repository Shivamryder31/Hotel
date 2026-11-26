package com.example.hotelbooking.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_amenities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;

    @Column(name = "total_rooms", nullable = false)
    private int totalRooms;

    protected Hotel() {
        // for JPA
    }

    /** Constructor used by tests; hotels are seeded through Flyway in the real app. */
    public Hotel(Long id, String name, String location, String description,
                 List<String> amenities, BigDecimal pricePerNight, int totalRooms) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.amenities = new ArrayList<>(amenities);
        this.pricePerNight = pricePerNight;
        this.totalRooms = totalRooms;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public List<String> getAmenities() { return amenities; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public int getTotalRooms() { return totalRooms; }
}
