package com.miniups.service;

import com.miniups.model.entity.LeafAlloc;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Segment Buffer Implementation for Leaf-Segment Algorithm
 * 
 * This class implements the core double-buffering mechanism that enables
 * sub-5ms ID generation latency. It manages current and next segments,
 * handles seamless transitions, and provides thread-safe ID dispensing.
 * 
 * Key Features:
 * - Double-buffering with current and next segments
 * - Lock-free atomic operations for ID dispensing
 * - Automatic prefetch triggering at configurable thresholds
 * - Thread-safe segment transitions
 * - Comprehensive performance monitoring
 * 
 * Thread Safety:
 * - All public methods are thread-safe
 * - Uses atomic operations for high-frequency ID generation
 * - Segment transitions are protected by ReentrantLock
 * - Prefetch coordination prevents duplicate allocations
 * 
 * Performance Characteristics:
 * - Zero blocking for ID generation (lock-free)
 * - Sub-microsecond latency for memory-based operations
 * - Automatic load balancing across threads
 * - Minimal memory footprint per buffer
 * 
 * @author Mini-UPS Development Team
 * @version 1.0
 */
public class SegmentBuffer {
    private static final Logger log = LoggerFactory.getLogger(SegmentBuffer.class);
    
    @Getter
    private final String bizTag;
    
    /**
     * Current segment being used for ID generation
     */
    private volatile Segment currentSegment;
    
    /**
     * Next segment prepared for seamless transition
     */
    private volatile Segment nextSegment;
    
    /**
     * Lock for protecting segment transitions
     * Only used during segment switching, not for ID generation
     */
    private final ReentrantLock transitionLock = new ReentrantLock();
    
    /**
     * Prefetch coordination flags
     */
    private final AtomicBoolean prefetchInProgress = new AtomicBoolean(false);
    private volatile Instant prefetchStartTime;
    
    /**
     * Performance monitoring
     */
    private final AtomicLong totalIdsGenerated = new AtomicLong(0);
    private final AtomicLong totalSegmentSwitches = new AtomicLong(0);
    private volatile Instant lastUsedTime = Instant.now();
    private volatile Instant creationTime = Instant.now();
    
    /**
     * Configuration
     */
    private static final double PREFETCH_THRESHOLD = 0.2; // Prefetch when 20% remaining
    
    public SegmentBuffer(String bizTag) {
        this.bizTag = bizTag;
        log.debug("Created segment buffer for bizTag: {}", bizTag);
    }
    
    /**
     * Initialize the buffer with the first segment
     */
    public void initializeWithSegment(LeafAlloc.Segment segment) {
        if (currentSegment != null) {
            throw new IllegalStateException("Buffer already initialized");
        }
        
        this.currentSegment = new Segment(segment);
        log.debug("Initialized segment buffer for bizTag: {} with segment: {}", 
                bizTag, segment);
    }
    
    /**
     * Get the next ID from the current segment
     * 
     * This method is optimized for maximum throughput with lock-free
     * atomic operations. It handles segment transitions automatically.
     * 
     * @return Next unique ID
     * @throws RuntimeException if no segments are available
     */
    public long nextId() {
        lastUsedTime = Instant.now();
        
        // Try to get ID from current segment (lock-free fast path)
        Segment current = currentSegment;
        if (current != null && current.hasNext()) {
            long id = current.nextId();
            totalIdsGenerated.incrementAndGet();
            return id;
        }
        
        // Need to switch to next segment or wait for prefetch
        return getIdWithSegmentTransition();
    }
    
    /**
     * Handle ID generation when segment transition is needed
     * 
     * This method handles the less common case where we need to switch
     * to the next segment or wait for prefetch completion.
     */
    private long getIdWithSegmentTransition() {
        transitionLock.lock();
        try {
            // Check again under lock (double-checked locking pattern)
            Segment current = currentSegment;
            if (current != null && current.hasNext()) {
                long id = current.nextId();
                totalIdsGenerated.incrementAndGet();
                return id;
            }
            
            // Switch to next segment if available
            if (nextSegment != null) {
                switchToNextSegment();
                return nextId(); // Recursive call with new current segment
            }
            
            // No next segment available - this shouldn't happen in normal operation
            throw new RuntimeException("No available segments for bizTag: " + bizTag + 
                                     ". This indicates a prefetch failure.");
            
        } finally {
            transitionLock.unlock();
        }
    }
    
    /**
     * Switch current segment to next segment
     */
    private void switchToNextSegment() {
        if (nextSegment == null) {
            throw new IllegalStateException("No next segment available");
        }
        
        log.debug("Switching to next segment for bizTag: {} ({})", 
                bizTag, nextSegment.getOriginalSegment());
        
        currentSegment = nextSegment;
        nextSegment = null;
        totalSegmentSwitches.incrementAndGet();
    }
    
    /**
     * Check if prefetch should be triggered
     * 
     * @return true if current segment is running low and prefetch isn't in progress
     */
    public boolean shouldPrefetch() {
        Segment current = currentSegment;
        return current != null && 
               current.getRemainingRatio() < PREFETCH_THRESHOLD &&
               !prefetchInProgress.get() &&
               nextSegment == null;
    }
    
