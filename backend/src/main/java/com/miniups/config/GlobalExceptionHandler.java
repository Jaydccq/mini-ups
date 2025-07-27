/**
 * Global Exception Handler
 *
 * Functionality:
 * - Unified handling of all exceptions in the application
 * - Provides standardized error response format
 * - Prevents sensitive information leakage
 *
 * Exception Types Handled:
 * - Authentication Exceptions: login failures, invalid tokens, etc.
 * - Authorization Exceptions: insufficient permissions, role mismatches, etc.
 * - Validation Exceptions: input format errors, missing required fields, etc.
 * - Business Exceptions: user not found, duplicate email, etc.
 * - System Exceptions: database errors, network issues, etc.
 *
 * Security Features:
 * - Does not expose internal system information
 * - Records detailed error logs for debugging
 * - Returns user-friendly error messages
 *
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.config;

import com.miniups.exception.*;
import com.miniups.model.dto.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Value("${app.environment:production}")
    private String environment;
    
    @Value("${app.exception.include-stack-trace:false}")
    private boolean includeStackTrace;
    
    private final ExceptionMetricsConfig exceptionMetrics;
    
    public GlobalExceptionHandler(ExceptionMetricsConfig exceptionMetrics) {
        this.exceptionMetrics = exceptionMetrics;
    }

    /**
     * 统一构建错误响应的方法
     * 
     * @param ex 异常对象
     * @param errorCode 错误代码
     * @param defaultMessage 默认错误消息
     * @param status HTTP状态码
     * @param data 可选的附加数据
     * @return 标准化的错误响应
     */
    private <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(
            Exception ex, String errorCode, String defaultMessage, HttpStatus status, T data) {
        
        recordExceptionMetrics(ex);
        
        String message = defaultMessage;
        if (ex instanceof BaseBusinessException) {
            message = ex.getMessage();
        } else if ("development".equals(environment)) {
            message = ex.getMessage() != null ? ex.getMessage() : defaultMessage;
        }
        
        if (status.is5xxServerError()) {
            logger.error("Exception handled: code={}, message={}, type={}", 
                        errorCode, message, ex.getClass().getSimpleName(), ex);
        } else if (status.is4xxClientError()) {
            logger.warn("Client error handled: code={}, message={}, type={}", 
                       errorCode, message, ex.getClass().getSimpleName());
        } else {
            logger.info("Exception handled: code={}, message={}, type={}", 
                       errorCode, message, ex.getClass().getSimpleName());
        }
        
        ApiResponse<T> response = ApiResponse.error(errorCode, message);
        if (data != null) {
            response.setData(data);
        }
        
        return new ResponseEntity<>(response, status);
    }
    

    private void recordExceptionMetrics(Exception exception) {
        if (exceptionMetrics != null) {
            String endpoint = ExceptionMetricsConfig.getCurrentEndpoint();
            exceptionMetrics.recordException(exception, endpoint);
        }
    }

    /**
     * Handle user already exists exception
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, WebRequest request) {
        return buildErrorResponse(ex, "USER_ALREADY_EXISTS", ex.getMessage(), HttpStatus.BAD_REQUEST, null);
    }

    /**
     * Handle user not found exception
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(
            UserNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex, "USER_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND, null);
    }

    /**
     * Handle invalid credentials exception
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialsException(
            InvalidCredentialsException ex, WebRequest request) {
        return buildErrorResponse(ex, "INVALID_CREDENTIALS", ex.getMessage(), HttpStatus.UNAUTHORIZED, null);
    }

    /**
     * Handle input validation exception
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        return buildErrorResponse(ex, "VALIDATION_ERROR", "Please check input data format", HttpStatus.BAD_REQUEST, fieldErrors);
    }

    /**
     * Handle authentication exception
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        return buildErrorResponse(ex, "AUTHENTICATION_ERROR", "Invalid username or password", HttpStatus.UNAUTHORIZED, null);
    }

    /**
     * Handle bad credentials exception
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        return buildErrorResponse(ex, "BAD_CREDENTIALS", "Invalid username or password", HttpStatus.UNAUTHORIZED, null);
    }

    /**
     * Handle access denied exception
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        return buildErrorResponse(ex, "ACCESS_DENIED", "You do not have permission to perform this operation", HttpStatus.FORBIDDEN, null);
    }

    /**
     * Handle authorization denied exception (Spring Security 6.x)
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(
            AuthorizationDeniedException ex, WebRequest request) {
        return buildErrorResponse(ex, "ACCESS_DENIED", "You do not have permission to perform this operation", HttpStatus.FORBIDDEN, null);
    }

    /*
     * RuntimeException handling removed - these exceptions should be handled by
     * the global Exception handler with 500 status code to properly indicate
     * server-side errors rather than client errors (400).
     * 
     * If specific RuntimeException subclasses need custom handling, 
     * add explicit handlers for those specific types instead.
     */

    /**
     * Handle illegal argument exception
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Invalid request parameters";
        return buildErrorResponse(ex, "INVALID_PARAMETER", message, HttpStatus.BAD_REQUEST, null);
    }

    /**
     * Handle null pointer exception
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(
            NullPointerException ex, WebRequest request) {
        return buildErrorResponse(ex, "SYSTEM_ERROR", "Internal server error, please try again later", HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    /**
     * Handle external service exceptions
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalServiceException(
            ExternalServiceException ex, WebRequest request) {
        String userMessage = "development".equals(environment) 
            ? ex.getMessage() 
            : "External service is temporarily unavailable, please try again later";
        return buildErrorResponse(ex, ex.getErrorCode(), userMessage, HttpStatus.SERVICE_UNAVAILABLE, null);
    }

    /**
     * Handle database operation exceptions
     */
    @ExceptionHandler(DatabaseOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseOperationException(
            DatabaseOperationException ex, WebRequest request) {
        String userMessage = "development".equals(environment) 
            ? ex.getMessage() 
            : "Data operation failed, please try again later";
        return buildErrorResponse(ex, ex.getErrorCode(), userMessage, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex, ex.getErrorCode(), ex.getMessage(), HttpStatus.NOT_FOUND, null);
    }

    /**
     * Handle duplicate resource exceptions
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        return buildErrorResponse(ex, ex.getErrorCode(), ex.getMessage(), HttpStatus.CONFLICT, null);
    }

    /**
     * Handle business validation exceptions
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusinessValidationException(
            BusinessValidationException ex, WebRequest request) {
        return buildErrorResponse(ex, ex.getErrorCode(), ex.getMessage(), HttpStatus.BAD_REQUEST, ex.getValidationDetails());
    }

    /**
     * Handle data access exceptions
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        String userMessage = "development".equals(environment) 
            ? "Database access error: " + ex.getMessage() 
            : "Data access error, please try again later";
        return buildErrorResponse(ex, "DATA_ACCESS_ERROR", userMessage, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    /**
     * Handle data integrity violation exceptions
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        String userMessage = "Data integrity constraint violation. Please check your input data.";
        if ("development".equals(environment)) {
            userMessage += " Details: " + ex.getMessage();
        }
        return buildErrorResponse(ex, "DATA_INTEGRITY_ERROR", userMessage, HttpStatus.BAD_REQUEST, null);
    }

    /**
     * Handle HTTP method not supported exceptions
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String supportedMethods = String.join(", ", ex.getSupportedMethods());
        String message = String.format("HTTP method '%s' is not supported. Supported methods: %s", 
                                     ex.getMethod(), supportedMethods);
        return buildErrorResponse(ex, "METHOD_NOT_SUPPORTED", message, HttpStatus.METHOD_NOT_ALLOWED, null);
    }

    /**
     * Handle missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        String message = String.format("Required parameter '%s' of type '%s' is missing", 
                                     ex.getParameterName(), ex.getParameterType());
        return buildErrorResponse(ex, "MISSING_PARAMETER", message, HttpStatus.BAD_REQUEST, null);
    }

    /**
     * Handle base business exceptions
     */
    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseBusinessException(
            BaseBusinessException ex, WebRequest request) {
        return buildErrorResponse(ex, ex.getErrorCode(), ex.getMessage(), HttpStatus.BAD_REQUEST, null);
    }


    /**
     * Handle all other uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {
        String userMessage = "development".equals(environment) 
            ? "System error: " + ex.getMessage()
            : "Internal server error, please try again later";
        return buildErrorResponse(ex, "SYSTEM_ERROR", userMessage, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }
    
}