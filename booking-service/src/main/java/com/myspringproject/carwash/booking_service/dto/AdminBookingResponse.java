package com.myspringproject.carwash.booking_service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.myspringproject.carwash.booking_service.entity.Booking.BookingStatus;

public record AdminBookingResponse(
        UUID bookingId,
        UUID slotId,
        UUID customerId,
        UUID washerId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BookingStatus status,
        double price,
        LocalDateTime paymentTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
