
package com.miniups.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.TruckStatus;
import com.miniups.service.TruckManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TruckManagementControllerTest.TestConfig.class)
public class TruckManagementControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public TruckManagementService truckManagementService() {
            return mock(TruckManagementService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TruckManagementService truckManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    private Truck testTruck1;
    private Truck testTruck2;

    @BeforeEach
    void setUp() {
        testTruck1 = new Truck();
        testTruck1.setId(1L);
        testTruck1.setStatus(TruckStatus.IDLE);

        testTruck2 = new Truck();
        testTruck2.setId(2L);
        testTruck2.setStatus(TruckStatus.EN_ROUTE);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void whenGetAllTrucks_thenReturnJsonArray() throws Exception {
        List<Map<String, Object>> allTrucks = Arrays.asList(
            Map.of("truck_id", "TRUCK001", "status", "IDLE", "status_display", "Idle", "current_x", 10, "current_y", 20, "capacity", 1000, "available", true),
            Map.of("truck_id", "TRUCK002", "status", "EN_ROUTE", "status_display", "En Route", "current_x", 15, "current_y", 25, "capacity", 1500, "available", false)
        );
        when(truckManagementService.getAllTruckStatuses()).thenReturn(allTrucks);

        mockMvc.perform(get("/api/trucks")
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
    }
}
