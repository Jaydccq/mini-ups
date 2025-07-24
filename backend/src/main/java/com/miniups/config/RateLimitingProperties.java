package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 速率限制配置属性
 * 
 * 定义API请求速率限制的配置参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limiting")
public class RateLimitingProperties {

    /**
     * 是否启用速率限制
     */
    private boolean enabled = true;

    /**
     * 货物创建速率限制
     */
    private ShipmentCreation shipmentCreation = new ShipmentCreation();

    /**
     * 通用API速率限制
     */
    private GeneralApi generalApi = new GeneralApi();

    @Data
    public static class ShipmentCreation {
        /**
         * 每分钟请求数限制
         */
        private int requestsPerMinute = 10;
    }

    @Data
    public static class GeneralApi {
        /**
         * 每分钟请求数限制
         */
        private int requestsPerMinute = 60;
    }
}