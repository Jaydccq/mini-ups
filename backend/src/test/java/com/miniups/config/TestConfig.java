package com.miniups.config;

import com.miniups.service.WorldSimulatorService;
import com.miniups.service.AmazonIntegrationService;
import com.miniups.service.EventPublisherService;
import com.miniups.config.ExceptionMetricsConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.Mockito;

/**
 * Test configuration for Mini-UPS application tests.
 * Provides mock beans and test-specific configurations.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Mock World Simulator Service for testing.
     * This prevents actual TCP connections during tests.
     *
     * @return Mock WorldSimulatorService instance
     */
    @Bean
    @Primary
    @Profile("test")
    public WorldSimulatorService worldSimulatorService() {
        return Mockito.mock(WorldSimulatorService.class);
    }

    /**
     * Mock Amazon Integration Service for testing.
     * This prevents actual HTTP calls to Amazon service during tests.
     *
     * @return Mock AmazonIntegrationService instance
     */
    @Bean
    @Primary
    @Profile("test")
    public AmazonIntegrationService amazonIntegrationService() {
        return Mockito.mock(AmazonIntegrationService.class);
    }

    /**
     * Mock Event Publisher Service for testing.
     * This prevents actual RabbitMQ calls during tests.
     *
     * @return Mock EventPublisherService instance
     */
    @Bean
    @Primary
    @Profile("test")
    public EventPublisherService eventPublisherService() {
        return Mockito.mock(EventPublisherService.class);
    }

    /**
     * Mock Exception Metrics Config for testing.
     * This prevents actual metrics collection during tests.
     *
     * @return Mock ExceptionMetricsConfig instance
     */
    @Bean
    @Primary
    @Profile("test")
    public ExceptionMetricsConfig exceptionMetricsConfig() {
        return Mockito.mock(ExceptionMetricsConfig.class);
    }

    /**
     * Password encoder bean for testing.
     * Uses BCrypt with lower strength for faster test execution.
     *
     * @return PasswordEncoder instance for testing
     */
    @Bean
    @Primary
    @Profile("test")
    public PasswordEncoder testPasswordEncoder() {
        // Use lower strength for faster test execution
        return new BCryptPasswordEncoder(4);
    }
}