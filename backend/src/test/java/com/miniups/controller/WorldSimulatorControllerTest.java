

package com.miniups.controller;

import com.miniups.service.WorldSimulatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorldSimulatorController.class)
@ActiveProfiles("test")
@Import(WorldSimulatorControllerTest.TestConfig.class)
public class WorldSimulatorControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public WorldSimulatorService worldSimulatorService() {
            return Mockito.mock(WorldSimulatorService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorldSimulatorService worldSimulatorService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void whenSetSpeed_thenServiceIsCalled() throws Exception {
        mockMvc.perform(post("/api/simulator/speed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"speed\": 5}"))
                .andExpect(status().isOk());

        verify(worldSimulatorService).setSimulationSpeed(5);
    }
}

