package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用程序自定义配置属性
 * 
 * 用于定义和验证应用程序的自定义配置属性，
 * 解决IDE对自定义配置的警告问题。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    /**
     * CORS 配置
     */
    private Cors cors = new Cors();

    /**
     * RabbitMQ 配置
     */
    private RabbitMQ rabbitmq = new RabbitMQ();

    /**
     * 通知配置
     */
    private Notifications notifications = new Notifications();

    @Data
    public static class Cors {
        /**
         * 允许的来源列表
         */
        private String allowedOrigins = "http://localhost:3000";
    }

    @Data
    public static class RabbitMQ {
        /**
         * 测试配置
         */
        private Test test = new Test();

        @Data
        public static class Test {
            /**
             * 是否启用测试模式（使用模拟）
             */
            private boolean enabled = true;
        }
    }

    @Data
    public static class Notifications {
        /**
         * 是否启用通知
         */
        private boolean enabled = true;

        /**
         * 邮件通知配置
         */
        private Email email = new Email();

        /**
         * 短信通知配置
         */
        private Sms sms = new Sms();

        /**
         * 推送通知配置
         */
        private Push push = new Push();

        @Data
        public static class Email {
            private boolean enabled = true;
        }

        @Data
        public static class Sms {
            private boolean enabled = false;
        }

        @Data
        public static class Push {
            private boolean enabled = false;
        }
    }
}