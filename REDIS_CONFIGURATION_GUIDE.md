# 🔴 Redis配置完整指南

## 📖 指南简介

本指南详细介绍了Mini-UPS项目中Redis缓存的配置、部署和管理方法，包括开发环境、Docker环境和生产环境的完整配置。

---

## 📋 目录

1. [Redis基础概念](#1-redis基础概念)
2. [环境配置](#2-环境配置)
3. [Spring Boot集成](#3-spring-boot集成)
4. [Docker配置](#4-docker配置)
5. [生产环境配置](#5-生产环境配置)
6. [性能优化](#6-性能优化)
7. [监控与维护](#7-监控与维护)
8. [故障排除](#8-故障排除)

---

## 1. Redis基础概念

### 1.1 什么是Redis？

Redis（Remote Dictionary Server）是一个开源的内存数据结构存储系统，用作：
- **缓存**：提高数据访问速度
- **会话存储**：管理用户登录状态
- **消息队列**：处理异步任务
- **实时数据**：存储临时数据

### 1.2 Mini-UPS中的Redis用途

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   JWT Token     │    │   Token         │    │   WebSocket     │
│   黑名单管理     │    │   状态缓存       │    │   会话存储       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                │
                    ┌─────────────────┐
                    │   Redis缓存     │
                    │   数据存储       │
                    └─────────────────┘
```

**当前实际应用场景**：
1. **JWT Token黑名单**：管理已注销的JWT token，防止token重用 (TokenBlacklistService.java:32)
2. **Token缓存与验证**：快速验证用户身份，避免频繁数据库查询
3. **WebSocket会话管理**：维护实时连接状态和消息传递
4. **分布式锁机制**：防止并发操作冲突
5. **临时数据存储**：存储有TTL的临时业务数据

---

## 2. 环境配置

### 2.1 本地开发环境

#### macOS安装
```bash
# 使用Homebrew安装
brew install redis

# 启动Redis服务（自定义端口6380）
redis-server --port 6380 --daemonize yes

# 配置开机自启（可选）
brew services start redis
```

#### Ubuntu/Debian安装
```bash
# 更新包列表
sudo apt update

# 安装Redis
sudo apt install redis-server

# 修改配置文件
sudo nano /etc/redis/redis.conf

# 找到并修改端口
port 6380

# 重启Redis服务
sudo systemctl restart redis-server
sudo systemctl enable redis-server
```

#### Windows安装
```powershell
# 使用Chocolatey安装
choco install redis-64

# 或下载Windows版本
# https://github.com/MicrosoftArchive/redis/releases

# 启动Redis（指定端口）
redis-server.exe --port 6380
```

#### 验证安装
```bash
# 测试连接（注意端口6380）
redis-cli -p 6380 ping
# 应该返回：PONG

# 查看Redis信息
redis-cli -p 6380 info server
```

### 2.2 Docker环境配置

#### 单独运行Redis容器
```bash
# 运行Redis容器（端口映射6380:6379）
docker run -d \
  --name mini-ups-redis \
  --restart unless-stopped \
  -p 6380:6379 \
  redis:latest

# 测试连接
docker exec -it mini-ups-redis redis-cli ping
```

#### 自定义Redis配置
创建自定义配置文件：
```bash
# 创建配置目录
mkdir -p ./redis/config

# 创建Redis配置文件
cat > ./redis/config/redis.conf << 'EOF'
# 基础配置
port 6379
bind 0.0.0.0
protected-mode yes

# 内存配置
maxmemory 256mb
maxmemory-policy allkeys-lru

# 持久化配置
save 900 1
save 300 10
save 60 10000

# 日志配置
loglevel notice
logfile /var/log/redis/redis-server.log

# 安全配置
# requirepass yourpassword

# 客户端连接
timeout 300
tcp-keepalive 300
EOF
```

#### 使用自定义配置的Docker Compose
```yaml
# docker-compose.yml 中的Redis配置
redis:
  image: redis:7-alpine
  container_name: mini-ups-redis
  command: redis-server /usr/local/etc/redis/redis.conf
  ports:
    - "6380:6379"
  volumes:
    - ./redis/config/redis.conf:/usr/local/etc/redis/redis.conf:ro
    - redis-data:/data
    - ./logs/redis:/var/log/redis
  networks:
    - projectnet
  restart: unless-stopped
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 30s
    timeout: 3s
    retries: 3

volumes:
  redis-data:
```

---

## 3. Spring Boot集成

### 3.1 依赖配置

#### Maven依赖（已包含在pom.xml中）
```xml
<!-- Redis支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- JSON序列化 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### 3.2 配置文件详解

#### application.yml（主配置）
```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}    # Redis主机地址，支持环境变量
    port: ${REDIS_PORT:6380}         # Redis端口，默认6380
    database: ${REDIS_DATABASE:0}    # 数据库索引，默认0
    timeout: ${REDIS_TIMEOUT:2000ms} # 连接超时时间
    lettuce:                         # Lettuce连接池配置
      pool:
        max-active: 10               # 最大连接数
        max-idle: 8                  # 最大空闲连接
        min-idle: 0                  # 最小空闲连接

  data:
    redis:
      repositories:
        enabled: false               # 禁用Redis Repository
```

#### application-local.yml（本地开发）
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6380
      password: ${REDIS_PASSWORD:}
      timeout: 60000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

#### application-docker.yml（Docker环境）
```yaml
spring:
  redis:
    host: ${REDIS_HOST:redis}
    port: ${REDIS_PORT:6380}
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 10
        max-idle: 8
        min-idle: 0
```

#### application-prod.yml（生产环境）
```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6380}
    password: ${REDIS_PASSWORD}    # 生产环境必须设置密码
    database: 0
    timeout: 3000ms
    ssl: ${REDIS_SSL:false}
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 3000ms
      shutdown-timeout: 200ms
```

### 3.3 当前Redis配置实现

#### RedisConfig.java（实际代码）
```java
/**
 * Redis Configuration Class
 * 
 * 实际功能说明：
 * - 配置简化的Redis连接和序列化
 * - 设置RedisTemplate和StringRedisTemplate
 * - 使用GenericJackson2JsonRedisSerializer进行序列化
 * - 针对JWT token管理和缓存优化
 */
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // String serializer for keys - 可读性好的键名
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // JSON serializer for values - 支持复杂对象
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
```

#### TokenBlacklistService.java（实际使用案例）
```java
@Service
public class TokenBlacklistService {
    
    private static final String BLACKLIST_PREFIX = "blacklisted_token:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 将token添加到黑名单
     * 使用TTL自动清理过期token
     */
    public void blacklistToken(String tokenId, long expirationTime) {
        String key = BLACKLIST_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, "blacklisted", expirationTime, TimeUnit.SECONDS);
    }
    
    /**
     * 检查token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String tokenId) {
        String key = BLACKLIST_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

---

## 4. Docker配置

### 4.1 完整的Docker Compose配置

#### docker-compose.yml（当前实际配置）
```yaml
version: '3.8'

services:
  # PostgreSQL数据库
  database:
    image: postgres:15
    container_name: mini-ups-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB?POSTGRES_DB is not set}
      POSTGRES_USER: ${POSTGRES_USER?POSTGRES_USER is not set}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD?POSTGRES_PASSWORD is not set}
      TZ: UTC
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./database/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres}"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # Redis缓存
  redis:
    image: redis:7-alpine
    container_name: mini-ups-redis
    command: redis-server --requirepass ${REDIS_PASSWORD?REDIS_PASSWORD is not set}
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD?REDIS_PASSWORD is not set}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # Spring Boot后端
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: mini-ups-backend
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-production}
      DATABASE_URL: jdbc:postgresql://database:5432/${POSTGRES_DB?POSTGRES_DB is not set}
      DATABASE_USERNAME: ${POSTGRES_USER?POSTGRES_USER is not set}
      DATABASE_PASSWORD: ${POSTGRES_PASSWORD?POSTGRES_PASSWORD is not set}
      REDIS_HOST: redis
      REDIS_PORT: 6379                    # 容器内部端口
      REDIS_PASSWORD: ${REDIS_PASSWORD?REDIS_PASSWORD is not set}
      JWT_SECRET: ${JWT_SECRET?JWT_SECRET is not set}
      WORLD_SIMULATOR_HOST: ${WORLD_SIMULATOR_HOST:-localhost}
      AMAZON_API_URL: ${AMAZON_API_URL:-http://localhost:8080}
      NUM_TRUCKS: ${NUM_TRUCKS:-5}
      WORLD_ID: ${WORLD_ID:-}
    ports:
      - "8081:8081"
    depends_on:
      database:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/api/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped
    volumes:
      - ./logs:/app/logs

volumes:
  postgres_data:

networks:
  default:
    name: mini-ups-network
```

#### 关键差异说明
1. **密码认证**：当前配置使用环境变量设置Redis密码
2. **端口映射**：Redis容器内使用6379端口，外部可配置
3. **网络配置**：使用mini-ups-network网络
4. **环境变量**：所有敏感配置通过环境变量传递

### 4.2 Redis持久化配置

#### 创建Redis配置文件
```bash
# 创建Redis配置目录
mkdir -p ./redis/config

# 创建生产级Redis配置
cat > ./redis/config/redis.conf << 'EOF'
# ============ 基础配置 ============
port 6379
bind 0.0.0.0
protected-mode yes
tcp-backlog 511
timeout 300
tcp-keepalive 300

# ============ 内存配置 ============
maxmemory 256mb
maxmemory-policy allkeys-lru
maxmemory-samples 5

# ============ 持久化配置 ============
# RDB持久化
save 900 1      # 900秒内至少1个key变化
save 300 10     # 300秒内至少10个key变化  
save 60 10000   # 60秒内至少10000个key变化

stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir /data

# AOF持久化
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# ============ 日志配置 ============
loglevel notice
logfile /var/log/redis/redis-server.log
syslog-enabled no

# ============ 客户端配置 ============
maxclients 10000
timeout 300

# ============ 安全配置 ============
# requirepass yourpassword

# ============ 慢查询配置 ============
slowlog-log-slower-than 10000
slowlog-max-len 128

# ============ 其他配置 ============
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-size -2
list-compress-depth 0
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
EOF
```

---

## 5. 生产环境配置

### 5.1 安全配置

#### 设置Redis密码
```bash
# 在redis.conf中添加
requirepass your_very_strong_password_here

# 或通过环境变量
export REDIS_PASSWORD=your_very_strong_password_here
```

#### 网络安全
```bash
# 绑定特定IP（redis.conf）
bind 127.0.0.1 192.168.1.100

# 禁用危险命令
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command KEYS ""
rename-command CONFIG "CONFIG_9a8b7c6d"
```

### 5.2 性能优化配置

#### 内存优化
```conf
# redis.conf 性能配置
maxmemory 1gb
maxmemory-policy allkeys-lru
maxmemory-samples 10

# 禁用Swap
vm-enabled no

# 优化网络
tcp-keepalive 300
timeout 300
tcp-backlog 2048
```

#### JVM优化（Spring Boot）
```yaml
# application-prod.yml
spring:
  redis:
    lettuce:
      pool:
        max-active: 50      # 根据并发量调整
        max-idle: 20
        min-idle: 10
        max-wait: 3000ms
      shutdown-timeout: 200ms
    timeout: 3000ms
```

### 5.3 监控配置

#### Redis监控命令
```bash
# 查看Redis信息
redis-cli -p 6380 info

# 监控实时命令
redis-cli -p 6380 monitor

# 查看慢查询
redis-cli -p 6380 slowlog get 10

# 查看客户端连接
redis-cli -p 6380 client list
```

#### 健康检查脚本
```bash
#!/bin/bash
# redis-health-check.sh

REDIS_HOST="localhost"
REDIS_PORT="6380"
REDIS_PASSWORD=""

# 检查Redis是否运行
if redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD ping | grep -q "PONG"; then
    echo "✅ Redis is running"
    
    # 检查内存使用
    MEMORY_USAGE=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD info memory | grep used_memory_human | cut -d: -f2 | tr -d '\r')
    echo "📊 Memory usage: $MEMORY_USAGE"
    
    # 检查连接数
    CONNECTED_CLIENTS=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD info clients | grep connected_clients | cut -d: -f2 | tr -d '\r')
    echo "🔗 Connected clients: $CONNECTED_CLIENTS"
    
else
    echo "❌ Redis is not responding"
    exit 1
fi
```

---

## 6. 性能优化

### 6.1 当前实际缓存策略

#### JWT Token黑名单服务（实际实现）
```java
/**
 * JWT Token黑名单服务
 * 当前Mini-UPS系统中的实际Redis使用案例
 */
@Service
public class TokenBlacklistService {
    
    private static final String BLACKLIST_PREFIX = "blacklisted_token:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 将token添加到黑名单
     * 核心功能：防止已登出的token被重复使用
     */
    public void blacklistToken(String tokenId, long expirationTime) {
        try {
            String key = BLACKLIST_PREFIX + tokenId;
            
            // 设置过期时间，自动删除过期的token
            redisTemplate.opsForValue().set(key, "blacklisted", expirationTime, TimeUnit.SECONDS);
            
            logger.info("Token added to blacklist: tokenId={}", tokenId);
            
        } catch (Exception e) {
            logger.error("Failed to add token to blacklist: tokenId={}", tokenId, e);
            // 不抛出异常，避免影响退出登录流程
        }
    }
    
    /**
     * 检查token是否在黑名单中
     * 高频调用方法，优化了异常处理
     */
    public boolean isTokenBlacklisted(String tokenId) {
        try {
            String key = BLACKLIST_PREFIX + tokenId;
            Boolean exists = redisTemplate.hasKey(key);
            
            return Boolean.TRUE.equals(exists);
            
        } catch (Exception e) {
            logger.error("Failed to check token blacklist status: tokenId={}", tokenId, e);
            // 安全考虑：出错时认为token有效
            return false;
        }
    }
    
    /**
     * 获取黑名单token数量（监控用）
     */
    public long getBlacklistedTokenCount() {
        try {
            String pattern = BLACKLIST_PREFIX + "*";
            return redisTemplate.keys(pattern).size();
            
        } catch (Exception e) {
            logger.error("Failed to get blacklisted token count", e);
            return 0;
        }
    }
}
```

#### Redis键命名规范
当前系统使用的键命名模式：
```
blacklisted_token:{tokenId}  - JWT黑名单token
session:{sessionId}          - 用户会话数据  
websocket:{connectionId}     - WebSocket连接状态
cache:{entity}:{id}          - 实体数据缓存
temp:{operation}:{id}        - 临时操作数据
```

### 6.2 连接池优化

#### Lettuce连接池配置
```yaml
spring:
  redis:
    lettuce:
      pool:
        # 最大连接数（根据应用并发量调整）
        max-active: 20
        # 最大空闲连接数
        max-idle: 10
        # 最小空闲连接数
        min-idle: 5
        # 获取连接最大等待时间
        max-wait: 3000ms
      # 关闭超时时间
      shutdown-timeout: 200ms
    # 连接超时时间
    timeout: 3000ms
```

---

## 7. 监控与维护

### 7.1 Redis监控指标

#### 关键性能指标
```bash
# 内存使用情况
redis-cli -p 6380 info memory

# 统计信息
redis-cli -p 6380 info stats

# 客户端连接
redis-cli -p 6380 info clients

# 持久化状态
redis-cli -p 6380 info persistence
```

#### 监控脚本
```bash
#!/bin/bash
# redis-monitor.sh

REDIS_HOST="localhost"
REDIS_PORT="6380"
LOG_FILE="/var/log/redis-monitor.log"

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # 获取内存使用
    MEMORY_USED=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT info memory | grep used_memory_human | cut -d: -f2 | tr -d '\r')
    
    # 获取连接数
    CONNECTIONS=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT info clients | grep connected_clients | cut -d: -f2 | tr -d '\r')
    
    # 获取命令处理统计
    COMMANDS=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT info stats | grep total_commands_processed | cut -d: -f2 | tr -d '\r')
    
    echo "$TIMESTAMP - Memory: $MEMORY_USED, Connections: $CONNECTIONS, Commands: $COMMANDS" >> $LOG_FILE
    
    sleep 60
