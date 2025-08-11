package com.miniups.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test Environment Auto Configuration
 * 
 * Used to handle configuration requirements specific to the test environment,
 * including auto-configuration exclusions and test-specific Bean configuration
 */
@Configuration
@Profile("test")
@ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = true)
public class TestAutoConfiguration {
    
    // This class is primarily used to declare auto-configuration for the test environment
    // Actual RabbitMQ simulation configuration is handled in TestRabbitConfig
}