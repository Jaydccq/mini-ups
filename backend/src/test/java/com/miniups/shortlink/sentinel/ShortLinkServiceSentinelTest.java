package com.miniups.shortlink.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.miniups.shortlink.bloom.RedisBloomFilterService;
import com.miniups.shortlink.config.ShortLinkProperties;
import com.miniups.shortlink.dto.ShortLinkCreateRequest;
import com.miniups.shortlink.exception.ShortLinkRateLimitException;
import com.miniups.shortlink.model.ShortLinkRecord;
import com.miniups.shortlink.repository.ShortLinkRepository;
import com.miniups.shortlink.repository.ShortLinkRouteRepository;
import com.miniups.shortlink.service.impl.ShortLinkServiceImpl;
import com.miniups.shortlink.stream.ShortLinkStreamPublisher;
import com.miniups.shortlink.util.ShortLinkCodeGenerator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Integration test for Sentinel rate limiting in ShortLinkService.
 * Tests actual service methods with rate limiting applied.
 */
@ExtendWith(MockitoExtension.class)
class ShortLinkServiceSentinelTest {

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

    private ShortLinkServiceImpl shortLinkService;
    private ShortLinkProperties properties;

    @BeforeEach
    void setUp() {
        // Clear any existing Sentinel rules
        ParamFlowRuleManager.loadRules(Collections.emptyList());

        // Setup properties with low thresholds for testing
        properties = new ShortLinkProperties();
        properties.getSentinel().setCreateThresholdPerSecond(3);
        properties.getSentinel().setRedirectThresholdPerSecond(5);

        // Setup sharding properties
        properties.getSharding().setTableWeights("short_links_0:1");

        // Initialize Sentinel rules
        initializeSentinelRules();

        // Setup mocks
        setupMocks();

        // Create service instance
        shortLinkService = new ShortLinkServiceImpl(
                shortLinkRepository,
                shortLinkRouteRepository,
                codeGenerator,
                bloomFilterService,
                streamPublisher,
                redissonClient,
                properties,
                new SimpleMeterRegistry()
        );
    }

    @AfterEach
    void tearDown() {
        ParamFlowRuleManager.loadRules(Collections.emptyList());
    }

    private void initializeSentinelRules() {
        List<ParamFlowRule> rules = new ArrayList<>();

        ParamFlowRule createRule = new ParamFlowRule("shortlink-create")
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(properties.getSentinel().getCreateThresholdPerSecond());
        rules.add(createRule);

        ParamFlowRule redirectRule = new ParamFlowRule("shortlink-redirect")
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(properties.getSentinel().getRedirectThresholdPerSecond());
        rules.add(redirectRule);

        ParamFlowRuleManager.loadRules(rules);
    }

    private void setupMocks() {
        // Setup Redisson mocks
        when(redissonClient.getReadWriteLock(anyString())).thenReturn(readWriteLock);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
        when(readWriteLock.readLock()).thenReturn(readLock);
        doNothing().when(writeLock).lock();
        doNothing().when(writeLock).unlock();
        doNothing().when(readLock).lock();
        doNothing().when(readLock).unlock();

        // Setup code generator
        when(codeGenerator.generate(anyString(), any(), any())).thenReturn("test123");

        // Setup bloom filter
        when(bloomFilterService.mightContain(anyString())).thenReturn(false);

        // Setup repository mocks
        when(shortLinkRepository.findByShortCode(anyString())).thenReturn(Optional.empty());
        when(shortLinkRepository.insert(any())).thenAnswer(invocation -> {
            ShortLinkRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(1L);
            }
            return record;
        });
        doAnswer(invocation -> null).when(shortLinkRouteRepository).insertRoute(any());

