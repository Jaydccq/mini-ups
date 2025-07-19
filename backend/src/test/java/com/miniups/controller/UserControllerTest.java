package com.miniups.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.model.dto.user.CreateUserDto;
import com.miniups.model.dto.user.UpdateUserDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.enums.UserRole;
import com.miniups.service.UserService;
import com.miniups.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 单元测试
 * 测试用户管理API的各种场景
 */
@WebMvcTest(UserController.class)
@DisplayName("UserController API 测试")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto testUserDto;
    private UserDto testAdminDto;
    private UpdateUserDto updateUserDto;

    @BeforeEach
    void setUp() {
        // 创建测试用户DTO
        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setUsername("testuser");
        testUserDto.setEmail("test@example.com");
        testUserDto.setRole(UserRole.USER);
        testUserDto.setEnabled(true);

        // 创建测试管理员DTO  
        testAdminDto = new UserDto();
        testAdminDto.setId(2L);
        testAdminDto.setUsername("admin");
        testAdminDto.setEmail("admin@example.com");
        testAdminDto.setRole(UserRole.ADMIN);
        testAdminDto.setEnabled(true);

        // 创建更新用户DTO
        updateUserDto = new UpdateUserDto();
        updateUserDto.setEmail("updated@example.com");
        updateUserDto.setPhoneNumber("1234567890");
    }

    @Test
    @DisplayName("测试获取当前用户资料 - 成功")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetCurrentUserProfile_Success() throws Exception {
        // Mock service 返回
        when(userService.getCurrentUserInfo("testuser")).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Get user profile successful"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"));

        verify(userService, times(1)).getCurrentUserInfo("testuser");
    }

    @Test
    @DisplayName("测试获取当前用户资料 - 未认证")
    void testGetCurrentUserProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getCurrentUserInfo(anyString());
    }

    @Test
    @DisplayName("测试更新当前用户资料 - 成功")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testUpdateCurrentUserProfile_Success() throws Exception {
        // Mock service 返回
        when(userService.getCurrentUserInfo("testuser")).thenReturn(testUserDto);
        when(userService.updateUser(eq(1L), any(UpdateUserDto.class))).thenReturn(testUserDto);

        mockMvc.perform(put("/api/users/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User profile update successful"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService, times(1)).getCurrentUserInfo("testuser");
        verify(userService, times(1)).updateUser(eq(1L), any(UpdateUserDto.class));
    }

    @Test
    @DisplayName("测试更新当前用户资料 - 无效数据")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testUpdateCurrentUserProfile_InvalidData() throws Exception {
        UpdateUserDto invalidDto = new UpdateUserDto();
        invalidDto.setEmail("invalid-email"); // 无效邮箱格式

        mockMvc.perform(put("/api/users/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).getCurrentUserInfo(anyString());
        verify(userService, never()).updateUser(anyLong(), any(UpdateUserDto.class));
    }

    @Test
    @DisplayName("测试获取指定用户信息 - 管理员权限")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetUserById_AdminAccess() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Get user information successful"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.username").value("testuser"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("测试获取指定用户信息 - 用户访问自己的信息")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetUserById_UserAccessOwnInfo() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.id").value(1));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("测试获取指定用户信息 - 权限不足")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetUserById_AccessDenied() throws Exception {
        // 用户尝试访问其他用户的信息
        mockMvc.perform(get("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserById(anyLong());
    }

    @Test
    @DisplayName("测试获取用户列表 - 管理员权限")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetAllUsers_AdminAccess() throws Exception {
        List<UserDto> users = Arrays.asList(testUserDto, testAdminDto);
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Get user list successful"))
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.total").value(2));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("测试获取用户列表 - 按角色过滤")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetAllUsers_FilterByRole() throws Exception {
        List<UserDto> adminUsers = Arrays.asList(testAdminDto);
        when(userService.getUsersByRole(UserRole.ADMIN)).thenReturn(adminUsers);

        mockMvc.perform(get("/api/users")
                .param("role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users.length()").value(1))
                .andExpect(jsonPath("$.users[0].role").value("ADMIN"));

        verify(userService, times(1)).getUsersByRole(UserRole.ADMIN);
        verify(userService, never()).getAllUsers();
    }

    @Test
    @DisplayName("测试获取用户列表 - 普通用户权限不足")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetAllUsers_UserAccessDenied() throws Exception {
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }

    @Test
    @DisplayName("测试更新用户信息 - 管理员权限")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUpdateUser_AdminAccess() throws Exception {
        when(userService.updateUser(eq(1L), any(UpdateUserDto.class))).thenReturn(testUserDto);

        mockMvc.perform(put("/api/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User information updated successfully"))
                .andExpect(jsonPath("$.user.id").value(1));

        verify(userService, times(1)).updateUser(eq(1L), any(UpdateUserDto.class));
    }

    @Test
    @DisplayName("测试删除用户 - 管理员权限")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_AdminAccess() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User has been disabled"));

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName("测试启用用户 - 管理员权限")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testEnableUser_AdminAccess() throws Exception {
        doNothing().when(userService).enableUser(1L);

        mockMvc.perform(post("/api/users/1/enable")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User has been enabled"));

        verify(userService, times(1)).enableUser(1L);
    }

    @Test
    @DisplayName("测试获取用户公开资料 - 无需认证")
    void testGetUserPublicProfile_NoAuth() throws Exception {
        when(userService.getUserPublicProfile(1L)).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users/1/public")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Public profile fetched successfully"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.username").value("testuser"));

        verify(userService, times(1)).getUserPublicProfile(1L);
    }

    @Test
    @DisplayName("测试获取用户公开资料 - 用户不存在")
    void testGetUserPublicProfile_UserNotFound() throws Exception {
        when(userService.getUserPublicProfile(999L))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/users/999/public")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Query failed"))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userService, times(1)).getUserPublicProfile(999L);
    }

    @Test
    @DisplayName("测试服务异常处理")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testServiceException() throws Exception {
        when(userService.getCurrentUserInfo("testuser"))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("System error"))
                .andExpect(jsonPath("$.message").value("Get user profile failed"));

        verify(userService, times(1)).getCurrentUserInfo("testuser");
    }

    @Test
    @DisplayName("测试权限注解生效")
    @WithMockUser(username = "user", roles = {"USER"})
    void testSecurityAnnotations() throws Exception {
        // 测试用户尝试访问管理员功能
        mockMvc.perform(delete("/api/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(anyLong());
    }

    @Test
    @DisplayName("测试CSRF保护")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCSRFProtection() throws Exception {
        // 不带CSRF token的请求应该被拒绝
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserDto)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUser(anyLong(), any(UpdateUserDto.class));
    }
}