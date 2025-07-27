/**
 * Admin Controller Security Test
 * 
 * 目的:
 * - 专门测试 AdminController 的安全控制机制
 * - 验证管理员功能的严格权限控制
 * - 确保只有 ADMIN 角色能访问管理员端点
 * - 测试管理员操作的安全边界
 * 
 * 测试覆盖范围:
 * - 管理员仪表盘访问控制
 * - 车队管理权限验证
 * - 系统管理功能保护
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
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.defer-datasource-initialization=true",
    "spring.jpa.generate-ddl=true"
})
@Transactional
public class AdminControllerSecurityTest {

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
    private User operatorUser;
    private String adminToken;
    private String userToken;
    private String driverToken;
    private String operatorToken;

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
        adminUser = userRepository.save(adminUser);

        // 普通用户
        normalUser = new User();
        normalUser.setUsername("user_security");
        normalUser.setEmail("user.security@test.com");
        normalUser.setPassword(passwordEncoder.encode("user123"));
        normalUser.setRole(UserRole.USER);
        normalUser.setEnabled(true);
        normalUser = userRepository.save(normalUser);

        // 驱动员用户
        driverUser = new User();
        driverUser.setUsername("driver_security");
        driverUser.setEmail("driver.security@test.com");
        driverUser.setPassword(passwordEncoder.encode("driver123"));
        driverUser.setRole(UserRole.DRIVER);
        driverUser.setEnabled(true);
        driverUser = userRepository.save(driverUser);

        // 操作员用户（如果存在此角色）
        operatorUser = new User();
        operatorUser.setUsername("operator_security");
        operatorUser.setEmail("operator.security@test.com");
        operatorUser.setPassword(passwordEncoder.encode("operator123"));
        operatorUser.setRole(UserRole.OPERATOR);
        operatorUser.setEnabled(true);
        operatorUser = userRepository.save(operatorUser);
    }

    private void setupJwtTokens() {
        adminToken = jwtTokenProvider.generateToken(adminUser.getUsername());
        userToken = jwtTokenProvider.generateToken(normalUser.getUsername());
        driverToken = jwtTokenProvider.generateToken(driverUser.getUsername());
        operatorToken = jwtTokenProvider.generateToken(operatorUser.getUsername());
    }

    // ========================================
    // 管理员仪表盘访问控制测试
    // ========================================

    @Test
    @DisplayName("管理员仪表盘 - ADMIN 角色应能访问仪表盘统计")
    void testDashboardStatistics_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("管理员仪表盘 - 普通用户不能访问仪表盘统计")
    void testDashboardStatistics_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员仪表盘 - 驱动员不能访问仪表盘统计")
    void testDashboardStatistics_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员仪表盘 - 操作员不能访问仪表盘统计")
    void testDashboardStatistics_ShouldDenyOperator() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员仪表盘 - 未认证用户不能访问仪表盘")
    void testDashboardStatistics_ShouldDenyUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员仪表盘 - ADMIN 角色应能访问最近活动")
    void testRecentActivities_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/activities")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("管理员仪表盘 - 普通用户不能访问最近活动")
    void testRecentActivities_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/activities")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 车队管理权限测试
    // ========================================

    @Test
    @DisplayName("车队管理 - ADMIN 角色应能访问车队概览")
    void testFleetOverview_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/fleet/overview")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("车队管理 - 驱动员不能访问车队概览")
    void testFleetOverview_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/admin/fleet/overview")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("车队管理 - ADMIN 角色应能访问驱动员管理")
    void testDriverManagement_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/fleet/drivers")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("车队管理 - 驱动员不能访问驱动员管理")
    void testDriverManagement_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/admin/fleet/drivers")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 卡车管理 CRUD 操作权限测试
    // ========================================

    @Test
    @DisplayName("卡车管理 - ADMIN 角色应能创建卡车")
    void testCreateTruck_ShouldBeAccessibleByAdmin() throws Exception {
        String truckData = "{"
                + "\"capacity\":1000,"
                + "\"currentX\":100,"
                + "\"currentY\":200,"
                + "\"status\":\"IDLE\""
                + "}";

        mockMvc.perform(post("/api/admin/fleet/trucks")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(truckData))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车管理 - 普通用户不能创建卡车")
    void testCreateTruck_ShouldDenyNormalUser() throws Exception {
        String truckData = "{"
                + "\"capacity\":1000,"
                + "\"currentX\":100,"
                + "\"currentY\":200,"
                + "\"status\":\"IDLE\""
                + "}";

        mockMvc.perform(post("/api/admin/fleet/trucks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(truckData))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车管理 - 驱动员不能创建卡车")
    void testCreateTruck_ShouldDenyDriver() throws Exception {
        String truckData = "{"
                + "\"capacity\":1000,"
                + "\"currentX\":100,"
                + "\"currentY\":200,"
                + "\"status\":\"IDLE\""
                + "}";

        mockMvc.perform(post("/api/admin/fleet/trucks")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(truckData))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车管理 - ADMIN 角色应能更新卡车")
    void testUpdateTruck_ShouldBeAccessibleByAdmin() throws Exception {
        String updateData = "{"
                + "\"capacity\":1500,"
                + "\"status\":\"BUSY\""
                + "}";

        mockMvc.perform(put("/api/admin/fleet/trucks/1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateData))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车管理 - 普通用户不能更新卡车")
    void testUpdateTruck_ShouldDenyNormalUser() throws Exception {
        String updateData = "{"
                + "\"capacity\":1500,"
                + "\"status\":\"BUSY\""
                + "}";

        mockMvc.perform(put("/api/admin/fleet/trucks/1")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateData))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车管理 - ADMIN 角色应能删除卡车")
    void testDeleteTruck_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/fleet/trucks/1")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车管理 - 驱动员不能删除卡车")
    void testDeleteTruck_ShouldDenyDriver() throws Exception {
        mockMvc.perform(delete("/api/admin/fleet/trucks/1")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 驱动员管理 CRUD 操作权限测试
    // ========================================

    @Test
    @DisplayName("驱动员管理 - ADMIN 角色应能创建驱动员")
    void testCreateDriver_ShouldBeAccessibleByAdmin() throws Exception {
        String driverData = "{"
                + "\"username\":\"new_driver\","
                + "\"email\":\"newdriver@test.com\","
                + "\"firstName\":\"New\","
                + "\"lastName\":\"Driver\","
                + "\"licenseNumber\":\"DL123456\""
                + "}";

        mockMvc.perform(post("/api/admin/fleet/drivers")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(driverData))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("驱动员管理 - 普通用户不能创建驱动员")
    void testCreateDriver_ShouldDenyNormalUser() throws Exception {
        String driverData = "{"
                + "\"username\":\"unauthorized_driver\","
                + "\"email\":\"unauthorized@test.com\""
                + "}";

        mockMvc.perform(post("/api/admin/fleet/drivers")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(driverData))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("驱动员管理 - 驱动员不能创建其他驱动员")
    void testCreateDriver_ShouldDenyDriver() throws Exception {
        String driverData = "{"
                + "\"username\":\"another_driver\","
                + "\"email\":\"another@test.com\""
                + "}";

        mockMvc.perform(post("/api/admin/fleet/drivers")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(driverData))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("驱动员管理 - ADMIN 角色应能更新驱动员")
    void testUpdateDriver_ShouldBeAccessibleByAdmin() throws Exception {
        String updateData = "{"
                + "\"firstName\":\"Updated\","
                + "\"lastName\":\"Driver\","
                + "\"status\":\"ACTIVE\""
                + "}";

        mockMvc.perform(put("/api/admin/fleet/drivers/1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateData))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("驱动员管理 - ADMIN 角色应能删除驱动员")
    void testDeleteDriver_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/fleet/drivers/1")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ========================================
    // 订单管理权限测试
    // ========================================

    @Test
    @DisplayName("订单管理 - ADMIN 角色应能访问订单概要")
    void testOrderSummary_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/orders/summary")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("订单管理 - 普通用户不能访问订单概要")
    void testOrderSummary_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(get("/api/admin/orders/summary")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("订单管理 - 驱动员不能访问订单概要")
    void testOrderSummary_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/admin/orders/summary")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 分析和趋势权限测试
    // ========================================

    @Test
    @DisplayName("数据分析 - ADMIN 角色应能访问分析趋势")
    void testAnalyticsTrends_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/trends")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("数据分析 - 普通用户不能访问分析趋势")
    void testAnalyticsTrends_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/trends")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("数据分析 - 驱动员不能访问分析趋势")
    void testAnalyticsTrends_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/trends")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 系统健康状态权限测试
    // ========================================

    @Test
    @DisplayName("系统管理 - ADMIN 角色应能访问系统健康状态")
    void testSystemHealth_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/system/health")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("系统管理 - 普通用户不能访问系统健康状态")
    void testSystemHealth_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(get("/api/admin/system/health")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("系统管理 - 驱动员不能访问系统健康状态")
    void testSystemHealth_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/admin/system/health")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 边界条件和安全测试
    // ========================================

    @Test
    @DisplayName("边界条件 - 无效的 JWT 令牌应被拒绝")
    void testInvalidJwtToken_ShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("边界条件 - 过期的 JWT 令牌应被拒绝")
    void testExpiredJwtToken_ShouldBeRejected() throws Exception {
        // 使用一个明显过期的令牌格式
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTYwMDAwMDAwMH0.invalid";
        
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("安全测试 - 格式错误的 Authorization header 应被拒绝")
    void testMalformedAuthorizationHeader_ShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "InvalidFormat " + adminToken))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", adminToken)) // Missing "Bearer "
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("安全测试 - 空的 Authorization header 应被拒绝")
    void testEmptyAuthorizationHeader_ShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    // ========================================
    // 类级别安全注解测试
    // ========================================

    @Test
    @DisplayName("类级别安全 - 所有管理员端点都应要求 ADMIN 角色")
    void testClassLevelSecurity_AllEndpointsShouldRequireAdminRole() throws Exception {
        // 测试多个不同的管理员端点，确保类级别的 @PreAuthorize 注解生效
        String[] adminEndpoints = {
            "/api/admin/dashboard/statistics",
            "/api/admin/dashboard/activities",
            "/api/admin/fleet/overview",
            "/api/admin/fleet/drivers",
            "/api/admin/orders/summary",
            "/api/admin/analytics/trends",
            "/api/admin/system/health"
        };

        for (String endpoint : adminEndpoints) {
            // 普通用户应被拒绝
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
            
            // 驱动员应被拒绝
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + driverToken))
                    .andExpect(status().isForbidden());
            
            // 管理员应被允许
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }
}