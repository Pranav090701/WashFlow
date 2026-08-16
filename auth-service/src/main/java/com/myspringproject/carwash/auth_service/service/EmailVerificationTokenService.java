package com.myspringproject.carwash.auth_service.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.auth_service.exception.TokenInvalidException;

@Service
public class EmailVerificationTokenService {

    private final RedisService redisService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long ttlHours;
    private final String verificationUrlTemplate;

    public EmailVerificationTokenService(
            RedisService redisService,
            @Value("${auth.email-verification.ttl-hours}") long ttlHours,
            @Value("${auth.email-verification.url-template}") String verificationUrlTemplate) {
        this.redisService = redisService;
        this.ttlHours = ttlHours;
        this.verificationUrlTemplate = verificationUrlTemplate;
    }

    public VerificationLink createVerificationLink(UUID userId) {
        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);
        redisService.storeEmailVerificationToken(tokenHash, userId.toString(), Duration.ofHours(ttlHours));
        return new VerificationLink(rawToken, buildVerificationLink(rawToken), ttlHours);
    }

    public UUID consumeToken(String rawToken) {
        String tokenHash = hash(rawToken);
        String userId = redisService.getUserIdForEmailVerificationToken(tokenHash)
                .orElseThrow(() -> new TokenInvalidException("Verification token is invalid or expired"));
        redisService.deleteEmailVerificationToken(tokenHash);
        return UUID.fromString(userId);
    }

    private String generateRawToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String buildVerificationLink(String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        if (verificationUrlTemplate.contains("{token}")) {
            return verificationUrlTemplate.replace("{token}", encodedToken);
        }
        return String.format(verificationUrlTemplate, encodedToken);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    public record VerificationLink(String rawToken, String link, long expiresInHours) {
    }
}
