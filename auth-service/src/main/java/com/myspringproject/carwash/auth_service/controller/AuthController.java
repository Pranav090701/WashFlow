package com.myspringproject.carwash.auth_service.controller;

import com.myspringproject.carwash.auth_service.dto.LoginRequest;
import com.myspringproject.carwash.auth_service.dto.RegisterRequest;
import com.myspringproject.carwash.auth_service.dto.ResendVerificationRequest;
import com.myspringproject.carwash.auth_service.entity.User;
import com.myspringproject.carwash.auth_service.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication APIs", description = "Endpoints for user registration, login, token validation, and logout")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructs an instance of {@code AuthController} with the specified
     * {@link AuthService}.
     *
     * @param authService the authentication service to be used by this controller
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     * 
     * @param request RegisterRequest containing email, password, and role.
     * @return The created User object.
     */
    @Operation(summary = "Register a new user", description = "Creates a new user account with email, password, and role.")
    @ApiResponse(responseCode = "200", description = "User registered successfully")
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request.getEmail(), request.getPassword(), request.getRole());
        return ResponseEntity.ok(user);
    }

    /**
     * Login and get JWT token.
     * 
     * @param request LoginRequest containing email and password.
     * @return JWT token as a string.
     */
    @Operation(summary = "Login user", description = "Authenticates the user and returns a JWT token.")
    @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(token);
    }

    /**
     * Validate a JWT token.
     * 
     * @param authHeader Authorization header with Bearer token.
     * @return Success message if token is valid.
     */
    @Operation(summary = "Validate JWT token", description = "Checks if the provided JWT token is valid.")
    @ApiResponse(responseCode = "200", description = "Token is valid")
    @PostMapping("/validate")
    public ResponseEntity<String> validate(@RequestHeader(value = "Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.validate(token);
        return ResponseEntity.ok("Token is valid");
    }

    /**
     * Logout user (invalidate token).
     * 
     * @param authHeader Authorization header with Bearer token.
     * @return Success message after logout.
     */
    @Operation(summary = "Logout user", description = "Invalidates the JWT token for the user.")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.ok("Logged out successfully");
    }

    /**
     * Verify a JWT token via query parameter.
     * 
     * @param token JWT token as a query parameter.
     * @return Success message if token is verified.
     */
    @Operation(summary = "Verify JWT token", description = "Verifies the JWT token passed as a query parameter.")
    @ApiResponse(responseCode = "200", description = "Token verified successfully")
    @GetMapping("/verify")
    public ResponseEntity<String> verifyToken(@RequestParam String token) {
        authService.verifyToken(token);
        return ResponseEntity.ok("Verified successfully!");
    }

    @Operation(summary = "Resend email verification", description = "Creates a new email verification token and queues a verification email.")
    @ApiResponse(responseCode = "200", description = "Verification email queued when the account exists and is not already verified")
    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.getEmail());
        return ResponseEntity.ok("If the account exists and is unverified, a verification email will be sent.");
    }
}
