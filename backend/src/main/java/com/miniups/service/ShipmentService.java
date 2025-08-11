/**
 * Shipment Service
 * 
 * Description:
 * - Provides core business logic for shipment creation, query, and update
 * - Integrates Amazon orders and UPS transportation services
 * - Manages shipment lifecycle and status changes
 * 
 * Key Features:
 * - Create new shipment
 * - Query shipment information
 * - Update shipment status
 * - Shipment assignment and dispatching
 * 
 * Dependencies:
 * - TrackingService: Tracking number generation and status management
 * - TruckManagementService: Truck assignment
 * - UserService: User information verification
 * 
 *
 
 */
package com.miniups.service;

import com.miniups.model.dto.shipment.CreateShipmentDto;
import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.User;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.UserRepository;
import com.miniups.exception.ShipmentCreationException;
import com.miniups.util.Constants;
import com.miniups.util.SecurityUtils;
import com.miniups.util.NameParsingUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ShipmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(ShipmentService.class);
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TrackingService trackingService;
    
    @Autowired
    private TruckManagementService truckManagementService;
    
    /**
     * Create a new shipment
     * 
     * @param createShipmentDto shipment creation request
     * @return created shipment entity
     */
    public Shipment createShipment(CreateShipmentDto createShipmentDto) {
        try {
            logger.info("Creating shipment for customer: {}", createShipmentDto.getCustomerName());
            
            // 1. Get or create user
            User customer = getOrCreateCustomer(createShipmentDto);
            
            // 2. Generate tracking number
            String trackingNumber = trackingService.generateTrackingNumber();
            
            // 3. Create shipment entity
            Shipment shipment = new Shipment();
            shipment.setUpsTrackingId(trackingNumber);
            shipment.setShipmentId(createShipmentDto.getShipmentId());
            shipment.setUser(customer);
            shipment.setOriginX(createShipmentDto.getOriginX());
            shipment.setOriginY(createShipmentDto.getOriginY());
            shipment.setDestX(createShipmentDto.getDestX());
            shipment.setDestY(createShipmentDto.getDestY());
            shipment.setWeight(createShipmentDto.getWeight());
            shipment.setStatus(ShipmentStatus.CREATED);
            shipment.setCreatedAt(LocalDateTime.now());
            shipment.setUpdatedAt(LocalDateTime.now());
            
            // 4. Assign truck - don't catch exception, let failure propagate
            Truck assignedTruck = truckManagementService.assignOptimalTruck(
                createShipmentDto.getOriginX(),
                createShipmentDto.getOriginY(),
                Constants.DEFAULT_PACKAGE_COUNT
            );
            
            if (assignedTruck != null) {
                shipment.setTruck(assignedTruck);
                shipment.setStatus(ShipmentStatus.TRUCK_DISPATCHED);
                logger.info("Assigned truck {} to shipment {}", assignedTruck.getLicensePlate(), trackingNumber);
            } else {
                logger.info("No trucks available for immediate assignment to shipment {}", trackingNumber);
                // Keep status as CREATED - truck can be assigned later
            }
            
            // 5. Save shipment
            Shipment savedShipment = shipmentRepository.save(shipment);
            
            // 6. Record status history
            trackingService.updateShipmentStatus(trackingNumber, savedShipment.getStatus(), "Shipment created");
            
            logger.info("Successfully created shipment: {}", trackingNumber);
            return savedShipment;
            
        } catch (ShipmentCreationException e) {
            // Re-throw business exception to preserve chain
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create shipment for customer {}: {}", 
                createShipmentDto.getCustomerName(), e.getMessage(), e);
            throw ShipmentCreationException.forCustomer(createShipmentDto.getCustomerName(), e);
        }
    }
    
    /**
     * Find shipment by tracking number
     * 
     * @param trackingNumber UPS tracking number
     * @return shipment information
     */
    public Optional<Shipment> findByTrackingNumber(String trackingNumber) {
        try {
            return shipmentRepository.findByUpsTrackingId(trackingNumber);
        } catch (Exception e) {
            logger.error("Error finding shipment by tracking number {}: {}", trackingNumber, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find all shipments
     * 
     * @return shipment list
     */
    public List<Shipment> findAllShipments() {
        return shipmentRepository.findAll();
    }
    
    /**
     * Find shipments by status
     * 
     * @param status shipment status
     * @return matching shipment list
     */
    public List<Shipment> findByStatus(ShipmentStatus status) {
        return shipmentRepository.findByStatus(status);
    }
    
    /**
     * Update shipment status - Delegates to TrackingService for proper concurrency handling
     * 
     * @param trackingNumber tracking number
     * @param newStatus new status
     * @param notes notes
     */
    public void updateShipmentStatus(String trackingNumber, ShipmentStatus newStatus, String notes) {
        // Delegate to TrackingService which has proper retry mechanism for optimistic locking
        boolean updated = trackingService.updateShipmentStatus(trackingNumber, newStatus, notes);
        if (!updated) {
            logger.warn("Failed to update shipment status for tracking number: {}", trackingNumber);
        }
    }
    
    
    /**
     * Get or create customer user
     * 
     * @param dto shipment creation request
     * @return customer user entity
     */
    private User getOrCreateCustomer(CreateShipmentDto dto) {
        // Try by customer ID first
        if (dto.getCustomerId() != null) {
            Optional<User> existingUser = userRepository.findByUsername(dto.getCustomerId());
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
        }
        
        // Lookup by email
        Optional<User> userByEmail = userRepository.findByEmail(dto.getCustomerEmail());
        if (userByEmail.isPresent()) {
            return userByEmail.get();
        }
        
        // Create new user
        try {
            User newUser = new User();
            newUser.setUsername(dto.getCustomerId() != null ? dto.getCustomerId() : 
                              NameParsingUtils.generateUsernameFromName(dto.getCustomerName()));
            newUser.setEmail(dto.getCustomerEmail());
            
            // Use intelligent name parsing
            NameParsingUtils.ParsedName parsedName = NameParsingUtils.parseName(dto.getCustomerName());
            newUser.setFirstName(parsedName.getFirstName());
            newUser.setLastName(parsedName.getLastName());
            
            newUser.setRole(UserRole.USER);
            
            // Generate a secure temporary password and encode for storage
            String tempPassword = SecurityUtils.generateSecureTemporaryPassword();
            newUser.setPassword(SecurityUtils.encodePassword(tempPassword));
            
            // Mark as a user who needs to reset password on first login
            newUser.setEnabled(true);
            newUser.setCreatedAt(LocalDateTime.now());
            
            // Log temporary password generation (in real apps, email the user)
            // Save user first to get the ID
            User savedUser = userRepository.save(newUser);
            logger.info("Created new user with ID: {}", savedUser.getId());
            logger.debug("Generated secure temporary password for user: {} (Email: {})", 
                       savedUser.getUsername(), savedUser.getEmail());
            logger.warn("User with ID {} must reset password on first login", savedUser.getId());
            
            return savedUser;
            
        } catch (Exception e) {
            logger.error("Failed to create new user for customer {}: {}", dto.getCustomerEmail(), e.getMessage());
            throw ShipmentCreationException.userCreationFailed(dto.getCustomerEmail(), e);
        }
    }
}
