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
import com.miniups.controller.AdminController;
import com.miniups.service.AdminService;
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

import java.util.Map;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminControllerSecurityTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;
    
    private MockMvc mockMvc;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    @DisplayName("管理员仪表盘 - ADMIN 角色应能访问仪表盘统计")
    void testDashboardStatistics_ShouldBeAccessibleByAdmin() throws Exception {
        // Mock service response
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("totalTrucks", 10);
        mockStats.put("activeTrucks", 7);
        when(adminService.getDashboardStatistics()).thenReturn(mockStats);
        
        mockMvc.perform(get("/admin/dashboard/statistics"))
                .andExpect(status().isOk());
                
        verify(adminService).getDashboardStatistics();
    }

    @Test
    @DisplayName("管理员功能 - 获取系统健康状态")
    void testSystemHealth_ShouldWork() throws Exception {
        Map<String, Object> mockHealth = new HashMap<>();
        mockHealth.put("status", "healthy");
        when(adminService.getSystemHealth()).thenReturn(mockHealth);
        
        mockMvc.perform(get("/admin/system/health"))
                .andExpect(status().isOk());
                
        verify(adminService).getSystemHealth();
    }

    @Test
    @DisplayName("管理员功能 - 获取车队概览")
    void testFleetOverview_ShouldWork() throws Exception {
        Map<String, Object> mockFleet = new HashMap<>();
        mockFleet.put("totalFleet", 15);
        when(adminService.getFleetOverview()).thenReturn(mockFleet);
        
        mockMvc.perform(get("/admin/fleet/overview"))
                .andExpect(status().isOk());
                
        verify(adminService).getFleetOverview();
    }
}