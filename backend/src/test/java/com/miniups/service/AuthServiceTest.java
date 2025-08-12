
package com.miniups.service;

import com.miniups.exception.InvalidCredentialsException;
import com.miniups.exception.SystemException;
import com.miniups.exception.UserAlreadyExistsException;
import com.miniups.exception.UserNotFoundException;
import com.miniups.model.dto.auth.AuthResponseDto;
import com.miniups.model.dto.auth.LoginRequestDto;
import com.miniups.model.dto.auth.PasswordChangeDto;
import com.miniups.model.dto.auth.RegisterRequestDto;
import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import com.miniups.security.JwtTokenProvider;
import com.miniups.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 核心业务逻辑测试
 * 重点测试：用户注册、登录验证、密码管理、令牌处理
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AuthService 认证服务业务逻辑测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDto validRegisterRequest;
    private LoginRequestDto validLoginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        setupValidRegisterRequest();
        setupValidLoginRequest();
        setupTestUser();
    }

    @Test
    @DisplayName("用户注册 - 成功注册")
    void register_shouldSucceed_whenValidData() {
        // Given
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername(validRegisterRequest.getUsername()); // 使用注册请求中的用户名
        savedUser.setEmail(validRegisterRequest.getEmail());
        savedUser.setFirstName(validRegisterRequest.getFirstName());
        savedUser.setLastName(validRegisterRequest.getLastName());
        savedUser.setRole(UserRole.USER);
        savedUser.setEnabled(true);
        savedUser.setPassword("encodedPassword");
        
        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(savedUser.getUsername())).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600000L);

        // When
        AuthResponseDto response = authService.register(validRegisterRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getUsername()).isEqualTo(validRegisterRequest.getUsername());
        assertThat(response.getUser().getEmail()).isEqualTo(validRegisterRequest.getEmail());
        
        verify(userRepository).existsByUsername(validRegisterRequest.getUsername());
        verify(userRepository).existsByEmail(validRegisterRequest.getEmail());
        verify(passwordEncoder).encode(validRegisterRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateToken(savedUser.getUsername());
    }

    @Test
    @DisplayName("用户注册 - 用户名已存在")
    void register_shouldThrowException_whenUsernameExists() {
        // Given
        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessageContaining("username");

        verify(userRepository).existsByUsername(validRegisterRequest.getUsername());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("用户注册 - 邮箱已存在")
    void register_shouldThrowException_whenEmailExists() {
        // Given
        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessageContaining("email");

        verify(userRepository).existsByUsername(validRegisterRequest.getUsername());
        verify(userRepository).existsByEmail(validRegisterRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("用户登录 - 成功登录")
    void login_shouldSucceed_whenValidCredentials() {
        // Given
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getName()).thenReturn(testUser.getUsername());
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mockAuthentication);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(testUser.getUsername())).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600000L);

        // When
        AuthResponseDto response = authService.login(validLoginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getUsername()).isEqualTo(testUser.getUsername());
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername(testUser.getUsername());
        verify(jwtTokenProvider).generateToken(testUser.getUsername());
    }

    @Test
    @DisplayName("用户登录 - 凭证无效")
    void login_shouldThrowException_whenBadCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(validLoginRequest))
            .isInstanceOf(InvalidCredentialsException.class);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtTokenProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("用户登录 - 用户被禁用")
    void login_shouldThrowException_whenUserDisabled() {
        // Given
        // 基于实际测试结果，这个场景会抛出SystemException，因为service catch了所有异常
        // 并在catch块中包装成SystemException
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getName()).thenReturn(testUser.getUsername());
        
        testUser.setEnabled(false); // 禁用用户
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mockAuthentication);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        // When & Then
        // 禁用用户会直接触发InvalidCredentialsException
        assertThatThrownBy(() -> authService.login(validLoginRequest))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessageContaining("Account is disabled");

        verify(userRepository).findByUsername(testUser.getUsername());
        verify(jwtTokenProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("密码修改 - 成功修改")
    void changePassword_shouldSucceed_whenCurrentPasswordCorrect() {
        // Given
        String username = "testuser";
        String currentPassword = "currentPass123";
        String newPassword = "newPass456";
        String encodedCurrentPassword = "encodedCurrentPass";
        String encodedNewPassword = "encodedNewPass";
        
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword(currentPassword);
        passwordChangeDto.setNewPassword(newPassword);
        
        User user = TestDataFactory.createTestUser();
        user.setPassword(encodedCurrentPassword);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).thenReturn(true);
        when(passwordEncoder.matches(newPassword, encodedCurrentPassword)).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(userRepository.save(user)).thenReturn(user);

        // When
        authService.changePassword(username, passwordChangeDto);

        // Then
        assertThat(user.getPassword()).isEqualTo(encodedNewPassword);
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(currentPassword, encodedCurrentPassword);
        verify(passwordEncoder).matches(newPassword, encodedCurrentPassword);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("密码修改 - 当前密码错误")
    void changePassword_shouldFail_whenCurrentPasswordIncorrect() {
        // Given
        String username = "testuser";
        String wrongCurrentPassword = "wrongPassword";
        String encodedCurrentPassword = "encodedCurrentPass";
        
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword(wrongCurrentPassword);
        passwordChangeDto.setNewPassword("newPass456");
        
        User user = TestDataFactory.createTestUser();
        user.setPassword(encodedCurrentPassword);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongCurrentPassword, encodedCurrentPassword)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(username, passwordChangeDto))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessageContaining("Current password is incorrect");

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(wrongCurrentPassword, encodedCurrentPassword);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("密码修改 - 新密码与当前密码相同")
    void changePassword_shouldFail_whenNewPasswordSameAsCurrent() {
        // Given
        String username = "testuser";
        String currentPassword = "currentPass123";
        String newPassword = "currentPass123"; // 相同密码
        String encodedCurrentPassword = "encodedCurrentPass";
        
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword(currentPassword);
        passwordChangeDto.setNewPassword(newPassword);
        
        User user = TestDataFactory.createTestUser();
        user.setPassword(encodedCurrentPassword);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).thenReturn(true);
        when(passwordEncoder.matches(newPassword, encodedCurrentPassword)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(username, passwordChangeDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("New password must not be the same as current password");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("密码修改 - 用户不存在")
    void changePassword_shouldFail_whenUserNotFound() {
        // Given
        String username = "nonexistent";
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword("currentPass");
        passwordChangeDto.setNewPassword("newPass");
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(username, passwordChangeDto))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("令牌验证 - 有效令牌")
    void validateToken_shouldReturnTrue_whenTokenValid() {
        // Given
        String validToken = "valid.jwt.token";
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);

        // When
        boolean result = authService.validateToken(validToken);

        // Then
        assertThat(result).isTrue();
        verify(jwtTokenProvider).validateToken(validToken);
    }

    @Test
    @DisplayName("令牌验证 - 无效令牌")
    void validateToken_shouldReturnFalse_whenTokenInvalid() {
        // Given
        String invalidToken = "invalid.jwt.token";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When
        boolean result = authService.validateToken(invalidToken);

        // Then
        assertThat(result).isFalse();
        verify(jwtTokenProvider).validateToken(invalidToken);
    }

    @Test
    @DisplayName("令牌验证 - 异常处理")
    void validateToken_shouldReturnFalse_whenExceptionThrown() {
        // Given
        String token = "problematic.token";
        when(jwtTokenProvider.validateToken(token)).thenThrow(new RuntimeException("Token validation error"));

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertThat(result).isFalse();
        verify(jwtTokenProvider).validateToken(token);
    }

    @Test
    @DisplayName("从令牌获取用户名")
    void getUsernameFromToken_shouldReturnUsername() {
        // Given
        String token = "valid.jwt.token";
        String expectedUsername = "testuser";
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(expectedUsername);

        // When
        String username = authService.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo(expectedUsername);
        verify(jwtTokenProvider).getUsernameFromToken(token);
    }

    @Test
    @DisplayName("检查用户名可用性 - 可用")
    void isUsernameAvailable_shouldReturnTrue_whenUsernameAvailable() {
        // Given
        String username = "newuser";
        when(userRepository.existsByUsername(username)).thenReturn(false);

        // When
        boolean result = authService.isUsernameAvailable(username);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByUsername(username);
    }

    @Test
    @DisplayName("检查用户名可用性 - 不可用")
    void isUsernameAvailable_shouldReturnFalse_whenUsernameExists() {
        // Given
        String username = "existinguser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When
        boolean result = authService.isUsernameAvailable(username);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByUsername(username);
    }

    @Test
    @DisplayName("检查邮箱可用性 - 可用")
    void isEmailAvailable_shouldReturnTrue_whenEmailAvailable() {
        // Given
        String email = "new@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // When
        boolean result = authService.isEmailAvailable(email);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("检查邮箱可用性 - 不可用")
    void isEmailAvailable_shouldReturnFalse_whenEmailExists() {
        // Given
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When
        boolean result = authService.isEmailAvailable(email);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("异常处理 - 数据库保存异常")
    void register_shouldHandleDatabaseSaveException() {
        // Given
        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database save failed"));

        // When & Then
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
            .isInstanceOf(RuntimeException.class);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("边界条件 - 空用户名和邮箱验证")
    void availabilityCheck_shouldHandleEmptyAndNullValues() {
        // 测试空字符串
        when(userRepository.existsByUsername("")).thenReturn(false);
        when(userRepository.existsByEmail("")).thenReturn(false);
        
        assertThat(authService.isUsernameAvailable("")).isTrue();
        assertThat(authService.isEmailAvailable("")).isTrue();
        
        // 验证方法被调用
        verify(userRepository).existsByUsername("");
        verify(userRepository).existsByEmail("");
    }

    // Helper methods

    private void setupValidRegisterRequest() {
        validRegisterRequest = new RegisterRequestDto();
        validRegisterRequest.setUsername("newuser");
        validRegisterRequest.setEmail("newuser@example.com");
        validRegisterRequest.setPassword("password123");
        validRegisterRequest.setFirstName("New");
        validRegisterRequest.setLastName("User");
        validRegisterRequest.setPhone("1234567890");
        validRegisterRequest.setAddress("123 Test Street");
    }

    private void setupValidLoginRequest() {
        validLoginRequest = new LoginRequestDto();
        validLoginRequest.setUsernameOrEmail("testuser");
        validLoginRequest.setPassword("password123");
    }

    private void setupTestUser() {
        testUser = TestDataFactory.createTestUser();
        testUser.setId(1L);
        testUser.setEnabled(true);
        testUser.setRole(UserRole.USER);
        testUser.setPassword("encodedPassword123");
    }
}