done
```

### 7.2 备份与恢复

#### 自动备份脚本
```bash
#!/bin/bash
# redis-backup.sh

REDIS_HOST="localhost"
REDIS_PORT="6380"
BACKUP_DIR="/backup/redis"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行BGSAVE
redis-cli -h $REDIS_HOST -p $REDIS_PORT bgsave

# 等待备份完成
while [ $(redis-cli -h $REDIS_HOST -p $REDIS_PORT lastsave) -eq $(redis-cli -h $REDIS_HOST -p $REDIS_PORT lastsave) ]; do
    sleep 1
done

# 复制RDB文件
cp /var/lib/redis/dump.rdb $BACKUP_DIR/dump_$DATE.rdb

# 清理旧备份（保留7天）
find $BACKUP_DIR -name "dump_*.rdb" -mtime +7 -delete

echo "Redis backup completed: dump_$DATE.rdb"
```

#### 恢复数据
```bash
# 停止Redis服务
sudo systemctl stop redis

# 恢复RDB文件
cp /backup/redis/dump_20240101_120000.rdb /var/lib/redis/dump.rdb

# 设置正确的权限
chown redis:redis /var/lib/redis/dump.rdb

# 启动Redis服务
sudo systemctl start redis
```

---

## 8. 故障排除

### 8.1 常见问题

#### 连接问题
```bash
# 问题：连接被拒绝
# 解决方案：
1. 检查Redis是否运行
   systemctl status redis

