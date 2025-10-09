package com.miniups.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

/**
 * RabbitMQ Configuration for Enterprise Message Queue System with WebSocket STOMP Integration
 *
 * Provides comprehensive messaging infrastructure for:
 * - Event-driven architecture with transactional outbox pattern
 * - Real-time WebSocket messaging with STOMP protocol over RabbitMQ
 * - Reliable message delivery with publisher confirms and acknowledgments
 * - Dead letter queue handling for failed messages
 * - Performance optimization with connection pooling and prefetch settings
 * - High-throughput WebSocket broadcasting for real-time tracking updates
 *
 * Architecture Pattern: Event-Driven Microservices with Message-Oriented Middleware
 * Message Flow: Producer → RabbitMQ Exchange → Queue → Consumer → WebSocket STOMP Broadcast
 *
 * @author Mini-UPS Development Team
 * @version 2.0
 * @since 2024-12-01
 */
@Slf4j
@Configuration
@EnableRabbit
@ConditionalOnClass(ConnectionFactory.class)
@Profile("!test & !rabbitmq-disabled")
public class RabbitMQConfig {


    // Exchange Names
    public static final String TOPIC_EXCHANGE_NAME = "ups.events.topic";
    public static final String DLX_NAME = "ups.events.dlx";
    public static final String WEBSOCKET_EXCHANGE = "ups.websocket.topic";

    // Queue Names
    public static final String SHIPMENT_PROCESSOR_QUEUE = "q.shipment.processor";
    public static final String NOTIFICATIONS_QUEUE = "q.notifications";
    public static final String AUDIT_LOG_QUEUE = "q.audit_log";
    public static final String WORLD_SIMULATOR_QUEUE = "q.world_simulator";
    public static final String DEAD_LETTER_QUEUE = "q.dead_letter";
    public static final String WEBSOCKET_BROADCAST_QUEUE = "q.websocket.broadcast";
    public static final String TRACKING_UPDATE_QUEUE = "q.tracking.updates";

    // Routing Keys
    public static final String SHIPMENT_CREATE_ROUTING_KEY = "shipment.create.request";
    public static final String SHIPMENT_STATUS_ROUTING_KEY = "shipment.status.updated";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String TRUCK_DISPATCH_ROUTING_KEY = "truck.dispatch";
    public static final String AUDIT_LOG_ROUTING_KEY = "audit.log.created";
    public static final String WEBSOCKET_ROUTING_KEY = "websocket.#";
    public static final String TRACKING_UPDATE_ROUTING_KEY = "tracking.update.#";

