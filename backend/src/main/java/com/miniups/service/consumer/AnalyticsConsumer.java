/**
 * Analytics Consumer Service
 * 
 * Purpose:
 * - Processes audit log events from RabbitMQ for analytics and reporting
 * - Aggregates system activity data into queryable formats
 * - Generates business intelligence metrics and trends
 * - Supports real-time analytics dashboard updates
 * 
 * Features:
 * - Consumes audit log messages from RabbitMQ queue
 * - Aggregates data into time-series analytics
 * - Calculates performance metrics and KPIs
 * - Publishes real-time analytics updates via WebSocket
 * - Handles data persistence for historical analysis
 * 
 * Analytics Metrics:
 * - Order processing rates and trends
 * - User activity patterns and engagement
 * - System performance indicators
 * - Error rates and system health metrics
 * - Fleet utilization and efficiency
 * 
 * Data Processing:
 * - Event-driven data aggregation
 * - Real-time metric calculation
 * - Historical trend analysis
 * - Performance monitoring
 * - Business intelligence generation
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.service.consumer;

import com.miniups.config.RabbitMQConfig;
import com.miniups.model.entity.AuditLog;
import com.miniups.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AnalyticsConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsConsumer.class);
    
    private final AuditLogRepository auditLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis key prefixes for analytics data
    private static final String ACTION_COUNTS_KEY = "analytics:actionCounts";
    private static final String ENTITY_TYPE_COUNTS_KEY = "analytics:entityTypeCounts";
    private static final String USER_ACTIVITY_KEY = "analytics:userActivity";
    private static final String HOURLY_ACTIVITY_KEY = "analytics:hourlyActivity";
    private static final String ACTIVE_USERS_KEY = "analytics:activeUsers";
    
    public AnalyticsConsumer(AuditLogRepository auditLogRepository,
                           SimpMessagingTemplate messagingTemplate,
                           ObjectMapper objectMapper,
                           RedisTemplate<String, Object> redisTemplate) {
        this.auditLogRepository = auditLogRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Process audit log events for analytics
     * 
     * @param message Raw audit log message from RabbitMQ
     */
    @RabbitListener(queues = RabbitMQConfig.AUDIT_LOG_QUEUE)
    @Transactional
    public void processAuditLogForAnalytics(String message) {
        try {
            logger.debug("Processing audit log message for analytics: {}", message);
            
            // Parse the audit log event
            AuditLog auditLog = parseAuditLogMessage(message);
            
            if (auditLog != null) {
                // Update analytics metrics
                updateAnalyticsMetrics(auditLog);
                
                // Generate and publish real-time analytics
                publishAnalyticsUpdate(auditLog);
                
                // Store aggregated data (optional - for historical analysis)
                storeAnalyticsData(auditLog);
                
                logger.debug("Analytics processing completed for audit log: {}", auditLog.getId());
            }
            
        } catch (Exception e) {
            logger.error("Error processing audit log for analytics: {}", e.getMessage(), e);
            // Don't rethrow - we don't want to block the queue
        }
    }
    
    /**
     * Parse audit log message from JSON
     * 
     * @param message Raw JSON message
     * @return Parsed AuditLog object
     */
    private AuditLog parseAuditLogMessage(String message) {
        try {
            // Try to parse as AuditLog directly
            return objectMapper.readValue(message, AuditLog.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse audit log message: {}", e.getMessage());
            
            // Create a simple audit log from the raw message
            AuditLog auditLog = new AuditLog();
            auditLog.setOperationType("UNKNOWN");
            auditLog.setEntityType("SYSTEM");
            auditLog.setOperationDescription(message);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            return auditLog;
        }
    }
    
    /**
     * Update persistent analytics metrics in Redis
     * 
     * @param auditLog Audit log event
     */
    private void updateAnalyticsMetrics(AuditLog auditLog) {
        try {
            // Update action counts
            redisTemplate.opsForHash().increment(ACTION_COUNTS_KEY, auditLog.getOperationType(), 1);
            
            // Update entity type counts
            redisTemplate.opsForHash().increment(ENTITY_TYPE_COUNTS_KEY, auditLog.getEntityType(), 1);
            
            // Update user activity counts and active users set
            if (auditLog.getUserId() != null) {
                String userId = auditLog.getUserId().toString();
                redisTemplate.opsForHash().increment(USER_ACTIVITY_KEY, userId, 1);
                
                // Add user to active users set with expiration
                redisTemplate.opsForSet().add(ACTIVE_USERS_KEY, userId);
                redisTemplate.expire(ACTIVE_USERS_KEY, 24, TimeUnit.HOURS);
            }
            
            // Update hourly activity counts
            String hourKey = auditLog.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH"));
            redisTemplate.opsForHash().increment(HOURLY_ACTIVITY_KEY, hourKey, 1);
            
            // Set expiration for hourly data (keep for 7 days)
            redisTemplate.expire(HOURLY_ACTIVITY_KEY, 7, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.error("Error updating analytics metrics in Redis: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Publish real-time analytics updates via WebSocket
     * 
     * @param auditLog Audit log event
     */
    private void publishAnalyticsUpdate(AuditLog auditLog) {
        try {
            Map<String, Object> analyticsUpdate = new HashMap<>();
            analyticsUpdate.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            analyticsUpdate.put("event", auditLog.getOperationType());
            analyticsUpdate.put("entityType", auditLog.getEntityType());
            analyticsUpdate.put("userId", auditLog.getUserId());
            
            // Add current metrics
            analyticsUpdate.put("actionCounts", getCurrentActionCounts());
            analyticsUpdate.put("entityTypeCounts", getCurrentEntityTypeCounts());
            analyticsUpdate.put("totalEvents", getTotalEventCount());
            analyticsUpdate.put("activeUsers", getActiveUserCount());
            
            // Publish to admin dashboard
            messagingTemplate.convertAndSend("/topic/admin/analytics", analyticsUpdate);
            
            logger.debug("Published analytics update for action: {}", auditLog.getAction());
            
        } catch (Exception e) {
            logger.error("Error publishing analytics update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Store aggregated analytics data for historical analysis
     * 
     * @param auditLog Audit log event
     */
    private void storeAnalyticsData(AuditLog auditLog) {
        // For now, we'll just ensure the audit log is persisted
        // In the future, we might want to create separate analytics tables
        try {
            if (auditLog.getId() == null) {
                auditLogRepository.save(auditLog);
            }
        } catch (Exception e) {
            logger.error("Error storing analytics data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get current action counts for analytics from Redis
     * 
     * @return Map of action counts
     */
    public Map<String, Long> getCurrentActionCounts() {
        try {
            Map<Object, Object> rawCounts = redisTemplate.opsForHash().entries(ACTION_COUNTS_KEY);
            Map<String, Long> counts = new HashMap<>();
            rawCounts.forEach((key, value) -> 
                counts.put(key.toString(), ((Number) value).longValue()));
            return counts;
        } catch (Exception e) {
            logger.error("Error fetching action counts from Redis: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get current entity type counts for analytics from Redis
     * 
     * @return Map of entity type counts
     */
    public Map<String, Long> getCurrentEntityTypeCounts() {
        try {
            Map<Object, Object> rawCounts = redisTemplate.opsForHash().entries(ENTITY_TYPE_COUNTS_KEY);
            Map<String, Long> counts = new HashMap<>();
            rawCounts.forEach((key, value) -> 
                counts.put(key.toString(), ((Number) value).longValue()));
            return counts;
        } catch (Exception e) {
            logger.error("Error fetching entity type counts from Redis: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get total event count from Redis
     * 
     * @return Total number of events processed
     */
    public long getTotalEventCount() {
        try {
            return getCurrentActionCounts().values().stream()
                    .mapToLong(Long::longValue)
                    .sum();
        } catch (Exception e) {
            logger.error("Error calculating total event count: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Get active user count from Redis
     * 
     * @return Number of active users
     */
    public long getActiveUserCount() {
        try {
            return redisTemplate.opsForSet().size(ACTIVE_USERS_KEY);
        } catch (Exception e) {
            logger.error("Error fetching active user count from Redis: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Get hourly activity data for charts from Redis
     * 
     * @return Map of hourly activity counts
     */
    public Map<String, Long> getHourlyActivityData() {
        try {
            Map<Object, Object> rawData = redisTemplate.opsForHash().entries(HOURLY_ACTIVITY_KEY);
            Map<String, Long> data = new HashMap<>();
            rawData.forEach((key, value) -> 
                data.put(key.toString(), ((Number) value).longValue()));
            return data;
        } catch (Exception e) {
            logger.error("Error fetching hourly activity data from Redis: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get analytics summary for admin dashboard
     * 
     * @return Analytics summary data
     */
    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("totalEvents", getTotalEventCount());
        summary.put("activeUsers", getActiveUserCount());
        summary.put("actionCounts", getCurrentActionCounts());
        summary.put("entityTypeCounts", getCurrentEntityTypeCounts());
        summary.put("hourlyActivity", getHourlyActivityData());
        summary.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return summary;
    }
    
    /**
     * Reset analytics data (for testing or maintenance)
     */
    public void resetAnalyticsData() {
        try {
            redisTemplate.delete(ACTION_COUNTS_KEY);
            redisTemplate.delete(ENTITY_TYPE_COUNTS_KEY);
            redisTemplate.delete(USER_ACTIVITY_KEY);
            redisTemplate.delete(HOURLY_ACTIVITY_KEY);
            redisTemplate.delete(ACTIVE_USERS_KEY);
            
            logger.info("Analytics data reset completed");
        } catch (Exception e) {
            logger.error("Error resetting analytics data: {}", e.getMessage(), e);
        }
    }
}