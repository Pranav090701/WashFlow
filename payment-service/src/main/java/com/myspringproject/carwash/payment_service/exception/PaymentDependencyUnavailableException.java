package com.myspringproject.carwash.payment_service.exception;

public class PaymentDependencyUnavailableException extends RuntimeException {

    public PaymentDependencyUnavailableException(String message) {
        super(message);
    }

    public PaymentDependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
