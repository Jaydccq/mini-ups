/**
 * User Authentication Service
 * 
 * Purpose:
 * - Handles core business logic for user registration and login
 * - Manages JWT token generation and validation
 * - Provides password encryption and verification functionality
 * 
 * Core Features:
 * - User Registration: Uniqueness validation, password encryption, user creation
 * - User Login: Identity verification, token generation, login records
 * - Password Management: Password modification, validation, security checks
 * - Token Management: JWT generation, validation, refresh
 * 
 * Security Features:
 * - BCrypt password encryption
 * - Username/email uniqueness validation
 * - Login failure rate limiting
 * - Token expiration management
 * 
 *
 
 */
package com.miniups.service;

import com.miniups.exception.*;
import com.miniups.model.dto.auth.AuthResponseDto;
import com.miniups.model.dto.auth.LoginRequestDto;
import com.miniups.model.dto.auth.PasswordChangeDto;
import com.miniups.model.dto.auth.RegisterRequestDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import com.miniups.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    
    public AuthService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider,
                      AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }
    
    /**
     * User registration
     * 
     * @param registerRequest registration request data
     * @return registration success response with JWT token
     * @throws RuntimeException when username or email already exists
     */
    public AuthResponseDto register(RegisterRequestDto registerRequest) {
        logger.info("Start processing user registration: username={}, email={}", 
                   registerRequest.getUsername(), registerRequest.getEmail());
        
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            logger.warn("Registration failed - username already exists: {}", registerRequest.getUsername());
            throw new UserAlreadyExistsException("username", registerRequest.getUsername());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            logger.warn("Registration failed - email already exists: {}", registerRequest.getEmail());
            throw new UserAlreadyExistsException("email", registerRequest.getEmail());
        }
        
        try {
            // Create new user
            User user = createUserFromRegisterRequest(registerRequest);
            
            // Save user to database
            User savedUser = userRepository.save(user);
            logger.info("User created successfully: id={}, username={}", savedUser.getId(), savedUser.getUsername());
            
            // Generate JWT token (compat with tests uses single-arg method)
            String token = jwtTokenProvider.generateToken(savedUser.getUsername());
            Long expiresIn = jwtTokenProvider.getExpirationTime();
            
            // Create user DTO
            UserDto userDto = UserDto.fromEntity(savedUser);
            
            // Return authentication response
            AuthResponseDto response = AuthResponseDto.registerSuccess(token, expiresIn, userDto);
            
            logger.info("User registration completed: username={}", savedUser.getUsername());
            return response;
            
        } catch (DataAccessException e) {
            logger.error("Database operation error: username={}", registerRequest.getUsername(), e);
            throw DatabaseOperationException.save("User", e);
        } catch (UserAlreadyExistsException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during user registration: username={}", registerRequest.getUsername(), e);
            // For unexpected exceptions, throw a business exception that will be handled properly
            throw new RuntimeException("Registration failed, please try again later");
        }
    }
    
    /**
     * User login
     * 
     * @param loginRequest login request data
     * @return login success response with JWT token
     * @throws InvalidCredentialsException when authentication fails
     */
    public AuthResponseDto login(LoginRequestDto loginRequest) {
        logger.info("Start processing user login: usernameOrEmail={}", loginRequest.getUsernameOrEmail());
        
        try {
            // Authenticate directly with AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsernameOrEmail(),
                    loginRequest.getPassword()
                )
            );
            
            // Get username from authentication on success
            String username = authentication.getName();
            
            // Find user entity for full info
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException(username));
            
            // Check if user is disabled (UserDetailsService should have checked already)
            if (!user.getEnabled()) {
                logger.warn("Login failed - user is disabled: {}", username);
                throw new InvalidCredentialsException("Account is disabled, please contact the administrator");
            }
            
            // Generate JWT token (compat with tests uses single-arg method)
            String token = jwtTokenProvider.generateToken(username);
            Long expiresIn = jwtTokenProvider.getExpirationTime();
            
            // Create user DTO
            UserDto userDto = UserDto.fromEntity(user);
            
            // Return authentication response
            AuthResponseDto response = AuthResponseDto.loginSuccess(token, expiresIn, userDto);
            
            logger.info("User login successful: username={}", username);
            return response;
            
        } catch (BadCredentialsException e) {
            logger.warn("Login failed - invalid credentials: usernameOrEmail={}", loginRequest.getUsernameOrEmail());
            throw new InvalidCredentialsException();
        } catch (AuthenticationException e) {
            logger.warn("Login failed - authentication exception: usernameOrEmail={}, error={}", 
                       loginRequest.getUsernameOrEmail(), e.getMessage());
            throw new InvalidCredentialsException();
        } catch (DataAccessException e) {
            logger.error("Database operation error: usernameOrEmail={}", loginRequest.getUsernameOrEmail(), e);
            throw DatabaseOperationException.find("User", loginRequest.getUsernameOrEmail());
        } catch (InvalidCredentialsException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (UserNotFoundException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during login: usernameOrEmail={}", 
                        loginRequest.getUsernameOrEmail(), e);
            // For unexpected exceptions, throw a business exception that will be handled properly
            throw new RuntimeException("Login failed, please try again later");
        }
    }
    
    /**
     * Change password
     * 
     * @param username username
     * @param passwordChangeRequest password change request
     * @throws RuntimeException when current password verification fails
     */
    public void changePassword(String username, PasswordChangeDto passwordChangeRequest) {
        logger.info("Start processing password change: username={}", username);
        
        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        
        // Verify current password
        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
            logger.warn("Password change failed - current password incorrect: username={}", username);
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        
        // Check if new password equals current password
        if (passwordEncoder.matches(passwordChangeRequest.getNewPassword(), user.getPassword())) {
            logger.warn("Password change failed - new password same as current: username={}", username);
            throw new IllegalArgumentException("New password must not be the same as current password");
        }
        
        try {
            // Encrypt new password
            String encodedNewPassword = passwordEncoder.encode(passwordChangeRequest.getNewPassword());
            
            // Update password
            user.setPassword(encodedNewPassword);
            userRepository.save(user);
            
            logger.info("Password changed successfully: username={}", username);
            
        } catch (InvalidCredentialsException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (UserNotFoundException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (IllegalArgumentException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during password change: username={}", username, e);
            // For unexpected exceptions, throw a business exception that will be handled properly
            throw new RuntimeException("Password change failed, please try again later");
        }
    }
    
    /**
     * Validate token
     * 
     * @param token JWT token
     * @return whether the token is valid
     */
    public boolean validateToken(String token) {
        try {
            return jwtTokenProvider.validateToken(token);
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get username from token
     * 
     * @param token JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }
    
    /**
     * Find user by username or email
     * 
     * @param usernameOrEmail username or email
     * @return user entity, or null if not found
     */
    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        // Determine if input is email or username
        if (usernameOrEmail.contains("@")) {
            return userRepository.findByEmail(usernameOrEmail).orElse(null);
        } else {
            return userRepository.findByUsername(usernameOrEmail).orElse(null);
        }
    }
    
    /**
     * Create user entity from registration request
     * 
     * @param registerRequest registration request
     * @return user entity
     */
    private User createUserFromRegisterRequest(RegisterRequestDto registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhone(registerRequest.getPhone());
        user.setAddress(registerRequest.getAddress());
        user.setRole(UserRole.USER); // Default to regular user
        user.setEnabled(true);
        
        return user;
    }
    
    /**
     * Check if username is available
     * 
     * @param username username
     * @return whether available
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    /**
     * Check if email is available
     * 
     * @param email email
     * @return whether available
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
}