2. 检查端口是否正确
   netstat -tlnp | grep 6380

3. 检查防火墙设置
   sudo ufw status
   sudo ufw allow 6380

4. 检查bind配置
   grep bind /etc/redis/redis.conf
```

#### 内存问题
```bash
# 问题：内存不足
# 解决方案：
1. 检查内存使用
   redis-cli -p 6380 info memory

2. 设置内存限制和清理策略
   redis-cli -p 6380 config set maxmemory 1gb
   redis-cli -p 6380 config set maxmemory-policy allkeys-lru

3. 清理过期数据
   redis-cli -p 6380 eval "return redis.call('del', unpack(redis.call('keys', ARGV[1])))" 0 "expired:*"
```

#### 性能问题
```bash
# 问题：响应慢
# 解决方案：
1. 检查慢查询
   redis-cli -p 6380 slowlog get 10

2. 检查网络延迟
   redis-cli -p 6380 --latency

3. 优化数据结构
   redis-cli -p 6380 memory usage keyname
```

### 8.2 日志分析

#### Redis日志配置
```conf
# redis.conf
loglevel notice
logfile /var/log/redis/redis-server.log
syslog-enabled yes
syslog-ident redis
```

#### 日志分析脚本
```bash
#!/bin/bash
# redis-log-analyzer.sh