    /**
     * Main topic exchange for all business events
     * Uses topic routing for flexible message routing based on routing keys
     */
    @Bean
    public TopicExchange topicExchange() {
        return ExchangeBuilder.topicExchange(TOPIC_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    /**
     * Dead letter exchange for failed messages
     * Provides reliability by capturing messages that cannot be processed
     */
    @Bean
    public FanoutExchange deadLetterExchange() {
        return ExchangeBuilder.fanoutExchange(DLX_NAME)
                .durable(true)
                .build();
    }

    /**
     * WebSocket topic exchange for real-time updates
     * Routes messages to WebSocket STOMP endpoints for live client updates
     */
    @Bean
    public TopicExchange websocketExchange() {
        return ExchangeBuilder.topicExchange(WEBSOCKET_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Queue for processing shipment creation requests
     * High-priority queue with dead letter routing for reliability
     */
    @Bean
    public Queue shipmentProcessorQueue() {
        return QueueBuilder.durable(SHIPMENT_PROCESSOR_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.shipment.processor")
                .build();
    }

    /**
     * Queue for notification processing
     * Handles user notifications across multiple channels
     */
    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.notifications")
                .build();
    }

    /**
     * Queue for audit log processing
     * Captures all system events for compliance and monitoring
     */
    @Bean
    public Queue auditLogQueue() {
        return QueueBuilder.durable(AUDIT_LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.audit_log")
                .build();
    }

    /**
     * Queue for world simulator integration
     * Handles communication with external world simulation system
     */
    @Bean
    public Queue worldSimulatorQueue() {
        return QueueBuilder.durable(WORLD_SIMULATOR_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.world_simulator")
                .build();
    }

    /**
     * Dead letter queue for failed message handling
     * Stores messages that cannot be processed for manual intervention
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    /**
     * Queue for WebSocket broadcast messages with high throughput configuration
     * Optimized for real-time client updates with priority handling
     */
    @Bean
    public Queue websocketBroadcastQueue() {
        return QueueBuilder.durable(WEBSOCKET_BROADCAST_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.websocket.broadcast")
                .withArgument("x-max-priority", 10) // Priority queue for real-time updates
                .withArgument("x-max-length", 10000) // Prevent memory overflow
                .build();
    }

    /**
     * Queue for tracking updates with optimized performance settings
     * Handles GPS coordinates and shipment status changes
     */
    @Bean
    public Queue trackingUpdateQueue() {
        return QueueBuilder.durable(TRACKING_UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "failed.tracking.update")
                .withArgument("x-message-ttl", 60000) // 1 minute TTL for real-time data
                .withArgument("x-max-length", 5000) // Keep queue size manageable
                .build();
    }

    // Bindings: Connect queues to exchanges with routing patterns

    @Bean
    public Binding shipmentProcessorBinding() {
        return BindingBuilder.bind(shipmentProcessorQueue())
                .to(topicExchange())
                .with("shipment.create.*");
    }

    @Bean
    public Binding notificationsShipmentBinding() {
        return BindingBuilder.bind(notificationsQueue())
                .to(topicExchange())
                .with("shipment.#");
    }

    @Bean
    public Binding notificationsUserBinding() {
        return BindingBuilder.bind(notificationsQueue())
                .to(topicExchange())
                .with("user.#");
    }

    @Bean
    public Binding auditLogCreatedBinding() {
        return BindingBuilder.bind(auditLogQueue())
                .to(topicExchange())
                .with("*.*.created");
    }

    @Bean
    public Binding auditLogUpdatedBinding() {
        return BindingBuilder.bind(auditLogQueue())
                .to(topicExchange())
                .with("*.*.updated");
    }

    @Bean
    public Binding worldSimulatorBinding() {
        return BindingBuilder.bind(worldSimulatorQueue())
                .to(topicExchange())
                .with(TRUCK_DISPATCH_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange());
    }

    @Bean
    public Binding websocketBroadcastBinding() {
        return BindingBuilder.bind(websocketBroadcastQueue())
                .to(websocketExchange())
                .with(WEBSOCKET_ROUTING_KEY);
    }

    @Bean
    public Binding trackingUpdateBinding() {
        return BindingBuilder.bind(trackingUpdateQueue())
                .to(topicExchange())
                .with(TRACKING_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding trackingToWebSocketBinding() {
        return BindingBuilder.bind(websocketBroadcastQueue())
                .to(topicExchange())
                .with("tracking.update.*");
    }

    /**
     * JSON message converter for object serialization
     * Uses Jackson for consistent JSON handling across the application
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setCreateMessageIds(true);
        return converter;
    }

    /**
     * Enhanced RabbitTemplate for sending messages with reliability features
     * Configured with JSON converter, publisher confirms, and comprehensive error handling
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                       MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setExchange(TOPIC_EXCHANGE_NAME);
        template.setMandatory(true); // Ensure messages are routed to a queue

        // Publisher confirmation callback for reliability
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Message successfully published with correlation ID: {}", correlationData);
            } else {
                log.error("Failed to publish message with correlation ID: {}, cause: {}", correlationData, cause);
            }
        });

        // Returns callback for unrouted messages
        template.setReturnsCallback(returned -> {
            log.warn("Message returned: exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        });

        return template;
    }

    /**
     * Enhanced container factory for message listeners with performance optimization
     * Configures manual acknowledgment, retry policies, and concurrent processing
     */
    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);

        // Performance optimization settings
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1); // Process one message at a time for better load distribution

        // Enhanced error handling with logging
        factory.setErrorHandler(throwable -> {
            log.error("Error in RabbitMQ message processing: {}", throwable.getMessage(), throwable);
        });

        return factory;
    }

}