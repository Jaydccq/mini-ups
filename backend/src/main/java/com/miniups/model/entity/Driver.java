/**
 * Driver Entity
 * 
 * Description:
 * - Manages driver information and availability status
 * - Establishes bidirectional one-to-one relationship with trucks
 * - Tracks driver performance and work history
 * 
 * Key Fields:
 * - name: Driver's full name
 * - licenseNumber: Unique driver's license identifier
 * - email: Contact email address
 * - phone: Contact phone number
 * - status: Current driver status (UNASSIGNED/ASSIGNED/ON_DUTY/etc.)
 * - hireDate: When the driver was hired
 * 
 * Database Design:
 * - Table: drivers
 * - Indexes: license_number (unique), email (unique), status
 * - Bidirectional relationship with trucks table
 * 
 * Business Logic:
 * - One driver can be assigned to maximum one truck at a time
 * - Driver status must be UNASSIGNED to be assigned to a truck
 * - Driver license number must be unique across the system
 * 
 */
package com.miniups.model.entity;

import com.miniups.model.enums.DriverStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Driver extends BaseEntity {
    
    @NotBlank
    @Size(max = 100)
    private String name;
    
    @NotBlank
    @Size(max = 50)
    private String licenseNumber;
    
    @Email
    @NotBlank
    @Size(max = 100)
    private String email;
    
    @NotBlank
    @Size(max = 20)
    private String phone;
    
    @NotNull
    private DriverStatus status = DriverStatus.UNASSIGNED;
    
    private LocalDate hireDate;
    
    private LocalDateTime lastActive;
    
    private Integer totalDeliveries = 0;

    private Double rating = 4.0;
    
    // Foreign key field for truck assignment
    private Long truckId;

    // Transient field (loaded separately if needed)
    private Truck assignedTruck;
    
    // Constructors
    public Driver() {}
    
    public Driver(String name, String licenseNumber, String email, String phone) {
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.email = email;
        this.phone = phone;
        this.hireDate = LocalDate.now();
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLicenseNumber() {
        return licenseNumber;
    }
    
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public DriverStatus getStatus() {
        return status;
    }
    
    public void setStatus(DriverStatus status) {
        this.status = status;
    }
    
    public LocalDate getHireDate() {
        return hireDate;
    }
    
    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }
    
    public LocalDateTime getLastActive() {
        return lastActive;
    }
    
    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }
    
    public Integer getTotalDeliveries() {
        return totalDeliveries;
    }
    
    public void setTotalDeliveries(Integer totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }
    
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    public Truck getAssignedTruck() {
        return assignedTruck;
    }
    
    public void setAssignedTruck(Truck assignedTruck) {
        this.assignedTruck = assignedTruck;
    }
    
    // Helper methods
    public boolean isAvailableForAssignment() {
        return status.canBeAssigned() && assignedTruck == null;
    }
    
    public boolean canStartWork() {
        return status.canStartWork() && assignedTruck != null;
    }
    
    public void updateLastActive() {
        this.lastActive = LocalDateTime.now();
    }
    
    public void assignToTruck(Truck truck) {
        this.assignedTruck = truck;
        this.status = DriverStatus.ASSIGNED;
        updateLastActive();
    }
    
    public void unassignFromTruck() {
        this.assignedTruck = null;
        this.status = DriverStatus.UNASSIGNED;
        updateLastActive();
    }
    
    public void startWork() {
        if (canStartWork()) {
            this.status = DriverStatus.ON_DUTY;
            updateLastActive();
        }
    }
    
    public void finishWork() {
        if (status == DriverStatus.ON_DUTY) {
            this.status = DriverStatus.ASSIGNED;
            updateLastActive();
        }
    }
}