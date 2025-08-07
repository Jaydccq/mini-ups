package com.miniups.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.TruckStatus;
import com.miniups.service.TruckManagementService;
import com.miniups.controller.TruckManagementController;
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
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(MockitoExtension.class)
public class TruckManagementControllerTest {

    @Mock
    private TruckManagementService truckManagementService;

    @InjectMocks
    private TruckManagementController truckManagementController;

    private MockMvc mockMvc;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    private Truck testTruck1;
    private Truck testTruck2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(truckManagementController).build();
        
        testTruck1 = new Truck();
        testTruck1.setId(1L);
        testTruck1.setStatus(TruckStatus.IDLE);

        testTruck2 = new Truck();
        testTruck2.setId(2L);
        testTruck2.setStatus(TruckStatus.EN_ROUTE);
    }

    @Test
    @DisplayName("获取所有卡车状态 - 正常情况")
    public void whenGetAllTrucks_thenReturnJsonArray() throws Exception {
        List<Map<String, Object>> allTrucks = Arrays.asList(
            Map.of("truck_id", "TRUCK001", "status", "IDLE", "status_display", "Idle", "current_x", 10, "current_y", 20, "capacity", 1000, "available", true),
            Map.of("truck_id", "TRUCK002", "status", "EN_ROUTE", "status_display", "En Route", "current_x", 15, "current_y", 25, "capacity", 1500, "available", false)
        );
        when(truckManagementService.getAllTruckStatuses()).thenReturn(allTrucks);

        mockMvc.perform(get("/trucks")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Truck statuses retrieved successfully"))
                .andExpect(jsonPath("$.data.trucks").isArray())
                .andExpect(jsonPath("$.data.trucks.length()").value(2))
                .andExpect(jsonPath("$.data.total_count").value(2))
                .andExpect(jsonPath("$.data.trucks[0].truck_id").value("TRUCK001"))
                .andExpect(jsonPath("$.data.trucks[0].status").value("IDLE"))
                .andExpect(jsonPath("$.data.trucks[0].available").value(true))
                .andExpect(jsonPath("$.data.trucks[1].truck_id").value("TRUCK002"))
                .andExpect(jsonPath("$.data.trucks[1].status").value("EN_ROUTE"))
                .andExpect(jsonPath("$.data.trucks[1].available").value(false));
        
        verify(truckManagementService).getAllTruckStatuses();
    }

    @Test
    @DisplayName("获取车队统计信息 - 正常情况")
    public void whenGetFleetStatistics_thenReturnStatistics() throws Exception {
        Map<String, Object> statistics = Map.of(
            "total_trucks", 5,
            "available_trucks", 3,
            "busy_trucks", 2,
            "utilization_rate", 40.0
        );
        when(truckManagementService.getFleetStatistics()).thenReturn(statistics);

        mockMvc.perform(get("/trucks/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fleet statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.total_trucks").value(5))
                .andExpect(jsonPath("$.data.available_trucks").value(3))
                .andExpect(jsonPath("$.data.busy_trucks").value(2))
                .andExpect(jsonPath("$.data.utilization_rate").value(40.0));
        
        verify(truckManagementService).getFleetStatistics();
    }
}