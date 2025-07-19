package com.miniups.service;

import com.miniups.config.RabbitMQConfig;
import com.miniups.model.event.AuditLogPayload;
import com.miniups.model.event.BusinessEvent;
import com.miniups.model.event.NotificationPayload;
import com.miniups.model.event.ShipmentCreationPayload;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Event Publisher Service
 * 
 * Centralized service for publishing business events to RabbitMQ.
 * This service provides type-safe methods for publishing different
 * types of events and handles the technical details of message routing.
 * 
 * Key Features:
 * - Type-safe event publishing
 * - Automatic routing key generation
 * - Correlation ID support for request tracing
 * - Error handling and logging
 * - Consistent event metadata
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024
 */
@Service
public class EventPublisherService {
    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);
    private final RabbitTemplate rabbitTemplate;
    
    public EventPublisherService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Value("${spring.application.name:mini-ups-backend}")
    private String sourceService;

    /**
     * Publish a shipment creation event
     * 
     * @param payload The shipment creation data
     * @param correlationId Optional correlation ID for request tracing
     */
    public void publishShipmentCreationEvent(ShipmentCreationPayload payload, String correlationId) {
        try {
            BusinessEvent<ShipmentCreationPayload> event = BusinessEvent.create(
                    RabbitMQConfig.SHIPMENT_CREATE_ROUTING_KEY,
                    sourceService,
                    payload,
                    correlationId
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TOPIC_EXCHANGE_NAME,
                    RabbitMQConfig.SHIPMENT_CREATE_ROUTING_KEY,
                    event
            );

            log.info("Published shipment creation event: {} (correlationId: {})", 
                    event.getEventId(), correlationId);

        } catch (Exception e) {
            log.error("Failed to publish shipment creation event for payload: {}", payload, e);
            throw new RuntimeException("Failed to publish shipment creation event", e);
        }
    }

    /**
     * Publish an audit log event
     * 
     * @param payload The audit log data
     * @param correlationId Optional correlation ID for request tracing
     */
    public void publishAuditLogEvent(AuditLogPayload payload, String correlationId) {
        try {
            BusinessEvent<AuditLogPayload> event = BusinessEvent.create(
                    RabbitMQConfig.AUDIT_LOG_ROUTING_KEY,
                    sourceService,
                    payload,
                    correlationId
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TOPIC_EXCHANGE_NAME,
                    RabbitMQConfig.AUDIT_LOG_ROUTING_KEY,
                    event
            );

            log.debug("Published audit log event: {} for operation: {}", 
                    event.getEventId(), payload.getOperationType());

        } catch (Exception e) {
            log.error("Failed to publish audit log event for operation: {}", 
                    payload.getOperationType(), e);
            // Don't throw exception for audit logs to avoid impacting main business flow
        }
    }

    /**
     * Publish a notification event
     * 
     * @param payload The notification data
     * @param correlationId Optional correlation ID for request tracing
     */
    public void publishNotificationEvent(NotificationPayload payload, String correlationId) {
        try {
            // Determine routing key based on notification type and priority
            String routingKey = generateNotificationRoutingKey(payload);

            BusinessEvent<NotificationPayload> event = BusinessEvent.create(
                    routingKey,
                    sourceService,
                    payload,
                    correlationId
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TOPIC_EXCHANGE_NAME,
                    routingKey,
                    event
            );

            log.info("Published notification event: {} for user: {} (types: {})", 
                    event.getEventId(), payload.getRecipientUserId(), payload.getNotificationTypes());

        } catch (Exception e) {
            log.error("Failed to publish notification event for user: {}", 
                    payload.getRecipientUserId(), e);
            // Don't throw exception for notifications to avoid impacting main business flow
        }
    }

    /**
     * Publish a shipment status update event
     * 
     * @param shipmentId The ID of the shipment
     * @param oldStatus The previous status
     * @param newStatus The new status
     * @param correlationId Optional correlation ID for request tracing
     */
    public void publishShipmentStatusUpdateEvent(Long shipmentId, String oldStatus, 
                                                String newStatus, String correlationId) {
        try {
            // Create a simple status update payload
            var statusUpdatePayload = new java.util.HashMap<String, Object>();
            statusUpdatePayload.put("shipmentId", shipmentId);
            statusUpdatePayload.put("oldStatus", oldStatus);
            statusUpdatePayload.put("newStatus", newStatus);
            statusUpdatePayload.put("timestamp", java.time.Instant.now());

            BusinessEvent<Object> event = BusinessEvent.create(
                    RabbitMQConfig.SHIPMENT_STATUS_ROUTING_KEY,
                    sourceService,
                    statusUpdatePayload,
                    correlationId
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TOPIC_EXCHANGE_NAME,
                    RabbitMQConfig.SHIPMENT_STATUS_ROUTING_KEY,
                    event
            );

            log.info("Published shipment status update event: {} for shipment: {} ({} -> {})", 
                    event.getEventId(), shipmentId, oldStatus, newStatus);

        } catch (Exception e) {
            log.error("Failed to publish shipment status update event for shipment: {}", 
                    shipmentId, e);
            // Don't throw exception to avoid impacting main business flow
        }
    }

    /**
     * Publish a user registration event
     * 
     * @param userId The ID of the newly registered user
     * @param userEmail The email of the user
     * @param correlationId Optional correlation ID for request tracing
     */
    public void publishUserRegisteredEvent(Long userId, String userEmail, String correlationId) {
        try {
            var userRegistrationPayload = new java.util.HashMap<String, Object>();
            userRegistrationPayload.put("userId", userId);
            userRegistrationPayload.put("userEmail", userEmail);
            userRegistrationPayload.put("registrationTime", java.time.Instant.now());

            BusinessEvent<Object> event = BusinessEvent.create(
                    RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                    sourceService,
                    userRegistrationPayload,
                    correlationId
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TOPIC_EXCHANGE_NAME,
                    RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                    event
            );

            log.info("Published user registration event: {} for user: {}", 
                    event.getEventId(), userId);

        } catch (Exception e) {
            log.error("Failed to publish user registration event for user: {}", userId, e);
            // Don't throw exception to avoid impacting main business flow
        }
    }

    /**
     * Publish a truck dispatch event
     * 
     * @param truckId The ID of the truck being dispatched
     * @param shipmentIds List of shipment IDs assigned to the truck
     * @param correlationId Optional correlation ID for request tracing
     */
    public void publishTruckDispatchEvent(Long truckId, java.util.List<Long> shipmentIds, 
                                        String correlationId) {
        try {
            var truckDispatchPayload = new java.util.HashMap<String, Object>();
            truckDispatchPayload.put("truckId", truckId);
            truckDispatchPayload.put("shipmentIds", shipmentIds);
            truckDispatchPayload.put("dispatchTime", java.time.Instant.now());

            BusinessEvent<Object> event = BusinessEvent.create(
                    RabbitMQConfig.TRUCK_DISPATCH_ROUTING_KEY,
                    sourceService,
                    truckDispatchPayload,
                    correlationId
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TOPIC_EXCHANGE_NAME,
                    RabbitMQConfig.TRUCK_DISPATCH_ROUTING_KEY,
                    event
            );

            log.info("Published truck dispatch event: {} for truck: {} with {} shipments", 
                    event.getEventId(), truckId, shipmentIds.size());

        } catch (Exception e) {
            log.error("Failed to publish truck dispatch event for truck: {}", truckId, e);
            // Don't throw exception to avoid impacting main business flow
        }
    }

    /**
     * Generate appropriate routing key for notification events
     * Based on notification type and priority
     */
    private String generateNotificationRoutingKey(NotificationPayload payload) {
        String baseKey = "notification";
        
        if (payload.getPriority() == NotificationPayload.Priority.URGENT) {
            baseKey += ".urgent";
        } else if (payload.getPriority() == NotificationPayload.Priority.HIGH) {
            baseKey += ".high";
        } else {
            baseKey += ".normal";
        }

        if (payload.getCategory() != null) {
            baseKey += "." + payload.getCategory();
        }

        return baseKey;
    }
}