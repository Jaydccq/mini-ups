package com.miniups.shortlink.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.miniups.shortlink.config.ShortLinkProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Sentinel rate limiting functionality.
 * Tests user-based rate limiting for shortlink creation and redirection.
 */
class SentinelRateLimitIntegrationTest {

    private static final String RESOURCE_CREATE = "shortlink-create";
    private static final String RESOURCE_REDIRECT = "shortlink-redirect";

    private ShortLinkProperties properties;

    @BeforeEach
    void setUp() {
        // Clear any existing rules
        ParamFlowRuleManager.loadRules(Collections.emptyList());

        properties = new ShortLinkProperties();
        properties.getSentinel().setCreateThresholdPerSecond(5);
        properties.getSentinel().setRedirectThresholdPerSecond(10);

        // Initialize Sentinel rules
        initializeSentinelRules();
    }

    @AfterEach
    void tearDown() {
        // Clear rules after each test
        ParamFlowRuleManager.loadRules(Collections.emptyList());
        ContextUtil.exit();
    }

    private void initializeSentinelRules() {
        List<ParamFlowRule> rules = new ArrayList<>();
        ShortLinkProperties.Sentinel sentinel = properties.getSentinel();

        // Create rule for shortlink creation with user ID parameter
        ParamFlowRule createRule = new ParamFlowRule(RESOURCE_CREATE)
                .setParamIdx(0)  // First parameter is user ID
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(sentinel.getCreateThresholdPerSecond());
        rules.add(createRule);

        // Create rule for shortlink redirection with user ID parameter
        ParamFlowRule redirectRule = new ParamFlowRule(RESOURCE_REDIRECT)
                .setParamIdx(0)  // First parameter is user ID
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(sentinel.getRedirectThresholdPerSecond());
        rules.add(redirectRule);

        ParamFlowRuleManager.loadRules(rules);
    }

    @Test
    void createShortLink_shouldAllowWithinRateLimit() throws Exception {
        Long userId = 123L;
        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();

        ContextUtil.enter(RESOURCE_CREATE, String.valueOf(userId));
        try {
            // Should allow requests within rate limit
            for (int i = 0; i < allowedRequests; i++) {
                try (Entry ignored = SphU.entry(RESOURCE_CREATE, EntryType.OUT, 1, userId)) {
                    // Simulate processing
                    Thread.sleep(1);
                }
            }
        } finally {
            ContextUtil.exit();
        }

        // Should succeed without exceptions
        assertThat(true).isTrue(); // Test passes if no exceptions thrown
    }

    @Test
    void createShortLink_shouldBlockWhenExceedingRateLimit() throws Exception {
        Long userId = 123L;
        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();
        AtomicInteger blockedCount = new AtomicInteger(0);

        ContextUtil.enter(RESOURCE_CREATE, String.valueOf(userId));
        try {
            // Exceed rate limit
            for (int i = 0; i < allowedRequests + 5; i++) {
                try (Entry ignored = SphU.entry(RESOURCE_CREATE, EntryType.OUT, 1, userId)) {
                    // Simulate processing
                    Thread.sleep(1);
                } catch (BlockException e) {
                    blockedCount.incrementAndGet();
                }
            }
        } finally {
            ContextUtil.exit();
        }

        // Should have blocked some requests
        assertThat(blockedCount.get()).isGreaterThan(0);
    }

    @Test
    void redirectShortLink_shouldAllowWithinRateLimit() throws Exception {
        Long userId = 456L;
        int allowedRequests = properties.getSentinel().getRedirectThresholdPerSecond();

        ContextUtil.enter(RESOURCE_REDIRECT, String.valueOf(userId));
        try {
            // Should allow requests within rate limit
            for (int i = 0; i < allowedRequests; i++) {
                try (Entry ignored = SphU.entry(RESOURCE_REDIRECT, EntryType.OUT, 1, userId)) {
                    // Simulate processing
                    Thread.sleep(1);
                }
            }
        } finally {
            ContextUtil.exit();
        }

        // Should succeed without exceptions
        assertThat(true).isTrue();
    }

    @Test
    void redirectShortLink_shouldBlockWhenExceedingRateLimit() throws Exception {
        Long userId = 456L;
        int allowedRequests = properties.getSentinel().getRedirectThresholdPerSecond();
        AtomicInteger blockedCount = new AtomicInteger(0);

        ContextUtil.enter(RESOURCE_REDIRECT, String.valueOf(userId));
        try {
            // Exceed rate limit
            for (int i = 0; i < allowedRequests + 10; i++) {
                try (Entry ignored = SphU.entry(RESOURCE_REDIRECT, EntryType.OUT, 1, userId)) {
                    // Simulate processing
                    Thread.sleep(1);
                } catch (BlockException e) {
                    blockedCount.incrementAndGet();
                }
            }
        } finally {
            ContextUtil.exit();
        }

        // Should have blocked some requests
        assertThat(blockedCount.get()).isGreaterThan(0);
    }

