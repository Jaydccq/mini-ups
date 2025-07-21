package com.miniups.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

/**
 * RabbitMQ Configuration
 * 
 * Configures the RabbitMQ topology, message converters, and connection settings
 * for the Mini-UPS messaging infrastructure. This configuration implements
 * an event-driven architecture with reliable message processing.
 * 
 * Topology Design:
 * - Topic Exchange: ups.events.topic (main routing hub)
 * - Dead Letter Exchange: ups.events.dlx (reliability mechanism)
 * - Specialized queues for different business domains
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024
 */
@Configuration
@Profile("!test")
public class RabbitMQConfig {

    // Exchange Names
    public static final String TOPIC_EXCHANGE_NAME = "ups.events.topic";
    public static final String DLX_NAME = "ups.events.dlx";

    // Queue Names
    public static final String SHIPMENT_PROCESSOR_QUEUE = "q.shipment.processor";
    public static final String NOTIFICATIONS_QUEUE = "q.notifications";
    public static final String AUDIT_LOG_QUEUE = "q.audit_log";
    public static final String WORLD_SIMULATOR_QUEUE = "q.world_simulator";
    public static final String DEAD_LETTER_QUEUE = "q.dead_letter";

    // Routing Keys
    public static final String SHIPMENT_CREATE_ROUTING_KEY = "shipment.create.request";
    public static final String SHIPMENT_STATUS_ROUTING_KEY = "shipment.status.updated";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String TRUCK_DISPATCH_ROUTING_KEY = "truck.dispatch";
    public static final String AUDIT_LOG_ROUTING_KEY = "audit.log.created";

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
     * RabbitTemplate for sending messages
     * Configured with JSON converter and reliability features
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                       MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setExchange(TOPIC_EXCHANGE_NAME);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // Log failed message publication
                System.err.println("Message failed to reach exchange: " + cause);
            }
        });
        template.setReturnsCallback(returnedMessage -> {
            // Log returned messages (no matching queue)
            System.err.println("Message returned: " + returnedMessage);
        });
        return template;
    }

    /**
     * Container factory for message listeners
     * Configures manual acknowledgment and retry policies
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter messageConverter) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        
        // Configure error handling
        factory.setErrorHandler(throwable -> {
            System.err.println("Error in message processing: " + throwable.getMessage());
            throwable.printStackTrace();
        });
        
        return factory;
    }
}