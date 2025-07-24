package com.myspringproject.carwash.auth_service.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
    
}
