package com.miniups.worldsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the World Simulator.
 * 
 * The World Simulator is a Spring Boot application that provides a real-time
 * logistics environment simulation for the Mini-UPS distributed system. It serves
 * as the central coordination point for warehouse operations, truck movements, 
 * and package deliveries between UPS and Amazon services.
 * 
 * Key Features:
 * - TCP server for UPS (port 12345) and Amazon (port 23456) connections
 * - Real-time truck position simulation and warehouse management
 * - Protobuf-based message protocol with ACK reliability
 * - Configurable network flakiness injection for testing
 * - PostgreSQL persistence with Redis caching
 * - High-performance Netty-based networking
 * - Spring Boot integration with metrics and health checks
 * 
 * @author World Simulator Team
 * @version 1.0.0
 * @since 2025-01-13
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@ConfigurationPropertiesScan
public class WorldSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorldSimulatorApplication.class, args);
    }
}