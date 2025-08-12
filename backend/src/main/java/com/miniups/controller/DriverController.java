/**
 * Driver Management Controller
 * 
 * Description:
 * - REST API endpoints for driver management operations
 * - Provides CRUD operations and status management
 * - Supports filtering, pagination, and search functionality
 * 
 * API Endpoints:
 * - GET /api/drivers - Get all drivers with optional filtering
 * - GET /api/drivers/{driverId} - Get driver by ID
 * - POST /api/drivers - Create new driver
 * - PUT /api/drivers/{driverId} - Update driver information
 * - DELETE /api/drivers/{driverId} - Delete driver
 * - PUT /api/drivers/{driverId}/status - Update driver status
 * - GET /api/drivers/available - Get available drivers for assignment
 * - GET /api/drivers/search - Search drivers by name
 * - GET /api/drivers/statistics - Get driver statistics
 * 
 * Permission Control:
 * - View operations: Operator and above permissions
 * - Management operations: Admin permissions
 * - Statistics: Operator and above permissions
 * 
 */
package com.miniups.controller;

import com.miniups.model.dto.common.ApiResponse;
import com.miniups.model.dto.driver.CreateDriverDto;
import com.miniups.model.dto.driver.DriverDto;
import com.miniups.model.dto.driver.UpdateDriverDto;
import com.miniups.model.enums.DriverStatus;
import com.miniups.service.DriverService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/drivers")
@CrossOrigin(origins = "*")
public class DriverController {
    
    private static final Logger logger = LoggerFactory.getLogger(DriverController.class);
    
    @Autowired
    private DriverService driverService;
    
    /**
     * Get all drivers with optional filtering
     * 
     * @param status Optional status filter
     * @param page Page number (0-based)
     * @param size Page size
     * @param sort Sort field
     * @return List of drivers or paginated result
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<Object>> getAllDrivers(
            @RequestParam(required = false) DriverStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "name") String sort) {
        
        logger.info("Getting drivers - Status: {}, Page: {}, Size: {}", status, page, size);
        
        try {
            if (page != null && size != null) {
                // Paginated request
                Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
                Page<DriverDto> driverPage = driverService.getDriversWithPagination(status, pageable);
                
                return ResponseEntity.ok(ApiResponse.success("Drivers retrieved successfully", driverPage));
            } else {
                // Non-paginated request
                List<DriverDto> drivers = driverService.getAllDrivers(status);
                
                return ResponseEntity.ok(ApiResponse.success("Drivers retrieved successfully", drivers));
            }
        } catch (Exception e) {
            logger.error("Error retrieving drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve drivers: " + e.getMessage()));
        }
    }
    
    /**
     * Get driver by ID
     * 
     * @param driverId Driver ID
     * @return Driver details
     */
    @GetMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<DriverDto>> getDriverById(@PathVariable Long driverId) {
        logger.info("Getting driver by ID: {}", driverId);
        
        try {
            DriverDto driver = driverService.getDriverById(driverId);
            return ResponseEntity.ok(ApiResponse.success("Driver retrieved successfully", driver));
        } catch (Exception e) {
            logger.error("Error retrieving driver {}", driverId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to retrieve driver: " + e.getMessage()));
        }
    }
    
    /**
     * Create new driver
     * 
     * @param createDriverDto Driver creation data
     * @return Created driver
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriverDto>> createDriver(@Valid @RequestBody CreateDriverDto createDriverDto) {
        logger.info("Creating new driver: {}", createDriverDto.getName());
        
        try {
            DriverDto createdDriver = driverService.createDriver(createDriverDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Driver created successfully", createdDriver));
        } catch (Exception e) {
            logger.error("Error creating driver", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create driver: " + e.getMessage()));
        }
    }
    
    /**
     * Update existing driver
     * 
     * @param driverId Driver ID
     * @param updateDriverDto Driver update data
     * @return Updated driver
     */
    @PutMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriverDto>> updateDriver(
            @PathVariable Long driverId, 
            @Valid @RequestBody UpdateDriverDto updateDriverDto) {
        
        logger.info("Updating driver ID: {}", driverId);
        
        try {
            DriverDto updatedDriver = driverService.updateDriver(driverId, updateDriverDto);
            return ResponseEntity.ok(ApiResponse.success("Driver updated successfully", updatedDriver));
        } catch (Exception e) {
            logger.error("Error updating driver {}", driverId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to update driver: " + e.getMessage()));
        }
    }
    
    /**
     * Update driver status
     * 
     * @param driverId Driver ID
     * @param statusRequest Status update request
     * @return Updated driver
     */
    @PutMapping("/{driverId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<DriverDto>> updateDriverStatus(
            @PathVariable Long driverId,
            @RequestBody Map<String, String> statusRequest) {
        
        logger.info("Updating driver {} status", driverId);
        
        try {
            String statusString = statusRequest.get("status");
            if (statusString == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Status is required"));
            }
            
            DriverStatus newStatus = DriverStatus.valueOf(statusString.toUpperCase());
            DriverDto updatedDriver = driverService.updateDriverStatus(driverId, newStatus);
            
            return ResponseEntity.ok(ApiResponse.success("Driver status updated successfully", updatedDriver));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value for driver {}", driverId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status value"));
        } catch (Exception e) {
            logger.error("Error updating driver {} status", driverId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to update driver status: " + e.getMessage()));
        }
    }
    
    /**
     * Delete driver
     * 
     * @param driverId Driver ID
     * @return Success message
     */
    @DeleteMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteDriver(@PathVariable Long driverId) {
        logger.info("Deleting driver ID: {}", driverId);
        
        try {
            driverService.deleteDriver(driverId);
            return ResponseEntity.ok(ApiResponse.success("Driver deleted successfully", null));
        } catch (Exception e) {
            logger.error("Error deleting driver {}", driverId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to delete driver: " + e.getMessage()));
        }
    }
    
    /**
     * Get available drivers for assignment
     * 
     * @return List of available drivers
     */
    @GetMapping("/available")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<List<DriverDto>>> getAvailableDrivers() {
        logger.info("Getting available drivers for assignment");
        
        try {
            List<DriverDto> availableDrivers = driverService.getAvailableDrivers();
            return ResponseEntity.ok(ApiResponse.success("Available drivers retrieved successfully", availableDrivers));
        } catch (Exception e) {
            logger.error("Error retrieving available drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve available drivers: " + e.getMessage()));
        }
    }
    
    /**
     * Search drivers by name
     * 
     * @param name Search term
     * @return List of matching drivers
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<List<DriverDto>>> searchDrivers(@RequestParam String name) {
        logger.info("Searching drivers by name: {}", name);
        
        try {
            List<DriverDto> drivers = driverService.searchDriversByName(name);
            return ResponseEntity.ok(ApiResponse.success("Drivers search completed", drivers));
        } catch (Exception e) {
            logger.error("Error searching drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to search drivers: " + e.getMessage()));
        }
    }
    
    /**
     * Get driver statistics
     * 
     * @return Driver statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriverStatistics() {
        logger.info("Getting driver statistics");
        
        try {
            Map<String, Object> statistics = driverService.getDriverStatistics();
            return ResponseEntity.ok(ApiResponse.success("Driver statistics retrieved successfully", statistics));
        } catch (Exception e) {
            logger.error("Error retrieving driver statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve driver statistics: " + e.getMessage()));
        }
    }
}