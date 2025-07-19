/**
 * Exception Severity Enum
 * 
 * 异常严重等级枚举，用于分类异常的严重程度
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.model.enums;

public enum ExceptionSeverity {
    
    /**
     * 低级 - 用户输入错误、验证失败等
     */
    LOW("低级", "用户错误或轻微问题"),
    
    /**
     * 中级 - 业务逻辑问题、权限问题等
     */
    MEDIUM("中级", "业务逻辑问题"),
    
    /**
     * 高级 - 系统错误、外部服务故障、数据库问题等
     */
    HIGH("高级", "系统或基础设施问题"),
    
    /**
     * 严重 - 影响整个系统运行的关键错误
     */
    CRITICAL("严重", "系统关键故障");
    
    private final String chineseName;
    private final String description;
    
    ExceptionSeverity(String chineseName, String description) {
        this.chineseName = chineseName;
        this.description = description;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name();
    }
}