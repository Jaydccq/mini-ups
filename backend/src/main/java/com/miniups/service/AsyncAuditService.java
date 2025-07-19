package com.miniups.service;

import com.miniups.model.event.AuditLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;

/**
 * Asynchronous Audit Service
 * 
 * Provides high-level methods for auditing system operations without
 * blocking the main business flow. This service creates audit events
 * that are processed asynchronously by the audit log consumer.
 * 
 * Key Features:
 * - Non-blocking audit logging
 * - Automatic request context extraction
 * - Flexible metadata support
 * - Operation timing utilities
 * - Error-safe execution (won't break main flow)
 * 
 * Usage Example:
 * ```java
 * @Autowired
 * private AsyncAuditService auditService;
 * 
 * public void createShipment(ShipmentDto dto) {
 *     long startTime = System.currentTimeMillis();
 *     try {
 *         // ... business logic ...
 *         auditService.auditSuccess("shipment.created", shipmentId, 
 *                                 "Created shipment for user", startTime);
 *     } catch (Exception e) {
 *         auditService.auditFailure("shipment.created", null, 
 *                                 "Failed to create shipment", startTime, e);
 *         throw e;
 *     }
 * }
 * ```
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
public class AsyncAuditService {
    private static final Logger log = LoggerFactory.getLogger(AsyncAuditService.class);
    private final EventPublisherService eventPublisher;
    
    public AsyncAuditService(EventPublisherService eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Audit a successful operation
     * 
     * @param operationType The type of operation (e.g., "user.login", "shipment.created")
     * @param entityId The ID of the primary entity affected
     * @param description Human-readable description of what was done
     * @param startTimeMs Operation start time in milliseconds (for duration calculation)
     */
    public void auditSuccess(String operationType, Object entityId, String description, long startTimeMs) {
        auditOperation(operationType, entityId, description, "SUCCESS", null, null, startTimeMs, null);
    }

    /**
     * Audit a successful operation with additional metadata
     * 
     * @param operationType The type of operation
     * @param entityId The ID of the primary entity affected
     * @param description Human-readable description of what was done
     * @param startTimeMs Operation start time in milliseconds
     * @param additionalData Extra context data for this operation
     */
    public void auditSuccess(String operationType, Object entityId, String description, 
                           long startTimeMs, Map<String, Object> additionalData) {
        auditOperation(operationType, entityId, description, "SUCCESS", null, null, startTimeMs, additionalData);
    }

    /**
     * Audit a failed operation
     * 
     * @param operationType The type of operation
     * @param entityId The ID of the primary entity affected (may be null for failures)
     * @param description Human-readable description of what was attempted
     * @param startTimeMs Operation start time in milliseconds
     * @param error The exception that caused the failure
     */
    public void auditFailure(String operationType, Object entityId, String description, 
                           long startTimeMs, Throwable error) {
        String errorMessage = error != null ? error.getMessage() : "Unknown error";
        auditOperation(operationType, entityId, description, "FAILED", errorMessage, null, startTimeMs, null);
    }

    /**
     * Audit a failed operation with additional context
     * 
     * @param operationType The type of operation
     * @param entityId The ID of the primary entity affected (may be null for failures)
     * @param description Human-readable description of what was attempted
     * @param startTimeMs Operation start time in milliseconds
     * @param error The exception that caused the failure
     * @param additionalData Extra context data for this operation
     */
    public void auditFailure(String operationType, Object entityId, String description, 
                           long startTimeMs, Throwable error, Map<String, Object> additionalData) {
        String errorMessage = error != null ? error.getMessage() : "Unknown error";
        auditOperation(operationType, entityId, description, "FAILED", errorMessage, null, startTimeMs, additionalData);
    }

    /**
     * Audit an operation with custom result
     * 
     * @param operationType The type of operation
     * @param entityId The ID of the primary entity affected
     * @param description Human-readable description of what was done
     * @param result The result of the operation (SUCCESS, FAILED, PARTIAL, etc.)
     * @param startTimeMs Operation start time in milliseconds
     */
    public void auditWithResult(String operationType, Object entityId, String description, 
                              String result, long startTimeMs) {
        auditOperation(operationType, entityId, description, result, null, null, startTimeMs, null);
    }

    /**
     * Audit a login attempt
     * 
     * @param userId The user ID (null for failed logins)
     * @param username The username or email attempted
     * @param success Whether the login was successful
     * @param startTimeMs Operation start time in milliseconds
     * @param failureReason Reason for failure (if applicable)
     */
    public void auditLogin(Long userId, String username, boolean success, long startTimeMs, String failureReason) {
        String result = success ? "SUCCESS" : "FAILED";
        String description = success ? "User logged in successfully" : "Failed login attempt";
        
        Map<String, Object> additionalData = Map.of(
            "loginSuccess", success,
            "attemptedUsername", username != null ? username : "unknown"
        );
        
        if (!success && failureReason != null) {
            additionalData = new java.util.HashMap<>(additionalData);
            additionalData.put("failureReason", failureReason);
        }
        
        auditOperation("user.login", userId, description, result, failureReason, null, startTimeMs, additionalData);
    }

    /**
     * Audit a logout operation
     * 
     * @param userId The user ID
     * @param username The username
     * @param startTimeMs Operation start time in milliseconds
     */
    public void auditLogout(Long userId, String username, long startTimeMs) {
        Map<String, Object> additionalData = Map.of(
            "username", username != null ? username : "unknown"
        );
        
        auditOperation("user.logout", userId, "User logged out", "SUCCESS", null, null, startTimeMs, additionalData);
    }

    /**
     * Audit a data access operation
     * 
     * @param entityType The type of entity being accessed
     * @param entityId The ID of the entity
     * @param operation The operation performed (read, update, delete)
     * @param success Whether the operation was successful
     * @param startTimeMs Operation start time in milliseconds
     */
    public void auditDataAccess(String entityType, Object entityId, String operation, 
                              boolean success, long startTimeMs) {
        String operationType = entityType.toLowerCase() + "." + operation.toLowerCase();
        String result = success ? "SUCCESS" : "FAILED";
        String description = String.format("%s %s on %s", 
            operation.substring(0, 1).toUpperCase() + operation.substring(1), 
            entityType, entityId);
        
        auditOperation(operationType, entityId, description, result, null, entityType, startTimeMs, null);
    }

    /**
     * Core audit operation method
     * Creates and publishes an audit log event with all available context
     */
    private void auditOperation(String operationType, Object entityId, String description, 
                              String result, String errorMessage, String entityType, 
                              long startTimeMs, Map<String, Object> additionalData) {
        try {
            // Calculate operation duration
            long durationMs = System.currentTimeMillis() - startTimeMs;
            
            // Extract request context (if available)
            RequestContext requestContext = extractRequestContext();
            
            // Create audit log payload
            AuditLogPayload payload = new AuditLogPayload();
            payload.setOperationType(operationType);
            payload.setOperationDescription(description);
            payload.setOperationResult(result);
            payload.setOperationTimestamp(Instant.now());
            payload.setOperationDurationMs(durationMs);
            
            // Set entity information
            if (entityId != null) {
                payload.setEntityId(entityId.toString());
            }
            if (entityType != null) {
                payload.setEntityType(entityType);
            } else if (operationType.contains(".")) {
                // Extract entity type from operation type (e.g., "user.login" -> "User")
                String extractedType = operationType.split("\\.")[0];
                payload.setEntityType(extractedType.substring(0, 1).toUpperCase() + extractedType.substring(1));
            }
            
            // Set request context
            if (requestContext != null) {
                payload.setUserId(requestContext.userId);
                payload.setUsername(requestContext.username);
                payload.setSessionId(requestContext.sessionId);
                payload.setIpAddress(requestContext.ipAddress);
                payload.setUserAgent(requestContext.userAgent);
                payload.setEndpoint(requestContext.endpoint);
                payload.setHttpMethod(requestContext.httpMethod);
                payload.setResultCode(requestContext.statusCode);
            }
            
            // Set error information
            if (errorMessage != null) {
                payload.setErrorMessage(errorMessage);
            }
            
            // Set additional data
            if (additionalData != null) {
                payload.setAdditionalData(additionalData);
            }
            
            // Publish the event asynchronously
            eventPublisher.publishAuditLogEvent(payload, requestContext != null ? requestContext.correlationId : null);
            
        } catch (Exception e) {
            // Don't let audit failures break the main business flow
            log.warn("Failed to publish audit event for operation: {} (entityId: {})", operationType, entityId, e);
        }
    }

    /**
     * Extract request context from the current HTTP request (if available)
     */
    private RequestContext extractRequestContext() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            RequestContext context = new RequestContext();
            
            // Extract basic request information
            context.ipAddress = getClientIpAddress(request);
            context.userAgent = request.getHeader("User-Agent");
            context.endpoint = request.getRequestURI();
            context.httpMethod = request.getMethod();
            
            // Extract user information from security context (if available)
            try {
                var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && 
                    !"anonymousUser".equals(authentication.getPrincipal())) {
                    
                    // Try to extract user details from the authentication
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                        context.username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                    } else if (principal instanceof String) {
                        context.username = (String) principal;
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract user information from security context", e);
            }
            
            // Extract correlation ID from request headers
            context.correlationId = request.getHeader("X-Correlation-ID");
            
            // Extract session ID
            if (request.getSession(false) != null) {
                context.sessionId = request.getSession().getId();
            }
            
            return context;
            
        } catch (Exception e) {
            log.debug("Could not extract request context for audit", e);
            return null;
        }
    }

    /**
     * Extract client IP address from request, handling proxies and load balancers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerCandidates = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerCandidates) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Take the first IP if there are multiple (comma-separated)
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Helper class to hold request context information
     */
    private static class RequestContext {
        Long userId;
        String username;
        String sessionId;
        String ipAddress;
        String userAgent;
        String endpoint;
        String httpMethod;
        String correlationId;
        Integer statusCode;
    }
}