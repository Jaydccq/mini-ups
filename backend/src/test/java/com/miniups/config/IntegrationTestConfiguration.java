package com.miniups.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.shortlink.stream.ShortLinkStreamPublisher;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.redisson.api.RedissonClient;

/**
 * Test configuration for integration tests.
 * Provides mocked beans for external services (Redis, RabbitMQ, Kafka, RAG) when they're not available.
 * Activated with @ActiveProfiles("test")
 */
@TestConfiguration
@Profile("test")
public class IntegrationTestConfiguration {

    /**
     * Mock RedisConnectionFactory for tests that need Redis
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    /**
     * Mock RedisTemplate for tests that need Redis operations
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    /**
     * Mock StringRedisTemplate for string-based Redis operations
     */
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(template.opsForValue()).thenReturn(valueOps);
        return template;
    }

    /**
     * Mock RedissonClient for distributed lock operations
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        return Mockito.mock(RedissonClient.class);
    }

    /**
     * Mock RabbitTemplate for RabbitMQ operations
     */
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    /**
     * Mock KafkaTemplate for Kafka operations
     */
    @Bean
    @Primary
    public KafkaTemplate<String, String> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }

    /**
     * Mock ShortLinkStreamPublisher for stream operations
     */
    @Bean
    @Primary
    public ShortLinkStreamPublisher shortLinkStreamPublisher() {
        return Mockito.mock(ShortLinkStreamPublisher.class);
    }

    /**
     * Provide ObjectMapper for JSON serialization in tests
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
