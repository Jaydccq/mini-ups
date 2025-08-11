/**
 * Amazon Message Data Transfer Object
 * 
 * Functionality:
 * - Defines standard message format for communication between Amazon and UPS systems
 * - Supports all types of Amazon messages (ShipmentCreated, ShipmentLoaded, etc.)
 * - Provides message validation and serialization functionality
 * 
 * Message Types:
 * - ShipmentCreated: Amazon creates shipping order
 * - ShipmentLoaded: Amazon notifies package has been loaded
 * - ShipmentStatusRequest: Amazon queries shipping status
 * - AddressChange: Amazon requests address change
 * 
 *
 
 */
package com.miniups.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.Map;

public class AmazonMessageDto {
    
    @NotBlank(message = "Message type is required")
    @Pattern(regexp = "ShipmentCreated|ShipmentLoaded|ShipmentStatusRequest|AddressChange", 
             message = "Invalid message type")
    @JsonProperty("message_type")
    private String messageType;
    
    @NotNull(message = "Timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;
    
    @NotNull(message = "Payload is required")
    private Map<String, Object> payload;
    
    // Constructors
    public AmazonMessageDto() {}
    
    public AmazonMessageDto(String messageType, LocalDateTime timestamp, Map<String, Object> payload) {
        this.messageType = messageType;
        this.timestamp = timestamp;
        this.payload = payload;
    }
    
    // Getters and Setters
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
    
    // Helper methods
    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key, Class<T> type) {
        Object value = payload.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    public String getPayloadString(String key) {
        return getPayloadValue(key, String.class);
    }
    
    public Integer getPayloadInteger(String key) {
        Object value = payload.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    public boolean isShipmentCreated() {
        return "ShipmentCreated".equals(messageType);
    }
    
    public boolean isShipmentLoaded() {
        return "ShipmentLoaded".equals(messageType);
    }
    
    public boolean isShipmentStatusRequest() {
        return "ShipmentStatusRequest".equals(messageType);
    }
    
    public boolean isAddressChange() {
        return "AddressChange".equals(messageType);
    }
}