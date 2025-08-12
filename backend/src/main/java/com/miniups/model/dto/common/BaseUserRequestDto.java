/**
 * Base User Request DTO
 * 
 * Functionality:
 * - Provides common fields and validation for user-related DTOs
 * - Reduces code duplication between RegisterRequestDto and CreateUserDto
 * - Unifies user information validation rules
 * 
 * Common Fields:
 * - username: Username
 * - email: Email address
 * - password: Password
 * - firstName/lastName: Name
 * - phone: Phone number
 * - address: Address
 * 
 *
 
 */
package com.miniups.model.dto.common;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public abstract class BaseUserRequestDto {
    
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username length must be between 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
    private String username;
    
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email format is incorrect")
    @Size(max = 100, message = "Email length cannot exceed 100 characters")
    private String email;
    
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, max = 100, message = "Password length must be between 8-100 characters")
    private String password;
    
    @Size(max = 50, message = "First name length cannot exceed 50 characters")
    private String firstName;
    
    @Size(max = 50, message = "Last name length cannot exceed 50 characters")
    private String lastName;
    
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{0,20}$", message = "Phone number format is incorrect")
    private String phone;
    
    @Size(max = 500, message = "Address length cannot exceed 500 characters")
    private String address;
    
    // Constructors
    public BaseUserRequestDto() {}
    
    public BaseUserRequestDto(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    // Helper methods
    public boolean hasCompleteProfile() {
        return firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty() &&
               phone != null && !phone.trim().isEmpty();
    }
    
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName.trim() + " " + lastName.trim();
        } else if (firstName != null) {
            return firstName.trim();
        } else if (lastName != null) {
            return lastName.trim();
        }
        return username;
    }
}