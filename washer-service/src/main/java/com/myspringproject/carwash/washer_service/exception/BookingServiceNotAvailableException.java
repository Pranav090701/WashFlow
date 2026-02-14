package com.myspringproject.carwash.washer_service.exception;

public class BookingServiceNotAvailableException extends RuntimeException {
    public BookingServiceNotAvailableException(String message) {
        super(message);
    }

    public BookingServiceNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}