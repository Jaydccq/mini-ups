package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HTTP客户端配置属性
 * 
 * 定义HTTP客户端相关的配置参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "http")
public class HttpClientProperties {

    /**
     * 客户端配置
     */
    private Client client = new Client();

    @Data
    public static class Client {
        /**
         * 最大总连接数
         */
        private int maxTotalConnections = 100;

        /**
         * 每个路由的默认最大连接数
         */
        private int maxDefaultPerRoute = 20;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeoutMs = 10000;

        /**
         * Socket超时时间（毫秒）
         */
        private int socketTimeoutMs = 30000;
    }
}