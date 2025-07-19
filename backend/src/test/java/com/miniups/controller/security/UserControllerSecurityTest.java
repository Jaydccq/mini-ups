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
import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import com.miniups.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:usertest",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User normalUser1;
    private User normalUser2;
    private User driverUser;
    private String adminToken;
    private String user1Token;
    private String user2Token;
    private String driverToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        setupTestUsers();
        setupJwtTokens();
    }

    private void setupTestUsers() {
        // 管理员用户
        adminUser = new User();
        adminUser.setUsername("admin_security");
        adminUser.setEmail("admin.security@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser = userRepository.save(adminUser);

        // 普通用户 1
        normalUser1 = new User();
        normalUser1.setUsername("user1_security");
        normalUser1.setEmail("user1.security@test.com");
        normalUser1.setPassword(passwordEncoder.encode("user123"));
        normalUser1.setRole(UserRole.USER);
        normalUser1.setEnabled(true);
        normalUser1.setFirstName("Normal");
        normalUser1.setLastName("User1");
        normalUser1 = userRepository.save(normalUser1);

        // 普通用户 2
        normalUser2 = new User();
        normalUser2.setUsername("user2_security");
        normalUser2.setEmail("user2.security@test.com");
        normalUser2.setPassword(passwordEncoder.encode("user123"));
        normalUser2.setRole(UserRole.USER);
        normalUser2.setEnabled(true);
        normalUser2.setFirstName("Normal");
        normalUser2.setLastName("User2");
        normalUser2 = userRepository.save(normalUser2);

        // 驱动员用户
        driverUser = new User();
        driverUser.setUsername("driver_security");
        driverUser.setEmail("driver.security@test.com");
        driverUser.setPassword(passwordEncoder.encode("driver123"));
        driverUser.setRole(UserRole.DRIVER);
        driverUser.setEnabled(true);
        driverUser.setFirstName("Driver");
        driverUser.setLastName("User");
        driverUser = userRepository.save(driverUser);
    }

    private void setupJwtTokens() {
        adminToken = jwtTokenProvider.generateToken(adminUser.getUsername());
        user1Token = jwtTokenProvider.generateToken(normalUser1.getUsername());
        user2Token = jwtTokenProvider.generateToken(normalUser2.getUsername());
        driverToken = jwtTokenProvider.generateToken(driverUser.getUsername());
    }

    // ========================================
    // 个人资料访问控制测试
    // ========================================

    @Test
    @DisplayName("个人资料 - 用户应能访问自己的个人资料")
    void testGetCurrentUserProfile_ShouldBeAccessibleByOwner() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(normalUser1.getUsername()))
                .andExpect(jsonPath("$.data.email").value(normalUser1.getEmail()));
    }

    @Test
    @DisplayName("个人资料 - 未认证用户不能访问个人资料")
    void testGetCurrentUserProfile_ShouldReturn401ForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("个人资料 - 用户应能更新自己的个人资料")
    void testUpdateCurrentUserProfile_ShouldBeAllowedForOwner() throws Exception {
        String updateRequest = "{"
                + "\"firstName\":\"Updated\","
                + "\"lastName\":\"Name\","
                + "\"phone\":\"+1234567890\""
                + "}";

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Name"));
    }

    @Test
    @DisplayName("个人资料 - 普通用户不能修改管理员字段")
    void testUpdateCurrentUserProfile_ShouldNotAllowAdminFields() throws Exception {
        String maliciousUpdate = "{"
                + "\"firstName\":\"Updated\","
                + "\"role\":\"ADMIN\","
                + "\"enabled\":false"
                + "}";

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Updated"));

        // 验证角色没有被修改
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    // ========================================
    // 用户信息查看权限测试
    // ========================================

    @Test
    @DisplayName("用户信息 - 用户应能查看自己的详细信息")
    void testGetUserById_ShouldAllowUserToViewOwnInfo() throws Exception {
        mockMvc.perform(get("/api/users/" + normalUser1.getId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(normalUser1.getId()))
                .andExpect(jsonPath("$.data.username").value(normalUser1.getUsername()));
    }

    @Test
    @DisplayName("用户信息 - 用户不能查看其他用户的详细信息")
    void testGetUserById_ShouldDenyUserFromViewingOthersInfo() throws Exception {
        mockMvc.perform(get("/api/users/" + normalUser2.getId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("用户信息 - 管理员应能查看任何用户的信息")
    void testGetUserById_ShouldAllowAdminToViewAnyUserInfo() throws Exception {
        mockMvc.perform(get("/api/users/" + normalUser1.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(normalUser1.getId()))
                .andExpect(jsonPath("$.data.username").value(normalUser1.getUsername()));
    }

    @Test
    @DisplayName("用户信息 - 驱动员不能查看其他用户的详细信息")
    void testGetUserById_ShouldDenyDriverFromViewingOthersInfo() throws Exception {
        mockMvc.perform(get("/api/users/" + normalUser1.getId())
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 用户管理权限测试
    // ========================================

    @Test
    @DisplayName("用户管理 - 管理员应能获取用户列表")
    void testGetAllUsers_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.users").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(4)); // 4 个测试用户
    }

    @Test
    @DisplayName("用户管理 - 普通用户不能获取用户列表")
    void testGetAllUsers_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("用户管理 - 驱动员不能获取用户列表")
    void testGetAllUsers_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("用户管理 - 管理员应能创建新用户")
    void testCreateUser_ShouldBeAllowedForAdmin() throws Exception {
        String createRequest = "{"
                + "\"username\":\"new_user_test\","
                + "\"email\":\"newuser@test.com\","
                + "\"password\":\"password123\","
                + "\"firstName\":\"New\","
                + "\"lastName\":\"User\","
                + "\"role\":\"USER\""
                + "}";

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("new_user_test"))
                .andExpect(jsonPath("$.data.email").value("newuser@test.com"));
    }

    @Test
    @DisplayName("用户管理 - 普通用户不能创建新用户")
    void testCreateUser_ShouldDenyNormalUser() throws Exception {
        String createRequest = "{"
                + "\"username\":\"unauthorized_user\","
                + "\"email\":\"unauthorized@test.com\","
                + "\"password\":\"password123\","
                + "\"role\":\"USER\""
                + "}";

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("用户管理 - 管理员应能更新其他用户信息")
    void testUpdateUser_ShouldBeAllowedForAdmin() throws Exception {
        String updateRequest = "{"
                + "\"firstName\":\"Admin Updated\","
                + "\"lastName\":\"Name\","
                + "\"role\":\"DRIVER\""
                + "}";

        mockMvc.perform(put("/api/users/" + normalUser1.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Admin Updated"))
                .andExpect(jsonPath("$.data.role").value("DRIVER"));
    }

    @Test
    @DisplayName("用户管理 - 普通用户不能更新其他用户信息")
    void testUpdateUser_ShouldDenyNormalUser() throws Exception {
        String updateRequest = "{"
                + "\"firstName\":\"Malicious Update\","
                + "\"role\":\"ADMIN\""
                + "}";

        mockMvc.perform(put("/api/users/" + normalUser2.getId())
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("用户管理 - 管理员应能禁用用户")
    void testDeleteUser_ShouldBeAllowedForAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/" + normalUser1.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户管理 - 普通用户不能禁用其他用户")
    void testDeleteUser_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + normalUser2.getId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("用户管理 - 管理员应能启用用户")
    void testEnableUser_ShouldBeAllowedForAdmin() throws Exception {
        mockMvc.perform(post("/api/users/" + normalUser1.getId() + "/enable")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户管理 - 普通用户不能启用其他用户")
    void testEnableUser_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(post("/api/users/" + normalUser2.getId() + "/enable")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 公共信息访问测试
    // ========================================

    @Test
    @DisplayName("公共信息 - 任何人都应能访问用户公共资料")
    void testGetUserPublicProfile_ShouldBeAccessibleToEveryone() throws Exception {
        // 无需认证
        mockMvc.perform(get("/api/users/" + normalUser1.getId() + "/public"))
                .andExpect(status().isOk());

        // 普通用户可以访问
        mockMvc.perform(get("/api/users/" + normalUser1.getId() + "/public")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk());

        // 管理员可以访问
        mockMvc.perform(get("/api/users/" + normalUser1.getId() + "/public")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ========================================
    // 数据验证和边界条件测试
    // ========================================

    @Test
    @DisplayName("数据验证 - 创建用户时应验证必填字段")
    void testCreateUser_ShouldValidateRequiredFields() throws Exception {
        String invalidRequest = "{"
                + "\"username\":\"\","
                + "\"email\":\"invalid-email\","
                + "\"password\":\"\""
                + "}";

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("数据验证 - 更新个人资料时应验证数据格式")
    void testUpdateProfile_ShouldValidateDataFormat() throws Exception {
        String invalidUpdate = "{"
                + "\"email\":\"invalid-email-format\","
                + "\"phone\":\"invalid-phone\""
                + "}";

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUpdate))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("边界条件 - 访问不存在的用户应返回 404")
    void testGetUserById_ShouldReturn404ForNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/users/999999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("边界条件 - 更新不存在的用户应返回 404")
    void testUpdateUser_ShouldReturn404ForNonExistentUser() throws Exception {
        String updateRequest = "{\"firstName\":\"Test\"}";

        mockMvc.perform(put("/api/users/999999")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isNotFound());
    }

    // ========================================
    // 权限提升攻击防护测试
    // ========================================

    @Test
    @DisplayName("权限提升防护 - 普通用户不能将自己升级为管理员")
    void testPrivilegeEscalation_ShouldPreventUserFromBecomingAdmin() throws Exception {
        String maliciousUpdate = "{"
                + "\"role\":\"ADMIN\","
                + "\"enabled\":true"
                + "}";

        // 即使发送了包含角色的请求，也不应该被处理
        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousUpdate))
                .andExpect(status().isOk());

        // 验证角色没有被修改
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("权限提升防护 - 驱动员不能修改其他用户的角色")
    void testPrivilegeEscalation_ShouldPreventDriverFromModifyingOthers() throws Exception {
        String maliciousUpdate = "{"
                + "\"role\":\"USER\","
                + "\"enabled\":false"
                + "}";

        mockMvc.perform(put("/api/users/" + normalUser1.getId())
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousUpdate))
                .andExpect(status().isForbidden());
    }
}