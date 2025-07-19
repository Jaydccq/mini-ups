# Spring Boot 企业级开发完整教程

> 基于 Mini-UPS 物流系统项目的 Spring Boot 实战教程
> 
> 适合 Spring Boot 新手，结合真实企业级项目学习现代 Java 开发

## 📚 教程概览

本教程分为四个阶段，每阶段循序渐进：

1. **第一阶段：Spring Boot 基础概念与核心特性**
2. **第二阶段：分层架构设计与最佳实践**  
3. **第三阶段：数据库集成与 JPA 高级用法**
4. **第四阶段：安全认证与企业级特性**

---

## 🎯 第一阶段：Spring Boot 基础概念与核心特性

### 1.1 什么是 Spring Boot？

Spring Boot 是 Spring 框架的简化版本，它通过"约定优于配置"的理念，让 Java 企业级开发变得简单高效。

**核心优势：**
- 🚀 **零配置启动**：自动配置，开箱即用
- 📦 **起步依赖**：一站式依赖管理
- 🛠️ **内嵌服务器**：无需外部 Tomcat
- 📊 **生产就绪**：内置监控、健康检查

### 1.2 项目结构解析

让我们看看 Mini-UPS 项目的标准 Spring Boot 结构：

```
backend/
├── src/main/java/com/miniups/
│   ├── MiniUpsApplication.java          # 🚀 启动类
│   ├── config/                          # ⚙️ 配置类
│   │   ├── SecurityConfig.java          # 安全配置
│   │   ├── JpaConfig.java              # 数据库配置
│   │   └── RedisConfig.java            # 缓存配置
│   ├── controller/                      # 🌐 控制器层
│   │   ├── AuthController.java          # 认证API
│   │   ├── UserController.java          # 用户管理API
│   │   └── ShipmentController.java      # 订单API
│   ├── service/                         # 💼 业务逻辑层
│   │   ├── UserService.java            # 用户服务
│   │   ├── ShipmentService.java        # 订单服务
│   │   └── AuthService.java            # 认证服务
│   ├── repository/                      # 🗄️ 数据访问层
│   │   ├── UserRepository.java          # 用户数据访问
│   │   └── ShipmentRepository.java      # 订单数据访问
│   ├── model/                          # 📊 数据模型
│   │   ├── entity/                     # JPA实体
│   │   │   ├── User.java               # 用户实体
│   │   │   └── Shipment.java           # 订单实体
│   │   ├── dto/                        # 数据传输对象
│   │   │   ├── UserDto.java            # 用户DTO
│   │   │   └── ShipmentDto.java        # 订单DTO
│   │   └── enums/                      # 枚举类型
│   │       ├── UserRole.java           # 用户角色
│   │       └── ShipmentStatus.java     # 订单状态
│   └── exception/                      # ❌ 异常处理
│       ├── GlobalExceptionHandler.java # 全局异常处理
│       ├── UserNotFoundException.java  # 自定义异常
│       └── BusinessException.java      # 业务异常
└── src/main/resources/
    ├── application.yml                 # 主配置文件
    ├── application-local.yml           # 本地环境配置
    ├── application-docker.yml          # Docker环境配置
    └── static/                         # 静态资源
```

### 1.3 Spring Boot 启动类详解

**MiniUpsApplication.java** - 应用程序入口点：

```java
/**
 * Mini-UPS 应用程序主启动类
 * 
 * 核心功能：
 * - 自动配置 Spring Boot 应用程序
 * - 启用组件扫描和自动装配
 * - 激活企业级功能（异步处理、定时任务、JPA审计）
 * 
 * 注解说明：
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * @EnableAsync: 启用 @Async 异步方法支持
 * @EnableScheduling: 启用 @Scheduled 定时任务支持  
 * @EnableJpaAuditing: 启用 JPA 实体审计功能（自动设置创建时间、修改时间）
 */
@SpringBootApplication      // Spring Boot 核心注解
@EnableAsync               // 启用异步处理能力
@EnableScheduling          // 启用定时任务功能
@EnableJpaAuditing        // 启用 JPA 审计功能
public class MiniUpsApplication {
    
    /**
     * 应用程序入口点
     * 
     * Spring Boot 会自动：
     * 1. 扫描当前包及子包中的所有组件
     * 2. 根据类路径中的依赖进行自动配置
     * 3. 启动内嵌的 Tomcat 服务器
     * 4. 初始化 Spring 应用上下文
     */
    public static void main(String[] args) {
        SpringApplication.run(MiniUpsApplication.class, args);
    }
}
```

**关键理解点：**

1. **@SpringBootApplication** 是一个组合注解：
   - `@Configuration`：标记为配置类
   - `@EnableAutoConfiguration`：启用自动配置
   - `@ComponentScan`：启用组件扫描

2. **自动配置机制**：
   - Spring Boot 会根据类路径中的 jar 包自动配置应用
   - 比如检测到 `spring-boot-starter-web`，就自动配置 Web MVC
   - 检测到 `spring-boot-starter-data-jpa`，就自动配置 JPA

3. **内嵌服务器**：
   - 无需单独安装 Tomcat，应用自带 Web 服务器
   - 打包成 jar 文件就可以直接运行

### 1.4 配置文件系统

Spring Boot 使用 **application.yml**（或 .properties）进行配置：

```yaml
# application.yml - 主配置文件
server:
  port: 8081                            # 服务器端口
  servlet:
    context-path: /api                  # API前缀路径

spring:
  # 环境配置
  profiles:
    active: local                       # 激活本地环境配置
    
  # 数据源配置
  datasource:
    url: jdbc:postgresql://localhost:5432/ups_db
    username: postgres
    password: abc123
    driver-class-name: org.postgresql.Driver
    
    # HikariCP 连接池优化配置
    hikari:
      maximum-pool-size: 20             # 最大连接数
      minimum-idle: 5                   # 最小空闲连接
      idle-timeout: 300000              # 空闲超时时间
      connection-timeout: 20000         # 连接超时时间
      
  # JPA/Hibernate 配置
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update                  # 自动更新数据库结构
    show-sql: true                      # 显示 SQL 语句
    properties:
      hibernate:
        format_sql: true                # 格式化 SQL 输出
        use_sql_comments: true          # 显示 SQL 注释
        
  # Redis 缓存配置
  redis:
    host: localhost
    port: 6380
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8                   # 最大连接数
        max-idle: 8                     # 最大空闲连接
        min-idle: 0                     # 最小空闲连接

# JWT 认证配置
jwt:
  secret: YourVeryLongSecretKeyThatShouldBeAtLeast256BitsLong
  expiration: 86400000                  # 24小时过期时间

# 日志配置
logging:
  level:
    com.miniups: DEBUG                  # 应用程序日志级别
    org.springframework.security: DEBUG # Spring Security 日志
    org.hibernate.SQL: DEBUG           # Hibernate SQL 日志
  file:
    name: logs/mini-ups.log            # 日志文件路径
    
# 应用程序自定义配置
app:
  name: Mini-UPS System
  version: 1.0.0
  description: Enterprise Logistics Management System
```

### 1.5 多环境配置管理

Spring Boot 支持基于 Profile 的多环境配置：

**application-local.yml** - 本地开发环境：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ups_db
  jpa:
    show-sql: true                      # 开发环境显示SQL
    hibernate:
      ddl-auto: update                  # 自动更新表结构
logging:
  level:
    root: INFO
    com.miniups: DEBUG                  # 详细调试日志
```

**application-docker.yml** - Docker 容器环境：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://upsdb:5432/ups_db  # 容器内主机名
  redis:
    host: redis                         # Redis 容器名
logging:
  level:
    root: WARN
    com.miniups: INFO                   # 生产级别日志
```

**application-prod.yml** - 生产环境：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${RDS_ENDPOINT}:5432/ups_db
  jpa:
    show-sql: false                     # 生产环境不显示SQL
    hibernate:
      ddl-auto: validate                # 验证表结构，不自动修改
