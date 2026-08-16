package com.myspringproject.carwash.booking_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record LockedSlotQuoteResponse(
        UUID washerId,
        LocalDate date,
        LocalTime slotTime,
        BigDecimal amount,
        String currency) {
}
