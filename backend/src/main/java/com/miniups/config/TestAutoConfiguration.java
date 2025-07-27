package com.miniups.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 测试环境自动配置
 * 
 * 用于处理测试环境特有的配置需求，
 * 包括自动配置排除和测试特定的Bean配置
 */
@Configuration
@Profile("test")
@ConditionalOnProperty(name = "app.rabbitmq.test.enabled", havingValue = "true", matchIfMissing = true)
public class TestAutoConfiguration {
    
    // 这个类主要用于声明测试环境的自动配置
    // 实际的RabbitMQ模拟配置在TestRabbitConfig中处理
}