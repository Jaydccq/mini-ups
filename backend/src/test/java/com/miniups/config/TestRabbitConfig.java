package com.miniups.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for RabbitMQ and other components
 * 
 * Provides both real RabbitMQ configuration for integration tests and mock implementations
 * for unit tests. This allows flexible testing strategies based on configuration.
 * Updated for Spring Boot 3.4+ compatibility.
 */
@TestConfiguration
@Profile("test")
public class TestRabbitConfig {

    // Queue Names - must match production configuration
    public static final String AUDIT_LOG_QUEUE = "q.audit_log";
    public static final String SHIPMENT_PROCESSOR_QUEUE = "q.shipment.processor";
    public static final String NOTIFICATIONS_QUEUE = "q.notifications";
    public static final String WORLD_SIMULATOR_QUEUE = "q.world_simulator";
    public static final String DEAD_LETTER_QUEUE = "q.dead_letter";
    
    // Exchange Names
    public static final String TOPIC_EXCHANGE_NAME = "ups.events.topic";
    public static final String DLX_NAME = "ups.events.dlx";

    /**
     * Mock ExceptionMetricsConfig bean for testing
     * 
     * @return Mock ExceptionMetricsConfig that can be used in tests
     */
    @Bean
    @Primary
    public ExceptionMetricsConfig exceptionMetricsConfig() {
        return new ExceptionMetricsConfig(null) {
            @Override
            public void recordException(Exception exception, String endpoint) {
                // No-op for tests - just log
                System.out.println("Test: Exception recorded: " + exception.getClass().getSimpleName() + " at " + endpoint);
            }
        };
    }

    /**
     * Test RabbitMQ Topic Exchange
     * Only created when RabbitMQ test mode is enabled
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public TopicExchange testTopicExchange() {
        return ExchangeBuilder.topicExchange(TOPIC_EXCHANGE_NAME)
                .durable(false) // Non-durable for tests
                .build();
    }

    /**
     * Test Dead Letter Exchange
     * Only created when RabbitMQ test mode is enabled
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public FanoutExchange testDeadLetterExchange() {
        return ExchangeBuilder.fanoutExchange(DLX_NAME)
                .durable(false) // Non-durable for tests
                .build();
    }

    /**
     * Test Audit Log Queue - Critical for test success
     * This queue must exist or tests will fail with "NOT_FOUND - no queue 'q.audit_log'"
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public Queue testAuditLogQueue() {
        return QueueBuilder.durable(AUDIT_LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.audit_log")
                .build();
    }

    /**
     * Test Shipment Processor Queue
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public Queue testShipmentProcessorQueue() {
        return QueueBuilder.durable(SHIPMENT_PROCESSOR_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.shipment.processor")
                .build();
    }

    /**
     * Test Notifications Queue
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public Queue testNotificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.notifications")
                .build();
    }

    /**
     * Test World Simulator Queue
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public Queue testWorldSimulatorQueue() {
        return QueueBuilder.durable(WORLD_SIMULATOR_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.world_simulator")
                .build();
    }

    /**
     * Test Dead Letter Queue
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public Queue testDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    /**
     * Test Queue Bindings
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public Binding testAuditLogBinding() {
        return BindingBuilder.bind(testAuditLogQueue())
                .to(testTopicExchange())
                .with("*.*.created");
    }

    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public Binding testAuditLogUpdatedBinding() {
        return BindingBuilder.bind(testAuditLogQueue())
                .to(testTopicExchange())
                .with("*.*.updated");
    }

    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public Binding testDeadLetterBinding() {
        return BindingBuilder.bind(testDeadLetterQueue())
                .to(testDeadLetterExchange());
    }

    /**
     * JSON message converter for test environment
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public MessageConverter testJsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setCreateMessageIds(true);
        return converter;
    }

    /**
     * Test RabbitTemplate with proper configuration
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public RabbitTemplate testRabbitTemplate(ConnectionFactory connectionFactory, 
                                           MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setExchange(TOPIC_EXCHANGE_NAME);
        // Simplified error handling for tests
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Test: Message failed to reach exchange: " + cause);
            }
        });
        return template;
    }

    /**
     * Test container factory for message listeners
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = false)
    public SimpleRabbitListenerContainerFactory testRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter messageConverter) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        
        // Simplified error handling for tests
        factory.setErrorHandler(throwable -> {
            System.err.println("Test: Error in message processing: " + throwable.getMessage());
        });
        
        return factory;
    }
}