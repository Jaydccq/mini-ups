/**
 * Admin Dashboard Controller
 * 
 * Features:
 * - Provides centralized admin dashboard API endpoints
 * - Supports comprehensive business data aggregation
 * - Includes KPI metrics, fleet management, and analytics
 * - Implements role-based access control for administrative functions
 * 
 * API Endpoints:
 * - GET /api/admin/dashboard/statistics: Get dashboard KPI metrics
 * - GET /api/admin/dashboard/activities: Get recent system activities
 * - GET /api/admin/fleet/overview: Get fleet overview and status
 * - GET /api/admin/fleet/drivers: Get driver management data
 * - GET /api/admin/orders/summary: Get order management summary
 * - GET /api/admin/analytics/trends: Get business trend analytics
 * - GET /api/admin/system/health: Get system health status
 * 
 * Permission Control:
 * - All endpoints require ADMIN role
 * - Sensitive operations are logged for audit purposes
 * - Data aggregation optimized for dashboard performance
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.controller;

import com.miniups.model.dto.common.ApiResponse;
import com.miniups.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final AdminService adminService;
    
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
    
    /**
     * Get dashboard KPI statistics
     * 
     * @return Dashboard statistics including orders, revenue, fleet status
     */
    @GetMapping("/dashboard/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStatistics() {
        logger.debug("Fetching dashboard statistics");
        
        Map<String, Object> statistics = adminService.getDashboardStatistics();
        
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", statistics));
    }
    
    /**
     * Get recent system activities
     * 
     * @param pageable Pagination parameters
     * @return Recent system activities
     */
    @GetMapping("/dashboard/activities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentActivities(
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        
        logger.debug("Fetching recent activities");
        
        Map<String, Object> responseData = adminService.getRecentActivities(pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Recent activities retrieved successfully", responseData));
    }
    
    /**
     * Get fleet overview and status
     * 
     * @return Fleet overview with truck locations and status
     */
    @GetMapping("/fleet/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFleetOverview() {
        logger.debug("Fetching fleet overview");
        
        Map<String, Object> fleetOverview = adminService.getFleetOverview();
        
        return ResponseEntity.ok(ApiResponse.success("Fleet overview retrieved successfully", fleetOverview));
    }
    
    /**
     * Get driver management data
     * 
     * @return Driver information and assignments
     */
    @GetMapping("/fleet/drivers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriverManagement() {
        logger.debug("Fetching driver management data");
        
        Map<String, Object> responseData = adminService.getDriverManagement();
        
        return ResponseEntity.ok(ApiResponse.success("Driver management data retrieved successfully", responseData));
    }
    
    /**
     * Get order management summary
     * 
     * @return Order management data with filtering and bulk operations info
     */
    @GetMapping("/orders/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderSummary() {
        logger.debug("Fetching order management summary");
        
        Map<String, Object> responseData = adminService.getOrderSummary();
        
        return ResponseEntity.ok(ApiResponse.success("Order summary retrieved successfully", responseData));
    }
    
    /**
     * Get business trend analytics
     * 
     * @return Analytics data for charts and trends
     */
    @GetMapping("/analytics/trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsTrends() {
        logger.debug("Fetching analytics trends");
        
        Map<String, Object> trends = adminService.getAnalyticsTrends();
        
        return ResponseEntity.ok(ApiResponse.success("Analytics trends retrieved successfully", trends));
    }
    
    /**
     * Get system health status
     * 
     * @return System health metrics and status indicators
     */
    @GetMapping("/system/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        logger.debug("Fetching system health status");
        
        Map<String, Object> health = adminService.getSystemHealth();
        
        return ResponseEntity.ok(ApiResponse.success("System health retrieved successfully", health));
    }
    
    // ============================================
    // Fleet Management CRUD Operations
    // ============================================
    
    /**
     * Create a new truck
     * 
     * @param truckData Truck creation data
     * @return Created truck information
     */
    @PostMapping("/fleet/trucks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTruck(@RequestBody Map<String, Object> truckData) {
        logger.debug("Creating new truck: {}", truckData);
        
        Map<String, Object> createdTruck = adminService.createTruck(truckData);
        
        return ResponseEntity.ok(ApiResponse.success("Truck created successfully", createdTruck));
    }
    
    /**
     * Update an existing truck
     * 
     * @param truckId Truck ID to update
     * @param truckData Updated truck data
     * @return Updated truck information
     */
    @PutMapping("/fleet/trucks/{truckId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTruck(@PathVariable Long truckId, @RequestBody Map<String, Object> truckData) {
        logger.debug("Updating truck {}: {}", truckId, truckData);
        
        Map<String, Object> updatedTruck = adminService.updateTruck(truckId, truckData);
        
        return ResponseEntity.ok(ApiResponse.success("Truck updated successfully", updatedTruck));
    }
    
    /**
     * Delete a truck
     * 
     * @param truckId Truck ID to delete
     * @return Deletion confirmation
     */
    @DeleteMapping("/fleet/trucks/{truckId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteTruck(@PathVariable Long truckId) {
        logger.debug("Deleting truck {}", truckId);
        
        adminService.deleteTruck(truckId);
        
        return ResponseEntity.ok(ApiResponse.success("Truck deleted successfully", null));
    }
    
    /**
     * Create a new driver
     * 
     * @param driverData Driver creation data
     * @return Created driver information
     */
    @PostMapping("/fleet/drivers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDriver(@RequestBody Map<String, Object> driverData) {
        logger.debug("Creating new driver: {}", driverData);
        
        Map<String, Object> createdDriver = adminService.createDriver(driverData);
        
        return ResponseEntity.ok(ApiResponse.success("Driver created successfully", createdDriver));
    }
    
    /**
     * Update an existing driver
     * 
     * @param driverId Driver ID to update
     * @param driverData Updated driver data
     * @return Updated driver information
     */
    @PutMapping("/fleet/drivers/{driverId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDriver(@PathVariable Long driverId, @RequestBody Map<String, Object> driverData) {
        logger.debug("Updating driver {}: {}", driverId, driverData);
        
        Map<String, Object> updatedDriver = adminService.updateDriver(driverId, driverData);
        
        return ResponseEntity.ok(ApiResponse.success("Driver updated successfully", updatedDriver));
    }
    
    /**
     * Delete a driver
     * 
     * @param driverId Driver ID to delete
     * @return Deletion confirmation
     */
    @DeleteMapping("/fleet/drivers/{driverId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteDriver(@PathVariable Long driverId) {
        logger.debug("Deleting driver {}", driverId);
        
        adminService.deleteDriver(driverId);
        
        return ResponseEntity.ok(ApiResponse.success("Driver deleted successfully", null));
    }
}