        // Setup HTTP request mocks
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
    }

    @Test
    void createShortLink_shouldAllowWithinRateLimit() {
        Long userId = 123L;
        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");

        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();

        // Should allow requests within rate limit
        for (int i = 0; i < allowedRequests; i++) {
            var response = shortLinkService.createShortLink(userId, request);
            assertThat(response).isNotNull();
            assertThat(response.getShortCode()).isEqualTo("test123");
        }
    }

    @Test
    void createShortLink_shouldThrowRateLimitExceptionWhenExceeded() {
        Long userId = 123L;
        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");

        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();

        // Consume allowed requests
        for (int i = 0; i < allowedRequests; i++) {
            shortLinkService.createShortLink(userId, request);
        }

        // Next request should be rate limited
        assertThatThrownBy(() -> shortLinkService.createShortLink(userId, request))
                .isInstanceOf(ShortLinkRateLimitException.class)
                .hasMessageContaining("rate limit exceeded for user " + userId);
    }

    @Test
    void resolveRedirect_shouldAllowWithinRateLimit() {
        String shortCode = "test123";
        Long userId = 456L;

        // Setup redirect test
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setUserId(userId);
        record.setOriginalUrl("https://example.com");
        record.setActive(true);
        record.setExpirationAt(LocalDateTime.now().plusDays(1));

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));
        when(shortLinkRepository.incrementAccessCount(anyString(), any())).thenReturn(1);

        int allowedRequests = properties.getSentinel().getRedirectThresholdPerSecond();

        // Should allow requests within rate limit
        for (int i = 0; i < allowedRequests; i++) {
            String result = shortLinkService.resolveRedirect(shortCode, httpRequest);
            assertThat(result).isEqualTo("https://example.com");
        }
    }

    @Test
    void resolveRedirect_shouldThrowRateLimitExceptionWhenExceeded() {
        String shortCode = "test123";
        Long userId = 456L;

        // Setup redirect test
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setUserId(userId);
        record.setOriginalUrl("https://example.com");
        record.setActive(true);
        record.setExpirationAt(LocalDateTime.now().plusDays(1));

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));
        when(shortLinkRepository.incrementAccessCount(anyString(), any())).thenReturn(1);

        int allowedRequests = properties.getSentinel().getRedirectThresholdPerSecond();

        // Consume allowed requests
        for (int i = 0; i < allowedRequests; i++) {
            shortLinkService.resolveRedirect(shortCode, httpRequest);
        }

        // Next request should be rate limited
        assertThatThrownBy(() -> shortLinkService.resolveRedirect(shortCode, httpRequest))
                .isInstanceOf(ShortLinkRateLimitException.class)
                .hasMessageContaining("rate limit exceeded for owner " + userId);
    }

    @Test
    void differentUsers_shouldHaveIndependentRateLimits() throws InterruptedException {
        Long user1 = 111L;
        Long user2 = 222L;

        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");

        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();

        AtomicInteger user1Success = new AtomicInteger(0);
        AtomicInteger user1Failed = new AtomicInteger(0);
        AtomicInteger user2Success = new AtomicInteger(0);
        AtomicInteger user2Failed = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        try {
            // User 1 thread
            executor.submit(() -> {
                try {
                    for (int i = 0; i < allowedRequests + 2; i++) {
                        try {
                            shortLinkService.createShortLink(user1, request);
                            user1Success.incrementAndGet();
                        } catch (ShortLinkRateLimitException e) {
                            user1Failed.incrementAndGet();
                        }
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });

            // User 2 thread
            executor.submit(() -> {
                try {
                    for (int i = 0; i < allowedRequests + 2; i++) {
                        try {
                            shortLinkService.createShortLink(user2, request);
                            user2Success.incrementAndGet();
                        } catch (ShortLinkRateLimitException e) {
                            user2Failed.incrementAndGet();
                        }
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);

        } finally {
            executor.shutdown();
        }

        // Both users should have some successes and some failures
        assertThat(user1Success.get()).isGreaterThan(0);
        assertThat(user1Failed.get()).isGreaterThan(0);
        assertThat(user2Success.get()).isGreaterThan(0);
        assertThat(user2Failed.get()).isGreaterThan(0);

        // Each user should be limited independently
        assertThat(user1Success.get()).isLessThanOrEqualTo(allowedRequests + 1); // Allow some variance
        assertThat(user2Success.get()).isLessThanOrEqualTo(allowedRequests + 1);

        System.out.println("User1 - Success: " + user1Success.get() + ", Failed: " + user1Failed.get());
        System.out.println("User2 - Success: " + user2Success.get() + ", Failed: " + user2Failed.get());
    }

    @Test
    void anonymousUser_shouldBeRateLimited() {
        Long anonymousUserId = null; // Anonymous user

        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");

        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();

        // Consume allowed requests
        for (int i = 0; i < allowedRequests; i++) {
            var response = shortLinkService.createShortLink(anonymousUserId, request);
            assertThat(response).isNotNull();
        }

        // Next request should be rate limited
        assertThatThrownBy(() -> shortLinkService.createShortLink(anonymousUserId, request))
                .isInstanceOf(ShortLinkRateLimitException.class)
                .hasMessageContaining("rate limit exceeded for user -1");
    }

    @Test
    void mixedCreateAndRedirect_shouldHaveIndependentLimits() {
        Long userId = 789L;

        // Setup for creation
        ShortLinkCreateRequest createRequest = new ShortLinkCreateRequest();
        createRequest.setOriginalUrl("https://example.com");

        // Setup for redirection
        String shortCode = "test123";
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode(shortCode);
        record.setUserId(userId);
        record.setOriginalUrl("https://example.com");
        record.setActive(true);
        record.setExpirationAt(LocalDateTime.now().plusDays(1));

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(record));
        when(shortLinkRepository.incrementAccessCount(anyString(), any())).thenReturn(1);

        int createLimit = properties.getSentinel().getCreateThresholdPerSecond();
        int redirectLimit = properties.getSentinel().getRedirectThresholdPerSecond();

        // Consume create limit
        for (int i = 0; i < createLimit; i++) {
            shortLinkService.createShortLink(userId, createRequest);
        }

        // Create should now fail
        assertThatThrownBy(() -> shortLinkService.createShortLink(userId, createRequest))
                .isInstanceOf(ShortLinkRateLimitException.class);

        // But redirect should still work (independent limit)
        for (int i = 0; i < redirectLimit; i++) {
            String result = shortLinkService.resolveRedirect(shortCode, httpRequest);
            assertThat(result).isEqualTo("https://example.com");
        }

        // Now redirect should also fail
        assertThatThrownBy(() -> shortLinkService.resolveRedirect(shortCode, httpRequest))
                .isInstanceOf(ShortLinkRateLimitException.class);
    }

    @Test
    void rateLimitRecovery_shouldAllowRequestsAfterWindow() throws InterruptedException {
        Long userId = 555L;
        ShortLinkCreateRequest request = new ShortLinkCreateRequest();
        request.setOriginalUrl("https://example.com");

        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();

        // Consume all allowed requests
        for (int i = 0; i < allowedRequests; i++) {
            shortLinkService.createShortLink(userId, request);
        }

        // Verify rate limit is active
        assertThatThrownBy(() -> shortLinkService.createShortLink(userId, request))
                .isInstanceOf(ShortLinkRateLimitException.class);

        // Wait for rate limit window to reset
        Thread.sleep(1200); // 1 second + buffer

        // Should be able to make requests again
        var response = shortLinkService.createShortLink(userId, request);
        assertThat(response).isNotNull();
        assertThat(response.getShortCode()).isEqualTo("test123");
    }
}
