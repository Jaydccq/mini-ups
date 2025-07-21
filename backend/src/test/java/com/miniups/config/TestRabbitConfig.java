package com.miniups.config;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for RabbitMQ components
 * 
 * Provides mock implementations for RabbitMQ dependencies in test environment.
 * This allows tests to run without requiring actual RabbitMQ infrastructure.
 */
@TestConfiguration
@Profile("test")
public class TestRabbitConfig {

    /**
     * Mock RabbitTemplate bean for testing
     * 
     * @return Mock RabbitTemplate that can be used in tests
     */
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }
}