logging:
  level:
    root: ERROR
    com.miniups: WARN                   # 只记录警告和错误
```

**使用方式：**
```bash
# 本地开发
java -jar app.jar --spring.profiles.active=local

# Docker部署
java -jar app.jar --spring.profiles.active=docker

# 生产环境
java -jar app.jar --spring.profiles.active=prod
```

### 1.6 Spring Boot 自动配置原理

**自动配置的魔法：**

当你添加 `spring-boot-starter-web` 依赖时，Spring Boot 自动：

1. **检测类路径**：发现 Web 相关的类
2. **条件判断**：使用 `@ConditionalOnClass` 等注解判断是否需要配置
3. **自动装配**：创建必要的 Bean（如 DispatcherServlet、ViewResolver）
4. **默认配置**：提供合理的默认值，你也可以覆盖

```java
// Spring Boot 内部的自动配置示例
@Configuration
@ConditionalOnClass(DispatcherServlet.class)  // 只有类路径中有这个类才生效
@ConditionalOnWebApplication                   // 只在Web应用中生效
public class WebMvcAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean                  // 只有用户没有定义时才创建
    public ViewResolver viewResolver() {
        // 创建默认的视图解析器
        return new InternalResourceViewResolver();
    }
}
```

### 1.7 起步依赖（Starter Dependencies）

**什么是 Starter？**

Starter 是一组便捷的依赖描述符，包含了构建特定功能所需的所有依赖。

**Mini-UPS 项目使用的主要 Starter：**

```xml
<!-- pom.xml 依赖配置 -->
<dependencies>
    <!-- Web 开发 Starter：包含 Spring MVC、Tomcat、Jackson 等 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- 数据访问 Starter：包含 JPA、Hibernate、数据库驱动等 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- 安全认证 Starter：包含 Spring Security 完整功能 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Redis 缓存 Starter：包含 Redis 客户端和配置 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- 数据验证 Starter：包含 Bean Validation 功能 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- PostgreSQL 数据库驱动 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- JWT 支持 -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
</dependencies>
```

**Starter 的价值：**
- 🎯 **依赖管理简化**：不需要记忆复杂的依赖组合
- 🔧 **版本兼容性**：Spring Boot 确保所有依赖版本兼容
- 🚀 **快速启动**：添加一个依赖就能获得完整功能

### 1.8 Spring Boot 核心注解

让我们通过 Mini-UPS 项目了解重要注解：

#### 1.8.1 组件注解

```java
// @Component - 通用组件标记
@Component
public class TruckLocationTracker {
    // GPS位置追踪组件
}

// @Service - 业务逻辑层组件
@Service
@Transactional  // 事务管理
public class UserService {
    // 用户业务逻辑
}

// @Repository - 数据访问层组件
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 数据访问接口
}

// @Controller - Web控制器
@RestController  // @Controller + @ResponseBody
@RequestMapping("/api/users")
public class UserController {
    // REST API 控制器
}
```

#### 1.8.2 配置注解

```java
// @Configuration - 配置类标记
@Configuration
@EnableWebSecurity  // 启用Web安全
public class SecurityConfig {
    
    // @Bean - 定义Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // @Value - 注入配置值
    @Value("${jwt.secret}")
    private String jwtSecret;
}
```

#### 1.8.3 依赖注入注解

```java
@Service
public class ShipmentService {
    
    // 构造器注入（推荐方式）
    private final ShipmentRepository shipmentRepository;
    private final TruckService truckService;
    
    public ShipmentService(ShipmentRepository shipmentRepository, 
                          TruckService truckService) {
        this.shipmentRepository = shipmentRepository;
        this.truckService = truckService;
    }
    
    // @Autowired - 字段注入（不推荐）
    @Autowired
    private NotificationService notificationService;
    
    // @Qualifier - 指定具体Bean
    @Autowired
    @Qualifier("emailNotificationService")
    private NotificationService emailService;
}
```

### 1.9 实际运行项目

让我们启动 Mini-UPS 项目体验 Spring Boot：

**方式一：IDE 中运行**
```java
// 直接运行 MiniUpsApplication.main() 方法
public static void main(String[] args) {
    SpringApplication.run(MiniUpsApplication.class, args);
}
```

**方式二：Maven 命令**
```bash
cd backend
./mvnw spring-boot:run
```

**方式三：打包运行**
```bash
./mvnw clean package
java -jar target/mini-ups-backend-1.0.0.jar
```

**启动日志解读：**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.2.0)

2024-01-15 10:30:15.123  INFO 12345 --- [main] c.m.MiniUpsApplication: Starting MiniUpsApplication
2024-01-15 10:30:15.125  INFO 12345 --- [main] c.m.MiniUpsApplication: The following profiles are active: local
2024-01-15 10:30:16.234  INFO 12345 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer: Tomcat started on port(s): 8081 (http)
2024-01-15 10:30:16.245  INFO 12345 --- [main] c.m.MiniUpsApplication: Started MiniUpsApplication in 2.456 seconds
```

**成功标志：**
- ✅ 看到 Spring Boot Banner
- ✅ 显示 "Started MiniUpsApplication in X seconds"
- ✅ Tomcat 在端口 8081 启动
- ✅ 可以访问 http://localhost:8081/api

---

## 🎯 第一阶段学习总结

通过第一阶段学习，你应该掌握：

### ✅ 核心概念
- Spring Boot 的基本理念和优势
- 自动配置原理和工作机制
- 起步依赖的作用和使用方法
- 多环境配置管理策略

### ✅ 项目结构
- 标准的 Spring Boot 项目组织方式
- 各层职责划分（controller、service、repository）
- 配置文件的层次结构
- 包结构的最佳实践

### ✅ 核心注解
- `@SpringBootApplication` 组合注解的含义
- 组件注解（@Service、@Repository、@Controller）
- 配置注解（@Configuration、@Bean、@Value）
- 依赖注入方式和最佳实践

### ✅ 实践能力
- 能够创建和启动 Spring Boot 应用
- 理解启动日志的含义
- 掌握基本的配置方法
- 了解项目的基本运行机制

### 🎯 下一阶段预告

在第二阶段中，我们将深入学习：
- **分层架构设计**：Controller、Service、Repository 各层的详细实现
- **依赖注入深入**：IoC容器原理、Bean生命周期、循环依赖处理
- **AOP面向切面编程**：事务管理、日志记录、性能监控
- **异常处理机制**：全局异常处理、自定义异常、错误响应标准化

准备好进入更深入的 Spring Boot 世界了吗？🚀

---

## 🎯 第二阶段：Spring Boot 分层架构设计与最佳实践

基于 Mini-UPS 项目的实际代码，深入学习企业级分层架构的设计原理和实现方法。

### 2.1 分层架构核心理念

#### **什么是分层架构？**

分层架构是一种将应用程序组织成水平层次的架构模式，每一层都有特定的职责，并且只能与相邻的层进行交互。

**核心优势：**
- 🔗 **关注点分离**：每层专注于特定功能
- 🔄 **可维护性**：修改一层不影响其他层
- 🧪 **可测试性**：层与层之间便于单元测试
- 🚀 **可扩展性**：支持水平和垂直扩展

### 2.2 Mini-UPS 的分层架构实现

#### **🎮 表现层 (Presentation Layer) - Controller**

表现层负责处理 HTTP 请求和响应，是客户端与后端系统交互的入口点。

让我们分析 `UserController.java` 的设计：

```java
/**
 * 用户管理控制器 - 表现层核心组件
 * 
 * 设计理念：
 * - 专注于HTTP协议处理，不包含业务逻辑
 * - 统一的请求验证和响应格式
 * - 基于Spring Security的细粒度权限控制
 * - 完整的异常处理和日志记录
 */
@RestController                          // 组合注解：@Controller + @ResponseBody
@RequestMapping("/api/users")            // 统一的API路径前缀
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    // 依赖注入：通过构造器注入（最佳实践）
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
```

