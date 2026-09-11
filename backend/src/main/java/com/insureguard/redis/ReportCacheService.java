package com.insureguard.redis;

import com.insureguard.dto.AnalysisResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Explicit cache-aside implementation for analysis results, on top of the
 * declarative @Cacheable annotations. Kept simple and easy to explain:
 * check Redis -> on miss, caller fetches from MySQL -> write back to Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportCacheService {

    private static final String KEY_PREFIX = "analysis-result:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.cache.result-ttl-seconds}")
    private long ttlSeconds;

    public AnalysisResultResponse get(Long reportId) {
        Object cached = redisTemplate.opsForValue().get(KEY_PREFIX + reportId);
        if (cached instanceof AnalysisResultResponse response) {
            log.debug("Cache HIT for report {}", reportId);
            return response;
        }
        log.debug("Cache MISS for report {}", reportId);
        return null;
    }

    public void put(Long reportId, AnalysisResultResponse response) {
        redisTemplate.opsForValue().set(KEY_PREFIX + reportId, response, Duration.ofSeconds(ttlSeconds));
    }

    public void evict(Long reportId) {
        redisTemplate.delete(KEY_PREFIX + reportId);
    }
}
