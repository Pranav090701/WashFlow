package com.myspringproject.carwash.booking_service.exception;

public class SlotNotFoundException extends RuntimeException {
    public SlotNotFoundException(String message) {
        super(message);
    }

    public SlotNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
