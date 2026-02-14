package com.myspringproject.carwash.booking_service.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.myspringproject.carwash.booking_service.entity.Booking;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    
}
