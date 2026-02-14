package com.myspringproject.carwash.washer_service.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myspringproject.carwash.washer_service.entity.Rating;
import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingCacheService {

    private StringRedisTemplate redisTemplate;

    public RatingCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private ObjectMapper objectMapper;

    private static final String PREFIX = "ratings:washer:";

    public void cacheRatings(String washerId, List<Rating> ratings) {
        try {
            String key = PREFIX + washerId;
            String value = objectMapper.writeValueAsString(ratings);
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Rating> getRatingsFromCache(String washerId) {
        try {
            String key = PREFIX + washerId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, new TypeReference<List<Rating>>() {});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
