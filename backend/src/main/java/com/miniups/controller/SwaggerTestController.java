package com.miniups.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Swagger Test", description = "Test endpoints for Swagger UI verification")
public class SwaggerTestController {

    @GetMapping("/hello")
    @Operation(summary = "Hello World", description = "Simple hello world endpoint for testing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    public ResponseEntity<Map<String, String>> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from Mini-UPS API!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("swagger", "working");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check if the API is healthy")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "API is healthy")
    })
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "mini-ups-backend");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}