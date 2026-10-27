package com.example.shortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiter {
    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) { this.redis = redis; }

    public boolean allow(String ipAddress) {
        long minute = Instant.now().getEpochSecond() / 60;
        String key = "rate:" + ipAddress + ":" + minute;
        Long requests = redis.opsForValue().increment(key);
        if (requests != null && requests == 1) redis.expire(key, 60, TimeUnit.SECONDS);
        return requests != null && requests <= 20;
    }
}
