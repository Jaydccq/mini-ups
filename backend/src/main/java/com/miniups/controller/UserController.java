/**
 * User Management Controller
 * 
 * Features:
 * - Provides API endpoints for user information query and management
 * - Supports user information updates and status management
 * - Provides admin user management functionality
 * - Uses unified exception handling through GlobalExceptionHandler
 * 
 * API Endpoints:
 * - GET /api/users/profile: Get current user information
 * - PUT /api/users/profile: Update current user information
 * - GET /api/users/{id}: Get specified user information
 * - GET /api/users: Get user list (admin)
 * - POST /api/users: Create new user (admin)
 * - PUT /api/users/{id}: Update user information (admin)
 * - DELETE /api/users/{id}: Delete user (admin)
 * - POST /api/users/{id}/enable: Enable user (admin)
 * - GET /api/users/{id}/public: Get user public profile
 * 
 * Permission Control:
 * - Regular users can only view and modify their own information
 * - Admins can manage all users
 * - Sensitive operations require ADMIN role
 * 
 *
 
 */
package com.miniups.controller;

import com.miniups.model.dto.user.CreateUserDto;
import com.miniups.model.dto.user.UpdateUserDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.dto.common.ApiResponse;
import com.miniups.model.enums.UserRole;
import com.miniups.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Get current user profile
     * 
     * @return Detailed information of currently logged-in user
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.debug("Get user profile: username={}", username);
        
        UserDto userProfile = userService.getCurrentUserInfo(username);
        
        return ResponseEntity.ok(ApiResponse.success("Get user profile successful", userProfile));
    }
    
    /**
     * Update current user profile
     * 
     * @param updateRequest Update request data
     * @return Updated user information
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> updateCurrentUserProfile(@Valid @RequestBody UpdateUserDto updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.info("Update user profile: username={}", username);
        
        // Get current user information
        UserDto currentUser = userService.getCurrentUserInfo(username);
        
        // Ensure regular users cannot modify admin fields
        UpdateUserDto userSafeRequest = updateRequest.forUserSelfUpdate();
        
        // Update user information
        UserDto updatedUser = userService.updateUser(currentUser.getId(), userSafeRequest);
        
        logger.info("User profile update successful: username={}", username);
        
        return ResponseEntity.ok(ApiResponse.success("User profile update successful", updatedUser));
    }
    
    /**
     * Get specified user information
     * 
     * @param userId User ID
     * @return User information
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.user.id == #userId")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long userId) {
        logger.debug("Get user information: userId={}", userId);
        
        UserDto user = userService.getUserById(userId);
        
        return ResponseEntity.ok(ApiResponse.success("Get user information successful", user));
    }
    
    /**
     * Get user list (admin function)
     * 
     * @param role Optional role filter
     * @param pageable Pagination parameters
     * @return User list with pagination info
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers(
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        logger.debug("Get user list: role={}, page={}, size={}", role, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<UserDto> userPage;
        if (role != null) {
            userPage = userService.getUsersByRole(role, pageable);
        } else {
            userPage = userService.getAllUsers(pageable);
        }
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("users", userPage.getContent());
        responseData.put("currentPage", userPage.getNumber());
        responseData.put("totalPages", userPage.getTotalPages());
        responseData.put("totalElements", userPage.getTotalElements());
        responseData.put("size", userPage.getSize());
        responseData.put("hasNext", userPage.hasNext());
        responseData.put("hasPrevious", userPage.hasPrevious());
        
        logger.debug("Get user list successful, total {} users on page {}/{}", 
                    userPage.getNumberOfElements(), userPage.getNumber() + 1, userPage.getTotalPages());
        
        return ResponseEntity.ok(ApiResponse.success("Get user list successful", responseData));
    }
    
    /**
     * Create new user (admin function)
     * 
     * @param createRequest Create user request
     * @return Created user information
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserDto createRequest) {
        logger.info("Admin creating new user: username={}", createRequest.getUsername());
        
        UserDto createdUser = userService.createUser(createRequest);
        
        logger.info("User created successfully: userId={}, username={}", createdUser.getId(), createdUser.getUsername());
        
        return ResponseEntity.ok(ApiResponse.success("User created successfully", createdUser));
    }
    
    /**
     * Update user information (admin function)
     * 
     * @param userId User ID
     * @param updateRequest Update request data
     * @return Updated user information
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long userId,
                                        @Valid @RequestBody UpdateUserDto updateRequest) {
        logger.info("Admin updating user information: userId={}", userId);

        UserDto updatedUser = userService.updateUser(userId, updateRequest);

        logger.info("User information updated successfully: userId={}, username={}", userId, updatedUser.getUsername());
        
        return ResponseEntity.ok(ApiResponse.success("User information updated successfully", updatedUser));
    }

    /**
     * Disable user (admin function)
     *
     * @param userId User ID
     * @return Operation result
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        logger.info("Admin disabling user: userId={}", userId);

        userService.deleteUser(userId);

        logger.info("User disabled successfully: userId={}", userId);
        
        return ResponseEntity.ok(ApiResponse.success("User has been disabled"));
    }

    /**
     * Enable user (admin function)
     *
     * @param userId User ID
     * @return Operation result
     */
    @PostMapping("/{userId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable Long userId) {
        logger.info("Admin enabling user: userId={}", userId);

        userService.enableUser(userId);

        logger.info("User enabled successfully: userId={}", userId);
        
        return ResponseEntity.ok(ApiResponse.success("User has been enabled"));
    }

    /**
     * Get user public profile
     *
     * @param userId User ID
     * @return Public user information
     */
    @GetMapping("/{userId}/public")
    public ResponseEntity<ApiResponse<UserDto>> getUserPublicProfile(@PathVariable Long userId) {
        logger.debug("Fetching user public profile: userId={}", userId);

        UserDto publicProfile = userService.getUserPublicProfile(userId);

        return ResponseEntity.ok(ApiResponse.success("Public profile fetched successfully", publicProfile));
    }
}