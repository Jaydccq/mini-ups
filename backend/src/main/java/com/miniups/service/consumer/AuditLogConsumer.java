package com.miniups.service.consumer;

import com.miniups.config.RabbitMQConfig;
import com.miniups.model.entity.AuditLog;
import com.miniups.model.event.AuditLogPayload;
import com.miniups.model.event.BusinessEvent;
import com.miniups.repository.AuditLogRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;

/**
 * Audit Log Consumer
 * 
 * Processes audit log events asynchronously to record system operations
 * for compliance, monitoring, and debugging purposes. This consumer
 * ensures that audit information is captured without impacting
 * the performance of main business operations.
 * 
 * Key Features:
 * - Asynchronous audit log processing
 * - Idempotent message handling
 * - Error handling with dead letter support
 * - Performance optimization for high-volume logging
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024
 */
@Service
@ConditionalOnClass(RabbitListener.class)
@Profile("!rabbitmq-disabled")
public class AuditLogConsumer {
    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);

    private final AuditLogRepository auditLogRepository;
    
    public AuditLogConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Process audit log events from the queue
     * 
     * This method handles incoming audit log events, validates them,
     * and persists them to the database. It uses manual acknowledgment
     * to ensure reliable message processing.
     * 
     * @param event The business event containing audit log data
     * @param message The RabbitMQ message for acknowledgment
     * @param channel The RabbitMQ channel for acknowledgment
     */
    @RabbitListener(queues = RabbitMQConfig.AUDIT_LOG_QUEUE)
    @Transactional
    public void handleAuditLogEvent(BusinessEvent<AuditLogPayload> event, 
                                   Message message, 
                                   Channel channel) throws IOException {
        
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.debug("Processing audit log event: {} for operation: {}", 
                    event.getEventId(), event.getPayload().getOperationType());

            // Check for duplicate processing (idempotency)
            if (isDuplicateEvent(event.getEventId())) {
                log.debug("Skipping duplicate audit log event: {}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Validate the event payload
            AuditLogPayload payload = event.getPayload();
            if (!isValidAuditLogPayload(payload)) {
                log.warn("Invalid audit log payload in event: {}", event.getEventId());
                // Acknowledge invalid messages to remove them from queue
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Create and save audit log entity
            AuditLog auditLog = createAuditLogFromPayload(payload, event);
            auditLogRepository.save(auditLog);

            log.debug("Successfully processed audit log event: {} for operation: {}", 
                    event.getEventId(), payload.getOperationType());

            // Acknowledge successful processing
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process audit log event: {}", event.getEventId(), e);
            
            // Reject message and send to dead letter queue
            // false = don't requeue, let it go to DLQ for manual investigation
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Check if this event has already been processed
     * Implements idempotency by checking for existing audit logs with the same event ID
     * 
     * @param eventId The unique event identifier
     * @return true if this event has already been processed
     */
    private boolean isDuplicateEvent(String eventId) {
        // Check if we already have an audit log with this event ID
        return auditLogRepository.existsByEventId(eventId);
    }

    /**
     * Validate the audit log payload
     * Ensures that required fields are present and valid
     * 
     * @param payload The audit log payload to validate
     * @return true if the payload is valid for processing
     */
    private boolean isValidAuditLogPayload(AuditLogPayload payload) {
        if (payload == null) {
            return false;
        }

        // Check required fields
        if (payload.getOperationType() == null || payload.getOperationType().trim().isEmpty()) {
            log.warn("Audit log payload missing operation type");
            return false;
        }

        if (payload.getOperationResult() == null || payload.getOperationResult().trim().isEmpty()) {
            log.warn("Audit log payload missing operation result");
            return false;
        }

        // Validate operation timestamp
        if (payload.getOperationTimestamp() == null) {
            log.warn("Audit log payload missing operation timestamp");
            return false;
        }

        return true;
    }

    /**
     * Create an AuditLog entity from the event payload
     * Maps the payload data to the database entity
     * 
     * @param payload The audit log payload
     * @param event The business event containing metadata
     * @return A new AuditLog entity ready for persistence
     */
    private AuditLog createAuditLogFromPayload(AuditLogPayload payload, BusinessEvent<AuditLogPayload> event) {
        AuditLog auditLog = new AuditLog();
        
        // Event metadata
        auditLog.setEventId(event.getEventId());
        auditLog.setCorrelationId(event.getCorrelationId());
        auditLog.setEventTime(event.getEventTime());
        auditLog.setSourceService(event.getSourceService());
        
        // Operation details
        auditLog.setOperationType(payload.getOperationType());
        auditLog.setOperationDescription(payload.getOperationDescription());
        auditLog.setOperationResult(payload.getOperationResult());
        auditLog.setOperationTimestamp(payload.getOperationTimestamp());
        auditLog.setOperationDurationMs(payload.getOperationDurationMs());
        
        // User information
        auditLog.setUserId(payload.getUserId());
        auditLog.setUsername(payload.getUsername());
        auditLog.setSessionId(payload.getSessionId());
        
        // Request information
        auditLog.setIpAddress(payload.getIpAddress());
        auditLog.setUserAgent(payload.getUserAgent());
        auditLog.setEndpoint(payload.getEndpoint());
        auditLog.setHttpMethod(payload.getHttpMethod());
        auditLog.setRequestSize(payload.getRequestSize());
        auditLog.setResponseSize(payload.getResponseSize());
        
        // Entity information
        auditLog.setEntityId(payload.getEntityId());
        auditLog.setEntityType(payload.getEntityType());
        
        // Result information
        auditLog.setResultCode(payload.getResultCode());
        auditLog.setErrorMessage(payload.getErrorMessage());
        
        // Additional data (stored as JSON)
        if (payload.hasAdditionalData()) {
            try {
                auditLog.setAdditionalData(
                    new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(payload.getAdditionalData())
                );
            } catch (Exception e) {
                log.warn("Failed to serialize additional data for audit log: {}", event.getEventId(), e);
                auditLog.setAdditionalData("{}");
            }
        }
        
        // Set creation timestamp
        auditLog.setAuditCreatedAt(Instant.now());
        
        return auditLog;
    }
}