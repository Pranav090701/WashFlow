package com.myspringproject.carwash.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisServiceTest {
    
    @Autowired
    RedisTemplate<String,String> redisTemplate;

    @Test
    public void testRedis(){
        redisTemplate.opsForValue().set("name", "carwash");
        String value = redisTemplate.opsForValue().get("name");
        System.out.println(value);
        assertEquals("carwash",value);
    }

    
}
