package com.miniups.shortlink.bloom;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.miniups.shortlink.config.ShortLinkProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RedisBloomFilterService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ShortLinkProperties.Bloom bloomProps;
    private final Counter bloomCheckHitCounter;
    private final Counter bloomCheckMissCounter;
    private final Counter bloomAddCounter;

    public RedisBloomFilterService(StringRedisTemplate stringRedisTemplate,
                                   ShortLinkProperties properties,
                                   MeterRegistry meterRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.bloomProps = properties.getBloom();
        this.bloomCheckHitCounter = meterRegistry.counter("shortlink.bloom.check", "result", "hit");
        this.bloomCheckMissCounter = meterRegistry.counter("shortlink.bloom.check", "result", "miss");
        this.bloomAddCounter = meterRegistry.counter("shortlink.bloom.additions");
    }

    public void add(String value) {
        for (Integer seed : bloomProps.getHashSeeds()) {
            long index = hash(value, seed) % bloomProps.getBitSize();
            stringRedisTemplate.opsForValue().setBit(bloomProps.getRedisKey(), index, true);
        }
        bloomAddCounter.increment();
    }

    public boolean mightContain(String value) {
        for (Integer seed : bloomProps.getHashSeeds()) {
            long index = hash(value, seed) % bloomProps.getBitSize();
            Boolean bit = stringRedisTemplate.opsForValue().getBit(bloomProps.getRedisKey(), index);
            if (bit == null || !bit) {
                bloomCheckMissCounter.increment();
                return false;
            }
        }
        bloomCheckHitCounter.increment();
        return true;
    }

    private long hash(String value, int seed) {
        HashFunction function = Hashing.murmur3_32_fixed(seed);
        int hash = function.hashString(value, StandardCharsets.UTF_8).asInt();
        return Integer.toUnsignedLong(hash);
    }
}
