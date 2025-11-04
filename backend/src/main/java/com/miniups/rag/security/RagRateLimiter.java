package com.miniups.rag.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagRateLimiter {


    private static final Logger log = LoggerFactory.getLogger(RagRateLimiter.class);
    private final Cache<String, UsageBucket> cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build();

    public boolean tryConsume(String userId, int limit) {
        if (userId == null || limit <= 0) {
            return true;
        }
        UsageBucket bucket = cache.get(userId, key -> new UsageBucket());
        synchronized (bucket) {
            Instant now = Instant.now();
            if (now.isAfter(bucket.windowStart.plus(Duration.ofHours(1)))) {
                bucket.windowStart = now;
                bucket.count = 0;
            }
            if (bucket.count >= limit) {
                log.debug("RAG rate limit hit for user {}", userId);
                return false;
            }
            bucket.count++;
            return true;
        }
    }

    private static class UsageBucket {
        private Instant windowStart = Instant.now();
        private int count = 0;
    }
}
