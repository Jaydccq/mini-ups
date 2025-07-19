/**
 * Password Match Validator
 * 
 * Functionality:
 * - Implements validation logic for @PasswordsMatch annotation
 * - Uses reflection to get password field values for comparison
 * - Supports custom field name configuration
 * 
 * Validation Rules:
 * - Two password fields must be completely identical
 * - Supports null value checking
 * - Validation fails when fields don't exist
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordsMatchValidator.class);
    
    private String passwordFieldName;
    private String confirmPasswordFieldName;
    
    @Override
    public void initialize(PasswordsMatch constraintAnnotation) {
        this.passwordFieldName = constraintAnnotation.password();
        this.confirmPasswordFieldName = constraintAnnotation.confirmPassword();
    }
    
    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if (object == null) {
            return true; // Let other validators handle null object
        }
        
        try {
            // Get password field values
            Object password = getFieldValue(object, passwordFieldName);
            Object confirmPassword = getFieldValue(object, confirmPasswordFieldName);
            
            // If both fields are null, consider valid
            if (password == null && confirmPassword == null) {
                return true;
            }
            
            // If one is null, consider invalid
            if (password == null || confirmPassword == null) {
                return false;
            }
            
            // Compare if passwords are equal
            boolean isValid = password.equals(confirmPassword);
            
            if (!isValid) {
                // Customize error message location
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                       .addPropertyNode(confirmPasswordFieldName)
                       .addConstraintViolation();
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Password match validation exception: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Use reflection to get field value
     * 
     * @param object Target object
     * @param fieldName Field name
     * @return Field value
     * @throws NoSuchFieldException Field does not exist
     * @throws IllegalAccessException Field access exception
     */
    private Object getFieldValue(Object object, String fieldName) 
            throws NoSuchFieldException, IllegalAccessException {
        
        Class<?> clazz = object.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}