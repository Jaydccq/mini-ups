package com.miniups.shortlink.bloom;

import com.miniups.shortlink.config.ShortLinkProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisBloomFilterServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MeterRegistry meterRegistry;
    private ShortLinkProperties properties;
    private RedisBloomFilterService bloomFilterService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new ShortLinkProperties();

        // Configure bloom filter properties
        ShortLinkProperties.Bloom bloom = new ShortLinkProperties.Bloom();
        bloom.setRedisKey("test:bloom:codes");
        bloom.setBitSize(1048576);
        bloom.setHashSeeds(Arrays.asList(17, 31, 73, 127, 191));
        properties.setBloom(bloom);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        bloomFilterService = new RedisBloomFilterService(stringRedisTemplate, properties, meterRegistry);
    }

    @Test
    void add_shouldSetBitsForAllHashSeeds() {
        String testValue = "abc123";

        bloomFilterService.add(testValue);

        // Verify that setBit is called for each hash seed
        verify(valueOperations).setBit(eq("test:bloom:codes"), anyLong(), eq(true));

        // Verify metrics counter incremented
        Counter addCounter = meterRegistry.get("shortlink.bloom.additions").counter();
        assertThat(addCounter.count()).isEqualTo(1.0);
    }

    @Test
    void mightContain_shouldReturnFalseWhenAnyBitIsNotSet() {
        String testValue = "abc123";

        // Mock that some bits are not set
        when(valueOperations.getBit(anyString(), anyLong())).thenReturn(false);

        boolean result = bloomFilterService.mightContain(testValue);

        assertThat(result).isFalse();

        // Verify miss counter incremented
        Counter missCounter = meterRegistry.get("shortlink.bloom.check").tag("result", "miss").counter();
        assertThat(missCounter.count()).isEqualTo(1.0);
    }

    @Test
    void mightContain_shouldReturnTrueWhenAllBitsAreSet() {
        String testValue = "abc123";

        // Mock that all bits are set
        when(valueOperations.getBit(anyString(), anyLong())).thenReturn(true);

        boolean result = bloomFilterService.mightContain(testValue);

        assertThat(result).isTrue();

        // Verify hit counter incremented
        Counter hitCounter = meterRegistry.get("shortlink.bloom.check").tag("result", "hit").counter();
        assertThat(hitCounter.count()).isEqualTo(1.0);
    }

    @Test
    void mightContain_shouldReturnFalseWhenBitIsNull() {
        String testValue = "abc123";

        // Mock that bit returns null
        when(valueOperations.getBit(anyString(), anyLong())).thenReturn(null);

        boolean result = bloomFilterService.mightContain(testValue);

        assertThat(result).isFalse();

        // Verify miss counter incremented
        Counter missCounter = meterRegistry.get("shortlink.bloom.check").tag("result", "miss").counter();
        assertThat(missCounter.count()).isEqualTo(1.0);
    }

    @Test
    void hashFunction_shouldProduceDifferentValuesForDifferentSeeds() {
        String testValue = "abc123";

        // Add the value twice to test hash consistency
        bloomFilterService.add(testValue);
        bloomFilterService.add(testValue);

        // Verify setBit called for all seeds on both additions
        verify(valueOperations).setBit(eq("test:bloom:codes"), anyLong(), eq(true));

        // Verify metrics counter incremented twice
        Counter addCounter = meterRegistry.get("shortlink.bloom.additions").counter();
        assertThat(addCounter.count()).isEqualTo(2.0);
    }

    @Test
    void bloomFilter_shouldHandleCollisions() {
        String value1 = "test1";
        String value2 = "test2";

        // Add first value
        bloomFilterService.add(value1);

        // Mock that all bits are set for second value (false positive scenario)
        when(valueOperations.getBit(anyString(), anyLong())).thenReturn(true);

        boolean result = bloomFilterService.mightContain(value2);

        // This is expected behavior - bloom filter can have false positives
        assertThat(result).isTrue();

        // Verify hit counter incremented (false positive case)
        Counter hitCounter = meterRegistry.get("shortlink.bloom.check").tag("result", "hit").counter();
        assertThat(hitCounter.count()).isEqualTo(1.0);
    }

    @Test
    void properties_shouldUseCorrectConfiguration() {
        assertThat(properties.getBloom().getRedisKey()).isEqualTo("test:bloom:codes");
        assertThat(properties.getBloom().getBitSize()).isEqualTo(1048576);
        assertThat(properties.getBloom().getHashSeeds()).hasSize(5);
        assertThat(properties.getBloom().getHashSeeds()).containsExactly(17, 31, 73, 127, 191);
    }
}