**🎯 核心设计原则：**

1. **单一职责原则**：Controller 只负责请求处理，不包含业务逻辑
2. **依赖注入**：通过构造器注入依赖，便于测试和管理
3. **统一路径设计**：RESTful API 风格的路径规划

#### **🔒 权限控制与安全设计**

```java
/**
 * 获取当前用户资料 - 基础权限控制示例
 */
@GetMapping("/profile")
@PreAuthorize("isAuthenticated()")      // 方法级权限控制
public ResponseEntity<?> getCurrentUserProfile() {
    try {
        // 从Security上下文获取当前认证用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.debug("获取用户资料: username={}", username);
        
        // 调用业务层处理
        UserDto userProfile = userService.getCurrentUserInfo(username);
        
        // 构建标准响应格式
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "获取用户资料成功");
        response.put("user", userProfile);
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        logger.error("获取用户资料失败", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("系统错误", "获取用户资料失败"));
    }
}
```

**🔐 权限控制层次：**

1. **认证检查**：`@PreAuthorize("isAuthenticated()")` - 确保用户已登录
2. **角色权限**：`@PreAuthorize("hasRole('ADMIN')")` - 基于角色的访问控制
3. **复合权限**：`@PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")` - 复杂权限逻辑

#### **📝 数据验证与请求处理**

```java
/**
 * 更新用户资料 - 数据验证和安全处理示例
 */
@PutMapping("/profile")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> updateCurrentUserProfile(@Valid @RequestBody UpdateUserDto updateRequest) {
    try {
        // 获取当前用户身份
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // 获取当前用户信息
        UserDto currentUser = userService.getCurrentUserInfo(username);
        
        // 🔒 安全处理：确保普通用户不能修改管理员字段
        UpdateUserDto userSafeRequest = updateRequest.forUserSelfUpdate();
        
        // 调用业务层更新
        UserDto updatedUser = userService.updateUser(currentUser.getId(), userSafeRequest);
        
        // 标准成功响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "用户资料更新成功");
        response.put("user", updatedUser);
        
        logger.info("用户资料更新成功: username={}", username);
        return ResponseEntity.ok(response);
        
    } catch (RuntimeException e) {
        // 业务异常处理
        logger.error("更新用户资料失败: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(createErrorResponse("更新失败", e.getMessage()));
    } catch (Exception e) {
        // 系统异常处理
        logger.error("更新用户资料出现异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("系统错误", "更新用户资料失败"));
    }
}
```

**🎯 关键设计点：**

1. **@Valid 注解**：自动进行 Bean Validation 验证
2. **安全防护**：`forUserSelfUpdate()` 过滤敏感字段
3. **分层异常处理**：业务异常 vs 系统异常的区别处理
4. **统一响应格式**：成功和错误响应的标准化

#### **👨‍💼 管理员功能设计**

```java
/**
 * 获取用户列表 - 管理员功能示例
 */
@GetMapping
@PreAuthorize("hasRole('ADMIN')")       // 仅管理员可访问
public ResponseEntity<?> getAllUsers(@RequestParam(required = false) UserRole role) {
    try {
        logger.debug("获取用户列表: role={}", role);
        
        // 支持按角色筛选（可选参数）
        List<UserDto> users;
        if (role != null) {
            users = userService.getUsersByRole(role);
        } else {
            users = userService.getAllUsers();
        }
        
        // 管理员响应包含更多信息
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "获取用户列表成功");
        response.put("users", users);
        response.put("total", users.size());    // 统计信息
        
        logger.debug("获取用户列表成功，共{}个用户", users.size());
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        logger.error("获取用户列表出现异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("系统错误", "获取用户列表失败"));
    }
}
```

### 2.3 Controller 层最佳实践总结

#### **🏗️ 架构设计原则**

**1. RESTful API 设计**
```java
// 资源导向的URL设计
GET    /api/users           # 获取用户列表
POST   /api/users           # 创建新用户
GET    /api/users/{id}      # 获取特定用户
PUT    /api/users/{id}      # 更新用户信息
DELETE /api/users/{id}      # 删除用户

// 特殊操作的URL设计
GET    /api/users/profile        # 当前用户资料
PUT    /api/users/profile        # 更新当前用户资料
POST   /api/users/{id}/enable    # 启用用户（非标准REST）
GET    /api/users/{id}/public    # 公开资料
```

**2. HTTP 状态码使用**
```java
// 成功响应
ResponseEntity.ok(response)                          // 200 OK
ResponseEntity.status(HttpStatus.CREATED).body(...)  // 201 Created

// 错误响应
ResponseEntity.badRequest().body(...)                // 400 Bad Request
ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)... // 500 Internal Server Error
```

**3. 统一响应格式**
```java
// 成功响应格式
{
    "success": true,
    "message": "操作成功",
    "data": { ... },      // 实际数据
    "timestamp": 1640995200000
}

// 错误响应格式
{
    "success": false,
    "error": "ValidationError",
    "message": "用户名已存在",
    "timestamp": 1640995200000
}
```

**4. 权限控制策略**
```java
// 认证检查
@PreAuthorize("isAuthenticated()")

// 角色权限
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasRole('USER')")

// 复合权限（管理员或资源所有者）
@PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")

// 自定义权限表达式
@PreAuthorize("@userPermissionEvaluator.canAccessUser(authentication, #userId)")
```

#### **🔧 工具和注解使用**

**1. 核心注解解析**
```java
@RestController     // = @Controller + @ResponseBody，自动JSON序列化
@RequestMapping     // 定义基础路径和请求方法
@GetMapping        // = @RequestMapping(method = RequestMethod.GET)
@PostMapping       // = @RequestMapping(method = RequestMethod.POST)
@PutMapping        // = @RequestMapping(method = RequestMethod.PUT)
@DeleteMapping     // = @RequestMapping(method = RequestMethod.DELETE)

@PathVariable      // 路径参数绑定：/users/{id} -> @PathVariable Long id
@RequestParam      // 查询参数绑定：?role=ADMIN -> @RequestParam UserRole role
@RequestBody       // 请求体绑定：JSON -> DTO对象
@Valid             // 触发Bean Validation验证

@PreAuthorize      // 方法级权限控制
```

**2. 异常处理模式**
```java
try {
    // 业务逻辑调用
    ResultDto result = businessService.doSomething(request);
    return ResponseEntity.ok(createSuccessResponse(result));
    
} catch (BusinessException e) {
    // 业务异常：返回400 Bad Request
    logger.error("业务处理失败: {}", e.getMessage());
    return ResponseEntity.badRequest()
            .body(createErrorResponse("业务错误", e.getMessage()));
            
} catch (Exception e) {
    // 系统异常：返回500 Internal Server Error
    logger.error("系统异常", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("系统错误", "处理请求时发生错误"));
}
```

### 2.4 实际运行和测试

让我们通过实际的 API 调用来理解 Controller 层的工作原理：

```bash
# 1. 获取当前用户资料（需要认证）
curl -X GET http://localhost:8081/api/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"

# 2. 更新用户资料
curl -X PUT http://localhost:8081/api/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "张",
    "lastName": "三",
    "phone": "13800138000",
    "address": "北京市朝阳区xxx街道"
  }'

# 3. 管理员获取用户列表
curl -X GET "http://localhost:8081/api/users?role=USER" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json"

# 4. 创建新用户（管理员功能）
curl -X POST http://localhost:8081/api/users \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "SecurePassword123",
    "firstName": "新",
    "lastName": "用户",
    "role": "USER"
  }'
```

### 2.5 第二阶段学习总结

通过本阶段学习，你应该掌握：

#### ✅ 表现层设计
- Controller 的职责边界和设计原则
- RESTful API 的设计规范和实现
- HTTP 状态码的正确使用方法
- 统一响应格式的设计策略

