package com.myspringproject.carwash.payment_service.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingRequestDto(
        UUID washerId,
        LocalTime slotTime,
        LocalDate date) {
}
