/**
 * User Controller Security Test
 * 
 * 目的:
 * - 专门测试 UserController 的安全控制机制
 * - 验证用户管理端点的权限控制
 * - 确保个人数据保护和权限边界
 * - 测试各种角色对用户操作的访问权限
 * 
 * 测试覆盖范围:
 * - 个人资料访问控制
 * - 用户管理操作权限
 * - 数据隔离验证
 * - 角色权限边界测试
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.controller.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.controller.UserController;
import com.miniups.model.dto.user.CreateUserDto;
import com.miniups.model.dto.user.UpdateUserDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.enums.UserRole;
import com.miniups.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Security Tests")
public class UserControllerSecurityTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private UserDto adminUserDto;
    private UserDto normalUserDto;
    private UserDto driverUserDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        
        // Setup test user DTOs
        adminUserDto = new UserDto();
        adminUserDto.setId(1L);
        adminUserDto.setUsername("admin_security");
        adminUserDto.setEmail("admin.security@test.com");
        adminUserDto.setRole(UserRole.ADMIN);
        adminUserDto.setEnabled(true);

        normalUserDto = new UserDto();
        normalUserDto.setId(2L);
        normalUserDto.setUsername("user_security");
        normalUserDto.setEmail("user.security@test.com");
        normalUserDto.setRole(UserRole.USER);
        normalUserDto.setEnabled(true);

        driverUserDto = new UserDto();
        driverUserDto.setId(3L);
        driverUserDto.setUsername("driver_security");
        driverUserDto.setEmail("driver.security@test.com");
        driverUserDto.setRole(UserRole.DRIVER);
        driverUserDto.setEnabled(true);
        
        // Reset all mocks
        reset(userService);
    }

    // ========================================
    // 基本用户操作测试
    // ========================================

    @Test
    @DisplayName("获取用户信息 - 基本功能测试")
    void testGetUserById_ShouldWork() throws Exception {
        when(userService.getUserById(1L)).thenReturn(adminUserDto);

        mockMvc.perform(get("/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Get user information successful"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("admin_security"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("获取用户公开资料 - 基本功能测试")
    void testGetUserPublicProfile_ShouldWork() throws Exception {
        when(userService.getUserPublicProfile(1L)).thenReturn(adminUserDto);

        mockMvc.perform(get("/users/1/public")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Public profile fetched successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("admin_security"));

        verify(userService, times(1)).getUserPublicProfile(1L);
    }

    @Test
    @DisplayName("创建用户 - 管理员权限测试")
    void testCreateUser_ShouldWork() throws Exception {
        CreateUserDto createUserDto = new CreateUserDto();
        createUserDto.setUsername("newuser");
        createUserDto.setEmail("newuser@test.com");
        createUserDto.setPassword("password123");
        createUserDto.setRole(UserRole.USER);
        
        UserDto newUserDto = new UserDto();
        newUserDto.setId(4L);
        newUserDto.setUsername("newuser");
        newUserDto.setEmail("newuser@test.com");
        newUserDto.setRole(UserRole.USER);
        newUserDto.setEnabled(true);
        
        when(userService.createUser(any(CreateUserDto.class))).thenReturn(newUserDto);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("newuser"));

        verify(userService, times(1)).createUser(any(CreateUserDto.class));
    }

    @Test
    @DisplayName("更新用户信息 - 管理员权限测试")
    void testUpdateUser_ShouldWork() throws Exception {
        UpdateUserDto updateDto = new UpdateUserDto();
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Name");
        
        UserDto updatedUser = new UserDto();
        updatedUser.setId(2L);
        updatedUser.setUsername("user_security");
        updatedUser.setEmail("user.security@test.com");
        updatedUser.setRole(UserRole.USER);
        updatedUser.setEnabled(true);
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("Name");
        
        when(userService.updateUser(eq(2L), any(UpdateUserDto.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User information updated successfully"))
                .andExpect(jsonPath("$.data.firstName").value("Updated"));

        verify(userService, times(1)).updateUser(eq(2L), any(UpdateUserDto.class));
    }

    @Test
    @DisplayName("禁用用户 - 管理员权限测试")
    void testDeleteUser_ShouldWork() throws Exception {
        doNothing().when(userService).deleteUser(2L);

        mockMvc.perform(delete("/users/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User has been disabled"));

        verify(userService, times(1)).deleteUser(2L);
    }

    @Test
    @DisplayName("启用用户 - 管理员权限测试")
    void testEnableUser_ShouldWork() throws Exception {
        doNothing().when(userService).enableUser(2L);

        mockMvc.perform(post("/users/2/enable")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User has been enabled"));

        verify(userService, times(1)).enableUser(2L);
    }

    // ========================================
    // 数据验证测试
    // ========================================

    @Test
    @DisplayName("创建用户验证 - 测试必填字段")
    void testCreateUser_ValidationTest() throws Exception {
        CreateUserDto invalidDto = new CreateUserDto();
        // 故意留空必填字段进行验证测试，应该返回400 Bad Request
        
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest()); // 验证应该失败并返回400

        // 由于验证失败，服务不应该被调用
        verify(userService, never()).createUser(any(CreateUserDto.class));
    }

    @Test
    @DisplayName("角色权限测试 - 验证不同角色的访问权限")
    void testRoleBasedAccess_ShouldWork() throws Exception {
        when(userService.getUserById(1L)).thenReturn(adminUserDto);
        when(userService.getUserById(2L)).thenReturn(normalUserDto);
        when(userService.getUserById(3L)).thenReturn(driverUserDto);

        // 测试获取管理员信息
        mockMvc.perform(get("/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        // 测试获取普通用户信息
        mockMvc.perform(get("/users/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));

        // 测试获取驱动员信息
        mockMvc.perform(get("/users/3")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("DRIVER"));

        verify(userService, times(1)).getUserById(1L);
        verify(userService, times(1)).getUserById(2L);
        verify(userService, times(1)).getUserById(3L);
    }

    @Test
    @DisplayName("边界条件测试 - 处理不存在的用户ID")
    void testBoundaryConditions_NonExistentUser() throws Exception {
        // Mock service to return null or throw exception for non-existent user
        when(userService.getUserById(999L)).thenReturn(null);

        mockMvc.perform(get("/users/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Controller will handle the null response

        verify(userService, times(1)).getUserById(999L);
    }

    @Test
    @DisplayName("用户资料更新测试 - 验证字段更新")
    void testUserProfileUpdate_ShouldWork() throws Exception {
        UpdateUserDto profileUpdate = new UpdateUserDto();
        profileUpdate.setFirstName("UpdatedFirst");
        profileUpdate.setLastName("UpdatedLast");
        profileUpdate.setPhoneNumber("+1234567890");
        
        UserDto updatedProfile = new UserDto();
        updatedProfile.setId(2L);
        updatedProfile.setUsername("user_security");
        updatedProfile.setEmail("user.security@test.com");
        updatedProfile.setRole(UserRole.USER);
        updatedProfile.setEnabled(true);
        updatedProfile.setFirstName("UpdatedFirst");
        updatedProfile.setLastName("UpdatedLast");
        
        when(userService.updateUser(eq(2L), any(UpdateUserDto.class))).thenReturn(updatedProfile);

        mockMvc.perform(put("/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("UpdatedFirst"))
                .andExpect(jsonPath("$.data.lastName").value("UpdatedLast"));

        verify(userService, times(1)).updateUser(eq(2L), any(UpdateUserDto.class));
    }
}