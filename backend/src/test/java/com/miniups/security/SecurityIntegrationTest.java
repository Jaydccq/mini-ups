/**
 * Security Integration Test
 * 
 * 目的:
 * - 测试 Spring Security 配置的完整性
 * - 验证所有端点的访问控制是否正确实施
 * - 确保 JWT 认证和授权机制正常工作
 * - 防止未授权访问和权限提升漏洞
 * 
 * 测试覆盖范围:
 * - 公共端点访问验证
 * - 认证端点测试
 * - 角色基础访问控制测试
 * - JWT 令牌验证测试
 * - 权限提升防护测试
 * 
 * 测试场景:
 * 1. 公共端点无需认证即可访问
 * 2. 受保护端点需要有效 JWT 令牌
 * 3. 管理员端点仅限 ADMIN 角色访问
 * 4. 驱动员端点允许 DRIVER 和 ADMIN 角色访问
 * 5. 无效或过期令牌被拒绝
 * 6. 角色权限严格控制，防止权限提升
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.model.dto.auth.LoginRequestDto;
import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import com.miniups.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class SecurityIntegrationTest {

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
    private User normalUser;
    private User driverUser;
    private String adminToken;
    private String userToken;
    private String driverToken;

    @BeforeEach
    void setUp() {
        // 清理数据库
        userRepository.deleteAll();
        
        // 创建测试用户
        setupTestUsers();
        
        // 生成 JWT 令牌
        setupJwtTokens();
    }

    private void setupTestUsers() {
        // 创建管理员用户
        adminUser = new User();
        adminUser.setUsername("admin_test");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        // 创建普通用户
        normalUser = new User();
        normalUser.setUsername("user_test");
        normalUser.setEmail("user@test.com");
        normalUser.setPassword(passwordEncoder.encode("password123"));
        normalUser.setRole(UserRole.USER);
        normalUser.setEnabled(true);
        normalUser = userRepository.save(normalUser);

        // 创建驱动员用户
        driverUser = new User();
        driverUser.setUsername("driver_test");
        driverUser.setEmail("driver@test.com");
        driverUser.setPassword(passwordEncoder.encode("password123"));
        driverUser.setRole(UserRole.DRIVER);
        driverUser.setEnabled(true);
        driverUser = userRepository.save(driverUser);
    }

    private void setupJwtTokens() {
        adminToken = jwtTokenProvider.generateToken(adminUser.getUsername());
        userToken = jwtTokenProvider.generateToken(normalUser.getUsername());
        driverToken = jwtTokenProvider.generateToken(driverUser.getUsername());
    }

    // ========================================
    // 公共端点测试 - 无需认证
    // ========================================

    @Test
    @DisplayName("公共端点 - 认证端点应无需认证即可访问")
    void testPublicAuthEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsernameOrEmail("admin_test");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("公共端点 - 包裹追踪应无需认证即可访问")
    void testPublicTrackingEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/tracking/123"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("公共端点 - Webhook 端点应无需认证即可访问")
    void testPublicWebhookEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/webhooks/amazon")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("公共端点 - 健康检查应无需认证即可访问")
    void testPublicHealthEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // ========================================
    // 未认证访问测试 - 应返回 401
    // ========================================

    @Test
    @DisplayName("未认证访问 - 用户端点应返回 401 Unauthorized")
    void testProtectedUserEndpoints_ShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("未认证访问 - 管理员端点应返回 401 Unauthorized")
    void testAdminEndpoints_ShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(post("/api/admin/fleet/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("未认证访问 - 卡车管理端点应返回 401 Unauthorized")
    void testTruckEndpoints_ShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/trucks"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(put("/api/trucks/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ========================================
    // 管理员权限测试 - 需要 ADMIN 角色
    // ========================================

    @Test
    @DisplayName("管理员权限 - ADMIN 角色应能访问管理员端点")
    void testAdminEndpoints_ShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/admin/fleet/overview")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("管理员权限 - 普通用户访问管理员端点应返回 403 Forbidden")
    void testAdminEndpoints_ShouldReturn403WhenAccessedByNormalUser() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(post("/api/admin/fleet/trucks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员权限 - 驱动员访问管理员端点应返回 403 Forbidden")
    void testAdminEndpoints_ShouldReturn403WhenAccessedByDriver() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(delete("/api/admin/fleet/trucks/1")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 用户管理端点权限测试
    // ========================================

    @Test
    @DisplayName("用户管理 - ADMIN 应能访问用户管理端点")
    void testUserManagementEndpoints_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"new_user\",\"email\":\"new@test.com\",\"password\":\"password123\",\"role\":\"USER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户管理 - 普通用户访问用户管理端点应返回 403 Forbidden")
    void testUserManagementEndpoints_ShouldReturn403ForNormalUser() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("个人资料 - 用户应能访问自己的个人资料")
    void testUserProfile_ShouldBeAccessibleByOwner() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        
        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Updated\",\"lastName\":\"Name\"}"))
                .andExpect(status().isOk());
    }

    // ========================================
    // 卡车管理权限测试
    // ========================================

    @Test
    @DisplayName("卡车管理 - ADMIN 和 OPERATOR 应能查看卡车信息")
    void testTruckViewEndpoints_ShouldBeAccessibleByAdminAndOperator() throws Exception {
        mockMvc.perform(get("/api/trucks")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/trucks/statistics")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车管理 - 普通用户查看卡车信息应返回 403 Forbidden")
    void testTruckViewEndpoints_ShouldReturn403ForNormalUser() throws Exception {
        mockMvc.perform(get("/api/trucks")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/api/trucks/statistics")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车管理 - 只有 ADMIN 应能进行卡车分配操作")
    void testTruckAssignmentEndpoints_ShouldBeAccessibleOnlyByAdmin() throws Exception {
        mockMvc.perform(post("/api/trucks/1/assign")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"target_x\":100,\"target_y\":200}"))
                .andExpect(status().isOk());
        
        mockMvc.perform(post("/api/trucks/1/release")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车管理 - 驱动员进行卡车分配应返回 403 Forbidden")
    void testTruckAssignmentEndpoints_ShouldReturn403ForDriver() throws Exception {
        mockMvc.perform(post("/api/trucks/1/assign")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"target_x\":100,\"target_y\":200}"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(post("/api/trucks/1/release")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // JWT 令牌验证测试
    // ========================================

    @Test
    @DisplayName("JWT 验证 - 无效令牌应返回 401 Unauthorized")
    void testInvalidJwtToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT 验证 - 格式错误的 Authorization header 应返回 401")
    void testMalformedAuthorizationHeader_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "InvalidFormat " + userToken))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", userToken)) // Missing "Bearer "
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT 验证 - 过期令牌应返回 401 Unauthorized")
    void testExpiredJwtToken_ShouldReturn401() throws Exception {
        // 创建一个过期的令牌（这里需要模拟过期令牌，实际实现可能需要修改 JwtTokenProvider）
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0X3VzZXIiLCJpYXQiOjE2MDAwMDAwMDAsImV4cCI6MTYwMDAwMDAwMX0.invalid";
        
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    // ========================================
    // 跨域请求测试
    // ========================================

    @Test
    @DisplayName("CORS 配置 - 应支持预检请求")
    void testCorsPreflightRequest_ShouldBeSupported() throws Exception {
        mockMvc.perform(options("/api/users/profile")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
    }

    // ========================================
    // 边界条件和异常情况测试
    // ========================================

    @Test
    @DisplayName("边界条件 - 空的 Authorization header 应返回 401")
    void testEmptyAuthorizationHeader_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("边界条件 - 只有 Bearer 前缀无令牌应返回 401")
    void testBearerWithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("用户权限 - 用户访问他人信息应返回 403 Forbidden")
    void testUserAccessingOthersData_ShouldReturn403() throws Exception {
        // 假设 adminUser.getId() 不等于当前 normalUser
        mockMvc.perform(get("/api/users/" + adminUser.getId())
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("用户权限 - 用户应能访问自己的信息")
    void testUserAccessingOwnData_ShouldBeAllowed() throws Exception {
        mockMvc.perform(get("/api/users/" + normalUser.getId())
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }
}