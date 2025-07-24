package com.myspringproject.carwash.auth_service.controller;

import com.myspringproject.carwash.auth_service.dto.LoginRequest;
import com.myspringproject.carwash.auth_service.dto.RegisterRequest;
import com.myspringproject.carwash.auth_service.entity.User;
import com.myspringproject.carwash.auth_service.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request.getEmail(), request.getPassword(), request.getRole());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.validate(token);
        return ResponseEntity.ok("Token is valid");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyToken(@RequestParam String token) {
        authService.verifyToken(token);
        return ResponseEntity.ok("Verified successfully!");
    }
}
