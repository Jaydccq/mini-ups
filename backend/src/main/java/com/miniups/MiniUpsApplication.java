/**
 * Mini-UPS 后端应用程序主启动类
 * 
 * 基于Spring Boot的分布式物流系统后端服务，提供：
 * - 用户认证和权限管理
 * - 订单和包裹管理
 * - 车辆调度和路径优化
 * - 实时追踪和状态更新
 * - Amazon平台集成
 * - 世界模拟器集成
 * 
 * @author Mini-UPS Development Team
 * @version 1.0.0
 * @since 2024
 */
package com.miniups;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
@ConfigurationPropertiesScan
public class MiniUpsApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MiniUpsApplication.class, args);
    }
}
