/**
 * Amazon Integration API Controller
 * 
 * Features:
 * - Provides REST API endpoints for receiving Amazon system messages
 * - Handles various types of messages sent by Amazon
 * - Returns standardized response format
 * 
 * API Endpoints:
 * - POST /api/shipment - Receives ShipmentCreated messages
 * - POST /api/shipment_loaded - Receives ShipmentLoaded messages
 * - POST /api/shipment_status - Receives ShipmentStatusRequest messages
 * - POST /api/address_change - Receives AddressChange messages
 * 
 * Message Processing Flow:
 * 1. Receive HTTP request from Amazon
 * 2. Validate message format and required fields
 * 3. Call appropriate business service for processing
 * 4. Return standardized response
 * 5. Log processing results
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.controller;

import com.miniups.model.dto.AmazonMessageDto;
import com.miniups.model.dto.UpsResponseDto;
import com.miniups.model.dto.common.ApiResponse;
import com.miniups.service.AmazonIntegrationService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AmazonIntegrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AmazonIntegrationController.class);
    
    @Autowired
    private AmazonIntegrationService amazonIntegrationService;
    
    /**
     * Receives Amazon's ShipmentCreated message (Asynchronous Processing)
     * 
     * Amazon calls this endpoint to create new shipping orders.
     * This endpoint now processes requests asynchronously for better performance
     * and scalability. The request is quickly validated and queued for processing,
     * with status updates sent via webhook when processing completes.
     * 
     * @param message Complete message body sent by Amazon
     * @return UPS standard response with tracking information
     */
    @PostMapping("/shipment")
    public ResponseEntity<UpsResponseDto> handleShipmentCreated(@Valid @RequestBody AmazonMessageDto message) {
        logger.info("Received ShipmentCreated message from Amazon: {}", message.getPayload());
        
        // Validate message type
        if (!message.isShipmentCreated()) {
            UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                "Invalid message type. Expected 'ShipmentCreated', got: " + message.getMessageType());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Process the message asynchronously
        UpsResponseDto response = amazonIntegrationService.handleShipmentCreatedAsync(message);
        
        // Return appropriate HTTP status
        if (response.isError()) {
            return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
        }
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Receives Amazon's ShipmentLoaded message
     * 
     * Amazon calls this endpoint to notify that packages have been loaded onto truck
     * 
     * @param message Complete message body sent by Amazon
     * @return UPS standard response
     */
    @PostMapping("/shipment_loaded")
    public ResponseEntity<UpsResponseDto> handleShipmentLoaded(@Valid @RequestBody AmazonMessageDto message) {
        logger.info("Received ShipmentLoaded message from Amazon: {}", message.getPayload());
        
        // Validate message type
        if (!message.isShipmentLoaded()) {
            UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                "Invalid message type. Expected 'ShipmentLoaded', got: " + message.getMessageType());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Process the message
        UpsResponseDto response = amazonIntegrationService.handleShipmentLoaded(message);
        
        // Return appropriate HTTP status
        if (response.isError()) {
            return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Receives Amazon's ShipmentStatusRequest message
     * 
     * Amazon calls this endpoint to query shipping order status
     * 
     * @param message Complete message body sent by Amazon
     * @return Response containing shipping status information
     */
    @PostMapping("/shipment_status")
    public ResponseEntity<UpsResponseDto> handleShipmentStatusRequest(@Valid @RequestBody AmazonMessageDto message) {
        logger.info("Received ShipmentStatusRequest message from Amazon: {}", message.getPayload());
        
        // Validate message type
        if (!message.isShipmentStatusRequest()) {
            UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                "Invalid message type. Expected 'ShipmentStatusRequest', got: " + message.getMessageType());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Process the message
        UpsResponseDto response = amazonIntegrationService.handleShipmentStatusRequest(message);
        
        // Return appropriate HTTP status
        if (response.isError()) {
            return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Receives Amazon's AddressChange message
     * 
     * Amazon calls this endpoint to request delivery address changes
     * 
     * @param message Complete message body sent by Amazon
     * @return UPS standard response
     */
    @PostMapping("/address_change")
    public ResponseEntity<UpsResponseDto> handleAddressChange(@Valid @RequestBody AmazonMessageDto message) {
        logger.info("Received AddressChange message from Amazon: {}", message.getPayload());
        
        // Validate message type
        if (!message.isAddressChange()) {
            UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                "Invalid message type. Expected 'AddressChange', got: " + message.getMessageType());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Process the message
        UpsResponseDto response = amazonIntegrationService.handleAddressChange(message);
        
        // Return appropriate HTTP status
        if (response.isError()) {
            return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generic message processing endpoint (backup)
     * 
     * Can handle any type of Amazon message, routing to appropriate handler based on message_type
     */
    @PostMapping("/amazon/message")
    public ResponseEntity<UpsResponseDto> handleGenericMessage(@RequestBody Map<String, Object> rawMessage) {
        // Convert to standard message format
        AmazonMessageDto message = new AmazonMessageDto();
        message.setMessageType((String) rawMessage.get("message_type"));
        message.setTimestamp(LocalDateTime.now()); // Use current time if not provided
        
        // Safe cast with type checking
        Object payloadObj = rawMessage.get("payload");
        if (payloadObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) payloadObj;
            message.setPayload(payload);
        } else {
            message.setPayload(null);
        }
        
        // Route to appropriate handler
        if (message.isShipmentCreated()) {
            return handleShipmentCreated(message);
        } else if (message.isShipmentLoaded()) {
            return handleShipmentLoaded(message);
        } else if (message.isShipmentStatusRequest()) {
            return handleShipmentStatusRequest(message);
        } else if (message.isAddressChange()) {
            return handleAddressChange(message);
        } else {
            UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                "Unsupported message type: " + message.getMessageType());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint
     * 
     * Amazon can call this endpoint to check if UPS system is running normally
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "UPS Integration API",
            "version", "1.0.0"
        );
        
        return ResponseEntity.ok(health);
    }
}