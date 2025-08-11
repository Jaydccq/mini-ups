/**
 * Redis Configuration Class
 * 
 * Purpose:
 * - Configures Redis connectivity and serialization for the Mini-UPS application
 * - Provides optimized Redis templates for different data types and use cases
 * - Establishes caching strategy and session management through Redis
 * - Enables high-performance data storage and retrieval for real-time operations
 * 
 * Core Functionality:
 * - redisTemplate(): General-purpose Redis template with JSON serialization
 * - stringRedisTemplate(): Specialized template for string operations
 * - Custom serialization configuration for optimal performance
 * - Connection factory integration with Spring Boot auto-configuration
 * 
 * Serialization Strategy:
 * - Keys: StringRedisSerializer for human-readable keys and efficient storage
 * - Values: GenericJackson2JsonRedisSerializer for complex object serialization
 * - Hash Keys: StringRedisSerializer for consistent hash field naming
 * - Hash Values: GenericJackson2JsonRedisSerializer for structured data storage
 * 
 * Redis Template Types:
 * 1. RedisTemplate<String, Object>:
 *    - General-purpose template for complex objects
 *    - JSON serialization for easy debugging and cross-platform compatibility
 *    - Supports all Redis data structures (strings, hashes, lists, sets, sorted sets)
 * 
 * 2. StringRedisTemplate:
 *    - Optimized for string-only operations
 *    - Faster serialization/deserialization for simple data
 *    - Ideal for caching simple values, counters, and flags
 * 
 * Use Cases in Mini-UPS:
 * - Session storage for user authentication and WebSocket connections
 * - Caching frequently accessed data (user profiles, truck locations)
 * - Real-time truck location updates and tracking information
 * - Message queuing for asynchronous processing
 * - Rate limiting and throttling for API endpoints
 * 
 * Performance Features:
 * - Connection pooling through RedisConnectionFactory
 * - Efficient serialization reduces network overhead
 * - Pipelining support for batch operations
 * - Transaction support for atomic operations
 * - Pub/Sub messaging for real-time notifications
 * 
 * Integration Points:
 * - Works with Spring Cache abstraction for method-level caching
 * - Integrates with Spring Session for distributed session management
 * - Supports WebSocket session storage for real-time features
 * - Compatible with Spring Boot's Redis auto-configuration
 * 
 * Caching Strategies:
 * - Write-through caching for critical data consistency
 * - Cache-aside pattern for flexible data management
 * - Time-based expiration for temporary data
 * - LRU eviction policies for memory management
 * 
 * Data Structure Usage:
 * - Strings: Simple key-value pairs, counters, flags
 * - Hashes: User sessions, object properties, nested data
 * - Lists: Message queues, activity logs, recent items
 * - Sets: Unique collections, user preferences, tags
 * - Sorted Sets: Leaderboards, time-series data, rankings
 * 
 * Security Considerations:
 * - Redis connection authentication through connection factory
 * - Data serialization security with trusted class configuration
 * - Network encryption support for production environments
 * - Access control through Redis ACL integration
 * 
 * Monitoring and Debugging:
 * - Human-readable keys for easy Redis CLI debugging
 * - JSON serialization allows direct data inspection
 * - Connection metrics through Spring Boot Actuator
 * - Performance monitoring through Redis INFO commands
 * 
 * Configuration Dependencies:
 * - Redis server connection settings in application.properties
 * - Connection pool configuration for optimal performance
 * - Serialization library dependencies (Jackson)
 * - Optional Redis Cluster or Sentinel configuration
 * 
 * Best Practices:
 * - Use appropriate template type based on data complexity
 * - Implement proper key naming conventions for organization
 * - Set expiration times for temporary data to prevent memory leaks
 * - Monitor Redis memory usage and implement eviction policies
 * - Use pipeline operations for bulk data operations
 * 
 *
 
 * @since 2024-01-01
 */
package com.miniups.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // String serializer for keys
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // JSON serializer for values
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer =
            new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}