    @Test
    void differentUsers_shouldHaveIndependentRateLimits() throws InterruptedException, ExecutionException, TimeoutException {
        Long user1 = 111L;
        Long user2 = 222L;
        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger user1Blocked = new AtomicInteger(0);
        AtomicInteger user2Blocked = new AtomicInteger(0);

        try {
            // Submit tasks for both users concurrently
            Future<?> user1Task = executor.submit(() -> {
                ContextUtil.enter(RESOURCE_CREATE, String.valueOf(user1));
                try {
                    for (int i = 0; i < allowedRequests + 3; i++) {
                        try (Entry ignored = SphU.entry(RESOURCE_CREATE, EntryType.OUT, 1, user1)) {
                            Thread.sleep(10);
                        } catch (BlockException e) {
                            user1Blocked.incrementAndGet();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    ContextUtil.exit();
                }
            });

            Future<?> user2Task = executor.submit(() -> {
                ContextUtil.enter(RESOURCE_CREATE, String.valueOf(user2));
                try {
                    for (int i = 0; i < allowedRequests + 3; i++) {
                        try (Entry ignored = SphU.entry(RESOURCE_CREATE, EntryType.OUT, 1, user2)) {
                            Thread.sleep(10);
                        } catch (BlockException e) {
                            user2Blocked.incrementAndGet();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    ContextUtil.exit();
                }
            });

            user1Task.get(5, TimeUnit.SECONDS);
            user2Task.get(5, TimeUnit.SECONDS);

        } finally {
            executor.shutdown();
        }

        // Both users should have some requests blocked independently
        assertThat(user1Blocked.get()).isGreaterThan(0);
        assertThat(user2Blocked.get()).isGreaterThan(0);

        System.out.println("User1 blocked: " + user1Blocked.get() + ", User2 blocked: " + user2Blocked.get());
    }

    @Test
    void rateLimitRecovery_shouldAllowRequestsAfterTimeWindow() throws InterruptedException {
        Long userId = 789L;
        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();
        AtomicInteger firstRoundBlocked = new AtomicInteger(0);
        AtomicInteger secondRoundBlocked = new AtomicInteger(0);

        // First round: exceed rate limit
        ContextUtil.enter(RESOURCE_CREATE, String.valueOf(userId));
        try {
            for (int i = 0; i < allowedRequests + 3; i++) {
                try (Entry ignored = SphU.entry(RESOURCE_CREATE, EntryType.OUT, 1, userId)) {
                    Thread.sleep(10);
                } catch (BlockException e) {
                    firstRoundBlocked.incrementAndGet();
                }
            }
        } finally {
            ContextUtil.exit();
        }

        // Wait for rate limit window to reset (1 second + buffer)
        Thread.sleep(1200);

        // Second round: should be allowed again
        ContextUtil.enter(RESOURCE_CREATE, String.valueOf(userId));
        try {
            for (int i = 0; i < allowedRequests; i++) {
                try (Entry ignored = SphU.entry(RESOURCE_CREATE, EntryType.OUT, 1, userId)) {
                    Thread.sleep(10);
                } catch (BlockException e) {
                    secondRoundBlocked.incrementAndGet();
                }
            }
        } finally {
            ContextUtil.exit();
        }

        // First round should have blocks, second round should have fewer or none
        assertThat(firstRoundBlocked.get()).isGreaterThan(0);
        assertThat(secondRoundBlocked.get()).isLessThanOrEqualTo(firstRoundBlocked.get());

        System.out.println("First round blocked: " + firstRoundBlocked.get() +
                          ", Second round blocked: " + secondRoundBlocked.get());
    }

    @Test
    void anonymousUser_shouldHaveIndependentRateLimit() throws InterruptedException {
        Long anonymousUserId = -1L; // Anonymous user representation
        int allowedRequests = properties.getSentinel().getCreateThresholdPerSecond();
        AtomicInteger blockedCount = new AtomicInteger(0);

        ContextUtil.enter(RESOURCE_CREATE, String.valueOf(anonymousUserId));
        try {
            // Exceed rate limit for anonymous user
            for (int i = 0; i < allowedRequests + 5; i++) {
                try (Entry ignored = SphU.entry(RESOURCE_CREATE, EntryType.OUT, 1, anonymousUserId)) {
                    Thread.sleep(1);
                } catch (BlockException e) {
                    blockedCount.incrementAndGet();
                }
            }
        } finally {
            ContextUtil.exit();
        }

        // Anonymous user should also be rate limited
        assertThat(blockedCount.get()).isGreaterThan(0);
    }

    @Test
    void highThroughputScenario_shouldMaintainRateLimit() throws InterruptedException, ExecutionException {
        Long userId = 999L;
        int allowedRequests = properties.getSentinel().getRedirectThresholdPerSecond();
        int totalRequests = allowedRequests * 3; // 3x the allowed rate
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        try {
            for (int i = 0; i < totalRequests; i++) {
                executor.submit(() -> {
                    ContextUtil.enter(RESOURCE_REDIRECT, String.valueOf(userId));
                    try (Entry ignored = SphU.entry(RESOURCE_REDIRECT, EntryType.OUT, 1, userId)) {
                        successCount.incrementAndGet();
                        Thread.sleep(1); // Simulate processing
                    } catch (BlockException e) {
                        blockedCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        ContextUtil.exit();
                        latch.countDown();
                    }
                });
            }

            // Wait for all requests to complete
            latch.await(10, TimeUnit.SECONDS);

        } finally {
            executor.shutdown();
        }

        // Verify rate limiting is working
        assertThat(successCount.get()).isLessThanOrEqualTo(allowedRequests * 2); // Allow some variance
        assertThat(blockedCount.get()).isGreaterThan(0);
        assertThat(successCount.get() + blockedCount.get()).isEqualTo(totalRequests);

        System.out.println("High throughput test - Success: " + successCount.get() +
                          ", Blocked: " + blockedCount.get() + ", Total: " + totalRequests);
    }

    @Test
    void multipleResources_shouldHaveIndependentLimits() throws InterruptedException {
        Long userId = 555L;
        int createLimit = properties.getSentinel().getCreateThresholdPerSecond();
        int redirectLimit = properties.getSentinel().getRedirectThresholdPerSecond();

        AtomicInteger createBlocked = new AtomicInteger(0);
        AtomicInteger redirectBlocked = new AtomicInteger(0);

        // Test create resource
        ContextUtil.enter(RESOURCE_CREATE, String.valueOf(userId));
        try {
            for (int i = 0; i < createLimit + 3; i++) {
                try (Entry ignored = SphU.entry(RESOURCE_CREATE, EntryType.OUT, 1, userId)) {
                    Thread.sleep(1);
                } catch (BlockException e) {
                    createBlocked.incrementAndGet();
                }
            }
        } finally {
            ContextUtil.exit();
        }

        // Test redirect resource (should be independent)
        ContextUtil.enter(RESOURCE_REDIRECT, String.valueOf(userId));
        try {
            for (int i = 0; i < redirectLimit + 5; i++) {
                try (Entry ignored = SphU.entry(RESOURCE_REDIRECT, EntryType.OUT, 1, userId)) {
                    Thread.sleep(1);
                } catch (BlockException e) {
                    redirectBlocked.incrementAndGet();
                }
            }
        } finally {
            ContextUtil.exit();
        }

        // Both resources should have independent limits
        assertThat(createBlocked.get()).isGreaterThan(0);
        assertThat(redirectBlocked.get()).isGreaterThan(0);

        System.out.println("Create blocked: " + createBlocked.get() +
                          ", Redirect blocked: " + redirectBlocked.get());
    }

    @Test
    void configurationValidation_shouldUseCorrectThresholds() {
        // Verify the configuration is set correctly
        assertThat(properties.getSentinel().getCreateThresholdPerSecond()).isEqualTo(5);
        assertThat(properties.getSentinel().getRedirectThresholdPerSecond()).isEqualTo(10);

        // Verify rules are loaded
        List<ParamFlowRule> rules = ParamFlowRuleManager.getRules();
        assertThat(rules).hasSize(2);

        ParamFlowRule createRule = rules.stream()
                .filter(rule -> RESOURCE_CREATE.equals(rule.getResource()))
                .findFirst()
                .orElse(null);

        ParamFlowRule redirectRule = rules.stream()
                .filter(rule -> RESOURCE_REDIRECT.equals(rule.getResource()))
                .findFirst()
                .orElse(null);

        assertThat(createRule).isNotNull();
        assertThat(createRule.getCount()).isEqualTo(5);
        assertThat(createRule.getParamIdx()).isEqualTo(0);

        assertThat(redirectRule).isNotNull();
        assertThat(redirectRule.getCount()).isEqualTo(10);
        assertThat(redirectRule.getParamIdx()).isEqualTo(0);
    }
}
