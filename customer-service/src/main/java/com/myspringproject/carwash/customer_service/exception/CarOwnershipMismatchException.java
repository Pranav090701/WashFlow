package com.myspringproject.carwash.customer_service.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CarOwnershipMismatchException extends RuntimeException {
    public CarOwnershipMismatchException(UUID carId, UUID userId) {
        super("User ID " + userId + " is not authorized to modify Car ID " + carId);
    }
}