/**
 * User Management Service
 * 
 * Purpose:
 * - Provides user information query and management functionality
 * - Supports user information updates and status management
 * - Provides user listing and search functionality
 * 
 * Core Features:
 * - User information CRUD operations
 * - User status management (enable/disable)
 * - User role and permission management
 * - User search and filtering
 * 
 * Access Control:
 * - Regular users can only view and modify their own information
 * - Administrators can manage all users
 * - Sensitive operations require additional permission verification
 * 
 *
 
 */
package com.miniups.service;

import com.miniups.exception.*;
import com.miniups.model.dto.user.CreateUserDto;
import com.miniups.model.dto.user.UpdateUserDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Get current user information
     * 
     * @param username Username
     * @return User information DTO
     * @throws RuntimeException When user not found
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUserInfo(String username) {
        logger.debug("Getting user information: username={}", username);
        
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            logger.error("User not found: username={}", username);
            throw new UserNotFoundException(username);
        }
        
        User user = userOptional.get();
        UserDto userDto = UserDto.fromEntity(user);
        
        logger.debug("Successfully retrieved user information: username={}, id={}", username, user.getId());
        return userDto;
    }
    
    /**
     * Get user information by user ID
     * 
     * @param userId User ID
     * @return User information DTO
     * @throws RuntimeException When user not found
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        logger.debug("Get user information by ID: userId={}", userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User not found: userId={}", userId);
            throw new UserNotFoundException("ID: " + userId);
        }
        
        User user = userOptional.get();
        UserDto userDto = UserDto.fromEntity(user);
        
        logger.debug("Successfully retrieved user information: userId={}, username={}", userId, user.getUsername());
        return userDto;
    }
    
    /**
     * Get user public profile
     * 
     * @param userId User ID
     * @return Public user information DTO (excluding sensitive information)
     */
    @Transactional(readOnly = true)
    public UserDto getUserPublicProfile(Long userId) {
        logger.debug("Getting user public profile: userId={}", userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User not found: userId={}", userId);
            throw new UserNotFoundException("ID: " + userId);
        }
        
        User user = userOptional.get();
        UserDto userDto = UserDto.publicProfile(user);
        
        logger.debug("Successfully retrieved user public profile: userId={}, username={}", userId, user.getUsername());
        return userDto;
    }
    
    /**
     * Update user information
     * 
     * @param userId User ID
     * @param updateRequest Update request data
     * @return Updated user information
     * @throws RuntimeException When user not found or email conflict occurs
     */
    public UserDto updateUser(Long userId, UpdateUserDto updateRequest) {
        logger.info("Update user information: userId={}", userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User not found: userId={}", userId);
            throw new UserNotFoundException("ID: " + userId);
        }
        
        User user = userOptional.get();
        
        try {
            // Update email (if provided and different)
            if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(updateRequest.getEmail())) {
                    logger.warn("Email already in use: email={}", updateRequest.getEmail());
                    throw new UserAlreadyExistsException("email", updateRequest.getEmail());
                }
                user.setEmail(updateRequest.getEmail());
            }
            
            // Update basic information
            if (updateRequest.getFirstName() != null) {
                user.setFirstName(updateRequest.getFirstName());
            }
            if (updateRequest.getLastName() != null) {
                user.setLastName(updateRequest.getLastName());
            }
            if (updateRequest.getPhone() != null) {
                user.setPhone(updateRequest.getPhone());
            }
            if (updateRequest.getAddress() != null) {
                user.setAddress(updateRequest.getAddress());
            }
            
            // Administrator-only field updates
            if (updateRequest.getRole() != null) {
                user.setRole(updateRequest.getRole());
            }
            if (updateRequest.getEnabled() != null) {
                user.setEnabled(updateRequest.getEnabled());
            }
            
            // Save updates
            User updatedUser = userRepository.save(user);
            UserDto userDto = UserDto.fromEntity(updatedUser);
            
            logger.info("User information updated successfully: userId={}, username={}", userId, user.getUsername());
            return userDto;
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Exception occurred while updating user information: userId={}", userId, e);
            throw new RuntimeException("Failed to update user information, please try again later");
        }
    }
    
    /**
     * Create new user (administrator function)
     * 
     * @param createRequest Create user request
     * @return Created user information
     * @throws RuntimeException When username or email already exists
     */
    public UserDto createUser(CreateUserDto createRequest) {
        logger.info("Creating new user: username={}, email={}, role={}", 
                   createRequest.getUsername(), createRequest.getEmail(), createRequest.getRole());
        
        // Check if username already exists
        if (userRepository.existsByUsername(createRequest.getUsername())) {
            logger.warn("Username already exists: username={}", createRequest.getUsername());
            throw new UserAlreadyExistsException("username", createRequest.getUsername());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(createRequest.getEmail())) {
            logger.warn("Email already exists: email={}", createRequest.getEmail());
            throw new UserAlreadyExistsException("email", createRequest.getEmail());
        }
        
        try {
            // Create user entity
            User user = createUserFromCreateRequest(createRequest);
            
            // Save user
            User savedUser = userRepository.save(user);
            UserDto userDto = UserDto.fromEntity(savedUser);
            
            logger.info("User created successfully: id={}, username={}, role={}", 
                       savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
            return userDto;
            
        } catch (Exception e) {
            logger.error("Exception occurred while creating user: username={}", createRequest.getUsername(), e);
            throw new RuntimeException("Failed to create user, please try again later");
        }
    }
    
    /**
     * Delete user (soft delete - disable account)
     * 
     * @param userId User ID
     */
    public void deleteUser(Long userId) {
        logger.info("Deleting user: userId={}", userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User not found: userId={}", userId);
            throw new UserNotFoundException("ID: " + userId);
        }
        
        User user = userOptional.get();
        
        // Soft delete - disable account
        user.setEnabled(false);
        userRepository.save(user);
        
        logger.info("User disabled: userId={}, username={}", userId, user.getUsername());
    }
    
    /**
     * Enable user account
     * 
     * @param userId User ID
     */
    public void enableUser(Long userId) {
        logger.info("Enabling user: userId={}", userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User not found: userId={}", userId);
            throw new UserNotFoundException("ID: " + userId);
        }
        
        User user = userOptional.get();
        user.setEnabled(true);
        userRepository.save(user);
        
        logger.info("User enabled: userId={}, username={}", userId, user.getUsername());
    }
    
    /**
     * Get all users list
     * 
     * @return User list
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        logger.debug("Getting all users list");
        
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = users.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
        
        logger.debug("Successfully retrieved user list, total {} users", userDtos.size());
        return userDtos;
    }
    
    /**
     * Get all users list with pagination
     * 
     * @param pageable Pagination parameters
     * @return User page
     */
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        logger.debug("Getting all users list with pagination: page={}, size={}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> userPage = userRepository.findAll(pageable);
        Page<UserDto> userDtoPage = userPage.map(UserDto::fromEntity);
        
        logger.debug("Successfully retrieved user page, total {} users on page {}/{}", 
                    userDtoPage.getNumberOfElements(), userDtoPage.getNumber() + 1, userDtoPage.getTotalPages());
        return userDtoPage;
    }
    
    /**
     * Get users list by role
     * 
     * @param role User role
     * @return User list of specified role
     */
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(UserRole role) {
        logger.debug("Getting users list by role: role={}", role);
        
        // Use repository method for better performance instead of filtering all users
        List<User> users = userRepository.findByRole(role);
        
        List<UserDto> userDtos = users.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
        
        logger.debug("Successfully retrieved role user list: role={}, count={}", role, userDtos.size());
        return userDtos;
    }
    
    /**
     * Get users list by role with pagination
     * 
     * @param role User role
     * @param pageable Pagination parameters
     * @return User page of specified role
     */
    @Transactional(readOnly = true)
    public Page<UserDto> getUsersByRole(UserRole role, Pageable pageable) {
        logger.debug("Getting users list by role with pagination: role={}, page={}, size={}", 
                    role, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> userPage = userRepository.findByRole(role, pageable);
        Page<UserDto> userDtoPage = userPage.map(UserDto::fromEntity);
        
        logger.debug("Successfully retrieved role user page: role={}, total {} users on page {}/{}", 
                    role, userDtoPage.getNumberOfElements(), userDtoPage.getNumber() + 1, userDtoPage.getTotalPages());
        return userDtoPage;
    }
    
    /**
     * Check if username is available
     * 
     * @param username Username
     * @return Whether available
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    /**
     * Check if email is available
     * 
     * @param email Email
     * @return Whether available
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
    
    /**
     * Create user entity from create user request
     * 
     * @param createRequest Create user request
     * @return User entity
     */
    private User createUserFromCreateRequest(CreateUserDto createRequest) {
        User user = new User();
        user.setUsername(createRequest.getUsername());
        user.setEmail(createRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createRequest.getPassword()));
        user.setFirstName(createRequest.getFirstName());
        user.setLastName(createRequest.getLastName());
        user.setPhone(createRequest.getPhone());
        user.setAddress(createRequest.getAddress());
        user.setRole(createRequest.getRole());
        user.setEnabled(createRequest.getEnabled());
        
        return user;
    }
}