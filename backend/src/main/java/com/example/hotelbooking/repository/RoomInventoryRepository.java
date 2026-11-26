package com.example.hotelbooking.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.hotelbooking.domain.InventoryId;
import com.example.hotelbooking.domain.RoomInventory;

public interface RoomInventoryRepository extends JpaRepository<RoomInventory, InventoryId> {

    /** Inventory rows for nights in [from, to) at one hotel. */
    @Query("select i from RoomInventory i "
            + "where i.id.hotelId = :hotelId and i.id.stayDate >= :from and i.id.stayDate < :to")
    List<RoomInventory> findForHotel(@Param("hotelId") Long hotelId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    /** Inventory rows for nights in [from, to) across all hotels. */
    @Query("select i from RoomInventory i where i.id.stayDate >= :from and i.id.stayDate < :to")
    List<RoomInventory> findAllInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
