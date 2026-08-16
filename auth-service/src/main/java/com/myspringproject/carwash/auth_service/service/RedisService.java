package com.myspringproject.carwash.auth_service.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final long INACTIVITY_EXPIRY_SECONDS = 20 * 60l;
    private static final String SESSION_PREFIX = "session:";
    private static final String EMAIL_VERIFICATION_PREFIX = "email_verification:";

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void storeSession(String userId, String token) {
        redisTemplate.opsForValue().set(SESSION_PREFIX + userId, token, INACTIVITY_EXPIRY_SECONDS, TimeUnit.SECONDS);
    }

    public boolean isSessionActive(String userId, String token) {
        String storedToken = redisTemplate.opsForValue().get(SESSION_PREFIX + userId);
        if (storedToken == null || !storedToken.equals(token)) return false;
        redisTemplate.expire(SESSION_PREFIX + userId, INACTIVITY_EXPIRY_SECONDS, TimeUnit.SECONDS);
        return true;
    }

    public void deleteSession(String userId) {
        redisTemplate.delete(SESSION_PREFIX + userId);
    }

    public void storeEmailVerificationToken(String tokenHash, String userId, Duration ttl) {
        redisTemplate.opsForValue().set(EMAIL_VERIFICATION_PREFIX + tokenHash, userId, ttl);
    }

    public Optional<String> getUserIdForEmailVerificationToken(String tokenHash) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(EMAIL_VERIFICATION_PREFIX + tokenHash));
    }

    public void deleteEmailVerificationToken(String tokenHash) {
        redisTemplate.delete(EMAIL_VERIFICATION_PREFIX + tokenHash);
    }
}
