/**
 * User Already Exists Exception
 * 
 * Thrown when attempting to create a username or email that already exists
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class UserAlreadyExistsException extends BaseBusinessException {
    
    public UserAlreadyExistsException(String field, String value) {
        super("USER_ALREADY_EXISTS", String.format("%s '%s' already exists", field, value), ExceptionSeverity.LOW);
    }
    
    public UserAlreadyExistsException(String message) {
        super("USER_ALREADY_EXISTS", message, ExceptionSeverity.LOW);
    }
}