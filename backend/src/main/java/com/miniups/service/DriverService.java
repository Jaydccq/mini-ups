/**
 * Driver Service
 * 
 * Description:
 * - Business logic layer for driver management operations
 * - Handles CRUD operations, status updates, and assignments
 * - Provides validation and business rule enforcement
 * 
 * Key Operations:
 * - Create, read, update, delete drivers
 * - Manage driver status transitions
 * - Handle truck assignments and unassignments
 * - Generate driver statistics and reports
 * - Validate business rules and constraints
 * 
 * Business Rules:
 * - Driver email and license number must be unique
 * - Only UNASSIGNED drivers can be assigned to trucks
 * - Driver status transitions must follow allowed patterns
 * - Cannot delete drivers with active assignments
 * 
 */
package com.miniups.service;

import com.miniups.exception.BusinessValidationException;
import com.miniups.exception.DuplicateResourceException;
import com.miniups.exception.ResourceNotFoundException;
import com.miniups.model.dto.driver.CreateDriverDto;
import com.miniups.model.dto.driver.DriverDto;
import com.miniups.model.dto.driver.UpdateDriverDto;
import com.miniups.model.entity.Driver;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.DriverStatus;
import com.miniups.repository.DriverRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DriverService {
    
    private static final Logger logger = LoggerFactory.getLogger(DriverService.class);
    
    @Autowired
    private DriverRepository driverRepository;
    
    /**
     * Get all drivers with optional filtering
     */
    @Transactional(readOnly = true)
    public List<DriverDto> getAllDrivers(DriverStatus status) {
        logger.info("Getting all drivers with status filter: {}", status);
        
        List<Driver> drivers;
        if (status != null) {
            drivers = driverRepository.findByStatus(status);
        } else {
            drivers = driverRepository.findAll();
        }
        
        return drivers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get drivers with pagination
     */
    @Transactional(readOnly = true)
    public Page<DriverDto> getDriversWithPagination(DriverStatus status, Pageable pageable) {
        logger.info("Getting drivers page {} with status filter: {}", pageable.getPageNumber(), status);
        
        Page<Driver> driverPage;
        if (status != null) {
            driverPage = driverRepository.findByStatus(status, pageable);
        } else {
            driverPage = driverRepository.findAll(pageable);
        }
        
        List<DriverDto> driverDtos = driverPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return new PageImpl<>(driverDtos, pageable, driverPage.getTotalElements());
    }
    
    /**
     * Get driver by ID
     */
    @Transactional(readOnly = true)
    public DriverDto getDriverById(Long driverId) {
        logger.info("Getting driver by ID: {}", driverId);
        
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        
        return convertToDto(driver);
    }
    
    /**
     * Create new driver
     */
    public DriverDto createDriver(CreateDriverDto createDriverDto) {
        logger.info("Creating new driver: {}", createDriverDto.getName());
        
        // Validate uniqueness
        validateDriverUniqueness(createDriverDto.getEmail(), createDriverDto.getLicenseNumber(), null);
        
        // Create driver entity
        Driver driver = new Driver(
            createDriverDto.getName(),
            createDriverDto.getLicenseNumber(),
            createDriverDto.getEmail(),
            createDriverDto.getPhone()
        );
        
        Driver savedDriver = driverRepository.save(driver);
        logger.info("Successfully created driver with ID: {}", savedDriver.getId());
        
        return convertToDto(savedDriver);
    }
    
    /**
     * Update existing driver
     */
    public DriverDto updateDriver(Long driverId, UpdateDriverDto updateDriverDto) {
        logger.info("Updating driver ID: {}", driverId);
        
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        
        // Validate uniqueness if email or license number is being updated
        if (updateDriverDto.hasEmail() || updateDriverDto.hasLicenseNumber()) {
            validateDriverUniqueness(
                updateDriverDto.hasEmail() ? updateDriverDto.getEmail() : null,
                updateDriverDto.hasLicenseNumber() ? updateDriverDto.getLicenseNumber() : null,
                driverId
            );
        }
        
        // Update fields
        if (updateDriverDto.hasName()) {
            driver.setName(updateDriverDto.getName());
        }
        if (updateDriverDto.hasEmail()) {
            driver.setEmail(updateDriverDto.getEmail());
        }
        if (updateDriverDto.hasPhone()) {
            driver.setPhone(updateDriverDto.getPhone());
        }
        if (updateDriverDto.hasLicenseNumber()) {
            driver.setLicenseNumber(updateDriverDto.getLicenseNumber());
        }
        
        Driver updatedDriver = driverRepository.save(driver);
        logger.info("Successfully updated driver ID: {}", driverId);
        
        return convertToDto(updatedDriver);
    }
    
    /**
     * Update driver status
     */
    public DriverDto updateDriverStatus(Long driverId, DriverStatus newStatus) {
        logger.info("Updating driver {} status to: {}", driverId, newStatus);
        
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        
        // Validate status transition
        validateStatusTransition(driver.getStatus(), newStatus, driver.getAssignedTruck() != null);
        
        driver.setStatus(newStatus);
        driver.updateLastActive();
        
        Driver updatedDriver = driverRepository.save(driver);
        logger.info("Successfully updated driver {} status to: {}", driverId, newStatus);
        
        return convertToDto(updatedDriver);
    }
    
    /**
     * Delete driver
     */
    public void deleteDriver(Long driverId) {
        logger.info("Deleting driver ID: {}", driverId);
        
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        
        // Cannot delete driver with active assignment
        if (driver.getAssignedTruck() != null) {
            throw new BusinessValidationException("DRIVER_DELETE", "Cannot delete driver with active truck assignment");
        }
        
        // Cannot delete driver who is currently working
        if (driver.getStatus() == DriverStatus.ON_DUTY) {
            throw new BusinessValidationException("DRIVER_DELETE", "Cannot delete driver who is currently on duty");
        }
        
        driverRepository.delete(driver);
        logger.info("Successfully deleted driver ID: {}", driverId);
    }
    
    /**
     * Get available drivers for assignment
     */
    @Transactional(readOnly = true)
    public List<DriverDto> getAvailableDrivers() {
        logger.info("Getting available drivers for assignment");
        
        List<Driver> availableDrivers = driverRepository.findAvailableDrivers(DriverStatus.UNASSIGNED);
        
        return availableDrivers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Search drivers by name
     */
    @Transactional(readOnly = true)
    public List<DriverDto> searchDriversByName(String name) {
        logger.info("Searching drivers by name: {}", name);
        
        List<Driver> drivers = driverRepository.findByNameContainingIgnoreCase(name);
        
        return drivers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get driver statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDriverStatistics() {
        logger.info("Getting driver statistics");
        
        Object[] stats = driverRepository.getDriverStatistics();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalDrivers", stats[0]);
        statistics.put("unassigned", stats[1]);
        statistics.put("assigned", stats[2]);
        statistics.put("onDuty", stats[3]);
        statistics.put("averageRating", stats[4]);
        
        // Add status distribution
        Map<String, Long> statusDistribution = new HashMap<>();
        for (DriverStatus status : DriverStatus.values()) {
            statusDistribution.put(status.name(), driverRepository.countByStatus(status));
        }
        statistics.put("statusDistribution", statusDistribution);
        
        return statistics;
    }
    
    // Private helper methods
    
    private void validateDriverUniqueness(String email, String licenseNumber, Long excludeDriverId) {
        if (email != null && driverRepository.existsByEmail(email)) {
            // Check if it's the same driver being updated
            if (excludeDriverId == null || !driverRepository.findByEmail(email)
                    .map(Driver::getId).equals(excludeDriverId)) {
                throw new DuplicateResourceException("Driver", email, "Email address already exists: " + email);
            }
        }
        
        if (licenseNumber != null && driverRepository.existsByLicenseNumber(licenseNumber)) {
            // Check if it's the same driver being updated
            if (excludeDriverId == null || !driverRepository.findByLicenseNumber(licenseNumber)
                    .map(Driver::getId).equals(excludeDriverId)) {
                throw new DuplicateResourceException("Driver", licenseNumber, "License number already exists: " + licenseNumber);
            }
        }
    }
    
    private void validateStatusTransition(DriverStatus currentStatus, DriverStatus newStatus, boolean hasTruckAssignment) {
        // Define allowed transitions based on business rules
        switch (currentStatus) {
            case UNASSIGNED:
                if (newStatus == DriverStatus.ASSIGNED && !hasTruckAssignment) {
                    throw new BusinessValidationException("STATUS_TRANSITION", "Cannot set status to ASSIGNED without truck assignment");
                }
                break;
            case ASSIGNED:
                if (newStatus == DriverStatus.UNASSIGNED && hasTruckAssignment) {
                    throw new BusinessValidationException("STATUS_TRANSITION", "Cannot set status to UNASSIGNED while assigned to truck");
                }
                break;
            case ON_DUTY:
                if (newStatus == DriverStatus.UNASSIGNED) {
                    throw new BusinessValidationException("STATUS_TRANSITION", "Cannot set status to UNASSIGNED while on duty");
                }
                break;
            case ON_LEAVE:
            case INACTIVE:
                // These statuses can transition to any other status
                break;
        }
    }
    
    private DriverDto convertToDto(Driver driver) {
        DriverDto dto = new DriverDto();
        dto.setId(driver.getId());
        dto.setName(driver.getName());
        dto.setEmail(driver.getEmail());
        dto.setPhone(driver.getPhone());
        dto.setLicenseNumber(driver.getLicenseNumber());
        dto.setStatus(driver.getStatus());
        dto.setHireDate(driver.getHireDate());
        dto.setLastActive(driver.getLastActive());
        dto.setTotalDeliveries(driver.getTotalDeliveries());
        dto.setRating(driver.getRating());
        dto.setAvailableForAssignment(driver.isAvailableForAssignment());
        dto.setCanStartWork(driver.canStartWork());
        
        // Calculate days since hire
        if (driver.getHireDate() != null) {
            dto.setDaysSinceHire(ChronoUnit.DAYS.between(driver.getHireDate(), LocalDate.now()));
        }
        
        // Add assigned truck information
        if (driver.getAssignedTruck() != null) {
            Truck truck = driver.getAssignedTruck();
            DriverDto.AssignedTruckInfo truckInfo = new DriverDto.AssignedTruckInfo(
                truck.getTruckId(),
                truck.getLicensePlate(),
                truck.getStatus().toString(),
                truck.getCurrentX() + "," + truck.getCurrentY()
            );
            dto.setAssignedTruck(truckInfo);
        }
        
        return dto;
    }
}