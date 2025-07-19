/**
 * Password Match Validation Annotation
 * 
 * Functionality:
 * - Used to validate if two password fields match
 * - Class-level annotation, can be applied to any DTO containing password fields
 * - Integrates with Spring Validation framework
 * 
 * Usage:
 * @PasswordsMatch(password = "newPassword", confirmPassword = "confirmPassword")
 * public class PasswordChangeDto { ... }
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordsMatch {
    
    String message() default "Password and confirm password do not match";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Password field name
     */
    String password() default "password";
    
    /**
     * Confirm password field name
     */
    String confirmPassword() default "confirmPassword";
}