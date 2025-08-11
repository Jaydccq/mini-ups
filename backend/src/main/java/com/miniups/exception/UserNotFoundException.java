/**
 * User Not Found Exception
 * 
 * Thrown when requested user does not exist
 * 
 *
 
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class UserNotFoundException extends BaseBusinessException {
    
    public UserNotFoundException(String username) {
        super("USER_NOT_FOUND", String.format("User '%s' does not exist", username), ExceptionSeverity.LOW);
    }
    
    public UserNotFoundException(String field, String value) {
        super("USER_NOT_FOUND", String.format("User %s '%s' does not exist", field, value), ExceptionSeverity.LOW);
    }
}