/**
 * Create Driver DTO
 * 
 * Description:
 * - Data transfer object for creating new drivers
 * - Contains validation annotations for data integrity
 * - Used in REST API POST requests
 * 
 * Validation Rules:
 * - Name: Required, 2-100 characters
 * - Email: Required, valid email format, unique
 * - Phone: Required, valid phone format
 * - License Number: Required, 5-50 characters, unique
 * 
 */
package com.miniups.model.dto.driver;

import jakarta.validation.constraints.*;

public class CreateDriverDto {
    
    @NotBlank(message = "Driver name is required")
    @Size(min = 2, max = 100, message = "Driver name must be between 2 and 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;
    
    @NotBlank(message = "License number is required")
    @Size(min = 5, max = 50, message = "License number must be between 5 and 50 characters")
    private String licenseNumber;
    
    // Constructors
    public CreateDriverDto() {}
    
    public CreateDriverDto(String name, String email, String phone, String licenseNumber) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
    }
    
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
}