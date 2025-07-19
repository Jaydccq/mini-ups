package com.miniups.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_operation_type", columnList = "operation_type"),
    @Index(name = "idx_audit_operation_timestamp", columnList = "operation_timestamp"),
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_event_id", columnList = "event_id", unique = true),
    @Index(name = "idx_audit_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_audit_result", columnList = "operation_result"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 50)
    private String eventId;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "source_service", length = 100)
    private String sourceService;

    @Column(name = "operation_type", nullable = false, length = 100)
    private String operationType;

    @Column(name = "operation_description", length = 500)
    private String operationDescription;

    @Column(name = "operation_result", nullable = false, length = 50)
    private String operationResult;

    @Column(name = "operation_timestamp", nullable = false)
    private Instant operationTimestamp;

    @Column(name = "operation_duration_ms")
    private Long operationDurationMs;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "endpoint", length = 255)
    private String endpoint;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "request_size")
    private Long requestSize;

    @Column(name = "response_size")
    private Long responseSize;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData;

    @Column(name = "audit_created_at", nullable = false)
    private Instant auditCreatedAt;

    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(operationResult) && 
               (resultCode == null || (resultCode >= 200 && resultCode < 300));
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(operationResult) || 
               (resultCode != null && resultCode >= 400);
    }

    public boolean isUserInitiated() {
        return userId != null;
    }

    public boolean hasAdditionalData() {
        return additionalData != null && !additionalData.trim().isEmpty() && 
               !"{}".equals(additionalData.trim());
    }

    public double getOperationDurationSeconds() {
        return operationDurationMs != null ? operationDurationMs / 1000.0 : 0.0;
    }

    // Setter methods for missing properties (non-duplicates)
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }
    public void setOperationResult(String operationResult) { this.operationResult = operationResult; }
    public void setOperationTimestamp(Instant operationTimestamp) { this.operationTimestamp = operationTimestamp; }
    public void setOperationDurationMs(Long operationDurationMs) { this.operationDurationMs = operationDurationMs; }
    public void setUsername(String username) { this.username = username; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public void setRequestSize(Long requestSize) { this.requestSize = requestSize; }
    public void setResponseSize(Long responseSize) { this.responseSize = responseSize; }
    public void setResultCode(Integer resultCode) { this.resultCode = resultCode; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @PrePersist
    protected void onCreate() {
        if (auditCreatedAt == null) {
            auditCreatedAt = Instant.now();
        }
    }

    public String getSummary() {
        return String.format("AuditLog{id=%d, operation='%s', user='%s', entity='%s:%s', result='%s'}", 
                getId(), operationType, username, entityType, entityId, operationResult);
    }
    
    public String getAction() {
        return operationType;
    }

    // Explicit getters and setters for Lombok compatibility
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getOperationDescription() { return operationDescription; }
    public void setOperationDescription(String operationDescription) { this.operationDescription = operationDescription; }
    
    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }
    
    public Instant getAuditCreatedAt() { return auditCreatedAt; }
    public void setAuditCreatedAt(Instant auditCreatedAt) { this.auditCreatedAt = auditCreatedAt; }
}