#### ✅ 权限控制
- Spring Security 权限注解的使用
- 认证与授权的区别和实现
- 复合权限表达式的编写方法
- 安全防护的最佳实践

#### ✅ 请求处理
- 参数绑定和数据验证的实现
- 异常处理的分层策略
- 日志记录的标准做法
- 响应格式的标准化设计

#### ✅ 实践能力
- 能够设计标准的 RESTful API
- 理解 Spring MVC 的工作机制
- 掌握权限控制的实现方法
- 具备基本的 API 测试能力

### 🎯 下一阶段预告

在第三阶段中，我们将深入学习：
- **💼 业务逻辑层 (Service Layer)**：事务管理、业务规则、服务编排
- **🗃️ 数据访问层 (Repository Layer)**：JPA 使用、查询优化、缓存策略
- **🔄 依赖注入深入**：IoC 容器、Bean 生命周期、循环依赖处理
- **📊 数据模型设计**：实体关系、DTO 模式、数据验证

准备好深入探索 Spring Boot 的业务核心了吗？🚀

---

## 🛡️ 企业级异常处理机制

在学习第三阶段之前，让我们先了解 Spring Boot 中一个非常重要的横切关注点——**全局异常处理**。

### 2.6 全局异常处理器深度解析

基于 Mini-UPS 项目的 `GlobalExceptionHandler.java`，我们来理解企业级异常处理的设计思路。

#### **🎯 为什么需要全局异常处理？**

在 Controller 层我们看到了大量的 try-catch 代码块，这带来了几个问题：
- 🔄 **代码重复**：每个方法都需要相似的异常处理逻辑
- 🧹 **代码冗余**：业务逻辑被异常处理代码污染
- 📊 **不一致性**：不同开发者可能使用不同的错误响应格式
- 🔒 **安全风险**：可能意外泄露敏感的系统信息

#### **🏗️ 全局异常处理器架构**

```java
/**
 * 全局异常处理器 - 企业级异常管理核心
 * 
 * 设计理念：
 * - 统一异常处理逻辑，消除代码重复
 * - 标准化错误响应格式，提升用户体验
 * - 分层异常处理，不同异常不同策略
 * - 安全防护，防止敏感信息泄露
 */
@ControllerAdvice                    // 全局控制器增强
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
```

**🎯 核心注解说明：**
- `@ControllerAdvice`：标记这是一个全局异常处理器
- 自动拦截所有 Controller 抛出的异常
- 提供统一的异常处理逻辑

#### **📊 分层异常处理策略**

**1. 业务异常处理**
```java
/**
 * 用户已存在异常处理 - 业务异常示例
 */
@ExceptionHandler(UserAlreadyExistsException.class)
public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExistsException(
        UserAlreadyExistsException ex, WebRequest request) {
    
    logger.warn("用户已存在异常: {}", ex.getMessage());
    
    // 返回标准化的错误响应
    ApiResponse<Void> response = ApiResponse.error("USER_ALREADY_EXISTS", ex.getMessage());
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
}
```

**特点：**
- 🎯 **精确匹配**：处理特定业务异常类型
- 📝 **日志记录**：使用 WARN 级别，记录业务问题
- 🏷️ **错误编码**：提供标准化的错误代码
- 📊 **HTTP状态码**：400 Bad Request 表示客户端错误

**2. 认证授权异常处理**
```java
/**
 * 认证异常处理 - 安全相关异常
 */
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
        AuthenticationException ex, WebRequest request) {
    
    logger.warn("认证失败: {}", ex.getMessage());
    
    // 🔒 安全考虑：不暴露具体的认证失败原因
    ApiResponse<Void> response = ApiResponse.error("AUTHENTICATION_ERROR", "用户名或密码错误");
    return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
}

/**
 * 权限不足异常处理
 */
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
        AccessDeniedException ex, WebRequest request) {
    
    logger.warn("权限不足: {}", ex.getMessage());
    
    ApiResponse<Void> response = ApiResponse.error("ACCESS_DENIED", "您没有执行此操作的权限");
    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
}
```

**安全特性：**
- 🔒 **信息脱敏**：不暴露内部认证逻辑
- 🏷️ **标准消息**：用户友好的错误提示
- 📊 **正确状态码**：401 未认证 vs 403 权限不足

**3. 数据验证异常处理**
```java
/**
 * 输入验证异常处理 - Bean Validation 集成
 */
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
        MethodArgumentNotValidException ex, WebRequest request) {
    
    logger.warn("输入验证失败: {}", ex.getMessage());
    
    // 📝 提取所有字段验证错误
    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        fieldErrors.put(fieldName, errorMessage);
    });
    
    // 🎯 返回详细的字段错误信息
    ApiResponse<Map<String, String>> response = ApiResponse.error("VALIDATION_ERROR", "请检查输入数据格式");
    response.setData(fieldErrors);  // 包含具体字段错误
    
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
}
```

**验证特性：**
- 🔍 **字段级错误**：精确指出哪个字段有问题
- 📋 **错误聚合**：一次返回所有验证错误
- 🎯 **用户友好**：清晰的错误描述

**4. 系统异常处理**
```java
/**
 * 空指针异常处理 - 系统级异常
 */
@ExceptionHandler(NullPointerException.class)
public ResponseEntity<ApiResponse<Void>> handleNullPointerException(
        NullPointerException ex, WebRequest request) {
    
    logger.error("空指针异常", ex);  // ERROR级别，包含完整堆栈
    
    // 🔒 安全处理：不暴露内部实现细节
    ApiResponse<Void> response = ApiResponse.error("SYSTEM_ERROR", "服务器内部错误，请稍后重试");
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
}

/**
 * 兜底异常处理 - 处理所有未捕获的异常
 */
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<Void>> handleGlobalException(
        Exception ex, WebRequest request) {
    
    logger.error("未处理的异常", ex);
    
    ApiResponse<Void> response = ApiResponse.error("SYSTEM_ERROR", "服务器内部错误，请稍后重试");
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
}
```

**系统异常特性：**
- 🚨 **完整日志**：ERROR 级别 + 完整堆栈跟踪
- 🔒 **信息隐藏**：不向用户暴露技术细节
- 🛡️ **兜底机制**：`Exception.class` 捕获所有未处理异常

#### **📋 标准化响应格式设计**

基于 `ApiResponse<T>` 类的统一响应格式：

```java
// 成功响应格式
{
    "success": true,
    "code": "SUCCESS",
    "message": "操作成功",
    "data": { /* 实际数据 */ },
    "timestamp": "2024-01-15T10:30:00Z"
}

// 业务错误响应
{
    "success": false,
    "code": "USER_ALREADY_EXISTS",
    "message": "用户名已存在",
    "data": null,
    "timestamp": "2024-01-15T10:30:00Z"
}

// 验证错误响应（包含字段详情）
{
    "success": false,
    "code": "VALIDATION_ERROR",
    "message": "请检查输入数据格式",
    "data": {
        "email": "邮箱格式不正确",
        "password": "密码长度至少8位"
    },
    "timestamp": "2024-01-15T10:30:00Z"
}

// 系统错误响应
{
    "success": false,
    "code": "SYSTEM_ERROR",
    "message": "服务器内部错误，请稍后重试",
    "data": null,
    "timestamp": "2024-01-15T10:30:00Z"
}
```

#### **🔧 Controller 层代码简化**

有了全局异常处理器，Controller 代码大大简化：

```java
// 📊 简化前（需要大量异常处理代码）
@GetMapping("/profile")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> getCurrentUserProfile() {
    try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        UserDto userProfile = userService.getCurrentUserInfo(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "获取用户资料成功");
        response.put("user", userProfile);
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        logger.error("获取用户资料失败", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("系统错误", "获取用户资料失败"));
    }
}

// ✨ 简化后（专注业务逻辑）
@GetMapping("/profile")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<UserDto>> getCurrentUserProfile() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    
    UserDto userProfile = userService.getCurrentUserInfo(username);
    
    return ResponseEntity.ok(ApiResponse.success("获取用户资料成功", userProfile));
}
```

