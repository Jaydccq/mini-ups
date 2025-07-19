/**
 * Duplicate Resource Exception
 * 
 * 资源重复异常，用于处理创建重复资源的错误
 * 
 * Common scenarios:
 * - 用户邮箱重复
 * - 用户名重复
 * - 订单号重复
 * - 追踪号重复
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.exception;

import com.miniups.model.enums.ExceptionSeverity;

public class DuplicateResourceException extends BaseBusinessException {
    
    private final String resourceType;
    private final String identifier;
    
    public DuplicateResourceException(String resourceType, String identifier) {
        super("DUPLICATE_RESOURCE", String.format("%s already exists with identifier: %s", resourceType, identifier), ExceptionSeverity.LOW);
        this.resourceType = resourceType;
        this.identifier = identifier;
    }
    
    public DuplicateResourceException(String resourceType, String identifier, String message) {
        super("DUPLICATE_RESOURCE", message, ExceptionSeverity.LOW);
        this.resourceType = resourceType;
        this.identifier = identifier;
    }
    
    // 便利方法用于常见重复资源异常
    public static DuplicateResourceException email(String email) {
        return new DuplicateResourceException("User", email, String.format("Email address %s is already registered", email));
    }
    
    public static DuplicateResourceException username(String username) {
        return new DuplicateResourceException("User", username, String.format("Username %s is already taken", username));
    }
    
    public static DuplicateResourceException trackingNumber(String trackingNumber) {
        return new DuplicateResourceException("Tracking", trackingNumber, String.format("Tracking number %s already exists", trackingNumber));
    }
}