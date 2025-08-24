package com.miniups.config;

import com.miniups.service.WorldSimulatorService;
import com.miniups.service.AmazonIntegrationService;
import com.miniups.service.EventPublisherService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Minimal core test configuration for non-web SpringBootTests.
 * Provides only external-service mocks and lightweight beans, avoiding web/security wiring.
 */
@TestConfiguration
@Profile("test")
public class TestCoreConfig {

    @Bean
    @Primary
    public WorldSimulatorService worldSimulatorService() {
        return Mockito.mock(WorldSimulatorService.class);
    }

    @Bean
    @Primary
    public AmazonIntegrationService amazonIntegrationService() {
        return Mockito.mock(AmazonIntegrationService.class);
    }

    @Bean
    @Primary
    public EventPublisherService eventPublisherService() {
        return Mockito.mock(EventPublisherService.class);
    }

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(4);
    }
}

