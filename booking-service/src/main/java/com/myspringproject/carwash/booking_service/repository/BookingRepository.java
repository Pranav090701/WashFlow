package com.myspringproject.carwash.booking_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.myspringproject.carwash.booking_service.entity.Booking;
import com.myspringproject.carwash.booking_service.entity.Booking.BookingStatus;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findBySlotIdAndUserId(UUID slotId, UUID userId);

    List<Booking> findByUserId(UUID userId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findBySlotIdIn(Collection<UUID> slotIds);
}
