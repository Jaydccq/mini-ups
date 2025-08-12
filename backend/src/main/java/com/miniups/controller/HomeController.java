/**
 * Home Controller
 * 
 * Purpose:
 * - Provides basic information and navigation for the Mini-UPS API
 * - Handles root path requests with useful API information
 * - Serves as an entry point for API discovery
 * 
 *
 
 */
package com.miniups.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @Value("${server.port:8081}")
    private String serverPort;

    @GetMapping("/")
    public ResponseEntity<?> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Mini-UPS Backend API");
        response.put("version", "1.0.0");
        response.put("status", "Running");
        response.put("timestamp", LocalDateTime.now());
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/actuator/health");
        endpoints.put("api_docs", "/api-docs");
        endpoints.put("swagger_ui", "/swagger-ui.html");
        endpoints.put("register", "POST /api/auth/register");
        endpoints.put("login", "POST /api/auth/login");
        endpoints.put("tracking", "GET /api/tracking/{trackingNumber}");
        
        response.put("available_endpoints", endpoints);
        response.put("documentation", "http://localhost:" + serverPort + "/swagger-ui.html");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api")
    public ResponseEntity<?> apiInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Mini-UPS API Service");
        response.put("version", "1.0.0");
        response.put("description", "RESTful API for Mini-UPS package delivery system");
        
        Map<String, String> authEndpoints = new HashMap<>();
        authEndpoints.put("register", "POST /api/auth/register");
        authEndpoints.put("login", "POST /api/auth/login");
        authEndpoints.put("profile", "GET /api/auth/me");
        authEndpoints.put("logout", "POST /api/auth/logout");
        
        Map<String, String> publicEndpoints = new HashMap<>();
        publicEndpoints.put("health", "GET /actuator/health");
        publicEndpoints.put("tracking", "GET /api/tracking/{trackingNumber}");
        publicEndpoints.put("api_docs", "GET /api-docs");
        
        response.put("authentication_endpoints", authEndpoints);
        response.put("public_endpoints", publicEndpoints);
        response.put("base_url", "http://localhost:" + serverPort);
        
        return ResponseEntity.ok(response);
    }

}