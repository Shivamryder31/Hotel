package com.example.hotelbooking.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false, updatable = false)
    private Hotel hotel;

    @Column(name = "guest_name", nullable = false)
    private String guestName;

    @Column(name = "guest_email", nullable = false)
    private String guestEmail;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int nights;

    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Booking() {
    }

    public Booking(String reference, Hotel hotel, String guestName, String guestEmail,
                   LocalDate checkIn, LocalDate checkOut, int nights,
                   BigDecimal pricePerNight, BigDecimal totalAmount, Instant createdAt) {
        this.reference = reference;
        this.hotel = hotel;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.pricePerNight = pricePerNight;
        this.totalAmount = totalAmount;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = createdAt;
    }

    public void cancel(Instant when) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = when;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public Hotel getHotel() { return hotel; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
