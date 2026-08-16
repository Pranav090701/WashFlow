package com.myspringproject.carwash.booking_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.myspringproject.carwash.booking_service.entity.Slot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Slot entity.
 * Provides basic CRUD and custom DB operations for slot data.
 */
@Repository
public interface SlotRepository extends JpaRepository<Slot, UUID> {

    List<Slot> findByWasherIdAndDate(UUID washerId, LocalDate date);

    List<Slot> findByDate(LocalDate date);

    List<Slot> findByWasherId(UUID washerId);

    Slot findByWasherIdAndDateAndStartTime(UUID washerId, LocalDate date, LocalTime startTime);
}

