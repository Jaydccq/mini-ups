package com.miniups.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Asynchronous processing configuration for the Mini-UPS application.
 * 
 * This configuration class sets up thread pool task executors for handling
 * asynchronous operations such as background processing, notifications,
 * and other non-blocking tasks.
 * 
 * Features:
 * - Configures a main application task executor
 * - Enables asynchronous method execution
 * - Provides thread pool management for background tasks
 * 
 * @author Mini-UPS Development Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Main application task executor for general asynchronous processing.
     * 
     * This executor is used for background tasks such as:
     * - Audit logging
     * - Notification processing  
     * - ID generation
     * - Other non-blocking operations
     * 
     * Configuration:
     * - Core pool size: 5 threads
     * - Max pool size: 10 threads
     * - Queue capacity: 25 tasks
     * - Keep alive time: 60 seconds (default)
     * 
     * @return configured ThreadPoolTaskExecutor for application use
     */
    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("App-Async-");
        executor.initialize();
        return executor;
    }

    /**
     * Task executor specifically for audit operations.
     * 
     * This executor handles audit logging and compliance-related
     * background tasks that should not impact main application performance.
     * 
     * Configuration:
     * - Core pool size: 2 threads
     * - Max pool size: 5 threads
     * - Queue capacity: 50 tasks
     * 
     * @return configured ThreadPoolTaskExecutor for audit operations
     */
    @Bean(name = "auditTaskExecutor")
    public Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Audit-Async-");
        executor.initialize();
        return executor;
    }
}