package com.myspringproject.carwash.washer_service.exception;

public class ProfileNotFoundException  extends RuntimeException {
    public ProfileNotFoundException(String message) {
        super(message);
    }
}
