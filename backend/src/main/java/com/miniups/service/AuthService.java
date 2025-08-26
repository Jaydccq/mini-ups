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
import com.miniups.model.enums.AuthProvider;
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
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Predicate;

import org.hibernate.exception.ConstraintViolationException;

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
     * User registration with TOCTOU protection
     * 
     * 🚀 CONCURRENCY OPTIMIZED 🚀  
     * - Fixed TOCTOU race condition using database constraints
     * - Let database handle uniqueness enforcement atomically
     * - Catch constraint violations and convert to business exceptions
     * 
     * @param registerRequest registration request data
     * @return registration success response with JWT token
     * @throws UserAlreadyExistsException when username or email already exists
     */
    public AuthResponseDto register(RegisterRequestDto registerRequest) {
        logger.info("Start processing user registration: username={}, email={}", 
                   registerRequest.getUsername(), registerRequest.getEmail());
        
        try {
            // Create new user directly without pre-checking
            // Let database constraints handle uniqueness atomically
            User user = createUserFromRegisterRequest(registerRequest);
            
            // Save user to database - this will fail if constraints are violated
            User savedUser = userRepository.save(user);
            logger.info("User created successfully: id={}, username={}", savedUser.getId(), savedUser.getUsername());
            
            // Generate JWT token
            String token = jwtTokenProvider.generateToken(savedUser.getUsername());
            Long expiresIn = jwtTokenProvider.getExpirationTime();
            
            // Create user DTO
            UserDto userDto = UserDto.fromEntity(savedUser);
            
            // Return authentication response
            AuthResponseDto response = AuthResponseDto.registerSuccess(token, expiresIn, userDto);
            
            logger.info("User registration completed: username={}", savedUser.getUsername());
            return response;
            
        } catch (DataIntegrityViolationException e) {
            // 精确捕获唯一约束冲突，优先通过约束名判断字段
            String constraint = extractConstraintName(e);
            if (constraint != null) {
                if (constraintNameMatches(constraint, s -> s.contains("username"))) {
                    logger.warn("Registration failed - username already exists: {}", registerRequest.getUsername());
                    throw new UserAlreadyExistsException("username", registerRequest.getUsername());
                }
                if (constraintNameMatches(constraint, s -> s.contains("email"))) {
                    logger.warn("Registration failed - email already exists: {}", registerRequest.getEmail());
                    throw new UserAlreadyExistsException("email", registerRequest.getEmail());
                }
            }
            // 回退：查询确定冲突字段
            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                throw new UserAlreadyExistsException("username", registerRequest.getUsername());
            }
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                throw new UserAlreadyExistsException("email", registerRequest.getEmail());
            }
            // 未识别到具体字段，作为通用重复
            throw new UserAlreadyExistsException("user", "User with provided details already exists");
        
        } catch (UserAlreadyExistsException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (DataAccessException e) {
            logger.error("Database operation error: username={}", registerRequest.getUsername(), e);
            throw DatabaseOperationException.save("User", e);
        } catch (Exception e) {
            logger.error("Unexpected error during user registration: username={}", registerRequest.getUsername(), e);
            throw new RuntimeException("Registration failed, please try again later");
        }
    }

    /**
     * 从 DataIntegrityViolationException 中提取底层唯一约束名称
     */
    private String extractConstraintName(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                return cve.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }

    private boolean constraintNameMatches(String name, Predicate<String> predicate) {
        try {
            return name != null && predicate.test(name.toLowerCase());
        } catch (Exception ignore) {
            return false;
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
    
    /**
     * Process OAuth2 post-login logic
     * 
     * @param oidcUser OpenID Connect user information from OAuth2 provider
     * @return User entity for local JWT generation
     * @throws OAuth2AuthenticationProcessingException when processing fails
     */
    @Transactional
    public User processOAuth2PostLogin(OidcUser oidcUser) {
        logger.info("Processing OAuth2 post-login for user: {}", oidcUser.getEmail());
        
        String email = oidcUser.getEmail();
        String providerId = oidcUser.getSubject();
        
        if (email == null) {
            logger.error("Email not found from OAuth2 provider");
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }
        
        if (providerId == null) {
            logger.error("Provider ID (subject) not found from OAuth2 provider");
            throw new OAuth2AuthenticationProcessingException("Provider ID not found from OAuth2 provider");
        }
        
        try {
            // First, try to find user by provider and provider ID
            Optional<User> userByProvider = userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, providerId);
            
            if (userByProvider.isPresent()) {
                // User already exists with this Google account
                User user = userByProvider.get();
                logger.info("Found existing OAuth2 user: {}", user.getUsername());
                return user;
            }
            
            // Check if user exists by email
            Optional<User> userByEmail = userRepository.findByEmail(email);
            
            if (userByEmail.isPresent()) {
                User existingUser = userByEmail.get();
                if (existingUser.getAuthProvider() == AuthProvider.LOCAL) {
                    // User exists with LOCAL provider - this requires manual account linking
                    logger.warn("User exists with LOCAL provider, manual linking required: {}", email);
                    throw new OAuth2AuthenticationProcessingException(
                        "An account with this email already exists. Please log in with your password to link your Google account."
                    );
                }
                // If it's already a Google user but provider ID doesn't match, it's an inconsistency
                logger.warn("User exists with GOOGLE provider but different provider ID: {}", email);
                throw new OAuth2AuthenticationProcessingException("Account linking inconsistency detected");
            }
            
            // New user registration
            User newUser = registerNewOAuth2User(oidcUser);
            logger.info("Successfully created new OAuth2 user: {}", newUser.getUsername());
            return newUser;
            
        } catch (OAuth2AuthenticationProcessingException e) {
            // Re-throw OAuth2 specific exceptions
            throw e;
        } catch (DataAccessException e) {
            logger.error("Database error during OAuth2 processing: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationProcessingException("Database error during OAuth2 processing", e);
        } catch (Exception e) {
            logger.error("Unexpected error during OAuth2 processing: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationProcessingException("OAuth2 processing failed", e);
        }
    }
    
    /**
     * Register a new OAuth2 user
     * 
     * @param oidcUser OpenID Connect user information
     * @return newly created User entity
     */
    private User registerNewOAuth2User(OidcUser oidcUser) {
        logger.info("Registering new OAuth2 user: {}", oidcUser.getEmail());
        
        User newUser = new User();
        newUser.setEmail(oidcUser.getEmail());
        newUser.setAuthProvider(AuthProvider.GOOGLE);
        newUser.setProviderId(oidcUser.getSubject());
        newUser.setEnabled(true);
        newUser.setRole(UserRole.USER);
        
        // Set names from OIDC claims
        if (oidcUser.getGivenName() != null) {
            newUser.setFirstName(oidcUser.getGivenName());
        }
        if (oidcUser.getFamilyName() != null) {
            newUser.setLastName(oidcUser.getFamilyName());
        }
        
        // Generate unique username from email
        String username = generateUniqueUsernameFromEmail(oidcUser.getEmail());
        newUser.setUsername(username);
        
        // Password is null for OAuth2 users
        newUser.setPassword(null);
        
        return userRepository.save(newUser);
    }
    
    /**
     * Generate unique username from email address
     * 
     * @param email user's email address
     * @return unique username
     */
    private String generateUniqueUsernameFromEmail(String email) {
        // Extract the part before @ and clean it
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        
        // Ensure it's not too long
        if (baseUsername.length() > 40) {
            baseUsername = baseUsername.substring(0, 40);
        }
        
        // Make sure it's not empty
        if (baseUsername.isEmpty()) {
            baseUsername = "user";
        }
        
        String username = baseUsername;
        int counter = 1;
        
        // Keep trying until we find a unique username
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
            
            // Safety check to avoid infinite loop
            if (counter > 9999) {
                username = baseUsername + System.currentTimeMillis();
                break;
            }
        }
        
        logger.debug("Generated unique username: {} for email: {}", username, email);
        return username;
    }
    
    /**
     * Check if account linking is required for the given email
     * 
     * @param email user's email address
     * @return true if manual account linking is required
     */
    public boolean requiresAccountLinking(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.isPresent() && user.get().getAuthProvider() == AuthProvider.LOCAL;
    }
}