**简化效果：**
- 🎯 **专注业务**：Controller 只关心业务逻辑
- 🧹 **代码整洁**：去除重复的异常处理代码
- 📊 **一致性**：所有异常都有统一的处理和响应格式

#### **🎯 异常处理最佳实践**

**1. 异常分层策略**
```java
// 业务异常（400级别）
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}

// 认证异常（401级别）
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) { super(message); }
}

// 权限异常（403级别）
public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) { super(message); }
}

// 资源未找到异常（404级别）
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
```

**2. 日志记录策略**
```java
// 业务异常：WARN 级别（预期内的问题）
logger.warn("用户已存在: username={}", username);

// 系统异常：ERROR 级别（需要修复的问题）
logger.error("数据库连接失败", ex);

// 安全相关：WARN 级别（潜在攻击尝试）
logger.warn("登录失败次数过多: ip={}", request.getRemoteAddr());
```

**3. 错误编码设计**
```java
// 业务相关
USER_ALREADY_EXISTS     - 用户已存在
USER_NOT_FOUND         - 用户不存在
INVALID_PASSWORD       - 密码格式错误

// 认证相关
AUTHENTICATION_ERROR   - 认证失败
ACCESS_DENIED         - 权限不足
TOKEN_EXPIRED         - 令牌过期

// 系统相关
VALIDATION_ERROR      - 数据验证失败
SYSTEM_ERROR         - 系统内部错误
DATABASE_ERROR       - 数据库异常
```

### 2.7 异常处理机制总结

通过全局异常处理器，我们实现了：

#### ✅ 架构优势
- **🔄 代码复用**：消除重复的异常处理逻辑
- **📊 响应一致**：标准化的 API 响应格式
- **🔒 安全防护**：防止敏感信息泄露
- **🧹 代码整洁**：Controller 专注业务逻辑

#### ✅ 运维友好
- **📝 日志分级**：便于问题排查和监控
- **🎯 错误编码**：快速定位问题类型
- **📊 统计支持**：便于错误率和异常统计

#### ✅ 用户体验
- **🎨 友好提示**：用户易懂的错误消息
- **🔍 详细反馈**：验证错误的具体字段信息
- **⚡ 快速响应**：统一的处理流程

---

## 🎯 第三阶段：业务逻辑层与数据访问层深度实战

在掌握了表现层和异常处理后，现在深入学习 Spring Boot 应用的核心——业务逻辑层和数据访问层的设计与实现。

### 3.1 分层架构的核心理念

#### **🏗️ 三层架构的职责分工**

```
🎮 Controller Layer (表现层)
     ↓ HTTP请求/响应处理
💼 Service Layer (业务逻辑层)  
     ↓ 事务管理/业务规则
🗃️ Repository Layer (数据访问层)
     ↓ 数据持久化
📊 Database Layer (数据库层)
```

**每层的核心职责：**
- **Service Layer**：业务规则、事务管理、数据验证、异常处理
- **Repository Layer**：数据访问、查询优化、缓存管理
- **Entity Layer**：领域模型、数据映射、关系定义

### 3.2 业务逻辑层 (Service Layer) 设计

基于 Mini-UPS 项目的 `UserService.java`，学习企业级业务层设计。

#### **💼 依赖注入与架构设计**

```java
/**
 * 用户管理服务 - 业务逻辑层核心
 * 
 * 设计理念：
 * - 封装业务规则和流程逻辑
 * - 管理事务边界和数据一致性
 * - 协调多个数据访问对象
 * - 处理业务异常和数据验证
 */
@Service                              // 标记为业务层组件
@Transactional                        // 类级别事务管理
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    // 通过构造器注入依赖（最佳实践）
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
```

**🎯 设计原则解析：**

1. **构造器依赖注入**：比字段注入更安全，便于单元测试
2. **final 字段**：确保依赖不可变，避免运行时修改
3. **类级别事务**：为所有方法提供默认事务管理
4. **单一职责**：专注用户管理业务，不涉及HTTP或数据库细节

#### **🔄 事务管理策略**

```java
/**
 * 查询操作 - 只读事务优化
 */
@Transactional(readOnly = true)
public UserDto getCurrentUserInfo(String username) {
    logger.debug("Getting user information: username={}", username);
    
    Optional<User> userOptional = userRepository.findByUsername(username);
    if (userOptional.isEmpty()) {
        logger.error("User not found: username={}", username);
        throw new UserNotFoundException(username);
    }
    
    User user = userOptional.get();
    return UserDto.fromEntity(user);
}

/**
 * 写操作 - 完整事务管理
 */
public UserDto updateUser(Long userId, UpdateUserDto updateRequest) {
    logger.info("Update user information: userId={}", userId);
    
    // 🔍 1. 数据查询和验证
    Optional<User> userOptional = userRepository.findById(userId);
    if (userOptional.isEmpty()) {
        throw new UserNotFoundException("ID: " + userId);
    }
    
    User user = userOptional.get();
    
    // 🔒 2. 业务规则验证
    if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
        if (userRepository.existsByEmail(updateRequest.getEmail())) {
            throw new UserAlreadyExistsException("邮箱", updateRequest.getEmail());
        }
    }
    
    // ✏️ 3. 数据更新
    updateUserFields(user, updateRequest);
    
    // 💾 4. 数据持久化
    User updatedUser = userRepository.save(user);
    
    logger.info("用户信息更新成功: userId={}, username={}", userId, user.getUsername());
    return UserDto.fromEntity(updatedUser);
}
```

**📊 事务设计要点：**

1. **读写分离**：查询使用 `readOnly = true` 提升性能
2. **事务边界**：一个业务方法 = 一个事务边界
3. **异常回滚**：RuntimeException 自动触发事务回滚
4. **数据一致性**：整个操作序列保证原子性

#### **🛡️ 业务异常处理模式**

```java
/**
 * 创建用户 - 完整的业务流程和异常处理
 */
public UserDto createUser(CreateUserDto createRequest) {
    logger.info("创建新用户: username={}, email={}, role={}", 
               createRequest.getUsername(), createRequest.getEmail(), createRequest.getRole());
    
    // 🔍 业务规则验证
    if (userRepository.existsByUsername(createRequest.getUsername())) {
        logger.warn("用户名已存在: username={}", createRequest.getUsername());
        throw new UserAlreadyExistsException("用户名", createRequest.getUsername());
    }
    
    if (userRepository.existsByEmail(createRequest.getEmail())) {
        logger.warn("邮箱已存在: email={}", createRequest.getEmail());
        throw new UserAlreadyExistsException("邮箱", createRequest.getEmail());
    }
    
    try {
        // 📝 实体创建和数据转换
        User user = createUserFromCreateRequest(createRequest);
        
        // 💾 数据持久化
        User savedUser = userRepository.save(user);
        UserDto userDto = UserDto.fromEntity(savedUser);
        
        logger.info("用户创建成功: id={}, username={}, role={}", 
                   savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
        return userDto;
        
    } catch (Exception e) {
        logger.error("创建用户时出现异常: username={}", createRequest.getUsername(), e);
        throw new RuntimeException("创建用户失败，请稍后重试");
    }
}

/**
 * 私有辅助方法 - 实体创建逻辑
 */
private User createUserFromCreateRequest(CreateUserDto createRequest) {
    User user = new User();
    user.setUsername(createRequest.getUsername());
    user.setEmail(createRequest.getEmail());
    user.setPassword(passwordEncoder.encode(createRequest.getPassword()));  // 🔒 密码加密
    user.setFirstName(createRequest.getFirstName());
    user.setLastName(createRequest.getLastName());
    user.setRole(createRequest.getRole());
    user.setEnabled(createRequest.getEnabled());
    
    return user;
}
```

**🎯 业务层最佳实践：**

