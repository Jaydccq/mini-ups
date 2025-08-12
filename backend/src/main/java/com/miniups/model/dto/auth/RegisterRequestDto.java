/**
 * User Registration Request Data Transfer Object
 * 
 * Functionality:
 * - Encapsulates all necessary information submitted during user registration
 * - Provides comprehensive input validation and constraint checking
 * - Supports collection of user basic information and contact details
 * 
 * Validation Rules:
 * - Username: 3-50 characters, uniqueness checked at Service layer
 * - Email: Standard email format, uniqueness checked at Service layer
 * - Password: 8-100 characters, recommended to include uppercase, lowercase and numbers
 * - Name and phone: Optional fields for complete user profile
 * 
 * Security Considerations:
 * - Password field is protected in toString method
 * - Supports password confirmation validation (frontend implementation)
 * - All inputs are validated to prevent injection attacks
 * 
 *
 
 */
package com.miniups.model.dto.auth;

import com.miniups.model.dto.common.BaseUserRequestDto;

public class RegisterRequestDto extends BaseUserRequestDto {
    
    // Constructors
    public RegisterRequestDto() {
        super();
    }
    
    public RegisterRequestDto(String username, String email, String password) {
        super(username, email, password);
    }
    
    @Override
    public String toString() {
        return "RegisterRequestDto{" +
                "username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", password='[PROTECTED]'" +
                ", firstName='" + getFirstName() + '\'' +
                ", lastName='" + getLastName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", address='" + getAddress() + '\'' +
                '}';
    }
}