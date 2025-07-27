

package com.miniups.controller;

import com.miniups.service.WorldSimulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorldSimulatorControllerTest {

    @Mock
    private WorldSimulatorService worldSimulatorService;
    
    @InjectMocks
    private WorldSimulatorController worldSimulatorController;

    @BeforeEach
    void setUp() {
        // Setup common mock behavior
        when(worldSimulatorService.isConnected()).thenReturn(true);
    }

    @Test
    public void whenSetSpeed_thenServiceIsCalled() throws Exception {
        // Arrange
        Map<String, Object> speedRequest = Map.of("speed", 5);
        
        // Act
        ResponseEntity<Map<String, Object>> response = worldSimulatorController.setSimulationSpeed(speedRequest);
        
        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("Simulation speed updated successfully", response.getBody().get("message"));
        assertEquals(5, response.getBody().get("speed"));
        
        verify(worldSimulatorService).setSimulationSpeed(5);
    }
}

