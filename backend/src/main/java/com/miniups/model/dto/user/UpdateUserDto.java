/**
 * Update User Information Request Data Transfer Object
 * 
 * Functionality:
 * - Used for user personal information update request data
 * - Supports partial field updates (patch operations)
 * - Provides both user self-modification and administrator modification modes
 * 
 * Update Rules:
 * - Users can update basic information except username
 * - Email updates require uniqueness validation
 * - Role updates can only be performed by administrators
 * - Password updates use dedicated PasswordChangeDto
 * 
 * Permission Control:
 * - Regular users can only update their own information
 * - Administrators can update any user's information
 * - Role field can only be modified by administrators
 * 
 *
 
 */
package com.miniups.model.dto.user;

import com.miniups.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateUserDto {
    
    @Email(message = "Email format is incorrect")
    @Size(max = 100, message = "Email length cannot exceed 100 characters")
    private String email;
    
    @Size(max = 50, message = "First name length cannot exceed 50 characters")
    private String firstName;
    
    @Size(max = 50, message = "Last name length cannot exceed 50 characters")
    private String lastName;
    
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{0,20}$", message = "Phone number format is incorrect")
    private String phone;
    
    @Size(max = 500, message = "Address length cannot exceed 500 characters")
    private String address;
    
    // Fields that can only be modified by administrators
    private UserRole role;
    private Boolean enabled;
    private String notes; // Administrator notes
    
    // Constructors
    public UpdateUserDto() {}
    
    // Static factory methods for different update scenarios
    public static UpdateUserDto userSelfUpdate() {
        return new UpdateUserDto(); // Only contains basic information fields
    }
    
    public static UpdateUserDto adminUpdate() {
        return new UpdateUserDto(); // Contains all updatable fields
    }
    
    public static UpdateUserDto contactInfoUpdate(String email, String phone, String address) {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setEmail(email);
        dto.setPhone(phone);
        dto.setAddress(address);
        return dto;
    }
    
    public static UpdateUserDto profileUpdate(String firstName, String lastName) {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        return dto;
    }
    
    public static UpdateUserDto adminRoleUpdate(UserRole role, Boolean enabled) {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setRole(role);
        dto.setEnabled(enabled);
        return dto;
    }
    
    // Getters and Setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
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
    
    // Alias method for test compatibility
    public void setPhoneNumber(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
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
    
    // Helper methods for determining what's being updated
    public boolean hasContactInfoUpdates() {
        return email != null || phone != null || address != null;
    }
    
    public boolean hasProfileUpdates() {
        return firstName != null || lastName != null;
    }
    
    public boolean hasAdminOnlyUpdates() {
        return role != null || enabled != null || notes != null;
    }
    
    public boolean hasAnyUpdates() {
        return hasContactInfoUpdates() || hasProfileUpdates() || hasAdminOnlyUpdates();
    }
    
    public boolean isRoleUpdate() {
        return role != null;
    }
    
    public boolean isStatusUpdate() {
        return enabled != null;
    }
    
    // Method to create a copy without admin-only fields (for user self-update)
    public UpdateUserDto forUserSelfUpdate() {
        UpdateUserDto userDto = new UpdateUserDto();
        userDto.setEmail(this.email);
        userDto.setFirstName(this.firstName);
        userDto.setLastName(this.lastName);
        userDto.setPhone(this.phone);
        userDto.setAddress(this.address);
        // Does not include role, enabled, notes
        return userDto;
    }
    
    @Override
    public String toString() {
        return "UpdateUserDto{" +
                "email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", role=" + role +
                ", enabled=" + enabled +
                ", hasNotes=" + (notes != null && !notes.trim().isEmpty()) +
                ", hasUpdates=" + hasAnyUpdates() +
                '}';
    }
}