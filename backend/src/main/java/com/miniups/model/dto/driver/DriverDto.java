/**
 * Driver Data Transfer Object
 * 
 * Description:
 * - Used for transferring driver data between layers
 * - Excludes sensitive information for security
 * - Includes computed fields for UI display
 * 
 * Key Fields:
 * - Basic driver information (id, name, email, phone)
 * - Current status and availability
 * - Assignment information (truck details if assigned)
 * - Performance metrics (rating, deliveries)
 * 
 */
package com.miniups.model.dto.driver;

import com.miniups.model.enums.DriverStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DriverDto {
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String licenseNumber;
    private DriverStatus status;
    private String statusDescription;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActive;
    
    private Integer totalDeliveries;
    private Double rating;
    
    // Assigned truck information
    private AssignedTruckInfo assignedTruck;
    
    // Computed fields
    private boolean availableForAssignment;
    private boolean canStartWork;
    private long daysSinceHire;
    
    public static class AssignedTruckInfo {
        private Integer truckId;
        private String plateNumber;
        private String status;
        private String currentLocation;
        
        public AssignedTruckInfo() {}
        
        public AssignedTruckInfo(Integer truckId, String plateNumber, String status, String currentLocation) {
            this.truckId = truckId;
            this.plateNumber = plateNumber;
            this.status = status;
            this.currentLocation = currentLocation;
        }
        
        // Getters and Setters
        public Integer getTruckId() { return truckId; }
        public void setTruckId(Integer truckId) { this.truckId = truckId; }
        
        public String getPlateNumber() { return plateNumber; }
        public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getCurrentLocation() { return currentLocation; }
        public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    }
    
    // Constructors
    public DriverDto() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    
    public DriverStatus getStatus() { return status; }
    public void setStatus(DriverStatus status) { 
        this.status = status; 
        this.statusDescription = status != null ? status.getDescription() : null;
    }
    
    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }
    
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    
    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }
    
    public Integer getTotalDeliveries() { return totalDeliveries; }
    public void setTotalDeliveries(Integer totalDeliveries) { this.totalDeliveries = totalDeliveries; }
    
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    
    public AssignedTruckInfo getAssignedTruck() { return assignedTruck; }
    public void setAssignedTruck(AssignedTruckInfo assignedTruck) { this.assignedTruck = assignedTruck; }
    
    public boolean isAvailableForAssignment() { return availableForAssignment; }
    public void setAvailableForAssignment(boolean availableForAssignment) { this.availableForAssignment = availableForAssignment; }
    
    public boolean isCanStartWork() { return canStartWork; }
    public void setCanStartWork(boolean canStartWork) { this.canStartWork = canStartWork; }
    
    public long getDaysSinceHire() { return daysSinceHire; }
    public void setDaysSinceHire(long daysSinceHire) { this.daysSinceHire = daysSinceHire; }
}