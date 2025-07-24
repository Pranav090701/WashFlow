package com.myspringproject.carwash.auth_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.myspringproject.carwash.auth_service.repository.TokenRepository;
import com.myspringproject.carwash.auth_service.repository.UserRepository;
import com.myspringproject.carwash.auth_service.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.myspringproject.carwash.auth_service.entity.Role;
import com.myspringproject.carwash.auth_service.entity.User;
import com.myspringproject.carwash.auth_service.entity.VerificationToken;
import com.myspringproject.carwash.auth_service.exception.InvalidRoleException;
import com.myspringproject.carwash.auth_service.exception.DuplicateEmailException;
import com.myspringproject.carwash.auth_service.exception.InvalidCredentialsException;
import com.myspringproject.carwash.auth_service.exception.TokenExpiredException;
import com.myspringproject.carwash.auth_service.exception.TokenInvalidException;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final TokenRepository tokenRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            RedisService redisService,
            TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
        this.tokenRepository = tokenRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    /**
     * Registers a new user with the provided email, password, and role.
     *
     * @param email    the user's email address
     * @param password the user's password
     * @param role     the user's role (CUSTOMER or WASHER)
     * @return the registered User entity
     * @throws InvalidRoleException    if the role is invalid
     * @throws DuplicateEmailException if the email already exists
     */
    public User register(String email, String password, String role) {
        logger.info("Entered register user for email {} role {}", email, role);
        role = role.toUpperCase();

        if (!role.equals(Role.CUSTOMER.name()) && !role.equals(Role.WASHER.name())) {
            throw new InvalidRoleException("Invalid role: " + role);
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.valueOf(role));

        userRepository.save(user);
        logger.info("Completed User Registration for email {} role {}", email, role);
        return user;
    }

    /**
     * Authenticates a user and generates a JWT token if credentials are valid.
     *
     * @param email    the user's email address
     * @param password the user's password
     * @return a JWT token for the authenticated session
     * @throws InvalidCredentialsException if credentials are invalid
     */
    public String login(String email, String password) {
        logger.info("Entered login for email {} ", email);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
            throw new InvalidCredentialsException("Invalid credentials");

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        logger.info("User found {}", user);
        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail(), user.getRole().name());
        redisService.storeSession(user.getId().toString(), token);

        return token;
    }

    /**
     * Validates the provided JWT token and checks if the session is active.
     *
     * @param token the JWT token to validate
     * @throws TokenExpiredException if the session is expired
     * @throws TokenInvalidException if the token is invalid
     */
    public void validate(String token) {
        try {
            DecodedJWT decodedJWT = jwtUtil.decodeToken(token);
            String userId = decodedJWT.getSubject();

            if (!redisService.isSessionActive(userId, token)) {
                throw new TokenExpiredException("Session expired");
            }
        } catch (Exception e) {
            throw new TokenInvalidException("Invalid token");
        }
    }

    /**
     * Logs out the user by validating and invalidating the provided JWT token.
     *
     * @param token the JWT token to invalidate
     * @throws TokenInvalidException if the token is invalid
     * @throws TokenExpiredException if the session is already expired
     */
    public void logout(String token) {
        try {
            // Validate token structure and signature
            DecodedJWT decodedJWT = jwtUtil.decodeToken(token);
            String userId = decodedJWT.getSubject();

            // Check if session is active
            if (!redisService.isSessionActive(userId, token)) {
                throw new TokenExpiredException("Session already expired or invalid");
            }

            // Invalidate session
            redisService.deleteSession(userId);
            logger.info("User with ID {} logged out successfully.", userId);
        } catch (TokenExpiredException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenInvalidException("Invalid token");
        }
    }

    /**
     * Verifies the provided verification token and marks the user as verified.
     *
     * @param token the verification token to verify
     * @throws TokenInvalidException if the token is invalid
     * @throws TokenExpiredException if the token has expired
     */
    public void verifyToken(@RequestParam String token) {
        Optional<VerificationToken> optionalToken = tokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            logger.warn("Invalid verification token: {}", token);
            throw new TokenInvalidException("Verification token is invalid");
        }

        VerificationToken verificationToken = optionalToken.get();

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Expired verification token for user: {}", verificationToken.getUser().getEmail());
            throw new TokenExpiredException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setVerified(true);
        userRepository.save(user);

        logger.info("Verified user {} with Id {}", user.getEmail(), user.getId());

        tokenRepository.delete(verificationToken);
    }
}
