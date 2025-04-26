package com.myspringproject.carwash.auth_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private static final long INACTIVITY_EXPIRY_SECONDS = 20 * 60;

    public void storeSession(String userId, String token) {
        redisTemplate.opsForValue().set("session:" + userId, token, INACTIVITY_EXPIRY_SECONDS, TimeUnit.SECONDS);
    }

    public boolean isSessionActive(String userId, String token) {
        String storedToken = redisTemplate.opsForValue().get("session:" + userId);
        if (storedToken == null || !storedToken.equals(token)) return false;
        redisTemplate.expire("session:" + userId, INACTIVITY_EXPIRY_SECONDS, TimeUnit.SECONDS);
        return true;
    }

    public void deleteSession(String userId) {
        redisTemplate.delete("session:" + userId);
    }
}