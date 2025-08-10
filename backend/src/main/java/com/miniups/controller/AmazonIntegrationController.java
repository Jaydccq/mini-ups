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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.model.dto.AmazonMessageDto;
import com.miniups.model.dto.UpsResponseDto;
import com.miniups.model.dto.common.ApiResponse;
import com.miniups.service.AmazonIntegrationService;
import com.miniups.service.CommunicationLogService;
import com.miniups.model.entity.CommunicationLog;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AmazonIntegrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AmazonIntegrationController.class);
    
    @Autowired
    private AmazonIntegrationService amazonIntegrationService;
    
    @Autowired
    private CommunicationLogService communicationLogService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Receives Amazon's ShipmentCreated message (Asynchronous Processing)
     * 
     * Amazon calls this endpoint to create new shipping orders.
     * This endpoint now processes requests asynchronously for better performance
     * and scalability. The request is quickly validated and queued for processing,
     * with status updates sent via webhook when processing completes.
     * 
     * @param rawJson Raw JSON string sent by Amazon
     * @return UPS standard response with tracking information
     */
    @PostMapping("/shipment")
    public ResponseEntity<UpsResponseDto> handleShipmentCreated(@RequestBody String rawJson) {
        long startTime = System.currentTimeMillis();
        CommunicationLog log = null;
        
        logger.info("Raw Amazon JSON received: {}", rawJson);
        
        try {
            // Parse JSON string to Map
            @SuppressWarnings("unchecked")
            Map<String, Object> rawMessage = objectMapper.readValue(rawJson, Map.class);
            
            // Log incoming message
            log = communicationLogService.logIncomingMessage("ShipmentCreated", "/api/shipment", rawMessage);
            
            // Convert Amazon format to UPS format
            AmazonMessageDto message = convertAmazonMessage(rawMessage);
            logger.info("Converted to ShipmentCreated message: {}", message.getMessageType());
            
            // Validate message type
            if (!message.isShipmentCreated()) {
                UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                    "Invalid message type. Expected 'ShipmentCreated', got: " + message.getMessageType());
                
                if (log != null) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    communicationLogService.updateLogWithResponse(log, errorResponse, 400, processingTime);
                }
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Process the message synchronously for reliability
            UpsResponseDto response = amazonIntegrationService.handleShipmentCreated(message);
            
            // Update log with response
            if (log != null) {
                long processingTime = System.currentTimeMillis() - startTime;
                int statusCode = response.isError() ? (response.getCode() >= 500 ? 500 : 400) : 200;
                communicationLogService.updateLogWithResponse(log, response, statusCode, processingTime);
            }
            
            // Return appropriate HTTP status
            if (response.isError()) {
                return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON: {}", e.getMessage());
            UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                "Invalid JSON format: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
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
        long startTime = System.currentTimeMillis();
        CommunicationLog log = null;
        
        logger.info("Received ShipmentLoaded message from Amazon: {}", message.getPayload());
        
        try {
            // Log incoming message
            log = communicationLogService.logIncomingMessage("ShipmentLoaded", "/api/shipment_loaded", message);
            
            // Validate message type
            if (!message.isShipmentLoaded()) {
                UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                    "Invalid message type. Expected 'ShipmentLoaded', got: " + message.getMessageType());
                
                if (log != null) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    communicationLogService.updateLogWithResponse(log, errorResponse, 400, processingTime);
                }
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Process the message
            UpsResponseDto response = amazonIntegrationService.handleShipmentLoaded(message);
            
            // Update log with response
            if (log != null) {
                long processingTime = System.currentTimeMillis() - startTime;
                int statusCode = response.isError() ? (response.getCode() >= 500 ? 500 : 400) : 200;
                communicationLogService.updateLogWithResponse(log, response, statusCode, processingTime);
            }
            
            // Return appropriate HTTP status
            if (response.isError()) {
                return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing ShipmentLoaded message", e);
            UpsResponseDto errorResponse = UpsResponseDto.error(3000, "Internal server error: " + e.getMessage());
            
            if (log != null) {
                long processingTime = System.currentTimeMillis() - startTime;
                communicationLogService.updateLogWithResponse(log, errorResponse, 500, processingTime);
            }
            
            return ResponseEntity.status(500).body(errorResponse);
        }
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
        long startTime = System.currentTimeMillis();
        CommunicationLog log = null;
        
        logger.info("Received ShipmentStatusRequest message from Amazon: {}", message.getPayload());
        
        try {
            // Log incoming message
            log = communicationLogService.logIncomingMessage("ShipmentStatusRequest", "/api/shipment_status", message);
            
            // Validate message type
            if (!message.isShipmentStatusRequest()) {
                UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                    "Invalid message type. Expected 'ShipmentStatusRequest', got: " + message.getMessageType());
                
                if (log != null) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    communicationLogService.updateLogWithResponse(log, errorResponse, 400, processingTime);
                }
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Process the message
            UpsResponseDto response = amazonIntegrationService.handleShipmentStatusRequest(message);
            
            // Update log with response
            if (log != null) {
                long processingTime = System.currentTimeMillis() - startTime;
                int statusCode = response.isError() ? (response.getCode() >= 500 ? 500 : 400) : 200;
                communicationLogService.updateLogWithResponse(log, response, statusCode, processingTime);
            }
            
            // Return appropriate HTTP status
            if (response.isError()) {
                return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing ShipmentStatusRequest message", e);
            UpsResponseDto errorResponse = UpsResponseDto.error(3000, "Internal server error: " + e.getMessage());
            
            if (log != null) {
                long processingTime = System.currentTimeMillis() - startTime;
                communicationLogService.updateLogWithResponse(log, errorResponse, 500, processingTime);
            }
            
            return ResponseEntity.status(500).body(errorResponse);
        }
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
        long startTime = System.currentTimeMillis();
        CommunicationLog log = null;
        
        logger.info("Received AddressChange message from Amazon: {}", message.getPayload());
        
        try {
            // Log incoming message
            log = communicationLogService.logIncomingMessage("AddressChange", "/api/address_change", message);
            
            // Validate message type
            if (!message.isAddressChange()) {
                UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                    "Invalid message type. Expected 'AddressChange', got: " + message.getMessageType());
                
                if (log != null) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    communicationLogService.updateLogWithResponse(log, errorResponse, 400, processingTime);
                }
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Process the message
            UpsResponseDto response = amazonIntegrationService.handleAddressChange(message);
            
            // Update log with response
            if (log != null) {
                long processingTime = System.currentTimeMillis() - startTime;
                int statusCode = response.isError() ? (response.getCode() >= 500 ? 500 : 400) : 200;
                communicationLogService.updateLogWithResponse(log, response, statusCode, processingTime);
            }
            
            // Return appropriate HTTP status
            if (response.isError()) {
                return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing AddressChange message", e);
            UpsResponseDto errorResponse = UpsResponseDto.error(3000, "Internal server error: " + e.getMessage());
            
            if (log != null) {
                long processingTime = System.currentTimeMillis() - startTime;
                communicationLogService.updateLogWithResponse(log, errorResponse, 500, processingTime);
            }
            
            return ResponseEntity.status(500).body(errorResponse);
        }
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
            return processShipmentCreated(message);
        } else if (message.isShipmentLoaded()) {
            return processShipmentLoaded(message);
        } else if (message.isShipmentStatusRequest()) {
            return processShipmentStatusRequest(message);
        } else if (message.isAddressChange()) {
            return processAddressChange(message);
        } else {
            UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                "Unsupported message type: " + message.getMessageType());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Process ShipmentCreated message (internal method for generic handler)
     * Note: This method bypasses validation since the message was manually converted
     */
    private ResponseEntity<UpsResponseDto> processShipmentCreated(AmazonMessageDto message) {
        logger.info("Processing ShipmentCreated message: {}", message.getPayload());
        
        // Validate manually since we bypass Spring validation
        if (message.getMessageType() == null || message.getMessageType().trim().isEmpty()) {
            UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                "Message type is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (message.getPayload() == null) {
            UpsResponseDto errorResponse = UpsResponseDto.error(1000, 
                "Payload is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Process the message synchronously for reliability
        UpsResponseDto response = amazonIntegrationService.handleShipmentCreated(message);
        
        // Return appropriate HTTP status
        if (response.isError()) {
            return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Process ShipmentLoaded message (internal method for generic handler)
     */
    private ResponseEntity<UpsResponseDto> processShipmentLoaded(AmazonMessageDto message) {
        logger.info("Processing ShipmentLoaded message: {}", message.getPayload());
        
        // Process the message
        UpsResponseDto response = amazonIntegrationService.handleShipmentLoaded(message);
        
        // Return appropriate HTTP status
        if (response.isError()) {
            return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Process ShipmentStatusRequest message (internal method for generic handler)
     */
    private ResponseEntity<UpsResponseDto> processShipmentStatusRequest(AmazonMessageDto message) {
        logger.info("Processing ShipmentStatusRequest message: {}", message.getPayload());
        
        // Process the message
        UpsResponseDto response = amazonIntegrationService.handleShipmentStatusRequest(message);
        
        // Return appropriate HTTP status
        if (response.isError()) {
            return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Process AddressChange message (internal method for generic handler)
     */
    private ResponseEntity<UpsResponseDto> processAddressChange(AmazonMessageDto message) {
        logger.info("Processing AddressChange message: {}", message.getPayload());
        
        // Process the message
        UpsResponseDto response = amazonIntegrationService.handleAddressChange(message);
        
        // Return appropriate HTTP status
        if (response.isError()) {
            return ResponseEntity.status(response.getCode() >= 500 ? 500 : 400).body(response);
        }
        
        return ResponseEntity.ok(response);
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
    
    /**
     * Convert Amazon message format to UPS message format
     * Handles snake_case to camelCase conversion and timestamp parsing
     */
    private AmazonMessageDto convertAmazonMessage(Map<String, Object> rawMessage) {
        logger.info("Converting raw Amazon message: {}", rawMessage);
        AmazonMessageDto dto = new AmazonMessageDto();
        
        // Handle message_type -> messageType
        String messageType = (String) rawMessage.get("message_type");
        if (messageType == null) {
            messageType = (String) rawMessage.get("messageType");
        }
        logger.info("Extracted messageType: {}", messageType);
        dto.setMessageType(messageType);
        
        // Handle timestamp
        Object timestampObj = rawMessage.get("timestamp");
        LocalDateTime timestamp = null;
        if (timestampObj instanceof String) {
            String timestampStr = (String) timestampObj;
            try {
                // Handle Amazon's timestamp format
                if (timestampStr.contains(".")) {
                    timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"));
                } else {
                    timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                }
            } catch (Exception e) {
                logger.warn("Failed to parse timestamp: {}, using current time", timestampStr);
                timestamp = LocalDateTime.now();
            }
        } else {
            timestamp = LocalDateTime.now();
        }
        dto.setTimestamp(timestamp);
        
        // Handle payload
        Object payloadObj = rawMessage.get("payload");
        if (payloadObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) payloadObj;
            dto.setPayload(payload);
        } else {
            dto.setPayload(null);
            logger.warn("Received payload is not a JSON object. Type: {}. Setting payload to null.",
                    payloadObj != null ? payloadObj.getClass().getName() : "null");
        }
        
        logger.info("Converted DTO: messageType={}, timestamp={}, payload={}", 
            dto.getMessageType(), dto.getTimestamp(), dto.getPayload());
        
        return dto;
    }
}