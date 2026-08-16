package com.myspringproject.carwash.payment_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class BookingConfirmationException extends RuntimeException {

    public BookingConfirmationException(String message, Throwable cause) {
        super(message, cause);
    }
}
