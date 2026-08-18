package com.myspringproject.carwash.booking_service.cache;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SlotCacheService {

    private static final String AVAILABLE_PREFIX = "slot:available:";  // slot:available:<washerId>:<date>
    private static final String LOCK_PREFIX = "slot:lock:";            // slot:lock:<washerId>:<date>:<startTime>

    
    private StringRedisTemplate redisTemplate;

    public SlotCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add slot to available cache
     */
    public void addAvailableSlot(String washerId, String date, String slotTime) {
        String key = AVAILABLE_PREFIX + washerId + ":" + date;
        redisTemplate.opsForSet().add(key, slotTime);
        Duration ttl = ttlUntilEndOfSlotDate(date);
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.expire(key, ttl);
        } else {
            redisTemplate.delete(key);
        }
    }

    /**
     * Check and remove slot from available, then lock it atomically
     */
    public boolean lockSlot(String washerId, String date, String slotTime, UUID customerId) {
    String availableKey = AVAILABLE_PREFIX + washerId + ":" + date;
    Long removed = redisTemplate.opsForSet().remove(availableKey, slotTime);

    if (removed != null && removed > 0) {
        String lockKey = LOCK_PREFIX + washerId + ":" + date + ":" + slotTime;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // Store the customer ID instead of "LOCKED"
        Boolean success = ops.setIfAbsent(lockKey, customerId.toString(), Duration.ofMinutes(10));
        return Boolean.TRUE.equals(success);
    }
    return false;
}

/**
 * Returns the customerId who currently holds the lock for this slot.
 * If no lock exists, returns null.
 */
public UUID getLockOwner(String washerId, String date, String slotTime) {
    String lockKey = LOCK_PREFIX + washerId + ":" + date + ":" + slotTime;
    ValueOperations<String, String> ops = redisTemplate.opsForValue();

    String customerIdStr = ops.get(lockKey);
    return (customerIdStr != null) ? UUID.fromString(customerIdStr) : null;
}

    /**
     * Check if slot is locked
     */
    public boolean isSlotLocked(String washerId, String date, String slotTime) {
        String lockKey = LOCK_PREFIX + washerId + ":" + date + ":" + slotTime;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * Remove from lock (after booking success or failure)
     */
    public void removeLock(String washerId, String date, String slotTime) {
        String lockKey = LOCK_PREFIX + washerId + ":" + date + ":" + slotTime;
        redisTemplate.delete(lockKey);
    }

    /**
     * Get all available slots for a washer on a date
     */
    public Set<String> getAvailableSlotsTiming(String washerId, String date) {
        String key = AVAILABLE_PREFIX + washerId + ":" + date;
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Remove slot from available set (used after successful booking)
     */
    public void removeFromAvailableSlots(String washerId, String date, String slotTime) {
        String key = AVAILABLE_PREFIX + washerId + ":" + date;
        redisTemplate.opsForSet().remove(key, slotTime);
    }

    public void removeAvailableSlotsBefore(LocalDate cutoffDate) {
        List<String> expiredKeys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(AVAILABLE_PREFIX + "*")
                .count(100)
                .build();

        try (Cursor<String> keys = redisTemplate.scan(options)) {
            while (keys.hasNext()) {
                String key = keys.next();
                LocalDate slotDate = parseDateFromAvailableKey(key);
                if (slotDate != null && slotDate.isBefore(cutoffDate)) {
                    expiredKeys.add(key);
                }
            }
        } catch (RuntimeException ex) {
            return;
        }

        if (!expiredKeys.isEmpty()) {
            redisTemplate.delete(expiredKeys);
        }
    }

    private Duration ttlUntilEndOfSlotDate(String date) {
        LocalDate slotDate = LocalDate.parse(date);
        LocalDateTime expiryTime = slotDate.plusDays(1).atStartOfDay();
        return Duration.between(LocalDateTime.now(), expiryTime);
    }

    private LocalDate parseDateFromAvailableKey(String key) {
        int lastSeparator = key.lastIndexOf(':');
        if (lastSeparator < 0 || lastSeparator == key.length() - 1) {
            return null;
        }

        try {
            return LocalDate.parse(key.substring(lastSeparator + 1));
        } catch (RuntimeException ex) {
            return null;
        }
    }
} 
