package com.myspringproject.carwash.auth_service.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final long INACTIVITY_EXPIRY_SECONDS = 20 * 60l;
    private static final String SESSION_PREFIX = "session:";

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
}