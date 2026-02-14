package com.myspringproject.carwash.booking_service.exception;

public class SlotLockExpiredException extends RuntimeException {
    public SlotLockExpiredException(String message) {
        super(message);
    }

    public SlotLockExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
