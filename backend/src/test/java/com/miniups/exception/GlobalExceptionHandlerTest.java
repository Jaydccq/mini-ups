/**
 * Global Exception Handler Test
 * 
 * 测试全局异常处理器的功能
 * 
 *
 
 */
package com.miniups.exception;

import com.miniups.config.ExceptionMetricsConfig;
import com.miniups.config.GlobalExceptionHandler;
import com.miniups.model.dto.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    
    @Mock
    private ExceptionMetricsConfig exceptionMetrics;
    
    @Mock
    private WebRequest webRequest;
    
    private GlobalExceptionHandler globalExceptionHandler;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        globalExceptionHandler = new GlobalExceptionHandler(exceptionMetrics);
    }
    
    @Test
    void testHandleUserNotFoundException() {
        // 准备测试数据
        UserNotFoundException exception = new UserNotFoundException("testuser");
        
        // 执行测试
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleUserNotFoundException(exception, webRequest);
        
        // 验证结果
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getError());
        assertFalse(response.getBody().isSuccess());
        
        // 验证异常监控被调用
        verify(exceptionMetrics, times(1)).recordException(eq(exception), anyString());
    }
    
    @Test
    void testHandleResourceNotFoundException() {
        // 准备测试数据
        ResourceNotFoundException exception = ResourceNotFoundException.shipment(123L);
        
        // 执行测试
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleResourceNotFoundException(exception, webRequest);
        
        // 验证结果
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Shipment"));
        assertTrue(response.getBody().getMessage().contains("123"));
        
        // 验证异常监控被调用
        verify(exceptionMetrics, times(1)).recordException(eq(exception), anyString());
    }
    
    @Test
    void testHandleDuplicateResourceException() {
        // 准备测试数据
        DuplicateResourceException exception = DuplicateResourceException.email("test@example.com");
        
        // 执行测试
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleDuplicateResourceException(exception, webRequest);
        
        // 验证结果
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("test@example.com"));
        
        // 验证异常监控被调用
        verify(exceptionMetrics, times(1)).recordException(eq(exception), anyString());
    }
    
    @Test
    void testHandleBusinessValidationException() {
        // 准备测试数据
        BusinessValidationException exception = BusinessValidationException.invalidShipmentStatus("PENDING", "DELIVERED");
        
        // 执行测试
        ResponseEntity<ApiResponse<Map<String, Object>>> response = globalExceptionHandler.handleBusinessValidationException(exception, webRequest);
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("PENDING"));
        assertTrue(response.getBody().getMessage().contains("DELIVERED"));
        
        // 验证异常监控被调用
        verify(exceptionMetrics, times(1)).recordException(eq(exception), anyString());
    }
    
    @Test
    void testHandleExternalServiceException() {
        // 准备测试数据
        ExternalServiceException exception = ExternalServiceException.amazonService("Connection timeout");
        
        // 执行测试
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleExternalServiceException(exception, webRequest);
        
        // 验证结果
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Amazon"));
        
        // 验证异常监控被调用
        verify(exceptionMetrics, times(1)).recordException(eq(exception), anyString());
    }
    
    @Test
    void testHandleDatabaseOperationException() {
        // 准备测试数据
        DatabaseOperationException exception = DatabaseOperationException.save("User", new RuntimeException("Database error"));
        
        // 执行测试
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleDatabaseOperationException(exception, webRequest);
        
        // 验证结果
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getError());
        
        // 验证异常监控被调用
        verify(exceptionMetrics, times(1)).recordException(eq(exception), anyString());
    }
    
    @Test
    void testHandleGlobalException() {
        // 准备测试数据
        RuntimeException exception = new RuntimeException("Unexpected error");
        
        // 执行测试
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleGlobalException(exception, webRequest);
        
        // 验证结果
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SYSTEM_ERROR", response.getBody().getError());
        assertFalse(response.getBody().isSuccess());
        
        // 验证异常监控被调用
        verify(exceptionMetrics, times(1)).recordException(eq(exception), anyString());
    }
}