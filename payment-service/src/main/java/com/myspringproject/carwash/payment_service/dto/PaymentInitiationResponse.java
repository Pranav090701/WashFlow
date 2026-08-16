package com.myspringproject.carwash.payment_service.dto;

import java.util.UUID;

import com.myspringproject.carwash.payment_service.entity.Payment.PaymentStatus;

public record PaymentInitiationResponse(
        UUID paymentId,
        String razorpayOrderId,
        Long amountSubunits,
        String currency,
        String razorpayKeyId,
        PaymentStatus status) {
}
