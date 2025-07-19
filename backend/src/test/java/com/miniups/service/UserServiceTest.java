package com.miniups.service;

import com.miniups.exception.UserNotFoundException;
import com.miniups.model.dto.user.UpdateUserDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import com.miniups.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 集成测试
 * 测试用户服务的业务逻辑
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserService 业务逻辑测试")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User testAdmin;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        // 创建测试用户实体
        testUser = TestDataFactory.createTestUser();
        testUser.setId(1L);

        testAdmin = TestDataFactory.createTestAdmin();
        testAdmin.setId(2L);

        // 创建测试用户DTO
        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setUsername("testuser");
        testUserDto.setEmail("test@example.com");
        testUserDto.setRole(UserRole.USER);
        testUserDto.setEnabled(true);
    }

    @Test
    @DisplayName("测试根据用户名获取用户信息 - 成功")
    void testGetCurrentUserInfo_Success() {
        // Mock repository 返回
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDto result = userService.getCurrentUserInfo("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.isEnabled());

        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("测试根据用户名获取用户信息 - 用户不存在")
    void testGetCurrentUserInfo_UserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.getCurrentUserInfo("nonexistent");
        });

        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("测试根据ID获取用户信息 - 成功")
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("测试根据ID获取用户信息 - 用户不存在")
    void testGetUserById_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(999L);
        });

        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("测试获取所有用户")
    void testGetAllUsers() {
        List<User> users = Arrays.asList(testUser, testAdmin);
        when(userRepository.findAll()).thenReturn(users);

        List<UserDto> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        assertEquals("admin", result.get(1).getUsername());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("测试根据角色获取用户")
    void testGetUsersByRole() {
        List<User> adminUsers = Arrays.asList(testAdmin);
        when(userRepository.findAll()).thenReturn(adminUsers);

        List<UserDto> result = userService.getUsersByRole(UserRole.ADMIN);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
        assertEquals(UserRole.ADMIN, result.get(0).getRole());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("测试更新用户信息 - 成功")
    void testUpdateUser_Success() {
        UpdateUserDto updateDto = new UpdateUserDto();
        updateDto.setEmail("updated@example.com");
        updateDto.setPhoneNumber("1234567890");
        updateDto.setFirstName("Updated");
        updateDto.setLastName("User");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.updateUser(1L, updateDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        
        // 验证用户信息被更新
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("测试更新用户信息 - 用户不存在")
    void testUpdateUser_UserNotFound() {
        UpdateUserDto updateDto = new UpdateUserDto();
        updateDto.setEmail("updated@example.com");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.updateUser(999L, updateDto);
        });

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("测试更新用户信息 - 邮箱已存在")
    void testUpdateUser_EmailExists() {
        UpdateUserDto updateDto = new UpdateUserDto();
        updateDto.setEmail("admin@example.com"); // 已存在的邮箱

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndIdNot("admin@example.com", 1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            userService.updateUser(1L, updateDto);
        });

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).existsByEmailAndIdNot("admin@example.com", 1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("测试禁用用户")
    void testDeleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.deleteUser(1L);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("测试禁用用户 - 用户不存在")
    void testDeleteUser_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(999L);
        });

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("测试启用用户")
    void testEnableUser_Success() {
        testUser.setEnabled(false); // 设置为禁用状态
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.enableUser(1L);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("测试启用用户 - 用户不存在")
    void testEnableUser_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.enableUser(999L);
        });

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("测试获取用户公开资料")
    void testGetUserPublicProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserPublicProfile(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        // 验证敏感信息被过滤
        assertNull(result.getEmail());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("测试获取用户公开资料 - 用户不存在")
    void testGetUserPublicProfile_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserPublicProfile(999L);
        });

        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("测试检查用户名是否可用 - 可用")
    void testIsUsernameAvailable_Available() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        boolean result = userService.isUsernameAvailable("newuser");

        assertTrue(result);
        verify(userRepository, times(1)).existsByUsername("newuser");
    }

    @Test
    @DisplayName("测试检查用户名是否可用 - 不可用")
    void testIsUsernameAvailable_NotAvailable() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        boolean result = userService.isUsernameAvailable("testuser");

        assertFalse(result);
        verify(userRepository, times(1)).existsByUsername("testuser");
    }

    @Test
    @DisplayName("测试检查邮箱是否可用 - 可用")
    void testIsEmailAvailable_Available() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        boolean result = userService.isEmailAvailable("new@example.com");

        assertTrue(result);
        verify(userRepository, times(1)).existsByEmail("new@example.com");
    }

    @Test
    @DisplayName("测试检查邮箱是否可用 - 不可用")
    void testIsEmailAvailable_NotAvailable() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        boolean result = userService.isEmailAvailable("test@example.com");

        assertFalse(result);
        verify(userRepository, times(1)).existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("测试用户DTO转换")
    void testUserToDto_Conversion() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getRole(), result.getRole());
        assertEquals(testUser.isEnabled(), result.isEnabled());
        // 验证密码不被包含在DTO中
        assertNull(result.getPassword());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("测试批量操作")
    void testBatchOperations() {
        List<User> users = Arrays.asList(testUser, testAdmin);
        when(userRepository.findByRole(UserRole.USER)).thenReturn(Arrays.asList(testUser));

        List<UserDto> result = userService.getUsersByRole(UserRole.USER);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(UserRole.USER, result.get(0).getRole());

        verify(userRepository, times(1)).findByRole(UserRole.USER);
    }

    @Test
    @DisplayName("测试空值处理")
    void testNullHandling() {
        assertThrows(IllegalArgumentException.class, () -> {
            userService.getCurrentUserInfo(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            userService.getCurrentUserInfo("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(null);
        });
    }

    @Test
    @DisplayName("测试数据库异常处理")
    void testDatabaseExceptionHandling() {
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> {
            userService.getUserById(1L);
        });

        verify(userRepository, times(1)).findById(1L);
    }
}