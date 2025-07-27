/**
 * Amazon Integration Service
 * 
 * Functionality:
 * - Handles all interaction logic with Amazon system
 * - Manages shipment creation, status updates, and address changes
 * - Provides functionality to send notifications to Amazon
 * 
 * Main Features:
 * - Process Amazon's ShipmentCreated messages
 * - Handle ShipmentLoaded notifications
 * - Process shipment status query requests
 * - Handle address change requests
 * - Send status update notifications to Amazon
 * 
 * Integration Pattern:
 * - Receive Amazon's HTTP REST requests
 * - Send HTTP notifications to Amazon system
 * - Maintain data consistency between both systems
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.service;

import com.miniups.model.dto.AmazonMessageDto;
import com.miniups.model.dto.ShipmentCreatedDto;
import com.miniups.model.dto.UpsResponseDto;
import com.miniups.model.event.ShipmentCreationPayload;
import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.Truck;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.TruckStatus;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.TruckRepository;
import com.miniups.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AmazonIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AmazonIntegrationService.class);
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @Autowired
    private TruckRepository truckRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TrackingService trackingService;
    
    @Autowired
    private WorldSimulatorService worldSimulatorService;
    
    @Autowired
    private TruckManagementService truckManagementService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired(required = false)
    private EventPublisherService eventPublisher;
    
    @Autowired
    private AsyncAuditService asyncAuditService;
    
    @Value("${amazon.base-url:http://host.docker.internal:8080}")
    private String amazonBaseUrl;
    
    /**
     * Handle Amazon's ShipmentCreated message
     * 
     * Business Process:
     * 1. Validate user information (find by email or create)
     * 2. Create shipment record
     * 3. Assign available truck
     * 4. Generate UPS tracking number
     * 5. Send truck to warehouse for pickup
     * 6. Return success confirmation
     */
    public UpsResponseDto handleShipmentCreated(AmazonMessageDto message) {
        try {
            logger.info("Processing ShipmentCreated message: {}", message.getPayload());
            
            // Extract payload data
            Map<String, Object> payload = message.getPayload();
            ShipmentCreatedDto dto = extractShipmentCreatedDto(payload);
            
            // Validate required fields
            if (dto.getShipmentId() == null || dto.getEmail() == null) {
                return UpsResponseDto.error(1001, "Missing required fields: shipment_id or email");
            }
            
            // Check if shipment already exists
            Optional<Shipment> existingShipment = shipmentRepository.findByShipmentId(dto.getShipmentId());
            if (existingShipment.isPresent()) {
                return UpsResponseDto.error(1002, "Shipment already exists: " + dto.getShipmentId());
            }
            
            // Find or create user
            User user = findOrCreateUser(dto);
            
            // Create shipment
            Shipment shipment = createShipment(dto, user);
            
            // Assign truck
            Truck assignedTruck = assignTruck(dto.getWarehouseId());
            if (assignedTruck == null) {
                return UpsResponseDto.error(2001, "No available trucks for pickup");
            }
            
            shipment.setTruck(assignedTruck);
            shipment.setUpsTrackingId(trackingService.generateTrackingNumber());
            shipment = shipmentRepository.save(shipment);
            
            logger.info("Created shipment {} with tracking number {}", 
                       shipment.getShipmentId(), shipment.getUpsTrackingId());
            
            // Send truck to warehouse (this would integrate with World Simulator)
            sendTruckToWarehouse(assignedTruck, dto.getWarehouseId(), shipment);
            
            return UpsResponseDto.success("Shipment created successfully");
            
        } catch (Exception e) {
            logger.error("Error processing ShipmentCreated message", e);
            return UpsResponseDto.error(3000, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle Amazon's ShipmentCreated message asynchronously
     * 
     * This method provides improved performance by:
     * 1. Quick validation and response to Amazon
     * 2. Asynchronous processing of heavy business logic
     * 3. Webhook notification when processing completes
     * 
     * Process Flow:
     * 1. Validate message format and required fields
     * 2. Generate preliminary tracking number
     * 3. Queue shipment creation event for async processing
     * 4. Return immediate response to Amazon with tracking info
     * 5. Background consumer handles actual shipment creation
     * 6. Webhook notification sent to Amazon when complete
     */
    public UpsResponseDto handleShipmentCreatedAsync(AmazonMessageDto message) {
        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();
        
        try {
            logger.info("Processing ShipmentCreated message asynchronously: {} (correlationId: {})", 
                       message.getPayload(), correlationId);
            
            // Extract payload data
            Map<String, Object> payload = message.getPayload();
            ShipmentCreatedDto dto = extractShipmentCreatedDto(payload);
            
            // Quick validation of required fields
            if (dto.getShipmentId() == null || dto.getEmail() == null) {
                asyncAuditService.auditFailure("shipment.async_create", dto.getShipmentId(), 
                                             "Missing required fields in async shipment creation", 
                                             startTime, new IllegalArgumentException("Missing required fields"));
                return UpsResponseDto.error(1001, "Missing required fields: shipment_id or email");
            }
            
            // Check if shipment already exists (quick database check)
            Optional<Shipment> existingShipment = shipmentRepository.findByShipmentId(dto.getShipmentId());
            if (existingShipment.isPresent()) {
                asyncAuditService.auditFailure("shipment.async_create", dto.getShipmentId(), 
                                             "Attempted to create duplicate shipment", 
                                             startTime, new IllegalStateException("Shipment already exists"));
                return UpsResponseDto.error(1002, "Shipment already exists: " + dto.getShipmentId());
            }
            
            // Quick user lookup or creation (lightweight operation)
            User user = findOrCreateUser(dto);
            
            // Generate tracking number immediately for response
            String trackingNumber = trackingService.generateTrackingNumber();
            
            // Create shipment creation payload for async processing
            ShipmentCreationPayload creationPayload = new ShipmentCreationPayload();
            creationPayload.setAmazonShipmentId(Long.valueOf(dto.getShipmentId()));
            creationPayload.setWarehouseId(dto.getWarehouseId().intValue());
            creationPayload.setDestX(dto.getDestinationX());
            creationPayload.setDestY(dto.getDestinationY());
            creationPayload.setUserId(user.getId());
            creationPayload.setPackageDescription("");
            // Add other relevant fields as needed
            
            // Publish event for asynchronous processing (if available)
            if (eventPublisher != null) {
                eventPublisher.publishShipmentCreationEvent(creationPayload, correlationId);
            }
            
            // Audit the successful queuing
            Map<String, Object> auditData = Map.of(
                "shipmentId", dto.getShipmentId(),
                "trackingNumber", trackingNumber,
                "warehouseId", dto.getWarehouseId(),
                "userId", user.getId(),
                "processingMode", "async"
            );
            asyncAuditService.auditSuccess("shipment.async_queued", dto.getShipmentId(), 
                                         "Shipment creation queued for async processing", 
                                         startTime, auditData);
            
            logger.info("Queued shipment {} for async processing with tracking number {} (correlationId: {})", 
                       dto.getShipmentId(), trackingNumber, correlationId);
            
            // Return immediate response with tracking information
            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("shipment_id", dto.getShipmentId());
            responsePayload.put("tracking_number", trackingNumber);
            responsePayload.put("status", "QUEUED_FOR_PROCESSING");
            responsePayload.put("estimated_processing_time", "2-5 minutes");
            responsePayload.put("correlation_id", correlationId);
            
            UpsResponseDto response = new UpsResponseDto("ShipmentQueued", responsePayload);
            return response;
            
        } catch (Exception e) {
            asyncAuditService.auditFailure("shipment.async_create", null, 
                                         "Error in async shipment creation", 
                                         startTime, e);
            logger.error("Error processing ShipmentCreated message asynchronously (correlationId: {})", 
                        correlationId, e);
            return UpsResponseDto.error(3000, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle Amazon's ShipmentLoaded message
     */
    public UpsResponseDto handleShipmentLoaded(AmazonMessageDto message) {
        try {
            String shipmentId = message.getPayloadString("shipment_id");
            
            if (shipmentId == null) {
                return UpsResponseDto.error(1001, "Missing shipment_id");
            }
            
            Optional<Shipment> shipmentOpt = shipmentRepository.findByShipmentId(shipmentId);
            if (shipmentOpt.isEmpty()) {
                return UpsResponseDto.error(2000, "Shipment not found: " + shipmentId);
            }
            
            Shipment shipment = shipmentOpt.get();
            shipment.updateStatus(ShipmentStatus.PICKED_UP);
            shipment.setPickupTime(LocalDateTime.now());
            shipmentRepository.save(shipment);
            
            // Start delivery process
            startDelivery(shipment);
            
            logger.info("Shipment {} marked as loaded and delivery started", shipmentId);
            
            return UpsResponseDto.success("Shipment loaded successfully");
            
        } catch (Exception e) {
            logger.error("Error processing ShipmentLoaded message", e);
            return UpsResponseDto.error(3000, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle Amazon's ShipmentStatusRequest message
     */
    public UpsResponseDto handleShipmentStatusRequest(AmazonMessageDto message) {
        try {
            String shipmentId = message.getPayloadString("shipment_id");
            
            if (shipmentId == null) {
                return UpsResponseDto.error(1001, "Missing shipment_id");
            }
            
            Optional<Shipment> shipmentOpt = shipmentRepository.findByShipmentId(shipmentId);
            if (shipmentOpt.isEmpty()) {
                return UpsResponseDto.error(2000, "Shipment not found: " + shipmentId);
            }
            
            Shipment shipment = shipmentOpt.get();
            
            Map<String, Object> statusPayload = new HashMap<>();
            statusPayload.put("shipment_id", shipment.getShipmentId());
            statusPayload.put("tracking_number", shipment.getUpsTrackingId());
            statusPayload.put("status", shipment.getStatus().toString());
            statusPayload.put("estimated_delivery", shipment.getEstimatedDelivery());
            statusPayload.put("actual_delivery", shipment.getActualDelivery());
            
            if (shipment.getTruck() != null) {
                statusPayload.put("truck_id", shipment.getTruck().getId());
                statusPayload.put("truck_status", shipment.getTruck().getStatus().toString());
            }
            
            UpsResponseDto response = new UpsResponseDto("ShipmentStatusResponse", statusPayload);
            
            logger.info("Provided status for shipment {}: {}", shipmentId, shipment.getStatus());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing ShipmentStatusRequest message", e);
            return UpsResponseDto.error(3000, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle Amazon's AddressChange message
     */
    public UpsResponseDto handleAddressChange(AmazonMessageDto message) {
        try {
            String shipmentId = message.getPayloadString("shipment_id");
            Integer newDestX = message.getPayloadInteger("destination_x");
            Integer newDestY = message.getPayloadInteger("destination_y");
            
            if (shipmentId == null || newDestX == null || newDestY == null) {
                return UpsResponseDto.error(1001, "Missing required fields");
            }
            
            Optional<Shipment> shipmentOpt = shipmentRepository.findByShipmentId(shipmentId);
            if (shipmentOpt.isEmpty()) {
                return UpsResponseDto.error(2000, "Shipment not found: " + shipmentId);
            }
            
            Shipment shipment = shipmentOpt.get();
            
            // Check if address can be changed
            if (!shipment.canChangeAddress()) {
                return UpsResponseDto.error(2002, "Address cannot be changed for shipment in status: " + shipment.getStatus());
            }
            
            // Update destination
            shipment.setDestX(newDestX);
            shipment.setDestY(newDestY);
            shipmentRepository.save(shipment);
            
            // If truck is already en route, update delivery destination
            if (shipment.getTruck() != null && shipment.getStatus() == ShipmentStatus.IN_TRANSIT) {
                updateTruckDestination(shipment.getTruck(), newDestX, newDestY);
            }
            
            logger.info("Updated destination for shipment {} to ({}, {})", 
                       shipmentId, newDestX, newDestY);
            
            return UpsResponseDto.success("Address updated successfully");
            
        } catch (Exception e) {
            logger.error("Error processing AddressChange message", e);
            return UpsResponseDto.error(3000, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Send truck dispatch notification to Amazon
     */
    public void notifyTruckDispatched(String truckId, String shipmentId) {
        try {
            UpsResponseDto notification = UpsResponseDto.truckDispatched(truckId, shipmentId);
            sendNotificationToAmazon("/api/webhooks/truck-dispatched", notification);
            
            logger.info("Notified Amazon: truck {} dispatched for shipment {}", truckId, shipmentId);
            
        } catch (Exception e) {
            logger.error("Error notifying Amazon of truck dispatch", e);
        }
    }
    
    /**
     * Send truck arrived at warehouse notification to Amazon
     */
    public void notifyTruckArrived(String truckId, String warehouseId, String shipmentId) {
        try {
            UpsResponseDto notification = UpsResponseDto.truckArrived(truckId, warehouseId, shipmentId);
            sendNotificationToAmazon("/api/webhooks/truck-arrived", notification);
            
            logger.info("Notified Amazon: truck {} arrived at warehouse {} for shipment {}", 
                       truckId, warehouseId, shipmentId);
            
        } catch (Exception e) {
            logger.error("Error notifying Amazon of truck arrival", e);
        }
    }
    
    /**
     * Send package delivered notification to Amazon
     */
    public void notifyShipmentDelivered(String shipmentId) {
        try {
            UpsResponseDto notification = UpsResponseDto.shipmentDelivered(shipmentId);
            sendNotificationToAmazon("/api/webhooks/shipment-delivered", notification);
            
            logger.info("Notified Amazon: shipment {} delivered", shipmentId);
            
        } catch (Exception e) {
            logger.error("Error notifying Amazon of shipment delivery", e);
        }
    }
    
    // Private helper methods
    
    private ShipmentCreatedDto extractShipmentCreatedDto(Map<String, Object> payload) {
        ShipmentCreatedDto dto = new ShipmentCreatedDto();
        
        // Handle user_id which might come as different types
        Object userIdObj = payload.get("user_id");
        if (userIdObj instanceof Number) {
            dto.setUserId(((Number) userIdObj).longValue());
        }
        
        dto.setEmail((String) payload.get("email"));
        dto.setShipmentId((String) payload.get("shipment_id"));
        
        // Handle warehouse_id
        Object warehouseIdObj = payload.get("warehouse_id");
        if (warehouseIdObj instanceof Number) {
            dto.setWarehouseId(((Number) warehouseIdObj).longValue());
        }
        
        // Handle coordinates
        Object destXObj = payload.get("destination_x");
        if (destXObj instanceof Number) {
            dto.setDestinationX(((Number) destXObj).intValue());
        }
        
        Object destYObj = payload.get("destination_y");
        if (destYObj instanceof Number) {
            dto.setDestinationY(((Number) destYObj).intValue());
        }
        
        dto.setUpsAccount((String) payload.get("ups_account"));
        
        return dto;
    }
    
    private User findOrCreateUser(ShipmentCreatedDto dto) {
        // Try to find existing user by email
        Optional<User> existingUser = userRepository.findByEmail(dto.getEmail());
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        // Create new user if not found
        User newUser = new User();
        newUser.setEmail(dto.getEmail());
        newUser.setPassword("temp_password"); // This should be handled properly in a real system
        newUser.setFirstName("Amazon User");
        newUser.setLastName("Customer");
        
        return userRepository.save(newUser);
    }
    
    private Shipment createShipment(ShipmentCreatedDto dto, User user) {
        Shipment shipment = new Shipment();
        shipment.setShipmentId(dto.getShipmentId());
        shipment.setUser(user);
        shipment.setOriginX(dto.getWarehouseId().intValue()); // Simplified: using warehouse_id as origin coordinates
        shipment.setOriginY(dto.getWarehouseId().intValue());
        shipment.setDestX(dto.getDestinationX());
        shipment.setDestY(dto.getDestinationY());
        shipment.setStatus(ShipmentStatus.CREATED);
        
        return shipment;
    }
    
    private Truck assignTruck(Long warehouseId) {
        // Use intelligent truck assignment from TruckManagementService
        // Assume warehouse coordinates - in real system this would come from warehouse data
        Integer warehouseX = warehouseId.intValue(); // Simplified mapping
        Integer warehouseY = warehouseId.intValue(); // Simplified mapping
        
        return truckManagementService.assignOptimalTruck(warehouseX, warehouseY, 3); // Normal priority
    }
    
    private void sendTruckToWarehouse(Truck truck, Long warehouseId, Shipment shipment) {
        // Send truck to warehouse via World Simulator
        if (worldSimulatorService.isConnected()) {
            try {
                // Send pickup command to World Simulator
                worldSimulatorService.sendTruckToPickup(truck.getTruckId(), warehouseId.intValue())
                    .thenAccept(success -> {
                        if (success) {
                            logger.info("Successfully sent truck {} to warehouse {}", truck.getTruckId(), warehouseId);
                            // Notify Amazon that truck has been dispatched
                            notifyTruckDispatched(truck.getId().toString(), shipment.getShipmentId());
                        } else {
                            logger.error("Failed to send truck {} to warehouse {}", truck.getTruckId(), warehouseId);
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error sending truck to warehouse", throwable);
                        return null;
                    });
            } catch (Exception e) {
                logger.error("Error calling World Simulator service", e);
                // Fallback: just notify Amazon
                notifyTruckDispatched(truck.getId().toString(), shipment.getShipmentId());
            }
        } else {
            logger.warn("World Simulator not connected, cannot send truck to warehouse");
            // Fallback: just notify Amazon
            notifyTruckDispatched(truck.getId().toString(), shipment.getShipmentId());
        }
    }
    
    private void startDelivery(Shipment shipment) {
        // Update shipment status to in transit
        shipment.updateStatus(ShipmentStatus.IN_TRANSIT);
        
        // Send delivery command to World Simulator
        if (shipment.getTruck() != null && worldSimulatorService.isConnected()) {
            try {
                // Prepare delivery locations
                Map<Long, int[]> deliveries = new HashMap<>();
                // Use shipment ID as package ID for World Simulator
                Long packageId = Long.valueOf(shipment.getShipmentId().hashCode() & 0x7FFFFFFF);
                deliveries.put(packageId, new int[]{shipment.getDestX(), shipment.getDestY()});
                
                // Send delivery command
                worldSimulatorService.sendTruckToDeliver(shipment.getTruck().getTruckId(), deliveries)
                    .thenAccept(success -> {
                        if (success) {
                            logger.info("Successfully started delivery for shipment {}", shipment.getShipmentId());
                        } else {
                            logger.error("Failed to start delivery for shipment {}", shipment.getShipmentId());
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error starting delivery via World Simulator", throwable);
                        return null;
                    });
                
                shipment.getTruck().setStatus(TruckStatus.DELIVERING);
                truckRepository.save(shipment.getTruck());
                
            } catch (Exception e) {
                logger.error("Error calling World Simulator for delivery", e);
            }
        } else {
            logger.warn("Cannot start delivery: truck not assigned or World Simulator not connected");
        }
    }
    
    private void updateTruckDestination(Truck truck, Integer newDestX, Integer newDestY) {
        // Update truck destination via World Simulator
        if (worldSimulatorService.isConnected()) {
            try {
                // Cancel current delivery and send new delivery command
                Map<Long, int[]> newDeliveries = new HashMap<>();
                // Find the shipment being delivered
                Optional<Shipment> currentShipment = shipmentRepository.findByTruck(truck);
                if (currentShipment.isPresent()) {
                    Long packageId = Long.valueOf(currentShipment.get().getShipmentId().hashCode() & 0x7FFFFFFF);
                    newDeliveries.put(packageId, new int[]{newDestX, newDestY});
                    
                    worldSimulatorService.sendTruckToDeliver(truck.getTruckId(), newDeliveries)
                        .thenAccept(success -> {
                            if (success) {
                                logger.info("Successfully updated truck {} destination to ({}, {})", truck.getTruckId(), newDestX, newDestY);
                            } else {
                                logger.error("Failed to update truck {} destination", truck.getTruckId());
                            }
                        })
                        .exceptionally(throwable -> {
                            logger.error("Error updating truck destination via World Simulator", throwable);
                            return null;
                        });
                } else {
                    logger.warn("No current shipment found for truck {}, cannot update destination", truck.getTruckId());
                }
            } catch (Exception e) {
                logger.error("Error calling World Simulator to update destination", e);
            }
        } else {
            logger.warn("World Simulator not connected, cannot update truck destination");
        }
    }
    
    private void sendNotificationToAmazon(String endpoint, UpsResponseDto notification) {
        try {
            String url = amazonBaseUrl + endpoint;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<UpsResponseDto> request = new HttpEntity<>(notification, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Successfully sent notification to Amazon: {}", endpoint);
            } else {
                logger.warn("Amazon responded with status {}: {}", 
                           response.getStatusCode(), response.getBody());
            }
            
        } catch (Exception e) {
            logger.error("Failed to send notification to Amazon at {}: {}", endpoint, e.getMessage());
        }
    }
}