    /**
     * Set the next segment (called by async prefetch)
     */
    public void setNextSegment(LeafAlloc.Segment segment) {
        transitionLock.lock();
        try {
            if (this.nextSegment != null) {
                log.warn("Overriding existing next segment for bizTag: {}", bizTag);
            }
            
            this.nextSegment = new Segment(segment);
            log.debug("Set next segment for bizTag: {} ({})", bizTag, segment);
            
        } finally {
            transitionLock.unlock();
        }
    }
    
    /**
     * Check if there's a next segment ready
     */
    public boolean hasNextSegment() {
        return nextSegment != null;
    }
    
    /**
     * Check if current segment is nearly exhausted
     */
    public boolean isCurrentSegmentNearlyExhausted() {
        Segment current = currentSegment;
        return current != null && current.getRemainingRatio() < 0.1; // Less than 10%
    }
    
    /**
     * Mark prefetch as in progress
     */
    public void markPrefetchInProgress() {
        prefetchInProgress.set(true);
        prefetchStartTime = Instant.now();
    }
    
    /**
     * Mark prefetch as complete
     */
    public void markPrefetchComplete() {
        prefetchInProgress.set(false);
        prefetchStartTime = null;
    }
    
    /**
     * Check if prefetch is currently in progress
     */
    public boolean isPrefetchInProgress() {
        return prefetchInProgress.get();
    }
    
    /**
     * Get prefetch start time
     */
    public Instant getPrefetchStartTime() {
        return prefetchStartTime;
    }
    
    /**
     * Get last used timestamp
     */
    public Instant getLastUsedTime() {
        return lastUsedTime;
    }
    
    /**
     * Calculate allocations per second based on usage history
     */
    public double getAllocationsPerSecond() {
        long totalIds = totalIdsGenerated.get();
        if (totalIds == 0) {
            return 0.0;
        }
        
        Duration lifetime = Duration.between(creationTime, lastUsedTime);
        long lifetimeSeconds = Math.max(1, lifetime.getSeconds());
        
        return (double) totalIds / lifetimeSeconds;
    }
    
    /**
     * Get comprehensive statistics for monitoring
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("bizTag", bizTag);
        stats.put("totalIdsGenerated", totalIdsGenerated.get());
        stats.put("totalSegmentSwitches", totalSegmentSwitches.get());
        stats.put("allocationsPerSecond", getAllocationsPerSecond());
        stats.put("lastUsedTime", lastUsedTime);
        stats.put("creationTime", creationTime);
        stats.put("prefetchInProgress", prefetchInProgress.get());
        stats.put("hasNextSegment", hasNextSegment());
        
        Segment current = currentSegment;
        if (current != null) {
            Map<String, Object> currentStats = new HashMap<>();
            currentStats.put("startId", current.originalSegment.getStartId());
            currentStats.put("endId", current.originalSegment.getEndId());
            currentStats.put("currentPosition", current.position.get());
            currentStats.put("remaining", current.getRemaining());
            currentStats.put("remainingRatio", current.getRemainingRatio());
            currentStats.put("size", current.originalSegment.size());
            stats.put("currentSegment", currentStats);
        }
        
        if (nextSegment != null) {
            Map<String, Object> nextStats = new HashMap<>();
            nextStats.put("startId", nextSegment.originalSegment.getStartId());
            nextStats.put("endId", nextSegment.originalSegment.getEndId());
            nextStats.put("size", nextSegment.originalSegment.size());
            stats.put("nextSegment", nextStats);
        }
        
        return stats;
    }
    
    /**
     * Internal segment wrapper that provides thread-safe ID dispensing
     */
    private static class Segment {
        private final LeafAlloc.Segment originalSegment;
        private final AtomicLong position;
        
        public Segment(LeafAlloc.Segment originalSegment) {
            this.originalSegment = originalSegment;
            this.position = new AtomicLong(originalSegment.getStartId());
        }
        
        /**
         * Get the next ID from this segment (thread-safe)
         */
        public long nextId() {
            long id = position.getAndIncrement();
            if (id > originalSegment.getEndId()) {
                throw new IllegalStateException("Segment exhausted");
            }
            return id;
        }
        
        /**
         * Check if this segment has more IDs available
         */
        public boolean hasNext() {
            return position.get() <= originalSegment.getEndId();
        }
        
        /**
         * Get remaining IDs in this segment
         */
        public long getRemaining() {
            return Math.max(0, originalSegment.getEndId() - position.get() + 1);
        }
        
        /**
         * Get remaining ratio (0.0 to 1.0)
         */
        public double getRemainingRatio() {
            long total = originalSegment.size();
            if (total == 0) return 0.0;
            
            return (double) getRemaining() / total;
        }
        
        /**
         * Get the original segment for debugging/monitoring
         */
        public LeafAlloc.Segment getOriginalSegment() {
            return originalSegment;
        }
    }
}