package com.miniups.service.consumer;

import com.miniups.config.RabbitMQConfig;
import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.Truck;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.event.BusinessEvent;
import com.miniups.model.event.ShipmentCreationPayload;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.TruckRepository;
import com.miniups.repository.UserRepository;
import com.miniups.service.AsyncAuditService;
import com.miniups.service.EventPublisherService;
import com.miniups.service.TrackingService;
import com.miniups.service.TruckManagementService;
import com.miniups.service.WorldSimulatorService;
import com.miniups.network.netty.service.NettyWorldSimulatorService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Shipment Creation Consumer
 * 
 * Processes shipment creation events asynchronously to handle the heavy
 * business logic operations like truck assignment, world simulator
 * integration, and database transactions without blocking the API response.
 * 
 * Key Features:
 * - Asynchronous shipment processing
 * - Truck assignment and optimization
 * - World simulator integration
 * - Comprehensive error handling
 * - Idempotent message processing
 * - Automatic retry with dead letter support
 * 
 * Processing Flow:
 * 1. Receive shipment creation event from queue
 * 2. Validate payload and check for duplicates
 * 3. Create shipment entity in database
 * 4. Assign optimal truck for pickup
 * 5. Integrate with world simulator
 * 6. Send status notifications
 * 7. Audit the complete process
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024
 */
@Service
@ConditionalOnClass(RabbitListener.class)
public class ShipmentCreationConsumer {
    private static final Logger log = LoggerFactory.getLogger(ShipmentCreationConsumer.class);

    private final ShipmentRepository shipmentRepository;
    private final TruckRepository truckRepository;
    private final UserRepository userRepository;
    private final TrackingService trackingService;
    private final TruckManagementService truckManagementService;
    private final WorldSimulatorService worldSimulatorService;
    private final NettyWorldSimulatorService nettyWorldSimulatorService;
    private final EventPublisherService eventPublisher;
    private final AsyncAuditService asyncAuditService;
    
    public ShipmentCreationConsumer(ShipmentRepository shipmentRepository, TruckRepository truckRepository,
                                    UserRepository userRepository, TrackingService trackingService,
                                    TruckManagementService truckManagementService, 
                                    @Autowired(required = false) WorldSimulatorService worldSimulatorService,
                                    @Autowired(required = false) NettyWorldSimulatorService nettyWorldSimulatorService,
                                    EventPublisherService eventPublisher, AsyncAuditService asyncAuditService) {
        this.shipmentRepository = shipmentRepository;
        this.truckRepository = truckRepository;
        this.userRepository = userRepository;
        this.trackingService = trackingService;
        this.truckManagementService = truckManagementService;
        this.worldSimulatorService = worldSimulatorService;
        this.nettyWorldSimulatorService = nettyWorldSimulatorService;
        this.eventPublisher = eventPublisher;
        this.asyncAuditService = asyncAuditService;
    }
    
    /**
     * Helper method to send truck to pickup using the available service.
     */
    private void sendTruckToPickup(Integer truckId, Integer warehouseId) {
        if (worldSimulatorService != null) {
            worldSimulatorService.sendTruckToPickup(truckId, warehouseId);
        } else if (nettyWorldSimulatorService != null) {
            try {
                nettyWorldSimulatorService.sendTruckToPickup(truckId, warehouseId).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to send truck to pickup via Netty service", e);
            }
        }
    }

