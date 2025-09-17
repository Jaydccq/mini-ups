package com.miniups.shortlink.service.impl;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.miniups.shortlink.bloom.RedisBloomFilterService;
import com.miniups.shortlink.config.ShortLinkProperties;
import com.miniups.shortlink.dto.ShortLinkCreateRequest;
import com.miniups.shortlink.dto.ShortLinkPageResponse;
import com.miniups.shortlink.dto.ShortLinkResponse;
import com.miniups.shortlink.dto.ShortLinkUpdateRequest;
import com.miniups.shortlink.exception.ShortLinkConflictException;
import com.miniups.shortlink.exception.ShortLinkNotFoundException;
import com.miniups.shortlink.exception.ShortLinkRateLimitException;
import com.miniups.shortlink.exception.ShortLinkServiceException;
import com.miniups.shortlink.model.ShortLinkRecord;
import com.miniups.shortlink.model.ShortLinkRouteRecord;
import com.miniups.shortlink.repository.ShortLinkRepository;
import com.miniups.shortlink.repository.ShortLinkRouteRepository;
import com.miniups.shortlink.stream.ShortLinkStreamPublisher;
import com.miniups.shortlink.util.ShortLinkCodeGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceImplTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ShortLinkRouteRepository shortLinkRouteRepository;

    @Mock
    private ShortLinkCodeGenerator codeGenerator;

    @Mock
    private RedisBloomFilterService bloomFilterService;

    @Mock
    private ShortLinkStreamPublisher streamPublisher;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RReadWriteLock readWriteLock;

    @Mock
    private RLock writeLock;

    @Mock
    private RLock readLock;

    @Mock
    private HttpServletRequest httpRequest;

    private MeterRegistry meterRegistry;
    private ShortLinkProperties properties;
    private ShortLinkServiceImpl shortLinkService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new ShortLinkProperties();

        // Configure sharding properties
        ShortLinkProperties.Sharding sharding = new ShortLinkProperties.Sharding();
        sharding.setTableWeights("short_links_0:4,short_links_1:3,short_links_2:2,short_links_3:1");
        properties.setSharding(sharding);

        // Mock Redisson locks
        when(redissonClient.getReadWriteLock(anyString())).thenReturn(readWriteLock);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
        when(readWriteLock.readLock()).thenReturn(readLock);
        doNothing().when(writeLock).lock();
        doNothing().when(writeLock).unlock();
        doNothing().when(readLock).lock();
        doNothing().when(readLock).unlock();

        shortLinkService = new ShortLinkServiceImpl(
                shortLinkRepository,
                shortLinkRouteRepository,
                codeGenerator,
                bloomFilterService,
                streamPublisher,
                redissonClient,
                properties,
                meterRegistry
        );
    }

    @Test
    void createShortLink_shouldCreateWithGeneratedCode() {
        Long userId = 123L;
        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");

        String generatedCode = "abc123";
        when(codeGenerator.generate(anyString(), eq(userId), eq(0))).thenReturn(generatedCode);
        when(bloomFilterService.mightContain(generatedCode)).thenReturn(false);
        when(shortLinkRepository.findByShortCode(generatedCode)).thenReturn(Optional.empty());
        when(shortLinkRepository.insert(any(ShortLinkRecord.class))).thenReturn(1);
        when(shortLinkRouteRepository.insertRoute(any(ShortLinkRouteRecord.class))).thenReturn(1);

        ShortLinkResponse response = shortLinkService.createShortLink(userId, request);

        assertThat(response.getShortCode()).isEqualTo(generatedCode);
        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.isActive()).isTrue();

        verify(bloomFilterService).add(generatedCode);
        verify(shortLinkRepository).insert(any(ShortLinkRecord.class));
        verify(shortLinkRouteRepository).insertRoute(any(ShortLinkRouteRecord.class));

        // Verify metrics counter incremented
        Counter shardCounter = meterRegistry.get("shortlink.shard.writes").counter();
        assertThat(shardCounter.count()).isEqualTo(1.0);
    }

    @Test
    void createShortLink_shouldCreateWithCustomCode() {
        Long userId = 123L;
        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomCode("custom123");

        when(bloomFilterService.mightContain("custom123")).thenReturn(false);
        when(shortLinkRepository.findByShortCode("custom123")).thenReturn(Optional.empty());
        when(shortLinkRepository.insert(any(ShortLinkRecord.class))).thenReturn(1);
        when(shortLinkRouteRepository.insertRoute(any(ShortLinkRouteRecord.class))).thenReturn(1);

        ShortLinkResponse response = shortLinkService.createShortLink(userId, request);

        assertThat(response.getShortCode()).isEqualTo("custom123");
        verify(codeGenerator, never()).generate(anyString(), any(), any());
    }

    @Test
    void createShortLink_shouldThrowConflictExceptionWhenCodeExists() {
        Long userId = 123L;
        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomCode("existing123");

        ShortLinkRecord existingRecord = new ShortLinkRecord();
        existingRecord.setShortCode("existing123");

        when(bloomFilterService.mightContain("existing123")).thenReturn(true);
        when(shortLinkRepository.findByShortCode("existing123")).thenReturn(Optional.of(existingRecord));

        assertThatThrownBy(() -> shortLinkService.createShortLink(userId, request))
                .isInstanceOf(ShortLinkConflictException.class);
    }

    @Test
    void createShortLink_shouldRetryOnCollision() {
        Long userId = 123L;
        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");

        // First attempt collides, second succeeds
        when(codeGenerator.generate(anyString(), eq(userId), eq(0))).thenReturn("collision1");
        when(codeGenerator.generate(anyString(), eq(userId), eq(1))).thenReturn("success2");

        when(bloomFilterService.mightContain("collision1")).thenReturn(true);
        when(bloomFilterService.mightContain("success2")).thenReturn(false);

        ShortLinkRecord existingRecord = new ShortLinkRecord();
        when(shortLinkRepository.findByShortCode("collision1")).thenReturn(Optional.of(existingRecord));
        when(shortLinkRepository.findByShortCode("success2")).thenReturn(Optional.empty());
        when(shortLinkRepository.insert(any(ShortLinkRecord.class))).thenReturn(1);
        when(shortLinkRouteRepository.insertRoute(any(ShortLinkRouteRecord.class))).thenReturn(1);

        ShortLinkResponse response = shortLinkService.createShortLink(userId, request);

        assertThat(response.getShortCode()).isEqualTo("success2");
        verify(codeGenerator).generate(anyString(), eq(userId), eq(0));
        verify(codeGenerator).generate(anyString(), eq(userId), eq(1));
    }

    @Test
    void createShortLink_shouldThrowExceptionAfterMaxRetries() {
        Long userId = 123L;
        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");

        // All attempts fail
        for (int i = 0; i < 5; i++) {
            when(codeGenerator.generate(anyString(), eq(userId), eq(i))).thenReturn("collision" + i);
            when(bloomFilterService.mightContain("collision" + i)).thenReturn(true);
        }

        assertThatThrownBy(() -> shortLinkService.createShortLink(userId, request))
                .isInstanceOf(ShortLinkServiceException.class)
                .hasMessageContaining("Unable to allocate unique short link code");
    }

    @Test
    void updateShortLink_shouldUpdateExistingRecord() {
        Long userId = 123L;
        String shortCode = "abc123";
        ShortLinkUpdateRequest request = new ShortLinkUpdateRequest();
        request.setOriginalUrl("https://updated.com");
        request.setActive(false);

        ShortLinkRecord existingRecord = new ShortLinkRecord();
        existingRecord.setShortCode(shortCode);
        existingRecord.setUserId(userId);
        existingRecord.setOriginalUrl("https://original.com");
        existingRecord.setActive(true);

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(existingRecord));
        when(shortLinkRepository.updateOriginalUrl(eq(shortCode), eq("https://updated.com"), any(LocalDateTime.class), any(), eq(false))).thenReturn(1);
        when(shortLinkRouteRepository.updateOriginalUrl(shortCode, "https://updated.com")).thenReturn(1);

        ShortLinkResponse response = shortLinkService.updateShortLink(userId, shortCode, request);

        assertThat(response.getShortCode()).isEqualTo(shortCode);
        assertThat(response.getOriginalUrl()).isEqualTo("https://updated.com");
        assertThat(response.isActive()).isFalse();

        verify(shortLinkRepository).updateOriginalUrl(eq(shortCode), eq("https://updated.com"), any(LocalDateTime.class), any(), eq(false));
        verify(shortLinkRouteRepository).updateOriginalUrl(shortCode, "https://updated.com");
    }

    @Test
    void updateShortLink_shouldThrowExceptionForNonExistentCode() {
        Long userId = 123L;
        String shortCode = "nonexistent";
        ShortLinkUpdateRequest request = new ShortLinkUpdateRequest();

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shortLinkService.updateShortLink(userId, shortCode, request))
                .isInstanceOf(ShortLinkNotFoundException.class);
    }

    @Test
    void updateShortLink_shouldThrowExceptionForUnauthorizedUser() {
        Long userId = 123L;
        Long ownerId = 456L;
        String shortCode = "abc123";
        ShortLinkUpdateRequest request = new ShortLinkUpdateRequest();

        ShortLinkRecord existingRecord = new ShortLinkRecord();
        existingRecord.setShortCode(shortCode);
        existingRecord.setUserId(ownerId);

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(existingRecord));

        assertThatThrownBy(() -> shortLinkService.updateShortLink(userId, shortCode, request))
                .isInstanceOf(ShortLinkServiceException.class)
                .hasMessageContaining("You do not have permission");
    }

    @Test
    void getShortLinkDetails_shouldReturnDetails() {
        Long userId = 123L;
        String shortCode = "abc123";

        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setUserId(userId);
        record.setOriginalUrl("https://example.com");
        record.setActive(true);

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));

        ShortLinkResponse response = shortLinkService.getShortLinkDetails(userId, shortCode);

        assertThat(response.getShortCode()).isEqualTo(shortCode);
        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(response.getUserId()).isEqualTo(userId);
    }

    @Test
    void listShortLinks_shouldReturnPagedResults() {
        int page = 0;
        int size = 10;

        ShortLinkRouteRecord route1 = new ShortLinkRouteRecord();
        route1.setShortCode("abc123");
        route1.setTableName("short_links_0");

        ShortLinkRouteRecord route2 = new ShortLinkRouteRecord();
        route2.setShortCode("def456");
        route2.setTableName("short_links_1");

        List<ShortLinkRouteRecord> routes = Arrays.asList(route1, route2);

        ShortLinkRecord record1 = new ShortLinkRecord();
        record1.setShortCode("abc123");
        record1.setOriginalUrl("https://example1.com");

        ShortLinkRecord record2 = new ShortLinkRecord();
        record2.setShortCode("def456");
        record2.setOriginalUrl("https://example2.com");

        when(shortLinkRouteRepository.listRoutes(page, size)).thenReturn(routes);
        when(shortLinkRepository.findByShortCode("abc123")).thenReturn(Optional.of(record1));
        when(shortLinkRepository.findByShortCode("def456")).thenReturn(Optional.of(record2));
        when(shortLinkRouteRepository.countRoutes()).thenReturn(20L);

        ShortLinkPageResponse response = shortLinkService.listShortLinks(page, size);

        assertThat(response.getRecords()).hasSize(2);
        assertThat(response.getTotal()).isEqualTo(20L);
        assertThat(response.getPage()).isEqualTo(page);
        assertThat(response.getSize()).isEqualTo(size);
    }

    @Test
    void resolveRedirect_shouldReturnOriginalUrl() {
        String shortCode = "abc123";
        Long userId = 123L;

        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setUserId(userId);
        record.setOriginalUrl("https://example.com");
        record.setActive(true);
        record.setExpirationAt(LocalDateTime.now().plusDays(1));

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));
        when(shortLinkRepository.incrementAccessCount(eq(shortCode), any(LocalDateTime.class))).thenReturn(1);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        String result = shortLinkService.resolveRedirect(shortCode, httpRequest);

        assertThat(result).isEqualTo("https://example.com");
        verify(shortLinkRepository).incrementAccessCount(eq(shortCode), any(LocalDateTime.class));
        verify(streamPublisher).publishAccessEvent(eq(record), eq("192.168.1.1"), eq("Mozilla/5.0"));
    }

    @Test
    void resolveRedirect_shouldThrowExceptionForNonExistentCode() {
        String shortCode = "nonexistent";

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shortLinkService.resolveRedirect(shortCode, httpRequest))
                .isInstanceOf(ShortLinkNotFoundException.class);
    }

    @Test
    void resolveRedirect_shouldThrowExceptionForInactiveLink() {
        String shortCode = "abc123";

        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setActive(false);

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> shortLinkService.resolveRedirect(shortCode, httpRequest))
                .isInstanceOf(ShortLinkServiceException.class)
                .hasMessageContaining("Short link is inactive");
    }

    @Test
    void resolveRedirect_shouldHandleExpiredLink() {
        String shortCode = "abc123";

        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setActive(true);
        record.setExpirationAt(LocalDateTime.now().minusDays(1)); // Expired

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));
        when(shortLinkRepository.deactivateIfExpired(eq(shortCode), any(LocalDateTime.class))).thenReturn(1);

        assertThatThrownBy(() -> shortLinkService.resolveRedirect(shortCode, httpRequest))
                .isInstanceOf(ShortLinkServiceException.class)
                .hasMessageContaining("Short link is inactive");

        verify(shortLinkRepository).deactivateIfExpired(eq(shortCode), any(LocalDateTime.class));
    }

    @Test
    void resolveRedirect_shouldExtractClientIpFromXForwardedFor() {
        String shortCode = "abc123";

        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setOriginalUrl("https://example.com");
        record.setActive(true);

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));
        when(shortLinkRepository.incrementAccessCount(eq(shortCode), any(LocalDateTime.class))).thenReturn(1);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        String result = shortLinkService.resolveRedirect(shortCode, httpRequest);

        assertThat(result).isEqualTo("https://example.com");
        verify(streamPublisher).publishAccessEvent(eq(record), eq("192.168.1.1"), eq("Mozilla/5.0"));
    }

    @Test
    void resolveRedirect_shouldFallbackToXRealIp() {
        String shortCode = "abc123";

        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setOriginalUrl("https://example.com");
        record.setActive(true);

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));
        when(shortLinkRepository.incrementAccessCount(eq(shortCode), any(LocalDateTime.class))).thenReturn(1);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("X-Real-IP")).thenReturn("192.168.1.2");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        String result = shortLinkService.resolveRedirect(shortCode, httpRequest);

        assertThat(result).isEqualTo("https://example.com");
        verify(streamPublisher).publishAccessEvent(eq(record), eq("192.168.1.2"), eq("Mozilla/5.0"));
    }

    @Test
    void resolveRedirect_shouldFallbackToRemoteAddr() {
        String shortCode = "abc123";

        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setOriginalUrl("https://example.com");
        record.setActive(true);

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));
        when(shortLinkRepository.incrementAccessCount(eq(shortCode), any(LocalDateTime.class))).thenReturn(1);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        String result = shortLinkService.resolveRedirect(shortCode, httpRequest);

        assertThat(result).isEqualTo("https://example.com");
        verify(streamPublisher).publishAccessEvent(eq(record), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }
}