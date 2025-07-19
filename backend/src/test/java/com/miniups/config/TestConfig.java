package com.miniups.config;

import com.miniups.service.WorldSimulatorService;
import com.miniups.service.AmazonIntegrationService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
     */
    @MockBean
    private WorldSimulatorService worldSimulatorService;

    /**
     * Mock Amazon Integration Service for testing.
     * This prevents actual HTTP calls to Amazon service during tests.
     */
    @MockBean
    private AmazonIntegrationService amazonIntegrationService;

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