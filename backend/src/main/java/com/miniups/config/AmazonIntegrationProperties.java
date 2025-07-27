package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Amazon 集成服务配置属性
 * 
 * 定义与Amazon服务集成的配置参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "amazon")
public class AmazonIntegrationProperties {

    /**
     * 基础URL
     */
    private String baseUrl = "http://localhost:8080";

    /**
     * Webhook配置
     */
    private Webhook webhook = new Webhook();

    /**
     * API配置
     */
    private Api api = new Api();

    /**
     * Webhook端点配置
     */
    private WebhookEndpoints webhookEndpoints = new WebhookEndpoints();

    @Data
    public static class Webhook {
        /**
         * Webhook密钥
         */
        private String secret = "default-webhook-secret-key";

        /**
         * 签名头
         */
        private String signatureHeader = "X-Amazon-Signature";

        /**
         * 认证配置
         */
        private Authentication authentication = new Authentication();

        @Data
        public static class Authentication {
            /**
             * 是否启用认证
             */
            private boolean enabled = true;
        }
    }

    @Data
    public static class Api {
        /**
         * API URL
         */
        private String url = "http://localhost:8080";

        /**
         * 超时时间（毫秒）
         */
        private int timeout = 5000;

        /**
         * 是否启用API集成
         */
        private boolean enabled = true;
    }

    @Data
    public static class WebhookEndpoints {
        /**
         * 卡车到达端点
         */
        private String truckArrived = "/api/webhooks/truck-arrived";

        /**
         * 卡车派遣端点
         */
        private String truckDispatched = "/api/webhooks/truck-dispatched";

        /**
         * 货物送达端点
         */
        private String shipmentDelivered = "/api/webhooks/shipment-delivered";

        /**
         * 货物详情请求端点
         */
        private String shipmentDetailRequest = "/api/webhooks/shipment-detail-request";
    }
}