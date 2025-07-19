package com.miniups.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.exception.BusinessValidationException;
import com.miniups.exception.InvalidCredentialsException;
import com.miniups.exception.UserAlreadyExistsException;
import com.miniups.model.dto.auth.AuthResponseDto;
import com.miniups.model.dto.auth.LoginRequestDto;
import com.miniups.model.dto.auth.PasswordChangeDto;
import com.miniups.model.dto.auth.RegisterRequestDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.enums.UserRole;
import com.miniups.service.AuthService;
import com.miniups.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController 认证API测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequestDto registerRequest;
    private LoginRequestDto loginRequest;
    private PasswordChangeDto passwordChangeRequest;
    private AuthResponseDto authResponse;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequestDto();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Password123!");
        // Note: confirmPassword field doesn't exist in current RegisterRequestDto
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        loginRequest = new LoginRequestDto();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("Password123!");

        passwordChangeRequest = new PasswordChangeDto();
        passwordChangeRequest.setCurrentPassword("Password123!");
        passwordChangeRequest.setNewPassword("NewPassword123!");
        passwordChangeRequest.setConfirmPassword("NewPassword123!");

        authResponse = new AuthResponseDto();
        authResponse.setAccessToken("jwt-token-123");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(3600L);

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setRole(UserRole.USER);
        userDto.setEnabled(true);
    }

    @Test
    @DisplayName("测试用户注册 - 成功")
    void testRegisterUser_Success() throws Exception {
        when(authService.register(any(RegisterRequestDto.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registration successful"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token-123"));

        verify(authService, times(1)).register(any(RegisterRequestDto.class));
    }

    @Test
    @DisplayName("测试用户注册 - 用户名已存在")
    void testRegisterUser_UsernameExists() throws Exception {
        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new UserAlreadyExistsException("Username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Username already exists"));

        verify(authService, times(1)).register(any(RegisterRequestDto.class));
    }

    @Test
    @DisplayName("测试用户登录 - 凭证无效")
    void testAuthenticateUser_InvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(authService, times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("测试修改密码 - 密码不匹配")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testChangePassword_PasswordMismatch() throws Exception {
        PasswordChangeDto mismatchRequest = new PasswordChangeDto();
        mismatchRequest.setCurrentPassword("Password123!");
        mismatchRequest.setNewPassword("NewPassword123!");
        mismatchRequest.setConfirmPassword("DifferentPassword123!");

        mockMvc.perform(post("/api/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_MISMATCH"));

        verify(authService, never()).changePassword(anyString(), any(PasswordChangeDto.class));
    }

    @Test
    @DisplayName("测试修改密码 - 当前密码错误")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testChangePassword_WrongCurrentPassword() throws Exception {
        doThrow(new InvalidCredentialsException("Current password is incorrect"))
                .when(authService).changePassword(eq("testuser"), any(PasswordChangeDto.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordChangeRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));

        verify(authService, times(1)).changePassword(eq("testuser"), any(PasswordChangeDto.class));
    }
}
