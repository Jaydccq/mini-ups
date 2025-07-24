/**
 * Package Tracking Service
 * 
 * Functionality:
 * - Generate unique UPS tracking numbers
 * - Provide package status query functionality
 * - Manage package tracking history records
 * 
 * Tracking Number Generation Rules:
 * - Format: UPS + timestamp + random number
 * - Length: Fixed 20 digits
 * - Uniqueness: Guaranteed through database constraints
 * 
 * Status Management:
 * - Real-time package status updates
 * - Record status change history
 * - Support status lookup and auditing
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.service;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.ShipmentStatusHistory;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.repository.ShipmentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class TrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrackingService.class);
    
    private static final String TRACKING_PREFIX = "UPS";
    private static final int MAX_RETRY_ATTEMPTS = 5;
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    private final Random random = new Random();
    private final AtomicLong sequenceCounter = new AtomicLong(0);
    
    /**
     * Generate unique UPS tracking number
     * 
     * Format: UPS + YYYYMMDDHHMMSS + 4-digit sequence number
     * Example: UPS202401151030450001
     * 
     * @return Unique tracking number
     */
    public synchronized String generateTrackingNumber() {
        int attempts = 0;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            String trackingNumber = createTrackingNumber();
            
            // Check if tracking number already exists
            if (!shipmentRepository.existsByUpsTrackingId(trackingNumber)) {
                logger.info("Generated tracking number: {}", trackingNumber);
                return trackingNumber;
            }
            
            attempts++;
            logger.warn("Tracking number collision, attempt {}: {}", attempts, trackingNumber);
        }
        
        // If we still have collisions after max attempts, add nanosecond timestamp
        String fallbackNumber = createTrackingNumber() + String.format("%04d", System.nanoTime() % 10000);
        logger.warn("Used fallback tracking number generation: {}", fallbackNumber);
        return fallbackNumber;
    }
    
    /**
     * Query package information by tracking number
     * 
     * @param trackingNumber UPS tracking number
     * @return Package shipment information
     */
    @Transactional(readOnly = true)
    public Optional<Shipment> findByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return shipmentRepository.findByUpsTrackingId(trackingNumber.trim());
    }
    
    /**
     * Update package status with retry mechanism for optimistic locking
     * 
     * @param trackingNumber UPS tracking number
     * @param newStatus New status
     * @param comment Status change comment
     * @return Whether update was successful
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 50, multiplier = 1.5, maxDelay = 500)
    )
    public boolean updateShipmentStatus(String trackingNumber, ShipmentStatus newStatus, String comment) {
        return doUpdateShipmentStatus(trackingNumber, newStatus, comment);
    }
    
    private boolean doUpdateShipmentStatus(String trackingNumber, ShipmentStatus newStatus, String comment) {
        try {
            Optional<Shipment> shipmentOpt = findByTrackingNumber(trackingNumber);
            
            if (shipmentOpt.isEmpty()) {
                logger.warn("Shipment not found for tracking number: {}", trackingNumber);
                return false;
            }
            
            Shipment shipment = shipmentOpt.get();
            ShipmentStatus oldStatus = shipment.getStatus();
            
            // Validate status transition
            if (!isValidStatusTransition(oldStatus, newStatus)) {
                logger.warn("Invalid status transition from {} to {} for tracking number: {}", 
                           oldStatus, newStatus, trackingNumber);
                return false;
            }
            
            // Update status
            shipment.setStatus(newStatus);
            
            // Add status history entry with comment
            ShipmentStatusHistory history = new ShipmentStatusHistory();
            history.setShipment(shipment);
            history.setStatus(newStatus);
            history.setTimestamp(LocalDateTime.now());
            history.setNotes(comment);
            
            shipment.getStatusHistory().add(history);
            
            // Update delivery time if delivered
            if (newStatus == ShipmentStatus.DELIVERED) {
                shipment.setActualDelivery(LocalDateTime.now());
            }
            
            shipmentRepository.save(shipment);
            
            logger.info("Updated shipment {} status from {} to {}", 
                       trackingNumber, oldStatus, newStatus);
            
            return true;
            
        } catch (OptimisticLockingFailureException e) {
            logger.debug("Optimistic locking failure for tracking number: {}, will retry", trackingNumber);
            throw e; // Re-throw to trigger retry
        } catch (Exception e) {
            logger.error("Error updating shipment status for tracking number: " + trackingNumber, e);
            return false;
        }
    }
    
    /**
     * Get package status history
     * 
     * @param trackingNumber UPS tracking number
     * @return Status history list
     */
    @Transactional(readOnly = true)
    public List<ShipmentStatusHistory> getStatusHistory(String trackingNumber) {
        Optional<Shipment> shipmentOpt = findByTrackingNumber(trackingNumber);
        
        if (shipmentOpt.isPresent()) {
            return shipmentOpt.get().getStatusHistory();
        }
        
        return List.of();
    }
    
    /**
     * Check if tracking number format is valid
     * 
     * @param trackingNumber Tracking number
     * @return Whether it's valid
     */
    public boolean isValidTrackingNumberFormat(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = trackingNumber.trim();
        
        // Check prefix
        if (!trimmed.startsWith(TRACKING_PREFIX)) {
            return false;
        }
        
        // Check minimum length (UPS + 14 digits + 4 random = 21 chars minimum)
        if (trimmed.length() < 21) {
            return false;
        }
        
        // Check that part after prefix contains only digits
        String numberPart = trimmed.substring(TRACKING_PREFIX.length());
        return numberPart.matches("\\d+");
    }
    
    /**
     * Get all packages for specified user
     * 
     * @param userId User ID
     * @return All shipment orders for the user
     */
    @Transactional(readOnly = true)
    public List<Shipment> getUserShipments(Long userId) {
        return shipmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    // Private helper methods
    
    private String createTrackingNumber() {
        // Get current timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        // Use atomic counter for thread-safe sequence generation
        long sequence = sequenceCounter.incrementAndGet() % 10000;
        
        // Format sequence to 4 digits with leading zeros
        String sequenceStr = String.format("%04d", sequence);
        
        return TRACKING_PREFIX + timestamp + sequenceStr;
    }
    
    private boolean isValidStatusTransition(ShipmentStatus fromStatus, ShipmentStatus toStatus) {
        // Define valid state transitions
        switch (fromStatus) {
            case CREATED:
                return toStatus == ShipmentStatus.PICKED_UP || toStatus == ShipmentStatus.CANCELLED;
                
            case PICKED_UP:
                return toStatus == ShipmentStatus.IN_TRANSIT || toStatus == ShipmentStatus.CANCELLED;
                
            case IN_TRANSIT:
                return toStatus == ShipmentStatus.OUT_FOR_DELIVERY || 
                       toStatus == ShipmentStatus.CANCELLED ||
                       toStatus == ShipmentStatus.EXCEPTION;
                
            case OUT_FOR_DELIVERY:
                return toStatus == ShipmentStatus.DELIVERED || 
                       toStatus == ShipmentStatus.DELIVERY_ATTEMPTED ||
                       toStatus == ShipmentStatus.EXCEPTION;
                
            case DELIVERY_ATTEMPTED:
                return toStatus == ShipmentStatus.OUT_FOR_DELIVERY || 
                       toStatus == ShipmentStatus.DELIVERED ||
                       toStatus == ShipmentStatus.RETURNED;
                
            case DELIVERED:
                // Delivered is typically a final state, but allow for corrections
                return toStatus == ShipmentStatus.EXCEPTION;
                
            case CANCELLED:
            case RETURNED:
                // These are final states
                return false;
                
            case EXCEPTION:
                // From exception, can go to most states depending on resolution
                return toStatus != ShipmentStatus.CREATED;
                
            default:
                return false;
        }
    }
}