1. **快速失败**：在方法开始就进行所有验证
2. **领域异常**：使用具体的业务异常类型
3. **日志记录**：记录关键业务操作和异常
4. **方法职责**：辅助方法处理数据转换逻辑

### 3.3 数据访问层 (Repository Layer) 设计

#### **🗃️ Spring Data JPA 方法命名约定**

```java
/**
 * 用户数据访问接口 - Repository层设计示例
 * 
 * 设计特点：
 * - 继承JpaRepository获得标准CRUD操作
 * - 方法命名自动生成SQL查询
 * - 类型安全的查询接口
 * - 性能优化的存在性检查
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 🔍 精确查询方法
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    // ⚡ 性能优化的存在性检查
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    // 📊 复杂查询示例
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.enabled = true")
    List<User> findActiveUsersByRole(@Param("role") UserRole role);
    
    // 📈 统计查询
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") UserRole role);
    
    // 🔄 批量操作
    @Modifying
    @Query("UPDATE User u SET u.enabled = false WHERE u.lastLoginTime < :cutoffTime")
    int disableInactiveUsers(@Param("cutoffTime") LocalDateTime cutoffTime);
}
```

**📋 Repository 设计模式：**

1. **方法命名约定**：`findBy`、`existsBy`、`countBy` 自动生成查询
2. **返回类型优化**：使用 `Optional` 避免 null 处理
3. **自定义查询**：使用 `@Query` 处理复杂业务逻辑
4. **性能优化**：`existsBy` 比 `findBy` 更高效

#### **🚀 继承 JpaRepository 的优势**

```java
// 自动获得的CRUD操作：
User user = userRepository.save(user);              // 保存或更新
Optional<User> user = userRepository.findById(1L);  // 按ID查询
List<User> users = userRepository.findAll();        // 查询所有
userRepository.deleteById(1L);                      // 按ID删除
long count = userRepository.count();                // 统计数量

// 分页和排序支持：
Pageable pageable = PageRequest.of(0, 10, Sort.by("username"));
Page<User> userPage = userRepository.findAll(pageable);
```

### 3.4 实体层 (Entity Layer) 设计

#### **📊 基础实体类设计**

```java
/**
 * 基础实体类 - 通用字段和审计功能
 * 
 * 设计目标：
 * - 提取所有实体的通用字段
 * - 自动管理创建和修改时间
 * - 乐观锁防止并发冲突
 * - 统一主键策略
 */
@MappedSuperclass                           // 不是实体，但可被继承
@EntityListeners(AuditingEntityListener.class)  // 启用JPA审计
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 自增主键
    private Long id;
    
    @CreatedDate                                        // 自动设置创建时间
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate                                   // 自动设置修改时间
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version                                           // 乐观锁版本号
    private Long version;
    
    // getters, setters, equals, hashCode...
}
```

#### **👤 用户实体设计**

```java
/**
 * 用户实体 - 完整的JPA映射示例
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_username", columnList = "username")
})
public class User extends BaseEntity {
    
    // 🏷️ 基本字段定义
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @NotBlank
    @Email
    @Size(max = 100)
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @NotBlank
    @Size(min = 8, max = 100)
    @Column(name = "password", nullable = false, length = 100)
    private String password;
    
    // 🎭 枚举类型映射
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;
    
    // 🔗 关系映射
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Shipment> shipments = new ArrayList<>();
    
    // 📅 时间字段
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
    
    // 🎯 业务方法
    public void updateLastLoginTime() {
        this.lastLoginTime = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return enabled && lastLoginTime != null && 
               lastLoginTime.isAfter(LocalDateTime.now().minusDays(30));
    }
}
```

**🎯 实体设计要点：**

1. **索引优化**：为频繁查询字段建立索引
2. **约束组合**：Bean Validation + JPA 约束
3. **关系映射**：懒加载避免 N+1 查询
4. **业务方法**：实体内封装业务逻辑

### 3.5 数据传输对象 (DTO) 模式

#### **🔄 DTO 转换策略**

```java
/**
 * 用户DTO - 安全的数据传输对象
 */
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    // 注意：不包含password等敏感字段
    
    /**
     * 完整信息转换 - 用于管理员查看
     */
    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        dto.setEnabled(user.getEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
    
    /**
     * 公开信息转换 - 用于公开接口
     */
    public static UserDto publicProfile(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        // 不包含邮箱、角色等敏感信息
        return dto;
    }
}
```

#### **📝 请求DTO设计**

```java
/**
 * 创建用户请求DTO
 */
public class CreateUserDto {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$", 
             message = "密码必须包含大小写字母和数字")
    private String password;
    
    @NotNull(message = "用户角色不能为空")
    private UserRole role;
    
    private Boolean enabled = true;  // 默认启用
}

/**
 * 更新用户请求DTO - 支持部分更新
 */
public class UpdateUserDto {
    @Email(message = "邮箱格式不正确")
    private String email;          // 可选字段
    
    @Size(max = 50)
    private String firstName;      // 可选字段
    
    @Size(max = 50)
    private String lastName;       // 可选字段
    
    // 管理员专用字段
    private UserRole role;
    private Boolean enabled;
    
    /**
     * 用户自我更新 - 过滤敏感字段
     */
    public UpdateUserDto forUserSelfUpdate() {
        UpdateUserDto safeDto = new UpdateUserDto();
        safeDto.setEmail(this.email);
        safeDto.setFirstName(this.firstName);
        safeDto.setLastName(this.lastName);
        // 不包含role和enabled字段
        return safeDto;
    }
}
```

### 3.6 Service层与Repository层的协作模式

#### **🔄 完整的业务流程示例**

```java
/**
 * 用户状态管理 - Service层协调多个操作
 */
@Service
@Transactional
public class UserService {
    
    /**
     * 用户登录业务逻辑
     */
    public LoginResponseDto login(LoginRequestDto loginRequest) {
        // 🔍 1. 查询用户
        Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());
        if (userOptional.isEmpty()) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }
        
        User user = userOptional.get();
        
        // 🔒 2. 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }
        
        // ✅ 3. 检查用户状态
        if (!user.getEnabled()) {
            throw new UserDisabledException("账户已被禁用");
        }
        
        // 📝 4. 更新登录时间
        user.updateLastLoginTime();
        userRepository.save(user);
        
        // 🎟️ 5. 生成JWT令牌
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        
        // 📊 6. 构建响应
        return LoginResponseDto.builder()
                .token(token)
                .user(UserDto.fromEntity(user))
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .build();
    }
    
    /**
     * 批量用户状态更新
     */
    public void disableInactiveUsers(int inactiveDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(inactiveDays);
        
        // 📊 使用Repository的批量更新
        int updatedCount = userRepository.disableInactiveUsers(cutoffTime);
        
        logger.info("禁用了{}个不活跃用户", updatedCount);
        
        // 🔔 发送通知（可选）
        if (updatedCount > 0) {
            notificationService.notifyAdmins("系统自动禁用了" + updatedCount + "个不活跃用户");
        }
    }
}
```

### 3.7 第三阶段学习总结

通过本阶段学习，你应该掌握：

#### ✅ Service层设计
- 业务逻辑封装和事务管理策略
- 依赖注入的最佳实践模式
- 业务异常处理和日志记录
- 多层服务协调和数据验证

#### ✅ Repository层设计
- Spring Data JPA 方法命名约定
- 自定义查询和性能优化策略
- 批量操作和分页查询实现
- 继承 JpaRepository 的优势

#### ✅ Entity层设计
- JPA 实体映射和关系设计
- 基础实体类和审计功能
- 索引优化和约束设计
- 乐观锁和并发控制

#### ✅ DTO模式
- 数据传输对象的安全设计
- 实体与DTO的转换策略
- 请求验证和数据过滤
- 多种转换场景的处理

#### ✅ 实践能力
- 能够设计完整的业务流程
- 理解事务边界和数据一致性
- 掌握查询优化和性能调优
- 具备企业级代码设计能力