    /**
     * Process shipment creation events from the queue
     * 
     * This method handles the complete shipment creation workflow
     * that was queued for asynchronous processing. It performs all
     * the heavy operations that would have blocked the API response.
     * 
     * @param event The business event containing shipment creation data
     * @param message The RabbitMQ message for acknowledgment
     * @param channel The RabbitMQ channel for acknowledgment
     */
    @RabbitListener(queues = RabbitMQConfig.SHIPMENT_PROCESSOR_QUEUE)
    @Transactional
    public void handleShipmentCreationEvent(BusinessEvent<ShipmentCreationPayload> event,
                                           Message message,
                                           Channel channel) throws IOException {
        
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        long startTime = System.currentTimeMillis();
        String correlationId = event.getCorrelationId();
        
        try {
            log.info("Processing shipment creation event: {} for Amazon shipment: {} (correlationId: {})",
                    event.getEventId(), event.getPayload().getAmazonShipmentId(), correlationId);

            // Validate the event payload
            ShipmentCreationPayload payload = event.getPayload();
            if (!isValidShipmentPayload(payload)) {
                log.warn("Invalid shipment creation payload in event: {}", event.getEventId());
                // Acknowledge invalid messages to remove them from queue
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Check for duplicate processing (idempotency)
            if (isDuplicateShipment(payload.getAmazonShipmentId())) {
                log.debug("Skipping duplicate shipment creation for Amazon shipment: {}", 
                         payload.getAmazonShipmentId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Process the shipment creation
            Shipment createdShipment = processShipmentCreation(payload, correlationId, startTime);

            // Audit successful processing
            Map<String, Object> auditData = Map.of(
                "shipmentId", createdShipment.getShipmentId(),
                "upsTrackingId", createdShipment.getUpsTrackingId(),
                "truckId", createdShipment.getTruck().getId(),
                "processingMode", "async_consumer",
                "eventId", event.getEventId()
            );
            asyncAuditService.auditSuccess("shipment.async_created", createdShipment.getShipmentId(), 
                                         "Shipment successfully created via async processing", 
                                         startTime, auditData);

            log.info("Successfully processed shipment creation event: {} for shipment: {} with tracking: {} (correlationId: {})",
                    event.getEventId(), createdShipment.getShipmentId(), 
                    createdShipment.getUpsTrackingId(), correlationId);

            // Acknowledge successful processing
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process shipment creation event: {} (correlationId: {})", 
                     event.getEventId(), correlationId, e);
            
            // Audit the failure
            asyncAuditService.auditFailure("shipment.async_created", 
                                         event.getPayload().getAmazonShipmentId().toString(), 
                                         "Failed to process shipment creation in async consumer", 
                                         startTime, e);
            
            // Reject message and send to dead letter queue
            // false = don't requeue, let it go to DLQ for manual investigation
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Validate the shipment creation payload
     * Ensures all required fields are present and valid
     * 
     * @param payload The shipment creation payload to validate
     * @return true if the payload is valid for processing
     */
    private boolean isValidShipmentPayload(ShipmentCreationPayload payload) {
        if (payload == null) {
            return false;
        }

        // Check required fields using the payload's built-in validation
        return payload.isValid();
    }

    /**
     * Check if this shipment has already been processed
     * Implements idempotency by checking for existing shipments
     * 
     * @param amazonShipmentId The Amazon shipment identifier
     * @return true if this shipment has already been processed
     */
    private boolean isDuplicateShipment(Long amazonShipmentId) {
        return shipmentRepository.findByShipmentId(amazonShipmentId.toString()).isPresent();
    }

    /**
     * Process the complete shipment creation workflow
     * This is the core business logic that was moved out of the API endpoint
     * 
     * @param payload The shipment creation data
     * @param correlationId The correlation ID for tracking
     * @param startTime The processing start time
     * @return The created shipment entity
     */
    private Shipment processShipmentCreation(ShipmentCreationPayload payload, String correlationId, long startTime) {
        try {
            // Step 1: Validate and get user
            User user = getUserById(payload.getUserId());
            if (user == null) {
                throw new IllegalStateException("User not found: " + payload.getUserId());
            }

            // Step 2: Assign truck for this shipment
            Truck assignedTruck = assignTruckForShipment(payload);
            if (assignedTruck == null) {
                throw new IllegalStateException("No available trucks for pickup from warehouse: " + payload.getWarehouseId());
            }

            // Step 3: Create shipment entity
            Shipment shipment = createShipmentEntity(payload, user, assignedTruck);

            // Step 4: Save shipment to database
            shipment = shipmentRepository.save(shipment);

            // Step 5: Integrate with world simulator
            integrateWithWorldSimulator(shipment, payload, correlationId);

            // Step 6: Publish status update events
            publishShipmentStatusEvents(shipment, correlationId);

            log.info("Completed shipment creation for Amazon shipment: {} with UPS tracking: {} (correlationId: {})",
                    payload.getAmazonShipmentId(), shipment.getUpsTrackingId(), correlationId);

            return shipment;

        } catch (Exception e) {
            log.error("Error in shipment creation process for Amazon shipment: {} (correlationId: {})", 
                     payload.getAmazonShipmentId(), correlationId, e);
            throw e;
        }
    }

    /**
     * Get user by ID with validation
     */
    private User getUserById(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        return userOpt.orElse(null);
    }

    /**
     * Assign the best available truck for this shipment
     * Uses the truck management service to find optimal assignment
     */
    private Truck assignTruckForShipment(ShipmentCreationPayload payload) {
        try {
            // Use truck management service to find the best truck
            return truckManagementService.assignOptimalTruck(payload.getWarehouseId(), 0, 1);
        } catch (Exception e) {
            log.error("Failed to assign truck for warehouse: {}", payload.getWarehouseId(), e);
            return null;
        }
    }

    /**
     * Create the shipment entity from the payload data
     */
    private Shipment createShipmentEntity(ShipmentCreationPayload payload, User user, Truck truck) {
        Shipment shipment = new Shipment();
        
        // Set basic shipment information
        shipment.setShipmentId(payload.getAmazonShipmentId().toString());
        shipment.setUpsTrackingId(trackingService.generateTrackingNumber());
        shipment.setUser(user);
        shipment.setTruck(truck);
        
        // Set warehouse and destination
        shipment.setOriginX(payload.getWarehouseId());
        shipment.setOriginY(0);
        shipment.setDestX(payload.getDestX());
        shipment.setDestY(payload.getDestY());
        
        // Set package information - use amazon order id as description
        if (payload.getAmazonShipmentId() != null) {
            shipment.setAmazonOrderId(payload.getAmazonShipmentId().toString());
        }
        
        // Set initial status and timestamps
        shipment.setStatus(ShipmentStatus.CREATED);
        // createdTime is inherited from BaseEntity
        shipment.setEstimatedDelivery(calculateEstimatedDelivery(payload));
        
        return shipment;
    }

    /**
     * Calculate estimated delivery time based on distance and current conditions
     */
    private LocalDateTime calculateEstimatedDelivery(ShipmentCreationPayload payload) {
        // Simple estimation: assume 1 hour for pickup + travel time
        // In a real system, this would consider distance, traffic, truck capacity, etc.
        return LocalDateTime.now().plusHours(2);
    }

    /**
     * Integrate with world simulator for truck dispatch
     */
    private void integrateWithWorldSimulator(Shipment shipment, ShipmentCreationPayload payload, String correlationId) {
        try {
            // Send truck to warehouse for pickup
            sendTruckToPickup(
                shipment.getTruck().getTruckId(), 
                payload.getWarehouseId()
            );
            
            log.debug("Sent truck {} to warehouse {} for shipment {} (correlationId: {})",
                     shipment.getTruck().getId(), payload.getWarehouseId(), 
                     shipment.getId(), correlationId);
                     
            // Update shipment status to TRUCK_DISPATCHED after successfully sending truck
            shipment.setStatus(ShipmentStatus.TRUCK_DISPATCHED);
            log.info("Updated shipment {} status to TRUCK_DISPATCHED (correlationId: {})", 
                    shipment.getId(), correlationId);
                     
        } catch (Exception e) {
            log.warn("Failed to integrate with world simulator for shipment: {} (correlationId: {})", 
                    shipment.getId(), correlationId, e);
            // Don't fail the entire process for world simulator issues
        }
    }

    /**
     * Publish events for shipment status updates and notifications
     */
    private void publishShipmentStatusEvents(Shipment shipment, String correlationId) {
        try {
            // Publish status update for other systems
            eventPublisher.publishShipmentStatusUpdateEvent(
                shipment.getId(), 
                null, 
                shipment.getStatus().toString(), 
                correlationId
            );
            
            // TODO: Publish notification event for user
            // This would trigger email/SMS notifications to the user
            
        } catch (Exception e) {
            log.warn("Failed to publish status events for shipment: {} (correlationId: {})", 
                    shipment.getId(), correlationId, e);
            // Don't fail the entire process for event publishing issues
        }
    }
}