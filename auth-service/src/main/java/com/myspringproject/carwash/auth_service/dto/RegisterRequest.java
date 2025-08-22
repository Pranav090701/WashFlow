package com.myspringproject.carwash.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object for registering a new user")
public class RegisterRequest {

    @Schema(description = "User's email address", example = "user@example.com")
    private String email;
    @Schema(description = "User's password", example = "password123")
    private String password;
    @Schema(description = "Role of the user (e.g., CUSTOMER, ADMIN, WASHER)", example = "CUSTOMER")
    private String role;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}