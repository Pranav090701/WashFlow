package com.myspringproject.carwash.auth_service.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.myspringproject.carwash.auth_service.repository.UserRepository;
import com.myspringproject.carwash.auth_service.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.myspringproject.carwash.auth_service.event.VerificationEmailEventPublisher;
import com.myspringproject.carwash.auth_service.entity.Role;
import com.myspringproject.carwash.auth_service.entity.User;
import com.myspringproject.carwash.auth_service.exception.InvalidRoleException;
import com.myspringproject.carwash.auth_service.exception.DuplicateEmailException;
import com.myspringproject.carwash.auth_service.exception.EmailNotVerifiedException;
import com.myspringproject.carwash.auth_service.exception.InvalidCredentialsException;
import com.myspringproject.carwash.auth_service.exception.TokenExpiredException;
import com.myspringproject.carwash.auth_service.exception.TokenInvalidException;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final VerificationEmailEventPublisher verificationEmailEventPublisher;
    private final boolean emailVerificationRequired;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            RedisService redisService,
            EmailVerificationTokenService emailVerificationTokenService,
            VerificationEmailEventPublisher verificationEmailEventPublisher,
            @Value("${auth.email-verification.required:false}") boolean emailVerificationRequired) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.verificationEmailEventPublisher = verificationEmailEventPublisher;
        this.emailVerificationRequired = emailVerificationRequired;
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

        User savedUser = userRepository.save(user);

        EmailVerificationTokenService.VerificationLink verificationLink =
                emailVerificationTokenService.createVerificationLink(savedUser.getId());
        verificationEmailEventPublisher.publish(savedUser, verificationLink.link(), verificationLink.expiresInHours());

        logger.info("Completed User Registration for email {} role {}. Verification email event created.", email, role);
        return savedUser;
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
        if (emailVerificationRequired && !user.isVerified()) {
            throw new EmailNotVerifiedException("Email verification is required before login");
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
    public void verifyToken(String token) {
        UUID userId = emailVerificationTokenService.consumeToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TokenInvalidException("Verification token does not belong to a valid user"));
        user.setVerified(true);
        userRepository.save(user);
        logger.info("Verified user {} with Id {}", user.getEmail(), user.getId());
    }

    public void resendVerification(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.info("Ignoring verification resend for unknown email {}", email);
            return;
        }

        User user = userOpt.get();
        if (user.isVerified()) {
            logger.info("Ignoring verification resend because user {} is already verified", user.getId());
            return;
        }

        EmailVerificationTokenService.VerificationLink verificationLink =
                emailVerificationTokenService.createVerificationLink(user.getId());
        verificationEmailEventPublisher.publish(user, verificationLink.link(), verificationLink.expiresInHours());
        logger.info("Queued verification resend for user {}", user.getId());
    }
}
