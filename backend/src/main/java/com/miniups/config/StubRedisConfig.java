package com.miniups.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Stub Redis Configuration for Testing
 * 
 * Provides in-memory stub implementations of Redis beans when 
 * spring.data.redis.enabled=false. This allows tests to run without 
 * a Redis server while still supporting components that depend on Redis.
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class StubRedisConfig {

    private final ConcurrentHashMap<String, Object> mockStorage = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return new StubRedisTemplate(mockStorage);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return new StubStringRedisTemplate();
    }

    /**
     * Stub implementation of RedisTemplate for testing
     */
    private static class StubRedisTemplate extends RedisTemplate<String, Object> {
        
        private final ConcurrentHashMap<String, Object> storage;
        private final StubValueOperations valueOps;

        public StubRedisTemplate(ConcurrentHashMap<String, Object> storage) {
            this.storage = storage;
            this.valueOps = new StubValueOperations(storage);
            setKeySerializer(new StringRedisSerializer());
        }

        @Override
        public void afterPropertiesSet() {
            // Skip validation that requires RedisConnectionFactory
        }

        @Override
        public ValueOperations<String, Object> opsForValue() {
            return valueOps;
        }

        @Override
        public Boolean hasKey(String key) {
            return storage.containsKey(key);
        }

        @Override
        public Boolean delete(String key) {
            return storage.remove(key) != null;
        }

        @Override
        public Long delete(Collection<String> keys) {
            long count = 0;
            for (String key : keys) {
                if (storage.remove(key) != null) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Set<String> keys(String pattern) {
            String prefix = pattern.replace("*", "");
            return storage.keySet().stream()
                    .filter(key -> key.startsWith(prefix))
                    .collect(Collectors.toSet());
        }

        @Override
        public Boolean expire(String key, long timeout, TimeUnit unit) {
            return true; // Stub: always succeed
        }

        @Override
        public Boolean expire(String key, Duration timeout) {
            return true; // Stub: always succeed
        }
    }

    /**
     * Stub implementation of ValueOperations
     */
    private static class StubValueOperations implements ValueOperations<String, Object> {
        
        private final ConcurrentHashMap<String, Object> storage;

        public StubValueOperations(ConcurrentHashMap<String, Object> storage) {
            this.storage = storage;
        }

        @Override
        public void set(String key, Object value) {
            storage.put(key, value);
        }

        @Override
        public void set(String key, Object value, long timeout, TimeUnit unit) {
            storage.put(key, value);
        }

        @Override
        public Boolean setIfAbsent(String key, Object value) {
            return storage.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
            return storage.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfPresent(String key, Object value) {
            if (storage.containsKey(key)) {
                storage.put(key, value);
                return true;
            }
            return false;
        }

        @Override
        public Boolean setIfPresent(String key, Object value, long timeout, TimeUnit unit) {
            return setIfPresent(key, value);
        }

        @Override
        public Object get(Object key) {
            return storage.get(key);
        }

        @Override
        public Object getAndDelete(String key) {
            return storage.remove(key);
        }

        @Override
        public Object getAndExpire(String key, long timeout, TimeUnit unit) {
            return storage.get(key);
        }

        @Override
        public Object getAndExpire(String key, Duration timeout) {
            return storage.get(key);
        }

        @Override
        public Object getAndPersist(String key) {
            return storage.get(key);
        }

        @Override
        public Object getAndSet(String key, Object value) {
            return storage.put(key, value);
        }

        @Override
        public Long increment(String key) {
            Object val = storage.compute(key, (k, v) -> {
                if (v == null) return 1L;
                if (v instanceof Number) return ((Number) v).longValue() + 1;
                return 1L;
            });
            return ((Number) val).longValue();
        }

        @Override
        public Long increment(String key, long delta) {
            Object val = storage.compute(key, (k, v) -> {
                if (v == null) return delta;
                if (v instanceof Number) return ((Number) v).longValue() + delta;
                return delta;
            });
            return ((Number) val).longValue();
        }

        @Override
        public Double increment(String key, double delta) {
            Object val = storage.compute(key, (k, v) -> {
                if (v == null) return delta;
                if (v instanceof Number) return ((Number) v).doubleValue() + delta;
                return delta;
            });
            return ((Number) val).doubleValue();
        }

        @Override
        public Long decrement(String key) {
            return increment(key, -1);
        }

        @Override
        public Long decrement(String key, long delta) {
            return increment(key, -delta);
        }

        @Override
        public Integer append(String key, String value) {
            storage.compute(key, (k, v) -> {
                if (v == null) return value;
                return v.toString() + value;
            });
            return value.length();
        }

        @Override
        public String get(String key, long start, long end) {
            Object val = storage.get(key);
            if (val == null) return null;
            String str = val.toString();
            int actualEnd = (int) Math.min(end + 1, str.length());
            return str.substring((int) start, actualEnd);
        }

        @Override
        public void set(String key, Object value, long offset) {
            storage.put(key, value);
        }

        @Override
        public Long size(String key) {
            Object val = storage.get(key);
            return val == null ? 0L : (long) val.toString().length();
        }

        @Override
        public Boolean setBit(String key, long offset, boolean value) {
            return false;
        }

        @Override
        public Boolean getBit(String key, long offset) {
            return false;
        }

        @Override
        public java.util.List<Long> bitField(String key, org.springframework.data.redis.connection.BitFieldSubCommands subCommands) {
            return java.util.Collections.emptyList();
        }

        @Override
        public RedisOperations<String, Object> getOperations() {
            return null;
        }

        @Override
        public void multiSet(java.util.Map<? extends String, ?> map) {
            storage.putAll((java.util.Map<String, Object>) map);
        }

        @Override
        public Boolean multiSetIfAbsent(java.util.Map<? extends String, ?> map) {
            for (String key : map.keySet()) {
                if (storage.containsKey(key)) return false;
            }
            storage.putAll((java.util.Map<String, Object>) map);
            return true;
        }

        @Override
        public java.util.List<Object> multiGet(Collection<String> keys) {
            return keys.stream().map(storage::get).collect(Collectors.toList());
        }

        @Override
        public void set(String key, Object value, Duration timeout) {
            storage.put(key, value);
        }

        @Override
        public Boolean setIfAbsent(String key, Object value, Duration timeout) {
            return storage.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfPresent(String key, Object value, Duration timeout) {
            return setIfPresent(key, value);
        }
    }

    /**
     * Stub StringRedisTemplate
     */
    private static class StubStringRedisTemplate extends StringRedisTemplate {
        
        private final ConcurrentHashMap<String, String> storage = new ConcurrentHashMap<>();
        private final StubStringValueOperations valueOps = new StubStringValueOperations(storage);

        @Override
        public void afterPropertiesSet() {
            // Skip validation that requires RedisConnectionFactory
        }

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOps;
        }

        @Override
        public Boolean hasKey(String key) {
            return storage.containsKey(key);
        }
    }

    private static class StubStringValueOperations implements ValueOperations<String, String> {
        private final ConcurrentHashMap<String, String> storage;

        public StubStringValueOperations(ConcurrentHashMap<String, String> storage) {
            this.storage = storage;
        }

        @Override
        public void set(String key, String value) {
            storage.put(key, value);
        }

        @Override
        public void set(String key, String value, long timeout, TimeUnit unit) {
            storage.put(key, value);
        }

        @Override
        public Boolean setIfAbsent(String key, String value) {
            return storage.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
            return storage.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfPresent(String key, String value) {
            if (storage.containsKey(key)) {
                storage.put(key, value);
                return true;
            }
            return false;
        }

        @Override
        public Boolean setIfPresent(String key, String value, long timeout, TimeUnit unit) {
            return setIfPresent(key, value);
        }

        @Override
        public String get(Object key) {
            return storage.get(key);
        }

        @Override
        public String getAndDelete(String key) {
            return storage.remove(key);
        }

        @Override
        public String getAndExpire(String key, long timeout, TimeUnit unit) {
            return storage.get(key);
        }

        @Override
        public String getAndExpire(String key, Duration timeout) {
            return storage.get(key);
        }

        @Override
        public String getAndPersist(String key) {
            return storage.get(key);
        }

        @Override
        public String getAndSet(String key, String value) {
            return storage.put(key, value);
        }

        @Override
        public Long increment(String key) {
            return 1L;
        }

        @Override
        public Long increment(String key, long delta) {
            return delta;
        }

        @Override
        public Double increment(String key, double delta) {
            return delta;
        }

        @Override
        public Long decrement(String key) {
            return -1L;
        }

        @Override
        public Long decrement(String key, long delta) {
            return -delta;
        }

        @Override
        public Integer append(String key, String value) {
            return value.length();
        }

        @Override
        public String get(String key, long start, long end) {
            return storage.get(key);
        }

        @Override
        public void set(String key, String value, long offset) {
            storage.put(key, value);
        }

        @Override
        public Long size(String key) {
            String val = storage.get(key);
            return val == null ? 0L : (long) val.length();
        }

        @Override
        public Boolean setBit(String key, long offset, boolean value) {
            return false;
        }

        @Override
        public Boolean getBit(String key, long offset) {
            return false;
        }

        @Override
        public java.util.List<Long> bitField(String key, org.springframework.data.redis.connection.BitFieldSubCommands subCommands) {
            return java.util.Collections.emptyList();
        }

        @Override
        public RedisOperations<String, String> getOperations() {
            return null;
        }

        @Override
        public void multiSet(java.util.Map<? extends String, ? extends String> map) {
            storage.putAll((java.util.Map<String, String>) map);
        }

        @Override
        public Boolean multiSetIfAbsent(java.util.Map<? extends String, ? extends String> map) {
            return true;
        }

        @Override
        public java.util.List<String> multiGet(Collection<String> keys) {
            return keys.stream().map(storage::get).collect(Collectors.toList());
        }

        @Override
        public void set(String key, String value, Duration timeout) {
            storage.put(key, value);
        }

        @Override
        public Boolean setIfAbsent(String key, String value, Duration timeout) {
            return storage.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfPresent(String key, String value, Duration timeout) {
            return setIfPresent(key, value);
        }
    }
}
