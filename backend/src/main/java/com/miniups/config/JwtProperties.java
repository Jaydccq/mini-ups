package com.miniups.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 * 
 * 定义JWT令牌相关的配置参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT密钥
     */
    private String secret;

    /**
     * 令牌过期时间（毫秒）
     */
    private long expiration = 86400000L; // 24小时
}