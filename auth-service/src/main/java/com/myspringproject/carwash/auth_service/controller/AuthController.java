package com.myspringproject.carwash.auth_service.controller;

import com.myspringproject.carwash.auth_service.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        return authService.register(body.get("email"), body.get("password"), body.get("role"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("email"), body.get("password"));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return authService.validate(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return authService.logout(token);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyToken(@RequestParam String token) {
        boolean isValid = authService.verifyToken(token);
        if(isValid){
            return ResponseEntity.ok("Verified successfully!");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }
    }
}
