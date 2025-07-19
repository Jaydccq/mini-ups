/**
 * Exception Metrics Config Test
 * 
 * 测试异常监控配置的高基数修复功能
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionMetricsConfigTest {
    
    @Test
    void testNormalizePathParameters_shouldReplaceNumericIds() {
        String result = ExceptionMetricsConfig.normalizePathParameters("/api/users/123/orders/456");
        assertEquals("/api/users/{id}/orders/{id}", result);
    }
    
    @Test
    void testNormalizePathParameters_shouldReplaceUUIDs() {
        String input = "/api/orders/550e8400-e29b-41d4-a716-446655440000/details";
        String result = ExceptionMetricsConfig.normalizePathParameters(input);
        assertEquals("/api/orders/{uuid}/details", result);
    }
    
    @Test
    void testNormalizePathParameters_shouldReplaceLongParams() {
        String input = "/api/files/very-long-filename-that-is-dynamic.pdf";
        String result = ExceptionMetricsConfig.normalizePathParameters(input);
        assertEquals("/api/files/{param}", result);
    }
    
    @Test
    void testNormalizePathParameters_shouldKeepShortStaticPaths() {
        String input = "/api/users/profile/settings";
        String result = ExceptionMetricsConfig.normalizePathParameters(input);
        assertEquals("/api/users/profile/settings", result);
    }
    
    @Test
    void testNormalizePathParameters_shouldHandleNullInput() {
        String result = ExceptionMetricsConfig.normalizePathParameters(null);
        assertEquals("UNKNOWN", result);
    }
    
    @Test
    void testNormalizePathParameters_shouldHandleMixedCase() {
        String input = "/api/users/123/orders/abc123def456/status";
        String result = ExceptionMetricsConfig.normalizePathParameters(input);
        assertEquals("/api/users/{id}/orders/{param}/status", result);
    }
}