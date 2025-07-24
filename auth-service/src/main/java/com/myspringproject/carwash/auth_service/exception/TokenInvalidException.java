package com.myspringproject.carwash.auth_service.exception;

public class TokenInvalidException extends RuntimeException {
    public TokenInvalidException(String message) {
        super(message);
    }
}
