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
 * @author Mini-UPS Team
 * @version 1.0.0
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
     * 用户注册
     * 
     * @param registerRequest 注册请求数据
     * @return 注册成功响应和JWT令牌
     * @throws RuntimeException 当用户名或邮箱已存在时抛出
     */
    public AuthResponseDto register(RegisterRequestDto registerRequest) {
        logger.info("开始处理用户注册: username={}, email={}", 
                   registerRequest.getUsername(), registerRequest.getEmail());
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            logger.warn("注册失败 - 用户名已存在: {}", registerRequest.getUsername());
            throw new UserAlreadyExistsException("用户名", registerRequest.getUsername());
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            logger.warn("注册失败 - 邮箱已存在: {}", registerRequest.getEmail());
            throw new UserAlreadyExistsException("邮箱", registerRequest.getEmail());
        }
        
        try {
            // 创建新用户
            User user = createUserFromRegisterRequest(registerRequest);
            
            // 保存用户到数据库
            User savedUser = userRepository.save(user);
            logger.info("用户创建成功: id={}, username={}", savedUser.getId(), savedUser.getUsername());
            
            // Generate JWT token
            String token = jwtTokenProvider.generateToken(savedUser.getUsername());
            Long expiresIn = jwtTokenProvider.getExpirationTime();
            
            // Create user DTO
            UserDto userDto = UserDto.fromEntity(savedUser);
            
            // Return authentication response
            AuthResponseDto response = AuthResponseDto.registerSuccess(token, expiresIn, userDto);
            
            logger.info("用户注册完成: username={}", savedUser.getUsername());
            return response;
            
        } catch (DataAccessException e) {
            logger.error("数据库操作异常: username={}", registerRequest.getUsername(), e);
            throw DatabaseOperationException.save("User", e);
        } catch (Exception e) {
            logger.error("用户注册过程中出现异常: username={}", registerRequest.getUsername(), e);
            throw new SystemException("USER_REGISTRATION_FAILED", "用户注册失败，请稍后重试", e);
        }
    }
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求数据
     * @return 登录成功响应和JWT令牌
     * @throws InvalidCredentialsException 当认证失败时抛出
     */
    public AuthResponseDto login(LoginRequestDto loginRequest) {
        logger.info("开始处理用户登录: usernameOrEmail={}", loginRequest.getUsernameOrEmail());
        
        try {
            // 直接使用 AuthenticationManager 进行认证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsernameOrEmail(),
                    loginRequest.getPassword()
                )
            );
            
            // 认证成功后，从认证信息中获取用户名
            String username = authentication.getName();
            
            // 查找用户实体以获取完整信息
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException(username));
            
            // 检查用户是否被禁用（虽然UserDetailsService应该已经检查了）
            if (!user.getEnabled()) {
                logger.warn("登录失败 - 用户已被禁用: {}", username);
                throw new InvalidCredentialsException("账户已被禁用，请联系管理员");
            }
            
            // Generate JWT token
            String token = jwtTokenProvider.generateToken(username);
            Long expiresIn = jwtTokenProvider.getExpirationTime();
            
            // Create user DTO
            UserDto userDto = UserDto.fromEntity(user);
            
            // Return authentication response
            AuthResponseDto response = AuthResponseDto.loginSuccess(token, expiresIn, userDto);
            
            logger.info("用户登录成功: username={}", username);
            return response;
            
        } catch (BadCredentialsException e) {
            logger.warn("登录失败 - 凭证无效: usernameOrEmail={}", loginRequest.getUsernameOrEmail());
            throw new InvalidCredentialsException();
        } catch (AuthenticationException e) {
            logger.warn("登录失败 - 认证异常: usernameOrEmail={}, error={}", 
                       loginRequest.getUsernameOrEmail(), e.getMessage());
            throw new InvalidCredentialsException();
        } catch (DataAccessException e) {
            logger.error("数据库操作异常: usernameOrEmail={}", loginRequest.getUsernameOrEmail(), e);
            throw DatabaseOperationException.find("User", loginRequest.getUsernameOrEmail());
        } catch (Exception e) {
            logger.error("登录过程中出现异常: usernameOrEmail={}", 
                        loginRequest.getUsernameOrEmail(), e);
            throw new SystemException("USER_LOGIN_FAILED", "登录失败，请稍后重试", e);
        }
    }
    
    /**
     * 修改密码
     * 
     * @param username 用户名
     * @param passwordChangeRequest 密码修改请求
     * @throws RuntimeException 当当前密码验证失败时抛出
     */
    public void changePassword(String username, PasswordChangeDto passwordChangeRequest) {
        logger.info("开始处理密码修改: username={}", username);
        
        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        
        // 验证当前密码
        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
            logger.warn("密码修改失败 - 当前密码错误: username={}", username);
            throw new InvalidCredentialsException("当前密码错误");
        }
        
        // 检查新密码是否与当前密码相同
        if (passwordEncoder.matches(passwordChangeRequest.getNewPassword(), user.getPassword())) {
            logger.warn("密码修改失败 - 新密码与当前密码相同: username={}", username);
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        
        try {
            // 加密新密码
            String encodedNewPassword = passwordEncoder.encode(passwordChangeRequest.getNewPassword());
            
            // 更新密码
            user.setPassword(encodedNewPassword);
            userRepository.save(user);
            
            logger.info("密码修改成功: username={}", username);
            
        } catch (Exception e) {
            logger.error("密码修改过程中出现异常: username={}", username, e);
            throw new RuntimeException("密码修改失败，请稍后重试");
        }
    }
    
    /**
     * 验证令牌
     * 
     * @param token JWT令牌
     * @return 令牌是否有效
     */
    public boolean validateToken(String token) {
        try {
            return jwtTokenProvider.validateToken(token);
        } catch (Exception e) {
            logger.warn("令牌验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 从令牌获取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }
    
    /**
     * 根据用户名或邮箱查找用户
     * 
     * @param usernameOrEmail 用户名或邮箱
     * @return 用户实体，如果不存在则返回null
     */
    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        // 判断输入是邮箱还是用户名
        if (usernameOrEmail.contains("@")) {
            return userRepository.findByEmail(usernameOrEmail).orElse(null);
        } else {
            return userRepository.findByUsername(usernameOrEmail).orElse(null);
        }
    }
    
    /**
     * 从注册请求创建用户实体
     * 
     * @param registerRequest 注册请求
     * @return 用户实体
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
        user.setRole(UserRole.USER); // 默认注册为普通用户
        user.setEnabled(true);
        
        return user;
    }
    
    /**
     * 检查用户名是否可用
     * 
     * @param username 用户名
     * @return 是否可用
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    /**
     * 检查邮箱是否可用
     * 
     * @param email 邮箱
     * @return 是否可用
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
}