package com.miniups.model.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class BusinessEvent<T> {

    private String eventId;
    private String eventType;
    private Instant eventTime;
    private String sourceService;
    private String correlationId;
    private T payload;
    private String version;

    public BusinessEvent() {}

    @JsonCreator
    public BusinessEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventTime") Instant eventTime,
            @JsonProperty("sourceService") String sourceService,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("payload") T payload,
            @JsonProperty("version") String version) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.sourceService = sourceService;
        this.correlationId = correlationId;
        this.payload = payload;
        this.version = version;
    }

    public static <T> BusinessEvent<T> create(String eventType, String sourceService, T payload, String correlationId) {
        BusinessEvent<T> event = new BusinessEvent<T>();
        event.eventId = UUID.randomUUID().toString();
        event.eventType = eventType;
        event.eventTime = Instant.now();
        event.sourceService = sourceService;
        event.correlationId = correlationId;
        event.payload = payload;
        event.version = "1.0";
        return event;
    }

    public boolean hasCorrelationId() {
        return correlationId != null && !correlationId.trim().isEmpty();
    }

    // Explicit getters and setters for Lombok compatibility
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    
    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    @Override
    public String toString() {
        return String.format("BusinessEvent{eventId='%s', eventType='%s', sourceService='%s', eventTime=%s}", 
                eventId, eventType, sourceService, eventTime);
    }
}