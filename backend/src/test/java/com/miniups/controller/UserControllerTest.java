/**
 * UserController 单元测试
 * 测试用户管理API的各种场景
 */
package com.miniups.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.model.dto.user.CreateUserDto;
import com.miniups.model.dto.user.UpdateUserDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.enums.UserRole;
import com.miniups.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController API 测试")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        
        // 创建测试用户DTO
        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setUsername("testuser");
        testUserDto.setEmail("test@example.com");
        testUserDto.setRole(UserRole.USER);
        testUserDto.setEnabled(true);
        
        // Reset all mocks to prevent test interference
        reset(userService);
    }

    @Test
    @DisplayName("测试获取指定用户信息 - 基本功能")
    void testGetUserById_ShouldWork() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUserDto);

        mockMvc.perform(get("/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Get user information successful"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("测试获取用户公开资料 - 基本功能")
    void testGetUserPublicProfile_ShouldWork() throws Exception {
        when(userService.getUserPublicProfile(1L)).thenReturn(testUserDto);

        mockMvc.perform(get("/users/1/public")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Public profile fetched successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService, times(1)).getUserPublicProfile(1L);
    }

    @Test
    @DisplayName("测试用户服务基本调用验证")
    void testGetUserById_ServiceCall() throws Exception {
        when(userService.getUserById(2L)).thenReturn(testUserDto);

        mockMvc.perform(get("/users/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService, times(1)).getUserById(2L);
    }

    @Test
    @DisplayName("创建新用户 - 成功")
    void testCreateUser_Success() throws Exception {
        CreateUserDto createUserDto = new CreateUserDto();
        createUserDto.setUsername("newuser");
        createUserDto.setEmail("newuser@example.com");
        createUserDto.setPassword("password123");
        createUserDto.setRole(UserRole.USER);
        
        UserDto newUserDto = new UserDto();
        newUserDto.setId(2L);
        newUserDto.setUsername("newuser");
        newUserDto.setEmail("newuser@example.com");
        newUserDto.setRole(UserRole.USER);
        newUserDto.setEnabled(true);
        
        when(userService.createUser(any(CreateUserDto.class))).thenReturn(newUserDto);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"));

        verify(userService, times(1)).createUser(any(CreateUserDto.class));
    }

    @Test
    @DisplayName("更新用户信息 - 成功")
    void testUpdateUser_Success() throws Exception {
        UpdateUserDto updateDto = new UpdateUserDto();
        updateDto.setEmail("updated@example.com");
        updateDto.setPhoneNumber("123-456-7890");
        
        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setRole(UserRole.USER);
        updatedUser.setEnabled(true);
        
        when(userService.updateUser(eq(1L), any(UpdateUserDto.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User information updated successfully"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"));

        verify(userService, times(1)).updateUser(eq(1L), any(UpdateUserDto.class));
    }

    @Test
    @DisplayName("禁用用户 - 成功")
    void testDisableUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User has been disabled"));

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName("启用用户 - 成功")
    void testEnableUser_Success() throws Exception {
        doNothing().when(userService).enableUser(1L);

        mockMvc.perform(post("/users/1/enable")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User has been enabled"));

        verify(userService, times(1)).enableUser(1L);
    }

}