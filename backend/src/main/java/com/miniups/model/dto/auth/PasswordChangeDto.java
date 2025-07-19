/**
 * Password Change Request Data Transfer Object
 * 
 * Functionality:
 * - Encapsulates information submitted when user changes password
 * - Supports current password verification to ensure security
 * - Provides new password strength validation
 * 
 * Security Validation:
 * - Must provide current password for identity verification
 * - New password must meet complexity requirements
 * - New password cannot be the same as current password
 * 
 * Validation Process:
 * 1. Verify current password correctness
 * 2. Check new password format and strength
 * 3. Confirm new password matches confirmation password
 * 4. Update password and invalidate existing tokens
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.dto.auth;

import com.miniups.validation.PasswordsMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordsMatch(password = "newPassword", confirmPassword = "confirmPassword")
public class PasswordChangeDto {
    
    @NotBlank(message = "Current password cannot be empty")
    private String currentPassword;
    
    @NotBlank(message = "New password cannot be empty")
    @Size(min = 8, max = 100, message = "New password length must be between 8-100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "New password must contain at least one uppercase letter, one lowercase letter and one number")
    private String newPassword;
    
    @NotBlank(message = "Confirm password cannot be empty")
    private String confirmPassword;
    
    // Constructors
    public PasswordChangeDto() {}
    
    public PasswordChangeDto(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
    
    // Getters and Setters
    public String getCurrentPassword() {
        return currentPassword;
    }
    
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    // Validation helper methods
    public boolean isNewPasswordDifferent() {
        return currentPassword != null && newPassword != null && 
               !currentPassword.equals(newPassword);
    }
    
    public boolean isPasswordsMatch() {
        return newPassword != null && confirmPassword != null && 
               newPassword.equals(confirmPassword);
    }
    
    @Override
    public String toString() {
        return "PasswordChangeDto{" +
                "currentPassword='[PROTECTED]'" +
                ", newPassword='[PROTECTED]'" +
                ", confirmPassword='[PROTECTED]'" +
                ", isNewPasswordDifferent=" + isNewPasswordDifferent() +
                '}';
    }
}