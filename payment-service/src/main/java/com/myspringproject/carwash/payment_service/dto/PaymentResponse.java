package com.myspringproject.carwash.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.myspringproject.carwash.payment_service.entity.Payment.PaymentStatus;

public record PaymentResponse(
        UUID paymentId,
        UUID customerId,
        UUID washerId,
        LocalDate date,
        LocalTime slotTime,
        BigDecimal amount,
        Long amountSubunits,
        String currency,
        PaymentStatus status,
        String razorpayOrderId,
        String razorpayPaymentId,
        UUID bookingId,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
