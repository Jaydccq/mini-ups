/**
 * Create User Request Data Transfer Object
 * 
 * Functionality:
 * - Used for administrator request data to create new user accounts
 * - Supports specifying user roles and permissions
 * - Provides complete user information collection
 * 
 * Permission Requirements:
 * - Only ADMIN role can use this DTO to create users
 * - Can create user accounts with any role
 * - Supports batch user creation scenarios
 * 
 * Validation Rules:
 * - All basic field validation (username, email, password)
 * - Role validity validation
 * - Format validation for optional fields
 * 
 * Differences from RegisterRequestDto:
 * - Administrator can specify user role
 * - Can set account enabled status
 * - Supports administrator-level user creation
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.dto.user;

import com.miniups.model.dto.common.BaseUserRequestDto;
import com.miniups.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public class CreateUserDto extends BaseUserRequestDto {
    
    @NotNull(message = "User role cannot be empty")
    private UserRole role;
    
    private Boolean enabled = true;
    
    private String notes; // Administrator notes
    
    // Constructors
    public CreateUserDto() {
        super();
    }
    
    public CreateUserDto(String username, String email, String password, UserRole role) {
        super(username, email, password);
        this.role = role;
    }
    
    // Static factory methods
    public static CreateUserDto forRegularUser(String username, String email, String password) {
        return new CreateUserDto(username, email, password, UserRole.USER);
    }
    
    public static CreateUserDto forDriver(String username, String email, String password) {
        CreateUserDto dto = new CreateUserDto(username, email, password, UserRole.DRIVER);
        return dto;
    }
    
    public static CreateUserDto forOperator(String username, String email, String password) {
        return new CreateUserDto(username, email, password, UserRole.OPERATOR);
    }
    
    public static CreateUserDto forAdmin(String username, String email, String password) {
        return new CreateUserDto(username, email, password, UserRole.ADMIN);
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    // Helper methods
    public boolean isPrivilegedRole() {
        return role == UserRole.ADMIN || role == UserRole.OPERATOR;
    }
    
    public boolean isServiceRole() {
        return role == UserRole.DRIVER || role == UserRole.OPERATOR;
    }
    
    @Override
    public String toString() {
        return "CreateUserDto{" +
                "username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", password='[PROTECTED]'" +
                ", firstName='" + getFirstName() + '\'' +
                ", lastName='" + getLastName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", role=" + role +
                ", enabled=" + enabled +
                ", hasNotes=" + (notes != null && !notes.trim().isEmpty()) +
                '}';
    }
}