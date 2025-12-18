package com.miniups.shortlink.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "shortlink.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ShortLinkRedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        String schema = "redis://"; // Simplified for now
        String address = schema + redisProperties.getHost() + ":" + redisProperties.getPort();
        var singleServer = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase());
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            singleServer.setPassword(redisProperties.getPassword());
        }
        return Redisson.create(config);
    }
}
