/**
 * Driver Repository Interface
 * 
 * Description:
 * - Data access layer for Driver entity operations
 * - Provides custom query methods for driver management
 * - Supports filtering by status and availability
 * 
 * Custom Query Methods:
 * - findByStatus: Get drivers by status (UNASSIGNED, ASSIGNED, etc.)
 * - findByLicenseNumber: Find driver by license number (unique)
 * - findByEmail: Find driver by email address (unique)
 * - findAvailableDrivers: Get drivers available for assignment
 * - countByStatus: Count drivers by status for statistics
 * 
 * Performance Considerations:
 * - License number and email have unique indexes for fast lookup
 * - Status field has index for efficient filtering
 * - Uses JPA derived queries for simple operations
 * 
 */
package com.miniups.repository;

import com.miniups.model.entity.Driver;
import com.miniups.model.enums.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    
    /**
     * Find drivers by status
     */
    List<Driver> findByStatus(DriverStatus status);
    
    /**
     * Find drivers by status with pagination
     */
    Page<Driver> findByStatus(DriverStatus status, Pageable pageable);
    
    /**
     * Find driver by license number (unique)
     */
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    
    /**
     * Find driver by email (unique)
     */
    Optional<Driver> findByEmail(String email);
    
    /**
     * Check if license number exists (for validation)
     */
    boolean existsByLicenseNumber(String licenseNumber);
    
    /**
     * Check if email exists (for validation)
     */
    boolean existsByEmail(String email);
    
    /**
     * Find available drivers (UNASSIGNED status and no assigned truck)
     */
    @Query("SELECT d FROM Driver d WHERE d.status = :status AND d.assignedTruck IS NULL")
    List<Driver> findAvailableDrivers(@Param("status") DriverStatus status);
    
    /**
     * Count drivers by status for statistics
     */
    long countByStatus(DriverStatus status);
    
    /**
     * Find drivers by name (case insensitive search)
     */
    @Query("SELECT d FROM Driver d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Driver> findByNameContainingIgnoreCase(@Param("name") String name);
    
    /**
     * Find top performing drivers by rating
     */
    @Query("SELECT d FROM Driver d WHERE d.status != :excludeStatus ORDER BY d.rating DESC, d.totalDeliveries DESC")
    List<Driver> findTopPerformingDrivers(@Param("excludeStatus") DriverStatus excludeStatus, Pageable pageable);
    
    /**
     * Get driver statistics
     */
    @Query("SELECT " +
           "COUNT(d) as totalDrivers, " +
           "COUNT(CASE WHEN d.status = 'UNASSIGNED' THEN 1 END) as unassigned, " +
           "COUNT(CASE WHEN d.status = 'ASSIGNED' THEN 1 END) as assigned, " +
           "COUNT(CASE WHEN d.status = 'ON_DUTY' THEN 1 END) as onDuty, " +
           "AVG(d.rating) as averageRating " +
           "FROM Driver d WHERE d.status != 'INACTIVE'")
    Object[] getDriverStatistics();
}