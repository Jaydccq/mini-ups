/**
 * Truck Controller Security Test
 * 
 * 目的:
 * - 专门测试 TruckManagementController 的安全控制机制
 * - 验证卡车管理端点的角色权限控制
 * - 确保运营人员和管理员的权限边界
 * - 测试卡车操作的安全性
 * 
 * 测试覆盖范围:
 * - 卡车查看权限 (ADMIN/OPERATOR)
 * - 卡车管理权限 (ADMIN only)
 * - 卡车分配和释放权限
 * - 系统级操作权限验证
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.controller.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.model.entity.User;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.UserRole;
import com.miniups.model.enums.TruckStatus;
import com.miniups.repository.UserRepository;
import com.miniups.repository.TruckRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public class TruckControllerSecurityTest {

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

    @Autowired
    private TruckRepository truckRepository;

    private User adminUser;
    private User normalUser;
    private User driverUser;
    private User operatorUser;
    private User systemUser;
    private String adminToken;
    private String userToken;
    private String driverToken;
    private String operatorToken;
    private String systemToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        truckRepository.deleteAll(); // 清理卡车数据
        setupTestUsers();
        setupTestTrucks(); // 创建测试卡车
        setupJwtTokens();
    }

    private void setupTestUsers() {
        // 管理员用户
        adminUser = new User();
        adminUser.setUsername("admin_truck");
        adminUser.setEmail("admin.truck@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        // 普通用户
        normalUser = new User();
        normalUser.setUsername("user_truck");
        normalUser.setEmail("user.truck@test.com");
        normalUser.setPassword(passwordEncoder.encode("user123"));
        normalUser.setRole(UserRole.USER);
        normalUser.setEnabled(true);
        normalUser = userRepository.save(normalUser);

        // 驱动员用户
        driverUser = new User();
        driverUser.setUsername("driver_truck");
        driverUser.setEmail("driver.truck@test.com");
        driverUser.setPassword(passwordEncoder.encode("driver123"));
        driverUser.setRole(UserRole.DRIVER);
        driverUser.setEnabled(true);
        driverUser = userRepository.save(driverUser);

        // 操作员用户
        operatorUser = new User();
        operatorUser.setUsername("operator_truck");
        operatorUser.setEmail("operator.truck@test.com");
        operatorUser.setPassword(passwordEncoder.encode("operator123"));
        operatorUser.setRole(UserRole.OPERATOR);
        operatorUser.setEnabled(true);
        operatorUser = userRepository.save(operatorUser);

        // 系统用户（用于系统级操作）- Using ADMIN role for system operations
        systemUser = new User();
        systemUser.setUsername("system_truck");
        systemUser.setEmail("system.truck@test.com");
        systemUser.setPassword(passwordEncoder.encode("system123"));
        systemUser.setRole(UserRole.ADMIN);
        systemUser.setEnabled(true);
        systemUser = userRepository.save(systemUser);
    }

    private void setupTestTrucks() {
        // Create test trucks with specific IDs that the security tests expect
        Truck truck1 = new Truck();
        truck1.setTruckId(1);
        truck1.setCurrentX(100);
        truck1.setCurrentY(200);
        truck1.setStatus(TruckStatus.IDLE);
        truck1.setCapacity(1000);
        truckRepository.save(truck1);

        Truck truck2 = new Truck();
        truck2.setTruckId(2);
        truck2.setCurrentX(150);
        truck2.setCurrentY(250);
        truck2.setStatus(TruckStatus.IDLE);
        truck2.setCapacity(1000);
        truckRepository.save(truck2);
    }

    private void setupJwtTokens() {
        adminToken = jwtTokenProvider.generateToken(adminUser.getUsername());
        userToken = jwtTokenProvider.generateToken(normalUser.getUsername());
        driverToken = jwtTokenProvider.generateToken(driverUser.getUsername());
        operatorToken = jwtTokenProvider.generateToken(operatorUser.getUsername());
        systemToken = jwtTokenProvider.generateToken(systemUser.getUsername());
    }

    // ========================================
    // 卡车查看权限测试 (ADMIN/OPERATOR)
    // ========================================

    @Test
    @DisplayName("卡车查看 - ADMIN 角色应能查看所有卡车")
    void testGetAllTrucks_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/trucks")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车查看 - OPERATOR 角色应能查看所有卡车")
    void testGetAllTrucks_ShouldBeAccessibleByOperator() throws Exception {
        mockMvc.perform(get("/api/trucks")
                .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车查看 - 普通用户不能查看卡车信息")
    void testGetAllTrucks_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(get("/api/trucks")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车查看 - 驱动员不能查看所有卡车信息")
    void testGetAllTrucks_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/trucks")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车查看 - 未认证用户不能查看卡车信息")
    void testGetAllTrucks_ShouldDenyUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/trucks"))
                .andExpect(status().isUnauthorized());
    }

    // ========================================
    // 卡车统计权限测试 (ADMIN/OPERATOR)
    // ========================================

    @Test
    @DisplayName("卡车统计 - ADMIN 角色应能查看车队统计")
    void testGetFleetStatistics_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/trucks/statistics")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车统计 - OPERATOR 角色应能查看车队统计")
    void testGetFleetStatistics_ShouldBeAccessibleByOperator() throws Exception {
        mockMvc.perform(get("/api/trucks/statistics")
                .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车统计 - 普通用户不能查看车队统计")
    void testGetFleetStatistics_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(get("/api/trucks/statistics")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车统计 - 驱动员不能查看车队统计")
    void testGetFleetStatistics_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/trucks/statistics")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 卡车状态更新权限测试 (ADMIN/OPERATOR)
    // ========================================

    @Test
    @DisplayName("卡车状态更新 - ADMIN 角色应能更新卡车状态")
    void testUpdateTruckStatus_ShouldBeAccessibleByAdmin() throws Exception {
        String statusUpdate = "{"
                + "\"x\":100,"
                + "\"y\":200,"
                + "\"status\":\"BUSY\""
                + "}";

        mockMvc.perform(put("/api/trucks/1/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusUpdate))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车状态更新 - OPERATOR 角色应能更新卡车状态")
    void testUpdateTruckStatus_ShouldBeAccessibleByOperator() throws Exception {
        String statusUpdate = "{"
                + "\"x\":150,"
                + "\"y\":250,"
                + "\"status\":\"IDLE\""
                + "}";

        mockMvc.perform(put("/api/trucks/1/status")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusUpdate))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车状态更新 - 普通用户不能更新卡车状态")
    void testUpdateTruckStatus_ShouldDenyNormalUser() throws Exception {
        String statusUpdate = "{"
                + "\"x\":100,"
                + "\"y\":200,"
                + "\"status\":\"BUSY\""
                + "}";

        mockMvc.perform(put("/api/trucks/1/status")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusUpdate))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车状态更新 - 驱动员不能更新卡车状态")
    void testUpdateTruckStatus_ShouldDenyDriver() throws Exception {
        String statusUpdate = "{"
                + "\"x\":100,"
                + "\"y\":200,"
                + "\"status\":\"BUSY\""
                + "}";

        mockMvc.perform(put("/api/trucks/1/status")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusUpdate))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 卡车分配权限测试 (仅 ADMIN)
    // ========================================

    @Test
    @DisplayName("卡车分配 - ADMIN 角色应能手动分配卡车")
    void testAssignTruck_ShouldBeAccessibleByAdmin() throws Exception {
        String assignmentRequest = "{"
                + "\"target_x\":300,"
                + "\"target_y\":400,"
                + "\"priority\":1"
                + "}";

        mockMvc.perform(post("/api/trucks/1/assign")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentRequest))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车分配 - OPERATOR 不能手动分配卡车")
    void testAssignTruck_ShouldDenyOperator() throws Exception {
        String assignmentRequest = "{"
                + "\"target_x\":300,"
                + "\"target_y\":400,"
                + "\"priority\":1"
                + "}";

        mockMvc.perform(post("/api/trucks/1/assign")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车分配 - 普通用户不能手动分配卡车")
    void testAssignTruck_ShouldDenyNormalUser() throws Exception {
        String assignmentRequest = "{"
                + "\"target_x\":300,"
                + "\"target_y\":400"
                + "}";

        mockMvc.perform(post("/api/trucks/1/assign")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车分配 - 驱动员不能手动分配卡车")
    void testAssignTruck_ShouldDenyDriver() throws Exception {
        String assignmentRequest = "{"
                + "\"target_x\":300,"
                + "\"target_y\":400"
                + "}";

        mockMvc.perform(post("/api/trucks/1/assign")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentRequest))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 卡车释放权限测试 (仅 ADMIN)
    // ========================================

    @Test
    @DisplayName("卡车释放 - ADMIN 角色应能释放卡车")
    void testReleaseTruck_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(post("/api/trucks/1/release")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("卡车释放 - OPERATOR 不能释放卡车")
    void testReleaseTruck_ShouldDenyOperator() throws Exception {
        mockMvc.perform(post("/api/trucks/1/release")
                .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车释放 - 普通用户不能释放卡车")
    void testReleaseTruck_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(post("/api/trucks/1/release")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("卡车释放 - 驱动员不能释放卡车")
    void testReleaseTruck_ShouldDenyDriver() throws Exception {
        mockMvc.perform(post("/api/trucks/1/release")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 最近卡车查找权限测试 (ADMIN/OPERATOR)
    // ========================================

    @Test
    @DisplayName("最近卡车查找 - ADMIN 角色应能查找最近卡车")
    void testFindNearestTruck_ShouldBeAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/api/trucks/nearest")
                .param("targetX", "100")
                .param("targetY", "200")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("最近卡车查找 - OPERATOR 角色应能查找最近卡车")
    void testFindNearestTruck_ShouldBeAccessibleByOperator() throws Exception {
        mockMvc.perform(get("/api/trucks/nearest")
                .param("targetX", "100")
                .param("targetY", "200")
                .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("最近卡车查找 - 普通用户不能查找最近卡车")
    void testFindNearestTruck_ShouldDenyNormalUser() throws Exception {
        mockMvc.perform(get("/api/trucks/nearest")
                .param("targetX", "100")
                .param("targetY", "200")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("最近卡车查找 - 驱动员不能查找最近卡车")
    void testFindNearestTruck_ShouldDenyDriver() throws Exception {
        mockMvc.perform(get("/api/trucks/nearest")
                .param("targetX", "100")
                .param("targetY", "200")
                .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 批量更新权限测试 (ADMIN/SYSTEM)
    // ========================================

    @Test
    @DisplayName("批量更新 - ADMIN 角色应能批量更新卡车")
    void testBatchUpdateTrucks_ShouldBeAccessibleByAdmin() throws Exception {
        String batchUpdate = "{"
                + "\"truck_updates\":["
                + "  {\"truck_id\":1,\"x\":100,\"y\":200,\"status\":\"BUSY\"},"
                + "  {\"truck_id\":2,\"x\":150,\"y\":250,\"status\":\"IDLE\"}"
                + "]"
                + "}";

        mockMvc.perform(post("/api/trucks/batch-update")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchUpdate))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("批量更新 - SYSTEM 角色应能批量更新卡车")
    void testBatchUpdateTrucks_ShouldBeAccessibleBySystem() throws Exception {
        String batchUpdate = "{"
                + "\"truck_updates\":["
                + "  {\"truck_id\":1,\"x\":100,\"y\":200,\"status\":\"BUSY\"}"
                + "]"
                + "}";

        mockMvc.perform(post("/api/trucks/batch-update")
                .header("Authorization", "Bearer " + systemToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchUpdate))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("批量更新 - OPERATOR 不能批量更新卡车")
    void testBatchUpdateTrucks_ShouldDenyOperator() throws Exception {
        String batchUpdate = "{"
                + "\"truck_updates\":["
                + "  {\"truck_id\":1,\"x\":100,\"y\":200,\"status\":\"BUSY\"}"
                + "]"
                + "}";

        mockMvc.perform(post("/api/trucks/batch-update")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchUpdate))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("批量更新 - 普通用户不能批量更新卡车")
    void testBatchUpdateTrucks_ShouldDenyNormalUser() throws Exception {
        String batchUpdate = "{"
                + "\"truck_updates\":["
                + "  {\"truck_id\":1,\"x\":100,\"y\":200,\"status\":\"BUSY\"}"
                + "]"
                + "}";

        mockMvc.perform(post("/api/trucks/batch-update")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchUpdate))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("批量更新 - 驱动员不能批量更新卡车")
    void testBatchUpdateTrucks_ShouldDenyDriver() throws Exception {
        String batchUpdate = "{"
                + "\"truck_updates\":["
                + "  {\"truck_id\":1,\"x\":100,\"y\":200,\"status\":\"BUSY\"}"
                + "]"
                + "}";

        mockMvc.perform(post("/api/trucks/batch-update")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchUpdate))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 数据验证和边界条件测试
    // ========================================

    @Test
    @DisplayName("数据验证 - 卡车状态更新应验证必填字段")
    void testUpdateTruckStatus_ShouldValidateRequiredFields() throws Exception {
        String invalidUpdate = "{"
                + "\"status\":\"BUSY\""
                + "}"; // 缺少 x 和 y 坐标

        mockMvc.perform(put("/api/trucks/1/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUpdate))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("数据验证 - 卡车分配应验证目标坐标")
    void testAssignTruck_ShouldValidateTargetCoordinates() throws Exception {
        String invalidAssignment = "{"
                + "\"priority\":1"
                + "}"; // 缺少目标坐标

        mockMvc.perform(post("/api/trucks/1/assign")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidAssignment))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("数据验证 - 批量更新应验证更新列表")
    void testBatchUpdateTrucks_ShouldValidateUpdateList() throws Exception {
        String invalidBatch = "{}"; // 缺少 truck_updates 字段

        mockMvc.perform(post("/api/trucks/batch-update")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBatch))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("边界条件 - 最近卡车查找应验证查询参数")
    void testFindNearestTruck_ShouldValidateQueryParameters() throws Exception {
        // 缺少必需的查询参数
        mockMvc.perform(get("/api/trucks/nearest")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    // ========================================
    // CORS 和安全头测试
    // ========================================

    @Test
    @DisplayName("CORS 支持 - 应支持跨域请求")
    void testCorsSupport_ShouldSupportCrossOriginRequests() throws Exception {
        mockMvc.perform(options("/api/trucks")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    // ========================================
    // 性能和负载测试
    // ========================================

    @Test
    @DisplayName("性能测试 - 卡车查询应在合理时间内响应")
    void testGetAllTrucks_ShouldRespondInReasonableTime() throws Exception {
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/trucks")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 查询应在 2 秒内完成
        assert duration < 2000 : "Truck query took too long: " + duration + "ms";
    }
}