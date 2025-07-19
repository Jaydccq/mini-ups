/**
 * User Not Found Exception
 * 
 * Thrown when requested user does not exist
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class UserNotFoundException extends BaseBusinessException {
    
    public UserNotFoundException(String username) {
        super("USER_NOT_FOUND", String.format("用户 '%s' 不存在", username), ExceptionSeverity.LOW);
    }
    
    public UserNotFoundException(String field, String value) {
        super("USER_NOT_FOUND", String.format("用户%s '%s' 不存在", field, value), ExceptionSeverity.LOW);
    }
}