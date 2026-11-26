package com.example.hotelbooking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.hotelbooking.domain.Booking;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByReference(String reference);

    Optional<Booking> findByReferenceAndGuestEmail(String reference, String guestEmail);

    /** Locking read used by cancellation so two simultaneous cancels cannot both release the room. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.reference = :reference and b.guestEmail = :email")
    Optional<Booking> findForUpdate(@Param("reference") String reference, @Param("email") String email);
}