LOG_FILE="/var/log/redis/redis-server.log"

echo "=== Redis日志分析 ==="
echo "最近的错误："
grep -i error $LOG_FILE | tail -10

echo -e "\n最近的警告："
grep -i warning $LOG_FILE | tail -10

echo -e "\n连接统计："
grep -c "Accepted" $LOG_FILE

echo -e "\n内存警告："
grep -i "memory" $LOG_FILE | tail -5
```

### 8.3 应急处理

#### Redis崩溃恢复
```bash
# 1. 检查Redis状态
systemctl status redis

# 2. 查看错误日志
tail -50 /var/log/redis/redis-server.log

# 3. 尝试重启
systemctl restart redis

# 4. 如果无法启动，检查配置文件
redis-server /etc/redis/redis.conf --test-config

# 5. 数据恢复
# 从最近的备份恢复数据
```

#### 应急配置
```yaml
# application-emergency.yml
spring:
  redis:
    # 禁用Redis（紧急情况下）
    enabled: false
  cache:
    type: simple  # 使用内存缓存替代
```

---

## 🔧 配置检查清单

### 开发环境检查
- [ ] Redis端口设置为6380（本地开发）
- [ ] 本地Redis服务正常启动
- [ ] Spring Boot能够连接Redis（检查 `localhost:6380`）
- [ ] JWT Token黑名单功能正常工作
- [ ] RedisTemplate和StringRedisTemplate配置正确

### Docker环境检查  
- [ ] Docker Compose配置正确（使用环境变量）
- [ ] Redis密码设置（通过 `REDIS_PASSWORD` 环境变量）
- [ ] 健康检查通过（带密码认证）
- [ ] 容器间网络连通（mini-ups-network）
- [ ] 后端服务能连接Redis容器

### 生产环境检查
- [ ] 设置强密码（`REDIS_PASSWORD` 环境变量）
- [ ] 配置内存限制和清理策略
- [ ] 启用持久化（RDB + AOF）
- [ ] 设置监控告警
- [ ] 配置备份策略
- [ ] JWT Token黑名单监控

---

## 📚 参考资源

### 官方文档
- [Redis官方文档](https://redis.io/docs/)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Lettuce Redis客户端](https://lettuce.io/)

### 配置示例
- [Redis配置生成器](https://download.redis.io/redis-stable/redis.conf)
- [Spring Boot Redis配置](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.nosql.redis)

### 监控工具
- [RedisInsight](https://redis.com/redis-enterprise/redis-insight/) - Redis GUI工具
- [Redis-stat](https://github.com/junegunn/redis-stat) - 命令行监控工具

---

## 🎯 总结

本指南基于Mini-UPS项目的实际Redis架构进行了全面更新。当前Redis实现的特点：

### 核心功能
1. **JWT Token黑名单管理**：防止已注销token重用 (TokenBlacklistService.java:32)
2. **简化配置**：使用GenericJackson2JsonRedisSerializer进行序列化
3. **环境变量驱动**：通过Docker环境变量管理配置
4. **健康检查**：Docker容器级别的Redis监控

### 架构优势
- **安全性**：支持密码认证和token黑名单
- **可维护性**：清晰的键命名规范和错误处理
- **可扩展性**：基于RedisTemplate的灵活操作
- **容器化**：完整的Docker集成

### 实际应用场景
- JWT认证安全增强
- WebSocket会话管理（准备就绪）
- 分布式缓存支持（架构就绪）
- 临时数据存储（TTL自动清理）

通过这个配置指南，开发者可以快速理解和维护Mini-UPS系统中的Redis组件。