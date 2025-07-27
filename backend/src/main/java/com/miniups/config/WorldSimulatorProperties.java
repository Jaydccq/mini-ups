package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * World Simulator 配置属性
 * 
 * 定义与世界模拟器服务集成的配置参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "world")
public class WorldSimulatorProperties {

    /**
     * 模拟器配置
     */
    private Simulator simulator = new Simulator();

    @Data
    public static class Simulator {
        /**
         * 模拟器主机地址
         */
        private String host = "localhost";

        /**
         * 模拟器端口
         */
        private int port = 12345;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 10000;

        /**
         * 读取超时时间（毫秒）
         */
        private int readTimeout = 10000;

        /**
         * 默认模拟速度
         */
        private int defaultSimSpeed = 1000;

        /**
         * 是否启用模拟器集成
         */
        private boolean enabled = true;
    }
}