package com.myspringproject.carwash.payment_service.exception;

public class PaymentSlotUnavailableException extends RuntimeException {

    public PaymentSlotUnavailableException(String message) {
        super(message);
    }

    public PaymentSlotUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
