/**
 * User Authentication Controller
 * 
 * Features:
 * - Handles user registration, login, password change and other authentication-related requests
 * - Provides JWT token issuance and validation functionality
 * - Supports user account status management
 * 
 * API Endpoints:
 * - POST /api/auth/register: User registration
 * - POST /api/auth/login: User login
 * - POST /api/auth/logout: User logout (optional)
 * - POST /api/auth/change-password: Change password
 * - GET /api/auth/me: Get current user information
 * - POST /api/auth/refresh: Refresh token (optional)
 * 
 * Security Features:
 * - Input validation and error handling
 * - Login failure attempt limiting
 * - JWT token security management
 * - CORS cross-origin support
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.controller;

import com.miniups.model.dto.auth.AuthResponseDto;
import com.miniups.model.dto.auth.LoginRequestDto;
import com.miniups.model.dto.auth.PasswordChangeDto;
import com.miniups.model.dto.auth.RegisterRequestDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.dto.common.ApiResponse;
import com.miniups.service.AuthService;
import com.miniups.service.UserService;
import com.miniups.service.TokenBlacklistService;
import com.miniups.security.JwtTokenProvider;
import com.miniups.exception.BusinessValidationException;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    
    public AuthController(AuthService authService, UserService userService, 
                         TokenBlacklistService tokenBlacklistService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.userService = userService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * User registration
     * 
     * @param registerRequest Registration request data
     * @return Registration success response and JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> registerUser(@Valid @RequestBody RegisterRequestDto registerRequest) {
        logger.info("User registration request: username={}, email={}", 
                   registerRequest.getUsername(), registerRequest.getEmail());
        
        AuthResponseDto response = authService.register(registerRequest);
        
        logger.info("User registration successful: username={}", registerRequest.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User registration successful", response));
    }
    
    /**
     * User login
     * 
     * @param loginRequest Login request data
     * @return Login success response and JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        logger.info("User login request: usernameOrEmail={}", loginRequest.getUsernameOrEmail());
        
        AuthResponseDto response = authService.login(loginRequest);
        
        logger.info("User login successful: usernameOrEmail={}", loginRequest.getUsernameOrEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    /**
     * Get current user information
     * 
     * @return Detailed information of currently logged-in user
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.debug("Get current user information request: username={}", username);
        
        UserDto currentUser = userService.getCurrentUserInfo(username);
        
        return ResponseEntity.ok(ApiResponse.success("Get user information successful", currentUser));
    }
    
    /**
     * Change password
     * 
     * @param passwordChangeRequest Password change request data
     * @return Password change result
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeDto passwordChangeRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.info("User change password request: username={}", username);
        
        // Validate new password confirmation
        if (!passwordChangeRequest.isPasswordsMatch()) {
            throw new BusinessValidationException("PASSWORD_MISMATCH", "New password does not match confirmation password");
        }
        
        authService.changePassword(username, passwordChangeRequest);
        
        logger.info("User password change successful: username={}", username);
        
        return ResponseEntity.ok(ApiResponse.success("Password change successful"));
    }
    
    /**
     * User logout
     * 
     * @param request HTTP request to extract JWT token
     * @return Logout success response
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logoutUser(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.info("User logout request: username={}", username);
        
        try {
            // Extract JWT token from request
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                // Get token ID and remaining time
                String tokenId = jwtTokenProvider.getTokenIdFromToken(jwt);
                long remainingTime = jwtTokenProvider.getRemainingTimeInSeconds(jwt);
                
                if (tokenId != null && remainingTime > 0) {
                    // Add token to blacklist
                    tokenBlacklistService.blacklistToken(tokenId, remainingTime);
                    logger.info("Token blacklisted successfully: tokenId={}, remainingTime={}s", tokenId, remainingTime);
                }
            }
        } catch (Exception e) {
            logger.error("Error during token blacklisting: username={}", username, e);
            // Don't fail the logout process if blacklisting fails
        }
        
        // Clear security context
        SecurityContextHolder.clearContext();
        
        logger.info("User logout successful: username={}", username);
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }
    
    /**
     * Extract JWT token from request Authorization header
     * 
     * @param request HTTP request
     * @return JWT token string or null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * Validate token validity
     * 
     * @return Token validation result
     */
    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("authenticated", true);
        
        return ResponseEntity.ok(ApiResponse.success("Token valid", data));
    }
    
    /**
     * Check if username is available
     * 
     * @param username Username to check
     * @return Availability check result
     */
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkUsernameAvailability(@RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        
        Map<String, Object> data = new HashMap<>();
        data.put("available", available);
        
        String message = available ? "Username available" : "Username already taken";
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
    /**
     * Check if email is available
     * 
     * @param email Email to check
     * @return Availability check result
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkEmailAvailability(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        
        Map<String, Object> data = new HashMap<>();
        data.put("available", available);
        
        String message = available ? "Email available" : "Email already taken";
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
}
