package com.example.hotelbooking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.hotelbooking.domain.Hotel;

import jakarta.persistence.LockModeType;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByLocationContainingIgnoreCaseOrderByNameAsc(String location);

    List<Hotel> findAllByOrderByNameAsc();

    /**
     * {@code SELECT ... FOR UPDATE} on the hotel row. Every write that changes a hotel's
     * availability takes this lock first, so concurrent bookings for the same hotel are serialised.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hotel h where h.id = :id")
    Optional<Hotel> findByIdForUpdate(@Param("id") Long id);
}