### 🎯 下一阶段预告

在第四阶段中，我们将学习：
- **🔒 Spring Security 安全配置**：JWT认证、权限控制、安全过滤器
- **⚙️ 配置管理与环境分离**：Profile、外部配置、敏感信息保护
- **📊 监控与运维**：Actuator健康检查、指标收集、日志管理
- **🚀 部署与优化**：Docker容器化、性能调优、生产环境最佳实践

准备好学习企业级Spring Boot应用的高级特性了吗？🚀

---

## 🔒 第四阶段：企业级安全配置与JWT认证实战

在掌握了业务逻辑和数据访问层后，现在学习现代Web应用最关键的部分——安全认证与授权系统的设计与实现。

### 4.1 Spring Security 安全架构设计

#### **🏗️ 企业级安全架构概览**

基于 Mini-UPS 项目的安全实现，我们来理解现代企业级安全架构：

```
┌─────────────────────────────────────────────────────────┐
│                Spring Security 安全架构                 │
├─────────────────────────────────────────────────────────┤
│  🔧 SecurityConfig.java - 安全配置核心                  │
│  ├── 🛡️ Filter Chain (安全过滤器链)                     │
│  │   ├── CORS Filter (跨域资源共享)                     │
│  │   ├── JwtAuthenticationFilter (JWT认证过滤器)        │
│  │   └── UsernamePasswordAuthenticationFilter           │
│  ├── 🔐 Authentication Provider (认证提供者)            │
│  │   └── DaoAuthenticationProvider + BCryptEncoder      │
│  └── 🎯 Authorization (授权配置)                        │
│      ├── 🌐 Public Endpoints (/api/auth/**)             │
│      ├── 👨‍💼 Admin Endpoints (/api/admin/** - ADMIN)     │
│      ├── 🚛 Driver Endpoints (/api/driver/** - DRIVER)  │
│      └── 🔒 Protected Endpoints (authenticated users)   │
└─────────────────────────────────────────────────────────┘
```

#### **🔧 SecurityConfig 核心配置解析**

```java
/**
 * Spring Security 安全配置类
 * 
 * 核心功能：
 * - 配置安全过滤器链
 * - 定义URL访问权限
 * - 集成JWT认证机制
 * - 处理CORS跨域问题
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // 启用方法级权限控制
public class SecurityConfig {
    
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * 安全过滤器链配置 - 整个安全架构的核心
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 🌐 CORS配置 - 支持前后端分离
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 🚫 禁用CSRF - JWT无状态认证不需要CSRF保护
            .csrf(csrf -> csrf.disable())
            
            // ❌ 异常处理配置
            .exceptionHandling(exception -> 
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            
            // 🔄 会话管理 - 无状态会话策略
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 🎯 URL访问权限配置
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()           // 认证端点公开
                .requestMatchers("/api/tracking/**").permitAll()       // 包裹追踪公开
                .requestMatchers("/api/admin/**").hasRole("ADMIN")     // 管理员专用
                .requestMatchers("/api/driver/**").hasAnyRole("DRIVER", "ADMIN")  // 司机和管理员
                .anyRequest().authenticated()                          // 其他请求需要认证
            );
        
        // 🔍 添加JWT过滤器到过滤器链
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

**🎯 配置要点解析：**

1. **无状态会话**：`SessionCreationPolicy.STATELESS` 确保无服务器状态
2. **过滤器顺序**：JWT过滤器在用户名密码过滤器之前执行
3. **权限分层**：公开端点 → 角色权限 → 认证权限的层次设计
4. **异常处理**：统一的认证失败处理机制

### 4.2 JWT认证机制深度实战

#### **🎟️ JWT令牌生成与验证**

```java
/**
 * JWT令牌提供者 - 令牌的生成、验证、解析核心类
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private int jwtExpirationInMs;
    
    /**
     * 生成JWT令牌
     * 
     * @param username 用户名
     * @param role 用户角色
     * @return JWT令牌字符串
     */
    public String generateToken(String username, UserRole role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        
        // 🏗️ 构建JWT令牌
        return Jwts.builder()
                .subject(username)                    // 主体：用户名
                .claim("role", role.name())          // 声明：用户角色
                .issuedAt(now)                       // 签发时间
                .expiration(expiryDate)              // 过期时间
                .signWith(getSigningKey())           // 签名密钥
                .compact();
    }
    
    /**
     * 验证JWT令牌有效性
     */
    public boolean validateToken(String token) {
        try {
            // 🔍 解析并验证令牌
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        }
        return false;
    }
    
    /**
     * 从令牌中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

#### **🔍 JWT认证过滤器实现**

```java
/**
 * JWT认证过滤器 - 每个请求的JWT验证入口
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // 🎯 1. 从请求头中提取JWT令牌
        String jwt = getJwtFromRequest(request);
        
        if (jwt != null && tokenProvider.validateToken(jwt)) {
            // 🔍 2. 从令牌中提取用户名
            String username = tokenProvider.getUsernameFromToken(jwt);
            
            // 📊 3. 加载用户详情
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // 🔐 4. 创建认证对象
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // ✅ 5. 设置到安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        // 🔄 6. 继续过滤器链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头中提取JWT令牌
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // 去除 "Bearer " 前缀
        }
        return null;
    }
}
```

**🔄 JWT认证流程详解：**

1. **令牌提取**：从 `Authorization: Bearer <token>` 头部提取JWT
2. **令牌验证**：验证签名、过期时间、格式有效性
3. **用户加载**：根据用户名从数据库加载用户详情
4. **认证创建**：构建Spring Security认证对象
5. **上下文设置**：将认证信息存储到SecurityContext
6. **请求继续**：将控制权交给下一个过滤器

### 4.3 用户认证服务实现

#### **🔐 AuthService 认证业务逻辑**

```java
/**
 * 认证服务 - 处理登录、注册、令牌管理等认证相关业务
 */
@Service
@Transactional
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    /**
     * 用户登录认证
     */
    public LoginResponseDto login(LoginRequestDto loginRequest) {
        logger.info("用户登录尝试: {}", loginRequest.getUsernameOrEmail());
        
        try {
            // 🔐 1. Spring Security 认证管理
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );
            
            // 👤 2. 获取认证用户详情
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            
            // 📝 3. 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
            
            // 🎟️ 4. 生成JWT令牌
            String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
            
            // 📊 5. 构建登录响应
            LoginResponseDto response = new LoginResponseDto();
            response.setToken(token);
            response.setTokenType("Bearer");
            response.setExpiresIn(tokenProvider.getExpirationTime());
            response.setUser(UserDto.fromEntity(user));
            
            logger.info("用户登录成功: username={}, role={}", user.getUsername(), user.getRole());
            return response;
            
        } catch (BadCredentialsException e) {
            logger.warn("登录失败 - 凭证错误: {}", loginRequest.getUsernameOrEmail());
            throw new InvalidCredentialsException("用户名或密码错误");
        } catch (DisabledException e) {
            logger.warn("登录失败 - 账户被禁用: {}", loginRequest.getUsernameOrEmail());
            throw new UserDisabledException("账户已被禁用，请联系管理员");
        }
    }
    
    /**
     * 用户注册
     */
    public UserDto register(RegisterRequestDto registerRequest) {
        logger.info("新用户注册: username={}, email={}", 
                   registerRequest.getUsername(), registerRequest.getEmail());
        
        // ✅ 1. 用户名唯一性检查
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("用户名", registerRequest.getUsername());
        }
        
        // ✅ 2. 邮箱唯一性检查
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("邮箱", registerRequest.getEmail());
        }
        
        // 🏗️ 3. 创建用户实体
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));  // 🔒 密码加密
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setRole(UserRole.USER);  // 默认用户角色
        user.setEnabled(true);
        
        // 💾 4. 保存用户
        User savedUser = userRepository.save(user);
        
        logger.info("用户注册成功: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        return UserDto.fromEntity(savedUser);
    }
    
    /**
     * 安全注销 - 令牌黑名单机制
     */
    public void logout(String token) {
        if (token != null && tokenProvider.validateToken(token)) {
            // 🚫 将令牌加入黑名单（可扩展Redis实现）
            String tokenId = tokenProvider.getTokenId(token);
            long expirationTime = tokenProvider.getExpirationTime(token);
            tokenBlacklistService.blacklistToken(tokenId, expirationTime);
            
            logger.info("用户安全注销，令牌已加入黑名单");
        }
    }
}
```

### 4.4 角色权限控制体系

#### **🎭 用户角色设计**

```java
/**
 * 用户角色枚举 - 基于角色的访问控制(RBAC)
 */
