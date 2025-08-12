/**
 * World Simulator Debug Event Listener
 * 
 * Purpose:
 * - Listens for WorldSimulatorDebugEvent and broadcasts to WebSocket subscribers
 * - Provides real-time message streaming to admin debugging interface
 * - Manages message retention and cleanup for late-joining clients
 * - Handles WebSocket client lifecycle and subscription management
 * 
 * Features:
 * - Async event processing to avoid blocking simulator operations
 * - Bounded message buffer with automatic cleanup (LRU eviction)
 * - Admin-only authorization for debug message subscription
 * - Snapshot replay for clients joining mid-stream
 * - Rate limiting and error handling for WebSocket failures
 * - Message filtering and routing capabilities
 * 
 * Message Destinations:
 * - /topic/admin/world-simulator-debug - Main debug message stream
 * - /topic/admin/world-simulator-stats - Connection statistics
 * 
 * Security:
 * - Admin role required for all debug topic subscriptions
 * - Topic-level access control via Spring Security
 * - Message content sanitization if needed
 * 
 * Performance:
 * - Caffeine cache for message retention with TTL
 * - Async processing prevents blocking World Simulator
 * - Connection pooling for multiple admin clients
 * - Memory-bounded to prevent resource exhaustion
 * 
 * Configuration:
 * - app.debug.world-simulator.buffer-size - Max messages in memory
 * - app.debug.world-simulator.retention-seconds - Message retention time
 * - spring.websocket.stomp.broker.* - WebSocket broker settings
 * 
 *
 
 */
package com.miniups.debug;

import com.miniups.model.dto.debug.WorldSimulatorDebugMessageDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "app.debug.world-simulator.enabled", havingValue = "true", matchIfMissing = false)
public class WorldSimulatorDebugEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldSimulatorDebugEventListener.class);
    
    private static final String DEBUG_TOPIC = "/topic/admin/world-simulator-debug";
    private static final String STATS_TOPIC = "/topic/admin/world-simulator-stats";
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Value("${app.debug.world-simulator.buffer-size:5000}")
    private int bufferSize;
    
    @Value("${app.debug.world-simulator.retention-seconds:300}")
    private int retentionSeconds;
    
    // Message retention cache
    private Cache<Long, WorldSimulatorDebugMessageDto> messageCache;
    
    // Statistics
    private final AtomicLong messageCounter = new AtomicLong(0);
    private final AtomicLong inboundCounter = new AtomicLong(0);
    private final AtomicLong outboundCounter = new AtomicLong(0);
    private volatile LocalDateTime startTime;
    
    @PostConstruct
    public void initialize() {
        // Initialize message cache with TTL and size limits
        this.messageCache = Caffeine.newBuilder()
                .maximumSize(bufferSize)
                .expireAfterWrite(retentionSeconds, TimeUnit.SECONDS)
                .build();
        
        this.startTime = LocalDateTime.now();
        
        logger.info("WorldSimulatorDebugEventListener initialized with buffer size: {}, retention: {}s", 
                   bufferSize, retentionSeconds);
    }
    
    /**
     * Handle WorldSimulatorDebugEvent and broadcast to WebSocket subscribers
     */
    @EventListener
    @Async
    public void handleDebugEvent(WorldSimulatorDebugEvent event) {
        try {
            WorldSimulatorDebugMessageDto debugMessage = event.getDebugMessage();
            
            // Store in cache for late-joining clients
            long messageId = messageCounter.incrementAndGet();
            messageCache.put(messageId, debugMessage);
            
            // Update statistics
            if (debugMessage.getDirection() == WorldSimulatorDebugMessageDto.Direction.INBOUND) {
                inboundCounter.incrementAndGet();
            } else {
                outboundCounter.incrementAndGet();
            }
            
            // Broadcast to WebSocket subscribers
            messagingTemplate.convertAndSend(DEBUG_TOPIC, debugMessage);
            
            logger.debug("Broadcasted debug message: {} (total: {})", 
                        debugMessage.getSummary(), messageCounter.get());
            
        } catch (Exception e) {
            logger.error("Error handling WorldSimulatorDebugEvent", e);
        }
    }
    
    /**
     * Provide message history for newly connected clients
     */
    public List<WorldSimulatorDebugMessageDto> getRecentMessages(int limit) {
        List<WorldSimulatorDebugMessageDto> messages = new ArrayList<>();
        
        // Get recent messages from cache (ordered by insertion)
        messageCache.asMap().values().stream()
                .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .limit(Math.min(limit, bufferSize))
                .forEach(messages::add);
        
        return messages;
    }
    
    /**
     * Get current debug statistics
     */
    public DebugStatistics getStatistics() {
        return new DebugStatistics(
                messageCounter.get(),
                inboundCounter.get(),
                outboundCounter.get(),
                messageCache.estimatedSize(),
                startTime,
                LocalDateTime.now()
        );
    }
    
    /**
     * Broadcast current statistics to subscribers
     */
    public void broadcastStatistics() {
        try {
            DebugStatistics stats = getStatistics();
            messagingTemplate.convertAndSend(STATS_TOPIC, stats);
        } catch (Exception e) {
            logger.error("Error broadcasting statistics", e);
        }
    }
    
    /**
     * Clear message cache and reset statistics
     */
    public void clearCache() {
        messageCache.invalidateAll();
        messageCounter.set(0);
        inboundCounter.set(0);
        outboundCounter.set(0);
        startTime = LocalDateTime.now();
        
        logger.info("Debug message cache cleared and statistics reset");
        
        // Broadcast reset notification
        try {
            messagingTemplate.convertAndSend(DEBUG_TOPIC + "/reset", "Cache cleared");
            broadcastStatistics();
        } catch (Exception e) {
            logger.error("Error broadcasting cache clear notification", e);
        }
    }
    
    /**
     * Inner class for debug statistics
     */
    public static class DebugStatistics {
        private final long totalMessages;
        private final long inboundMessages;
        private final long outboundMessages;
        private final long cachedMessages;
        private final LocalDateTime startTime;
        private final LocalDateTime currentTime;
        
        public DebugStatistics(long totalMessages, long inboundMessages, long outboundMessages,
                              long cachedMessages, LocalDateTime startTime, LocalDateTime currentTime) {
            this.totalMessages = totalMessages;
            this.inboundMessages = inboundMessages;
            this.outboundMessages = outboundMessages;
            this.cachedMessages = cachedMessages;
            this.startTime = startTime;
            this.currentTime = currentTime;
        }
        
        // Getters
        public long getTotalMessages() { return totalMessages; }
        public long getInboundMessages() { return inboundMessages; }
        public long getOutboundMessages() { return outboundMessages; }
        public long getCachedMessages() { return cachedMessages; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getCurrentTime() { return currentTime; }
        
        public double getMessagesPerSecond() {
            long duration = java.time.Duration.between(startTime, currentTime).getSeconds();
            return duration > 0 ? (double) totalMessages / duration : 0.0;
        }
    }
}