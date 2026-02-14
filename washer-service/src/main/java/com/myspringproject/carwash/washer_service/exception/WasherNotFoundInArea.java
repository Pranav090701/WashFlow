package com.myspringproject.carwash.washer_service.exception;

public class WasherNotFoundInArea extends RuntimeException {
    public WasherNotFoundInArea(String message) {
        super(message);
    }

    public WasherNotFoundInArea(String message, Throwable cause) {
        super(message, cause);
    }
    
}