public enum UserRole {
    USER("User"),                    // 普通用户：查看订单、包裹追踪
    ADMIN("Administrator"),          // 系统管理员：完全访问权限
    DRIVER("Driver"),               // 司机：车辆管理、配送操作
    OPERATOR("Operator");           // 操作员：订单处理、仓库管理
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
}
```

#### **🎯 方法级权限控制**

```java
/**
 * Controller中的权限控制注解使用示例
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // 🔐 基础认证：只要登录即可访问
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUserProfile() {
        // 获取当前用户资料
    }
    
    // 👨‍💼 角色权限：只有管理员可访问
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        // 获取所有用户列表
    }
    
    // 🎭 多角色权限：管理员或操作员可访问
    @PostMapping("/{userId}/assign-truck")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> assignTruck(@PathVariable Long userId) {
        // 分配卡车
    }
    
    // 🔒 复合权限：管理员或资源所有者可访问
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and authentication.principal.user.id == #userId)")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long userId) {
        // 更新用户信息
    }
}
```

#### **📊 权限控制矩阵**

| API端点 | 🌐 PUBLIC | 👤 USER | 🚛 DRIVER | 🔧 OPERATOR | 👨‍💼 ADMIN |
|---------|-----------|---------|-----------|-------------|----------|
| `POST /api/auth/login` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `POST /api/auth/register` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `GET /api/tracking/{id}` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `GET /api/users/profile` | ❌ | ✅ | ✅ | ✅ | ✅ |
| `GET /api/shipments` | ❌ | ✅ | ✅ | ✅ | ✅ |
| `POST /api/shipments` | ❌ | ✅ | ❌ | ✅ | ✅ |
| `GET /api/trucks` | ❌ | ❌ | ✅ | ✅ | ✅ |
| `POST /api/trucks` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `GET /api/admin/users` | ❌ | ❌ | ❌ | ❌ | ✅ |
| `DELETE /api/admin/users/{id}` | ❌ | ❌ | ❌ | ❌ | ✅ |

### 4.5 企业级安全最佳实践

#### **🔒 密码安全策略**

```java
/**
 * 密码安全配置
 */
@Configuration
public class PasswordConfig {
    
    /**
     * BCrypt密码编码器 - 企业级密码加密
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // 强度12，安全性高
    }
    
    /**
     * 密码复杂度验证器
     */
    @Component
    public class PasswordValidator {
        
        private static final String PASSWORD_PATTERN = 
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$";
        
        private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        
        public boolean isValid(String password) {
            return pattern.matcher(password).matches();
        }
        
        public String getRequirements() {
            return "密码必须包含：大写字母、小写字母、数字、特殊字符，长度8-20位";
        }
    }
}
```

#### **🔄 令牌安全管理**

```java
/**
 * 令牌黑名单服务 - 安全注销机制
 */
@Service
public class TokenBlacklistService {
    
    private static final String BLACKLIST_PREFIX = "blacklisted_token:";
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * 将令牌加入黑名单
     */
    public void blacklistToken(String tokenId, long expirationTime) {
        String key = BLACKLIST_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, "blacklisted", expirationTime, TimeUnit.SECONDS);
        
        logger.info("令牌已加入黑名单: tokenId={}", tokenId);
    }
    
    /**
     * 检查令牌是否在黑名单中
     */
    public boolean isTokenBlacklisted(String tokenId) {
        String key = BLACKLIST_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * 清理过期的黑名单条目
     */
    @Scheduled(fixedRate = 3600000)  // 每小时执行一次
    public void cleanupExpiredTokens() {
        // Redis TTL自动清理，无需手动处理
        logger.debug("黑名单清理任务执行完成");
    }
}
```

#### **🌐 CORS安全配置**

```java
/**
 * CORS配置 - 跨域资源共享安全策略
 */
@Configuration
public class CorsConfig {
    
    /**
     * 开发环境CORS配置
     */
    @Profile("dev")
    @Bean
    public CorsConfigurationSource devCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * 生产环境CORS配置 - 更严格的安全策略
     */
    @Profile("prod")
    @Bean
    public CorsConfigurationSource prodCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://yourdomain.com", "https://app.yourdomain.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(1800L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 4.6 安全配置文件管理

#### **⚙️ 环境分离配置**

```yaml
# application-dev.yml - 开发环境
jwt:
  secret: ${JWT_SECRET:devSecretKeyThatShouldBeAtLeast256BitsLongForHS256AlgorithmDevelopmentOnly}
  expiration: 86400000  # 24小时

security:
  cors:
    allowed-origins: "*"
  password:
    bcrypt-rounds: 10

logging:
  level:
    org.springframework.security: DEBUG

# application-prod.yml - 生产环境
jwt:
  secret: ${JWT_SECRET}  # 必须从环境变量获取
  expiration: 3600000    # 1小时

security:
  cors:
    allowed-origins: ${ALLOWED_ORIGINS:https://yourdomain.com}
  password:
    bcrypt-rounds: 12

logging:
  level:
    org.springframework.security: WARN
```

### 4.7 第四阶段学习总结

通过本阶段学习，你应该掌握：

#### ✅ Spring Security 配置
- 安全过滤器链的设计和配置
- 认证和授权机制的实现
- 无状态会话管理策略
- CORS跨域资源共享配置

#### ✅ JWT认证机制
- JWT令牌的生成、验证、解析
- 自定义JWT认证过滤器
- 令牌黑名单机制实现
- 安全注销和令牌管理

#### ✅ 权限控制体系
- 基于角色的访问控制(RBAC)
- 方法级权限注解使用
- 复合权限表达式设计
- 权限控制矩阵规划

#### ✅ 安全最佳实践
- BCrypt密码加密策略
- 密码复杂度验证规则
- 环境分离的安全配置
- 生产级安全加固措施

#### ✅ 实践能力
- 能够设计完整的认证授权系统
- 理解现代Web应用安全架构
- 掌握JWT无状态认证的实现
- 具备企业级安全开发能力

### 🎯 完整教程总结

恭喜你完成了整个 Spring Boot 企业级开发教程！

- ✅ **第一阶段**：Spring Boot 基础概念与核心特性
- ✅ **第二阶段**：分层架构设计与异常处理
- ✅ **第三阶段**：业务逻辑层与数据访问层
- ✅ **第四阶段**：企业级安全配置与JWT认证

### 🚀 下一步发展方向

**技术深化：**
- **微服务架构**：Spring Cloud、服务发现、配置中心
- **消息队列**：RabbitMQ、Apache Kafka异步处理
- **缓存优化**：Redis高级特性、分布式缓存
- **性能调优**：JVM调优、数据库优化、监控告警

**项目实战：**
- **扩展 Mini-UPS**：添加新功能、优化性能、增强安全
- **开发新项目**：运用所学知识构建自己的系统
- **开源贡献**：参与开源项目，提升技术影响力
- **技术分享**：写博客、做技术分享、帮助他人

你现在已经具备了企业级 Java 后端开发的核心技能，准备好迎接更大的挑战了吗？🚀

---

*💡 提示：继续动手实践 Mini-UPS 项目，尝试扩展新功能，并在实际项目中应用所学知识。*