package com.miniups.config;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for RabbitMQ and other components
 * 
 * Provides mock implementations for dependencies in test environment.
 * This allows tests to run without requiring actual infrastructure.
 * Updated for Spring Boot 3.4+ compatibility.
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

    /**
     * Mock ExceptionMetricsConfig bean for testing
     * 
     * @return Mock ExceptionMetricsConfig that can be used in tests
     */
    @Bean
    @Primary
    public ExceptionMetricsConfig exceptionMetricsConfig() {
        return Mockito.mock(ExceptionMetricsConfig.class);
    }
}