/**
 * Communication Log Service
 * 
 * Functionality:
 * - Manages logging of Amazon-UPS communication for debugging
 * - Provides methods to log incoming and outgoing messages
 * - Supports querying and filtering communication history
 * 
 * Features:
 * - Automatic JSON serialization of request/response objects
 * - Performance timing measurement
 * - Error tracking and categorization
 * - Debug interface data provision
 * 
 *
 
 */
package com.miniups.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.model.entity.CommunicationLog;
import com.miniups.repository.CommunicationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CommunicationLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(CommunicationLogService.class);
    
    @Autowired
    private CommunicationLogRepository communicationLogRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Log incoming message from Amazon
     */
    public CommunicationLog logIncomingMessage(String messageType, String endpoint, Object payload) {
        CommunicationLog log = CommunicationLog.incoming(messageType, endpoint);
        log.setPayload(serializeToJson(payload));
        
        // Extract common fields
        if (payload instanceof Map) {
            Map<?, ?> payloadMap = (Map<?, ?>) payload;
            extractCommonFields(log, payloadMap);
        }
        
        return communicationLogRepository.save(log);
    }
    
    /**
     * Log outgoing message to Amazon
     */
    public CommunicationLog logOutgoingMessage(String messageType, String endpoint, Object payload) {
        CommunicationLog log = CommunicationLog.outgoing(messageType, endpoint);
        log.setPayload(serializeToJson(payload));
        
        return communicationLogRepository.save(log);
    }
    
    /**
     * Update log with response information
     */
    public void updateLogWithResponse(CommunicationLog log, Object response, int statusCode, long processingTimeMs) {
        log.setResponse(serializeToJson(response));
        log.setStatusCode(statusCode);
        log.setProcessingTimeMs(processingTimeMs);
        log.setSuccess(statusCode >= 200 && statusCode < 300);
        
        communicationLogRepository.save(log);
    }
    
    /**
     * Mark log as error
     */
    public void markLogAsError(CommunicationLog log, String errorMessage, Integer statusCode) {
        log.markAsError(statusCode != null ? statusCode : 500, errorMessage);
        communicationLogRepository.save(log);
    }
    
    /**
     * Get recent communication logs for debug interface
     */
    @Transactional(readOnly = true)
    public List<CommunicationLog> getRecentLogs(int limit) {
        if (limit <= 50) {
            return communicationLogRepository.findTop50ByOrderByCreatedAtDesc();
        } else {
            return communicationLogRepository.findTop50ByOrderByCreatedAtDesc();
        }
    }
    
    /**
     * Get filtered logs for debugging
     */
    @Transactional(readOnly = true)
    public List<CommunicationLog> getFilteredLogs(String direction, String messageType, Boolean success, int hoursBack) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        return communicationLogRepository.findFilteredLogs(direction, messageType, success, since);
    }
    
    /**
     * Get communication statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCommunicationStatistics(int hoursBack) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        
        Map<String, Object> stats = new HashMap<>();
        
        // Message type counts
        List<Object[]> messageTypeCounts = communicationLogRepository.countMessageTypesSince(since);
        Map<String, Long> messageTypes = new HashMap<>();
        for (Object[] row : messageTypeCounts) {
            messageTypes.put((String) row[0], (Long) row[1]);
        }
        stats.put("messageTypes", messageTypes);
        
        // Success rates
        List<Object[]> successRates = communicationLogRepository.countSuccessRatesSince(since);
        Map<String, Long> successStats = new HashMap<>();
        for (Object[] row : successRates) {
            String key = (Boolean) row[0] ? "successful" : "failed";
            successStats.put(key, (Long) row[1]);
        }
        stats.put("successStats", successStats);
        
        // Average processing time
        Double avgProcessingTime = communicationLogRepository.averageProcessingTimeSince(since);
        stats.put("averageProcessingTimeMs", avgProcessingTime != null ? avgProcessingTime : 0.0);
        
        // Recent errors
        List<CommunicationLog> recentErrors = communicationLogRepository.findTop20BySuccessFalseOrderByCreatedAtDesc();
        stats.put("recentErrors", recentErrors);
        
        return stats;
    }
    
    /**
     * Get logs for specific shipment
     */
    @Transactional(readOnly = true)
    public List<CommunicationLog> getLogsForShipment(String shipmentId) {
        return communicationLogRepository.findByShipmentIdOrderByCreatedAtAsc(shipmentId);
    }
    
    // Private helper methods
    
    private String serializeToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }
    
    private void extractCommonFields(CommunicationLog log, Map<?, ?> payloadMap) {
        // Extract shipment_id
        Object shipmentId = payloadMap.get("shipment_id");
        if (shipmentId != null) {
            log.setShipmentId(String.valueOf(shipmentId));
        }
        
        // Extract truck_id
        Object truckId = payloadMap.get("truck_id");
        if (truckId instanceof Number) {
            log.setTruckId(((Number) truckId).intValue());
        }
        
        // Extract warehouse_id
        Object warehouseId = payloadMap.get("warehouse_id");
        if (warehouseId != null) {
            log.setWarehouseId(String.valueOf(warehouseId));
        }
        
        // Handle nested payload
        Object nestedPayload = payloadMap.get("payload");
        if (nestedPayload instanceof Map) {
            extractCommonFields(log, (Map<?, ?>) nestedPayload);
        }
    }
}