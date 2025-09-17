package com.miniups.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);
    private static final String CACHE_PREFIX = "auth:sms:";

    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.sms.code-length:6}")
    private int codeLength;

    @Value("${auth.sms.code-ttl-seconds:300}")
    private long codeTtlSeconds;

    public SmsCodeService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String generateAndStoreCode(String phoneNumber) {
        String code = generateNumericCode(codeLength);
        String cacheKey = cacheKey(phoneNumber);
        stringRedisTemplate.opsForValue().set(cacheKey, code, Duration.ofSeconds(codeTtlSeconds));
        log.info("Generated login SMS code for phone {}", obfuscate(phoneNumber));
        return code;
    }

    public boolean verifyCode(String phoneNumber, String code) {
        String cacheKey = cacheKey(phoneNumber);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        boolean matches = cached != null && cached.equals(code);
        if (matches) {
            stringRedisTemplate.delete(cacheKey);
        }
        return matches;
    }

    private String cacheKey(String phoneNumber) {
        return CACHE_PREFIX + phoneNumber;
    }

    private String generateNumericCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }

    private String obfuscate(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        String tail = phoneNumber.substring(phoneNumber.length() - 4);
        return "***" + tail;
    }
}

