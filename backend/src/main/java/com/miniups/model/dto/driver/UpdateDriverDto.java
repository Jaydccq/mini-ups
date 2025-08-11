/**
 * Update Driver DTO
 * 
 * Description:
 * - Data transfer object for updating existing drivers
 * - Contains validation annotations for data integrity
 * - Used in REST API PUT/PATCH requests
 * 
 * Validation Rules:
 * - All fields are optional for partial updates
 * - When provided, fields must meet the same validation as create
 * - Cannot update status directly (use separate endpoint)
 * 
 */
package com.miniups.model.dto.driver;

import jakarta.validation.constraints.*;

public class UpdateDriverDto {
    
    @Size(min = 2, max = 100, message = "Driver name must be between 2 and 100 characters")
    private String name;
    
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;
    
    @Size(min = 5, max = 50, message = "License number must be between 5 and 50 characters")
    private String licenseNumber;
    
    // Constructors
    public UpdateDriverDto() {}
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public String getLicenseNumber() {
        return licenseNumber;
    }
    
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
    
    // Helper methods
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }
    
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }
    
    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }
    
    public boolean hasLicenseNumber() {
        return licenseNumber != null && !licenseNumber.trim().isEmpty();
    }
    
    public boolean hasUpdates() {
        return hasName() || hasEmail() || hasPhone() || hasLicenseNumber();
    }
}