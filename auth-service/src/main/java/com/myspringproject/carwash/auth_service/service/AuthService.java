package com.myspringproject.carwash.auth_service.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.myspringproject.carwash.auth_service.repository.UserRepository;
import com.myspringproject.carwash.auth_service.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.myspringproject.carwash.auth_service.entity.Role;
import com.myspringproject.carwash.auth_service.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisService redisService;

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public ResponseEntity<?> register(String email, String password, String role) {
        logger.info("Entered register user for email {} role {}",email,role);
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.valueOf(role));

        userRepository.save(user);
        logger.info("Completed User Registration for email {} role {}",email,role);
        return ResponseEntity.ok("User registered");
    }

    public ResponseEntity<?> login(String email, String password) {
        logger.info("Entered login for email {} ",email);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid credentials");

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        logger.info("User found {}", user);
        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail(), user.getRole().name());
        redisService.storeSession(user.getId().toString(), token);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> validate(String token) {
        try {
            DecodedJWT decodedJWT = jwtUtil.decodeToken(token);
            String userId = decodedJWT.getSubject();

            if (!redisService.isSessionActive(userId, token)) {
                return ResponseEntity.status(401).body("Session expired");
            }

            return ResponseEntity.ok("Token is valid");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token");
        }
    }

    public ResponseEntity<?> logout(String token) {
        String userId = jwtUtil.extractUserId(token);
        redisService.deleteSession(userId);
        return ResponseEntity.ok("Logged out");
    }
}
