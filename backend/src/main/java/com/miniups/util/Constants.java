/**
 * Application Constants
 * 
 * 功能说明：
 * - 定义系统范围内的常量
 * - 避免魔术数字和硬编码值
 * - 提高代码可维护性和可读性
 * 
 * 常量分类：
 * - 业务规则常量
 * - 系统限制常量
 * - 默认值常量
 * - 配置相关常量
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.util;

public final class Constants {
    
    // 防止实例化
    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // ==================== 坐标系统常量 ====================
    
    /**
     * 世界坐标系的最大X坐标值
     */
    public static final int MAX_COORDINATE_X = 1000;
    
    /**
     * 世界坐标系的最大Y坐标值  
     */
    public static final int MAX_COORDINATE_Y = 1000;
    
    /**
     * 世界坐标系的最小坐标值
     */
    public static final int MIN_COORDINATE = 0;
    
    // ==================== 运单相关常量 ====================
    
    /**
     * 默认包裹数量
     */
    public static final int DEFAULT_PACKAGE_COUNT = 1;
    
    /**
     * 运单ID最大长度
     */
    public static final int MAX_SHIPMENT_ID_LENGTH = 50;
    
    /**
     * 客户姓名最大长度
     */
    public static final int MAX_CUSTOMER_NAME_LENGTH = 100;
    
    /**
     * 特殊说明最大长度
     */
    public static final int MAX_SPECIAL_INSTRUCTIONS_LENGTH = 500;
    
    // ==================== 包裹重量常量 ====================
    
    /**
     * 包裹最小重量 (kg)
     */
    public static final double MIN_PACKAGE_WEIGHT = 0.1;
    
    /**
     * 包裹最大重量 (kg)
     */
    public static final double MAX_PACKAGE_WEIGHT = 50.0;
    
    // ==================== 用户相关常量 ====================
    
    /**
     * 用户名最大长度
     */
    public static final int MAX_USERNAME_LENGTH = 50;
    
    /**
     * 用户名最小长度
     */
    public static final int MIN_USERNAME_LENGTH = 3;
    
    /**
     * 密码最小长度
     */
    public static final int MIN_PASSWORD_LENGTH = 8;
    
    /**
     * 密码最大长度
     */
    public static final int MAX_PASSWORD_LENGTH = 128;
    
    // ==================== 业务规则常量 ====================
    
    /**
     * 追踪号长度
     */
    public static final int TRACKING_NUMBER_LENGTH = 12;
    
    /**
     * 追踪号前缀
     */
    public static final String TRACKING_NUMBER_PREFIX = "UPS";
    
    /**
     * 默认运输优先级
     */
    public static final String DEFAULT_PRIORITY = "STANDARD";
    
    // ==================== 缓存相关常量 ====================
    
    /**
     * JWT令牌缓存过期时间 (秒)
     */
    public static final long JWT_TOKEN_CACHE_EXPIRY = 3600; // 1 hour
    
    /**
     * 用户会话缓存过期时间 (秒)
     */
    public static final long USER_SESSION_CACHE_EXPIRY = 1800; // 30 minutes
    
    // ==================== 系统限制常量 ====================
    
    /**
     * 分页查询默认页面大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * 分页查询最大页面大小
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * 并发处理最大重试次数
     */
    public static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * 数据库连接超时时间 (毫秒)
     */
    public static final int DATABASE_CONNECTION_TIMEOUT = 30000; // 30 seconds
    
    // ==================== 正则表达式常量 ====================
    
    /**
     * 邮箱验证正则表达式
     */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    /**
     * 电话号码验证正则表达式
     */
    public static final String PHONE_REGEX = "^[\\+]?[1-9][\\d\\s\\-\\(\\)]{7,15}$";
    
    /**
     * 运输优先级验证正则表达式
     */
    public static final String PRIORITY_REGEX = "STANDARD|EXPRESS|OVERNIGHT";
    
    // ==================== 错误消息常量 ====================
    
    /**
     * 通用业务异常消息
     */
    public static final String GENERIC_BUSINESS_ERROR = "A business error occurred while processing your request";
    
    /**
     * 用户未找到错误消息
     */
    public static final String USER_NOT_FOUND_ERROR = "User not found";
    
    /**
     * 运单未找到错误消息
     */
    public static final String SHIPMENT_NOT_FOUND_ERROR = "Shipment not found";
    
    /**
     * 无效凭据错误消息
     */
    public static final String INVALID_CREDENTIALS_ERROR = "Invalid username or password";
}