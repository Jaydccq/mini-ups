/**
 * User Login Request Data Transfer Object
 * 
 * Functionality:
 * - Encapsulates credential information submitted during user login
 * - Supports login with username or email
 * - Provides input validation to ensure data integrity
 * 
 * Security Features:
 * - Does not store plaintext passwords, only used for transmission
 * - Supports Bean Validation
 * - Prevents null values and invalid input
 * 
 * Usage Scenarios:
 * - User login API request body
 * - Frontend login form data binding
 * - Starting point of JWT authentication flow
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDto {
    
    @NotBlank(message = "Username or email cannot be empty")
    @Size(min = 3, max = 100, message = "Username or email length must be between 3-100 characters")
    private String usernameOrEmail;
    
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 100, message = "Password length must be between 6-100 characters")
    private String password;
    
    // Constructors
    public LoginRequestDto() {}
    
    public LoginRequestDto(String usernameOrEmail, String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }
    
    // Getters and Setters
    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }
    
    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    // Helper methods
    public boolean isEmail() {
        return usernameOrEmail != null && usernameOrEmail.contains("@");
    }
    
    @Override
    public String toString() {
        return "LoginRequestDto{" +
                "usernameOrEmail='" + usernameOrEmail + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}