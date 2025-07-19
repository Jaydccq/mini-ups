# 🚀 Java后端开发完整教程：从零搭建物流系统

## 📖 教程简介

本教程将手把手教你如何从零开始构建一个类似Mini-UPS的物流管理系统。我们会从最基础的概念开始，一步步搭建一个完整的企业级后端应用。

### 你将学到什么
- Java Spring Boot框架的完整使用
- 数据库设计与JPA操作
- REST API开发与测试
- 用户认证与JWT安全
- 缓存与性能优化
- 项目架构设计

### 适合人群
- Java基础知识（变量、类、方法）
- 没有实际项目经验的新手
- 想要系统学习后端开发的同学

---

## 📋 目录

1. [基础概念理解](#1-基础概念理解)
2. [开发环境搭建](#2-开发环境搭建)
3. [项目初始化](#3-项目初始化)
4. [数据库设计](#4-数据库设计)
5. [用户模块开发](#5-用户模块开发)
6. [包裹模块开发](#6-包裹模块开发)
7. [卡车管理模块](#7-卡车管理模块)
8. [安全认证系统](#8-安全认证系统)
9. [API测试](#9-api测试)
10. [高级功能](#10-高级功能)

---

## 1. 基础概念理解

### 1.1 什么是MVC架构？

MVC（Model-View-Controller）是一种软件架构模式：

```
用户请求 -> Controller(控制器) -> Service(业务逻辑) -> Repository(数据访问) -> Database(数据库)
                 ↓
              View(视图) <- Model(数据模型)
```

**举个例子**：用户要查看自己的包裹
1. **Controller**: 接收"查看包裹"的请求
2. **Service**: 处理业务逻辑（检查权限、查询数据）
3. **Repository**: 从数据库获取包裹信息
4. **Model**: 包裹数据
5. **View**: 返回给用户的JSON数据

### 1.2 Spring Boot核心概念

#### Entity（实体类）
- **作用**：对应数据库表的Java类
- **例子**：User类对应users表
```java
@Entity  // 标记这是一个数据库实体
@Table(name = "users")  // 对应数据库的users表
public class User {
    @Id  // 主键
    private Long id;
    private String username;
    private String email;
}
```

#### Repository（数据访问层）
- **作用**：负责与数据库交互
- **例子**：UserRepository负责用户数据的增删改查
```java
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);  // 根据邮箱查找用户
}
```

#### Service（业务逻辑层）
- **作用**：处理具体的业务逻辑
- **例子**：UserService处理用户注册、登录等业务
```java
@Service
public class UserService {
    public User registerUser(String email, String password) {
        // 1. 检查邮箱是否已存在
        // 2. 加密密码
        // 3. 保存用户
        // 4. 返回结果
    }
}
```

#### Controller（控制器）
- **作用**：接收HTTP请求，调用Service，返回响应
- **例子**：UserController处理用户相关的API请求
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        User user = userService.registerUser(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(user);
    }
}
```

### 1.3 为什么要分层？

**好处**：
- **职责分离**：每一层只负责自己的事情
- **易于维护**：修改一层不影响其他层
- **可测试性**：可以单独测试每一层
- **可复用性**：Service可以被多个Controller使用

---

## 2. 开发环境搭建

### 2.1 必需软件安装

#### Java 17
```bash
# Windows: 从Oracle官网下载JDK 17
# macOS: 使用Homebrew
brew install openjdk@17

# 验证安装
java -version
```

#### IntelliJ IDEA
1. 从JetBrains官网下载Community版本（免费）
2. 安装时选择Java开发

#### PostgreSQL数据库
```bash
# Windows: 从官网下载安装包
# macOS: 使用Homebrew
brew install postgresql
brew services start postgresql

# 创建数据库
createdb mini_ups_tutorial
```

#### Postman
- 从官网下载安装，用于API测试

### 2.2 IntelliJ IDEA配置

#### 安装插件
1. File -> Settings -> Plugins
2. 搜索并安装：
   - **Lombok**：减少重复代码
   - **Database Navigator**：数据库管理
   - **REST Client**：API测试

#### 配置JDK
1. File -> Project Structure -> SDKs
2. 添加Java 17的路径

---

## 3. 项目初始化

### 3.1 使用Spring Initializr创建项目

1. 打开 https://start.spring.io/
2. 配置项目：
   ```
   Project: Maven
   Language: Java
   Spring Boot: 3.2.0
   Group: com.tutorial
   Artifact: ups-system
   Name: UPS Tutorial System
   Package name: com.tutorial.ups
   Packaging: Jar
   Java: 17
   ```

3. 添加依赖（Dependencies）：
   - **Spring Web**：Web开发
   - **Spring Data JPA**：数据库操作
   - **Spring Security**：安全认证
   - **PostgreSQL Driver**：数据库驱动
   - **Validation**：数据验证
   - **Spring Boot DevTools**：开发工具

4. 点击"Generate"下载项目

### 3.2 导入项目到IntelliJ

1. 解压下载的zip文件
2. IntelliJ IDEA -> Open -> 选择项目文件夹
3. 等待Maven下载依赖（右下角有进度条）

### 3.3 项目结构解析

```
ups-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.tutorial.ups/
│   │   │       ├── UpsSystemApplication.java  # 主启动类
│   │   │       ├── controller/                # 控制器层
│   │   │       ├── service/                   # 业务逻辑层
│   │   │       ├── repository/                # 数据访问层
│   │   │       ├── model/                     # 数据模型
│   │   │       │   ├── entity/                # 实体类
│   │   │       │   ├── dto/                   # 数据传输对象
│   │   │       │   └── enums/                 # 枚举类
│   │   │       ├── config/                    # 配置类
│   │   │       └── security/                  # 安全相关
│   │   └── resources/
│   │       ├── application.yml                # 配置文件
│   │       └── static/                        # 静态资源
│   └── test/                                  # 测试代码
├── pom.xml                                    # Maven配置
└── README.md
```

**为什么这样组织？**
- **按功能分层**：每个包负责不同的职责
- **易于查找**：知道功能就知道在哪个包
- **团队协作**：多人开发时不会冲突

### 3.4 配置application.yml

创建`src/main/resources/application.yml`：

```yaml
# 应用基本配置
spring:
  application:
    name: ups-tutorial-system
  
  # 数据库配置
  datasource:
    url: jdbc:postgresql://localhost:5432/mini_ups_tutorial
    username: postgres
    password: your_password  # 改成你的密码
    driver-class-name: org.postgresql.Driver
  
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: create-drop  # 开发阶段用，会自动创建表
    show-sql: true          # 显示SQL语句，方便调试
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true    # 格式化SQL语句
  
  # 开发工具配置
  devtools:
    restart:
      enabled: true         # 代码改动后自动重启

# 服务器配置
server:
  port: 8080               # 应用运行端口

# 日志配置
logging:
  level:
    com.tutorial.ups: DEBUG # 我们项目的日志级别
    org.springframework.security: DEBUG  # 安全组件日志
```

**配置说明**：
- `ddl-auto: create-drop`：每次启动都重新创建表，适合开发阶段
- `show-sql: true`：在控制台显示执行的SQL语句
- `port: 8080`：应用在8080端口运行

---

## 4. 数据库设计

### 4.1 分析业务需求

我们要构建一个物流系统，需要这些核心功能：
- **用户管理**：注册、登录、用户信息
- **包裹管理**：创建包裹、追踪状态
- **运输管理**：卡车、司机、配送

### 4.2 设计数据表

#### 用户表 (users)
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### 包裹表 (packages)
```sql
CREATE TABLE packages (
    id BIGSERIAL PRIMARY KEY,
    tracking_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    recipient_name VARCHAR(100) NOT NULL,
    recipient_address TEXT NOT NULL,
    recipient_phone VARCHAR(20),
    weight DECIMAL(10,2),
    dimensions VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### 卡车表 (trucks)
```sql
CREATE TABLE trucks (
    id BIGSERIAL PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    capacity DECIMAL(10,2),
    current_location_x INTEGER,
    current_location_y INTEGER,
    status VARCHAR(30) NOT NULL DEFAULT 'IDLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 4.3 创建基础实体类

#### BaseEntity（基础实体）
所有实体类的父类，包含公共字段：

```java
package com.tutorial.ups.model.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 基础实体类
 * 
 * 说明：所有实体类的父类，包含公共字段
 * - id: 主键，自动生成
 * - createdAt: 创建时间，自动设置
 * - updatedAt: 更新时间，自动更新
 * 
 * 为什么要这样设计？
 * 1. 避免重复代码：每个表都需要这些字段
 * 2. 审计功能：知道数据什么时候创建和修改
 * 3. 统一标准：所有实体都有相同的基础结构
 */
@MappedSuperclass  // 标记为父类，不会创建对应的表
@EntityListeners(AuditingEntityListener.class)  // 启用审计功能
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 主键自动生成
    private Long id;
    
    @CreatedDate  // 创建时自动设置
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate  // 更新时自动设置
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 在持久化之前设置创建时间
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    // 在更新之前设置更新时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

#### 启用JPA审计
在主启动类上添加注解：

```java
package com.tutorial.ups;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // 启用JPA审计功能
public class UpsSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(UpsSystemApplication.class, args);
    }
}
```

### 4.4 创建枚举类

#### UserRole（用户角色）
```java
package com.tutorial.ups.model.enums;

/**
 * 用户角色枚举
 * 
 * 说明：定义系统中的用户角色类型
 * - USER: 普通用户，可以寄送和接收包裹
 * - ADMIN: 系统管理员，拥有所有权限
 * - DRIVER: 司机，负责配送包裹
 * - OPERATOR: 操作员，处理订单和客服
 * 
 * 为什么用枚举？
 * 1. 类型安全：避免错误的字符串值
 * 2. 代码清晰：一目了然有哪些角色
 * 3. IDE支持：自动补全和检查
 */
public enum UserRole {
    USER("普通用户"),
    ADMIN("管理员"),
    DRIVER("司机"),
    OPERATOR("操作员");
    
    private final String description;
    
    UserRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

#### PackageStatus（包裹状态）
```java
package com.tutorial.ups.model.enums;

/**
 * 包裹状态枚举
 * 
 * 说明：定义包裹在物流过程中的各个状态
 * 完整流程：CREATED -> PICKED_UP -> IN_TRANSIT -> OUT_FOR_DELIVERY -> DELIVERED
 */
public enum PackageStatus {
    CREATED("已创建"),
    PICKED_UP("已取件"),
    IN_TRANSIT("运输中"),
    OUT_FOR_DELIVERY("配送中"),
    DELIVERED("已送达"),
    CANCELLED("已取消"),
    LOST("丢失"),
    RETURNED("已退回");
    
    private final String description;
    
    PackageStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

---

## 5. 用户模块开发

### 5.1 创建User实体类

```java
package com.tutorial.ups.model.entity;

import com.tutorial.ups.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户实体类
 * 
 * 功能说明：
 * - 存储用户基本信息（用户名、邮箱、密码等）
 * - 管理用户角色和权限
 * - 与包裹建立关联关系
 * 
 * 数据验证：
 * - 用户名：3-50字符，必填，唯一
 * - 邮箱：有效邮箱格式，必填，唯一
 * - 密码：8-255字符，必填
 * 
 * 数据库映射：
 * - 表名：users
 * - 索引：email、username（提高查询性能）
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_username", columnList = "username")
})
public class User extends BaseEntity {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100字符")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 255, message = "密码长度必须在8-255字符之间")
    @Column(name = "password", nullable = false)
    private String password;
    
    @Size(max = 50, message = "名字长度不能超过50字符")
    @Column(name = "first_name", length = 50)
    private String firstName;
    
    @Size(max = 50, message = "姓氏长度不能超过50字符")
    @Column(name = "last_name", length = 50)
    private String lastName;
    
    @Pattern(regexp = "^[\\d-+()\\s]*$", message = "电话号码格式不正确")
    @Size(max = 20, message = "电话号码长度不能超过20字符")
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    @Enumerated(EnumType.STRING)  // 以字符串形式存储枚举
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;  // 默认为普通用户
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;  // 默认启用
    
    // 一对多关系：一个用户可以有多个包裹
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Package> packages = new ArrayList<>();
    
    // 构造函数
    public User() {}
    
    public User(String username, String email, String password, UserRole role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public List<Package> getPackages() {
        return packages;
    }
    
    public void setPackages(List<Package> packages) {
        this.packages = packages;
    }
    
    /**
     * 获取用户全名
     * 优先级：firstName + lastName > firstName > lastName > username
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }
    
    /**
     * 检查用户是否为管理员
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }
    
    /**
     * 检查用户是否为司机
     */
    public boolean isDriver() {
        return UserRole.DRIVER.equals(this.role);
    }
}
```

### 5.2 创建UserRepository

```java
package com.tutorial.ups.repository;

import com.tutorial.ups.model.entity.User;
import com.tutorial.ups.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 * 
 * 功能说明：
 * - 继承JpaRepository，自动获得基本的CRUD操作
 * - 定义自定义查询方法
 * - 支持分页和排序
 * 
 * 方法命名规则：
 * - findBy...：查询方法
 * - existsBy...：检查是否存在
 * - countBy...：统计数量
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据邮箱查找用户
     * 用途：登录验证、检查邮箱是否已注册
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据用户名查找用户
     * 用途：登录验证、检查用户名是否已存在
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 检查邮箱是否已存在
     * 用途：注册时验证邮箱唯一性
     */
    boolean existsByEmail(String email);
    
    /**
     * 检查用户名是否已存在
     * 用途：注册时验证用户名唯一性
     */
    boolean existsByUsername(String username);
    
    /**
     * 根据角色查找用户列表
     * 用途：获取所有司机、管理员等
     */
    List<User> findByRole(UserRole role);
    
    /**
     * 根据启用状态查找用户
     * 用途：获取所有活跃用户或被禁用用户
     */
    Page<User> findByEnabled(Boolean enabled, Pageable pageable);
    
    /**
     * 根据角色和启用状态查找用户
     * 用途：获取活跃的司机用户
     */
    List<User> findByRoleAndEnabled(UserRole role, Boolean enabled);
    
    /**
     * 模糊查询用户（用户名或邮箱）
     * 用途：用户搜索功能
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 统计各角色用户数量
     * 用途：数据统计
     */
    @Query("SELECT u.role, COUNT(u) FROM User u WHERE u.enabled = true GROUP BY u.role")
    List<Object[]> countUsersByRole();
}
```

**Repository解释**：
- `JpaRepository<User, Long>`：自动提供基本的CRUD操作
- `Optional<User>`：避免空指针异常的安全方式
- `@Query`：自定义SQL查询
- `Pageable`：分页查询支持

### 5.3 创建数据传输对象（DTO）

#### 注册请求DTO
```java
package com.tutorial.ups.model.dto;

import jakarta.validation.constraints.*;

/**
 * 用户注册请求DTO
 * 
 * 说明：用于接收前端注册请求的数据
 * 为什么不直接用User实体？
 * 1. 安全性：避免暴露内部字段（如id、createdAt等）
 * 2. 灵活性：可以包含确认密码等字段
 * 3. 验证：针对性的验证规则
 */
public class RegisterRequestDto {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度必须在8-50字符之间")
    private String password;
    
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    
    // 构造函数
    public RegisterRequestDto() {}
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    /**
     * 验证两次密码是否一致
     */
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
```

#### 登录请求DTO
```java
package com.tutorial.ups.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 */
public class LoginRequestDto {
    
    @NotBlank(message = "邮箱不能为空")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    // 构造函数
    public LoginRequestDto() {}
    
    public LoginRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    // Getters and Setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}
```

#### 用户响应DTO
```java
package com.tutorial.ups.model.dto;

import com.tutorial.ups.model.entity.User;
import com.tutorial.ups.model.enums.UserRole;

import java.time.LocalDateTime;

/**
 * 用户响应DTO
 * 
 * 说明：返回给前端的用户信息
 * 注意：不包含密码等敏感信息
 */
public class UserResponseDto {
    
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private UserRole role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    
    // 构造函数
    public UserResponseDto() {}
    
    /**
     * 从User实体创建DTO
     */
    public static UserResponseDto fromEntity(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setRole(user.getRole());
        dto.setEnabled(user.getEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
```

### 5.4 创建UserService

```java
package com.tutorial.ups.service;

import com.tutorial.ups.model.dto.RegisterRequestDto;
import com.tutorial.ups.model.dto.UserResponseDto;
import com.tutorial.ups.model.entity.User;
import com.tutorial.ups.model.enums.UserRole;
import com.tutorial.ups.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户业务逻辑服务
 * 
 * 功能说明：
 * - 处理用户注册、登录、信息管理等业务逻辑
 * - 数据验证和业务规则检查
 * - 与数据库交互（通过Repository）
 * 
 * 注解说明：
 * - @Service：标记为业务逻辑层组件
 * - @Transactional：启用事务管理
 * - @Autowired：依赖注入
 */
@Service
@Transactional  // 类级别事务，所有方法都在事务中执行
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 构造函数注入（推荐方式）
     * 好处：
     * 1. 依赖明确：一目了然需要哪些依赖
     * 2. 不可变：依赖注入后不能修改
     * 3. 测试友好：可以传入mock对象
     */
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * 用户注册
     * 
     * 业务流程：
     * 1. 验证用户名和邮箱唯一性
     * 2. 验证密码确认
     * 3. 加密密码
     * 4. 保存用户
     * 5. 返回用户信息
     */
    public UserResponseDto registerUser(RegisterRequestDto request) {
        // 1. 业务验证
        validateRegistration(request);
        
        // 2. 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // 密码加密
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(UserRole.USER);  // 默认为普通用户
        user.setEnabled(true);
        
        // 3. 保存到数据库
        User savedUser = userRepository.save(user);
        
        // 4. 转换为DTO返回
        return UserResponseDto.fromEntity(savedUser);
    }
    
    /**
     * 验证注册信息
     */
    private void validateRegistration(RegisterRequestDto request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在：" + request.getUsername());
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("邮箱已被注册：" + request.getEmail());
        }
        
        // 检查密码确认
        if (!request.isPasswordConfirmed()) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
    }
    
    /**
     * 根据邮箱查找用户
     * 用途：登录验证
     */
    @Transactional(readOnly = true)  // 只读事务，提高性能
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * 根据ID查找用户
     */
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponseDto::fromEntity);
    }
    
    /**
     * 获取所有用户（分页）
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDto> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponseDto::fromEntity);
    }
    
    /**
     * 搜索用户
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDto> searchUsers(String keyword, Pageable pageable) {
        return userRepository.searchUsers(keyword, pageable)
                .map(UserResponseDto::fromEntity);
    }
    
    /**
     * 根据角色查找用户
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> findByRole(UserRole role) {
        return userRepository.findByRole(role)
                .stream()
                .map(UserResponseDto::fromEntity)
                .toList();
    }
    
    /**
     * 启用/禁用用户
     */
    public UserResponseDto toggleUserEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));
        
        user.setEnabled(!user.getEnabled());
        User savedUser = userRepository.save(user);
        
        return UserResponseDto.fromEntity(savedUser);
    }
    
    /**
     * 更新用户信息
     */
    public UserResponseDto updateUser(Long userId, RegisterRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));
        
        // 检查用户名是否被其他用户使用
        if (!user.getUsername().equals(request.getUsername()) 
            && userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在：" + request.getUsername());
        }
        
        // 检查邮箱是否被其他用户使用
        if (!user.getEmail().equals(request.getEmail()) 
            && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("邮箱已被注册：" + request.getEmail());
        }
        
        // 更新用户信息
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        
        // 如果提供了新密码，则更新密码
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (!request.isPasswordConfirmed()) {
                throw new IllegalArgumentException("两次输入的密码不一致");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        User savedUser = userRepository.save(user);
        return UserResponseDto.fromEntity(savedUser);
    }
    
    /**
     * 删除用户（软删除）
     */
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));
        
        user.setEnabled(false);  // 软删除：设置为禁用状态
        userRepository.save(user);
    }
}
```

**Service层解释**：
- `@Transactional`：确保数据一致性，出错时自动回滚
- `PasswordEncoder`：密码加密，保护用户安全
- 业务验证：在保存数据前检查业务规则
- DTO转换：保护内部数据结构

### 5.5 创建UserController

```java
package com.tutorial.ups.controller;

import com.tutorial.ups.model.dto.LoginRequestDto;
import com.tutorial.ups.model.dto.RegisterRequestDto;
import com.tutorial.ups.model.dto.UserResponseDto;
import com.tutorial.ups.model.enums.UserRole;
import com.tutorial.ups.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 * 
 * 功能说明：
 * - 处理用户相关的HTTP请求
 * - 调用Service层处理业务逻辑
 * - 返回标准的HTTP响应
 * 
 * 注解说明：
 * - @RestController：标记为REST控制器，自动将返回值转为JSON
 * - @RequestMapping：定义基础路径
 * - @PostMapping、@GetMapping等：定义HTTP方法和路径
 * - @Valid：启用参数验证
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * 用户注册
     * 
     * URL: POST /api/users/register
     * 请求体: RegisterRequestDto (JSON格式)
     * 返回: UserResponseDto
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        try {
            UserResponseDto user = userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取用户信息
     * 
     * URL: GET /api/users/{id}
     * 路径参数: id (用户ID)
     * 返回: UserResponseDto
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取所有用户（分页）
     * 
     * URL: GET /api/users?page=0&size=10&sort=createdAt,desc
     * 查询参数:
     * - page: 页码（从0开始）
     * - size: 每页大小
     * - sort: 排序字段和方向
     */
    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        // 创建排序对象
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );
        
        // 创建分页对象
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserResponseDto> users = userService.findAllUsers(pageable);
        return ResponseEntity.ok(users);
    }
    
    /**
     * 搜索用户
     * 
     * URL: GET /api/users/search?keyword=john&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponseDto> users = userService.searchUsers(keyword, pageable);
        return ResponseEntity.ok(users);
    }
    
    /**
     * 根据角色获取用户
     * 
     * URL: GET /api/users/by-role?role=DRIVER
     */
    @GetMapping("/by-role")
    public ResponseEntity<List<UserResponseDto>> getUsersByRole(@RequestParam UserRole role) {
        List<UserResponseDto> users = userService.findByRole(role);
        return ResponseEntity.ok(users);
    }
    
    /**
     * 更新用户信息
     * 
     * URL: PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody RegisterRequestDto request) {
        try {
            UserResponseDto user = userService.updateUser(id, request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 启用/禁用用户
     * 
     * URL: PATCH /api/users/{id}/toggle-enabled
     */
    @PatchMapping("/{id}/toggle-enabled")
    public ResponseEntity<UserResponseDto> toggleUserEnabled(@PathVariable Long id) {
        try {
            UserResponseDto user = userService.toggleUserEnabled(id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除用户（软删除）
     * 
     * URL: DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

**Controller解释**：
- `@Valid`：自动验证请求参数
- `ResponseEntity`：标准HTTP响应，包含状态码和数据
- 路径参数(`@PathVariable`)：从URL获取参数
- 查询参数(`@RequestParam`)：从URL查询字符串获取参数
- 异常处理：捕获业务异常并返回适当的HTTP状态码

### 5.6 测试用户模块

#### 启动应用
1. 在IntelliJ中右键点击`UpsSystemApplication.java`
2. 选择"Run 'UpsSystemApplication'"
3. 看到控制台输出"Started UpsSystemApplication"表示启动成功
4. 浏览器访问 http://localhost:8080 确认服务器运行

#### 使用Postman测试

**1. 测试用户注册**
```
POST http://localhost:8080/api/users/register
Content-Type: application/json

{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "password123",
    "confirmPassword": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "123-456-7890",
    "address": "123 Main St, New York, NY"
}
```

**2. 测试获取用户信息**
```
GET http://localhost:8080/api/users/1
```

**3. 测试获取所有用户**
```
GET http://localhost:8080/api/users?page=0&size=5&sort=createdAt,desc
```

**4. 测试搜索用户**
```
GET http://localhost:8080/api/users/search?keyword=john
```

#### 在IntelliJ中使用HTTP Client

创建文件`test-user-api.http`：
```http
### 注册新用户
POST http://localhost:8080/api/users/register
Content-Type: application/json

{
    "username": "jane_smith",
    "email": "jane@example.com",
    "password": "password123",
    "confirmPassword": "password123",
    "firstName": "Jane",
    "lastName": "Smith"
}

### 获取用户信息
GET http://localhost:8080/api/users/1

### 获取所有用户
GET http://localhost:8080/api/users

### 搜索用户
GET http://localhost:8080/api/users/search?keyword=jane
```

点击每个请求旁边的绿色箭头即可执行。

---

## 6. 包裹模块开发

### 6.1 创建Package实体类

```java
package com.tutorial.ups.model.entity;

import com.tutorial.ups.model.enums.PackageStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 包裹实体类
 * 
 * 功能说明：
 * - 存储包裹的基本信息（收件人、地址、重量等）
 * - 管理包裹状态的变化
 * - 与用户建立多对一关系
 * 
 * 业务规则：
 * - 每个包裹有唯一的追踪号码
 * - 包裹必须属于某个用户
 * - 包裹状态按照固定流程变化
 */
@Entity
@Table(name = "packages", indexes = {
    @Index(name = "idx_package_tracking_number", columnList = "trackingNumber"),
    @Index(name = "idx_package_user", columnList = "user_id"),
    @Index(name = "idx_package_status", columnList = "status")
})
public class Package extends BaseEntity {
    
    @NotBlank(message = "追踪号码不能为空")
    @Size(max = 50, message = "追踪号码长度不能超过50字符")
    @Column(name = "tracking_number", nullable = false, unique = true, length = 50)
    private String trackingNumber;
    
    @NotBlank(message = "收件人姓名不能为空")
    @Size(max = 100, message = "收件人姓名长度不能超过100字符")
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;
    
    @NotBlank(message = "收件人地址不能为空")
    @Column(name = "recipient_address", nullable = false, columnDefinition = "TEXT")
    private String recipientAddress;
    
    @Pattern(regexp = "^[\\d-+()\\s]*$", message = "电话号码格式不正确")
    @Size(max = 20, message = "电话号码长度不能超过20字符")
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "重量必须大于0")
    @DecimalMax(value = "1000.0", message = "重量不能超过1000kg")
    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;
    
    @Size(max = 100, message = "尺寸描述长度不能超过100字符")
    @Column(name = "dimensions", length = 100)
    private String dimensions;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @DecimalMin(value = "0.0", message = "配送坐标X必须大于等于0")
    @Column(name = "delivery_x")
    private Integer deliveryX;
    
    @DecimalMin(value = "0.0", message = "配送坐标Y必须大于等于0")
    @Column(name = "delivery_y")
    private Integer deliveryY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PackageStatus status = PackageStatus.CREATED;
    
    // 多对一关系：多个包裹属于一个用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // 构造函数
    public Package() {}
    
    public Package(String trackingNumber, User user, String recipientName, String recipientAddress) {
        this.trackingNumber = trackingNumber;
        this.user = user;
        this.recipientName = recipientName;
        this.recipientAddress = recipientAddress;
    }
    
    // Getters and Setters
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public String getRecipientName() {
        return recipientName;
    }
    
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }
    
    public String getRecipientAddress() {
        return recipientAddress;
    }
    
    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }
    
    public String getRecipientPhone() {
        return recipientPhone;
    }
    
    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public String getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getDeliveryX() {
        return deliveryX;
    }
    
    public void setDeliveryX(Integer deliveryX) {
        this.deliveryX = deliveryX;
    }
    
    public Integer getDeliveryY() {
        return deliveryY;
    }
    
    public void setDeliveryY(Integer deliveryY) {
        this.deliveryY = deliveryY;
    }
    
    public PackageStatus getStatus() {
        return status;
    }
    
    public void setStatus(PackageStatus status) {
        this.status = status;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    /**
     * 检查包裹是否可以更新状态
     */
    public boolean canUpdateStatus(PackageStatus newStatus) {
        if (this.status == PackageStatus.DELIVERED || this.status == PackageStatus.CANCELLED) {
            return false; // 已送达或已取消的包裹不能更新状态
        }
        return true;
    }
    
    /**
     * 获取配送地址坐标
     */
    public String getDeliveryCoordinates() {
        if (deliveryX != null && deliveryY != null) {
            return String.format("(%d, %d)", deliveryX, deliveryY);
        }
        return null;
    }
}
```

### 6.2 创建PackageRepository

```java
package com.tutorial.ups.repository;

import com.tutorial.ups.model.entity.Package;
import com.tutorial.ups.model.entity.User;
import com.tutorial.ups.model.enums.PackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 包裹数据访问层
 */
@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {
    
    /**
     * 根据追踪号码查找包裹
     */
    Optional<Package> findByTrackingNumber(String trackingNumber);
    
    /**
     * 检查追踪号码是否已存在
     */
    boolean existsByTrackingNumber(String trackingNumber);
    
    /**
     * 根据用户查找包裹
     */
    Page<Package> findByUser(User user, Pageable pageable);
    
    /**
     * 根据用户ID查找包裹
     */
    Page<Package> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据状态查找包裹
     */
    List<Package> findByStatus(PackageStatus status);
    
    /**
     * 根据用户和状态查找包裹
     */
    List<Package> findByUserAndStatus(User user, PackageStatus status);
    
    /**
     * 根据时间范围查找包裹
     */
    @Query("SELECT p FROM Package p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Package> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * 统计各状态包裹数量
     */
    @Query("SELECT p.status, COUNT(p) FROM Package p GROUP BY p.status")
    List<Object[]> countPackagesByStatus();
    
    /**
     * 统计用户的包裹数量
     */
    long countByUser(User user);
    
    /**
     * 查找需要配送的包裹（已取件状态）
     */
    @Query("SELECT p FROM Package p WHERE p.status = 'PICKED_UP' ORDER BY p.createdAt")
    List<Package> findPackagesReadyForDelivery();
    
    /**
     * 搜索包裹（追踪号码或收件人姓名）
     */
    @Query("SELECT p FROM Package p WHERE " +
           "LOWER(p.trackingNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Package> searchPackages(@Param("keyword") String keyword, Pageable pageable);
}
```

### 6.3 创建包裹相关DTO

#### 创建包裹请求DTO
```java
package com.tutorial.ups.model.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 创建包裹请求DTO
 */
public class CreatePackageRequestDto {
    
    @NotBlank(message = "收件人姓名不能为空")
    @Size(max = 100, message = "收件人姓名长度不能超过100字符")
    private String recipientName;
    
    @NotBlank(message = "收件人地址不能为空")
    private String recipientAddress;
    
    @Pattern(regexp = "^[\\d-+()\\s]*$", message = "电话号码格式不正确")
    private String recipientPhone;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "重量必须大于0")
    @DecimalMax(value = "1000.0", message = "重量不能超过1000kg")
    private BigDecimal weight;
    
    private String dimensions;
    private String description;
    
    @Min(value = 0, message = "配送坐标X必须大于等于0")
    private Integer deliveryX;
    
    @Min(value = 0, message = "配送坐标Y必须大于等于0")
    private Integer deliveryY;
    
    // 构造函数
    public CreatePackageRequestDto() {}
    
    // Getters and Setters
    public String getRecipientName() {
        return recipientName;
    }
    
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }
    
    public String getRecipientAddress() {
        return recipientAddress;
    }
    
    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }
    
    public String getRecipientPhone() {
        return recipientPhone;
    }
    
    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public String getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getDeliveryX() {
        return deliveryX;
    }
    
    public void setDeliveryX(Integer deliveryX) {
        this.deliveryX = deliveryX;
    }
    
    public Integer getDeliveryY() {
        return deliveryY;
    }
    
    public void setDeliveryY(Integer deliveryY) {
        this.deliveryY = deliveryY;
    }
}
```

#### 包裹响应DTO
```java
package com.tutorial.ups.model.dto;

import com.tutorial.ups.model.entity.Package;
import com.tutorial.ups.model.enums.PackageStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 包裹响应DTO
 */
public class PackageResponseDto {
    
    private Long id;
    private String trackingNumber;
    private String recipientName;
    private String recipientAddress;
    private String recipientPhone;
    private BigDecimal weight;
    private String dimensions;
    private String description;
    private Integer deliveryX;
    private Integer deliveryY;
    private PackageStatus status;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 构造函数
    public PackageResponseDto() {}
    
    /**
     * 从Package实体创建DTO
     */
    public static PackageResponseDto fromEntity(Package packageEntity) {
        PackageResponseDto dto = new PackageResponseDto();
        dto.setId(packageEntity.getId());
        dto.setTrackingNumber(packageEntity.getTrackingNumber());
        dto.setRecipientName(packageEntity.getRecipientName());
        dto.setRecipientAddress(packageEntity.getRecipientAddress());
        dto.setRecipientPhone(packageEntity.getRecipientPhone());
        dto.setWeight(packageEntity.getWeight());
        dto.setDimensions(packageEntity.getDimensions());
        dto.setDescription(packageEntity.getDescription());
        dto.setDeliveryX(packageEntity.getDeliveryX());
        dto.setDeliveryY(packageEntity.getDeliveryY());
        dto.setStatus(packageEntity.getStatus());
        dto.setCreatedAt(packageEntity.getCreatedAt());
        dto.setUpdatedAt(packageEntity.getUpdatedAt());
        
        if (packageEntity.getUser() != null) {
            dto.setUserId(packageEntity.getUser().getId());
            dto.setUserName(packageEntity.getUser().getUsername());
        }
        
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public String getRecipientName() {
        return recipientName;
    }
    
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }
    
    public String getRecipientAddress() {
        return recipientAddress;
    }
    
    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }
    
    public String getRecipientPhone() {
        return recipientPhone;
    }
    
    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public String getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getDeliveryX() {
        return deliveryX;
    }
    
    public void setDeliveryX(Integer deliveryX) {
        this.deliveryX = deliveryX;
    }
    
    public Integer getDeliveryY() {
        return deliveryY;
    }
    
    public void setDeliveryY(Integer deliveryY) {
        this.deliveryY = deliveryY;
    }
    
    public PackageStatus getStatus() {
        return status;
    }
    
    public void setStatus(PackageStatus status) {
        this.status = status;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 获取配送坐标字符串
     */
    public String getDeliveryCoordinates() {
        if (deliveryX != null && deliveryY != null) {
            return String.format("(%d, %d)", deliveryX, deliveryY);
        }
        return null;
    }
}
```

### 6.4 创建PackageService

```java
package com.tutorial.ups.service;

import com.tutorial.ups.model.dto.CreatePackageRequestDto;
import com.tutorial.ups.model.dto.PackageResponseDto;
import com.tutorial.ups.model.entity.Package;
import com.tutorial.ups.model.entity.User;
import com.tutorial.ups.model.enums.PackageStatus;
import com.tutorial.ups.repository.PackageRepository;
import com.tutorial.ups.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 包裹业务逻辑服务
 */
@Service
@Transactional
public class PackageService {
    
    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public PackageService(PackageRepository packageRepository, UserRepository userRepository) {
        this.packageRepository = packageRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * 创建包裹
     */
    public PackageResponseDto createPackage(Long userId, CreatePackageRequestDto request) {
        // 1. 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));
        
        // 2. 生成唯一的追踪号码
        String trackingNumber = generateTrackingNumber();
        
        // 3. 创建包裹实体
        Package packageEntity = new Package();
        packageEntity.setTrackingNumber(trackingNumber);
        packageEntity.setUser(user);
        packageEntity.setRecipientName(request.getRecipientName());
        packageEntity.setRecipientAddress(request.getRecipientAddress());
        packageEntity.setRecipientPhone(request.getRecipientPhone());
        packageEntity.setWeight(request.getWeight());
        packageEntity.setDimensions(request.getDimensions());
        packageEntity.setDescription(request.getDescription());
        packageEntity.setDeliveryX(request.getDeliveryX());
        packageEntity.setDeliveryY(request.getDeliveryY());
        packageEntity.setStatus(PackageStatus.CREATED);
        
        // 4. 保存到数据库
        Package savedPackage = packageRepository.save(packageEntity);
        
        return PackageResponseDto.fromEntity(savedPackage);
    }
    
    /**
     * 生成唯一的追踪号码
     * 格式：UPS + 年月日 + 随机6位数字
     * 例如：UPS20241215123456
     */
    private String generateTrackingNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        int randomPart = 100000 + random.nextInt(900000); // 6位随机数
        
        String trackingNumber;
        do {
            trackingNumber = "UPS" + datePart + randomPart;
            randomPart = 100000 + random.nextInt(900000); // 如果重复则重新生成
        } while (packageRepository.existsByTrackingNumber(trackingNumber));
        
        return trackingNumber;
    }
    
    /**
     * 根据追踪号码查找包裹
     */
    @Transactional(readOnly = true)
    public Optional<PackageResponseDto> findByTrackingNumber(String trackingNumber) {
        return packageRepository.findByTrackingNumber(trackingNumber)
                .map(PackageResponseDto::fromEntity);
    }
    
    /**
     * 根据ID查找包裹
     */
    @Transactional(readOnly = true)
    public Optional<PackageResponseDto> findById(Long id) {
        return packageRepository.findById(id)
                .map(PackageResponseDto::fromEntity);
    }
    
    /**
     * 获取用户的所有包裹
     */
    @Transactional(readOnly = true)
    public Page<PackageResponseDto> findUserPackages(Long userId, Pageable pageable) {
        return packageRepository.findByUserId(userId, pageable)
                .map(PackageResponseDto::fromEntity);
    }
    
    /**
     * 获取所有包裹
     */
    @Transactional(readOnly = true)
    public Page<PackageResponseDto> findAllPackages(Pageable pageable) {
        return packageRepository.findAll(pageable)
                .map(PackageResponseDto::fromEntity);
    }
    
    /**
     * 搜索包裹
     */
    @Transactional(readOnly = true)
    public Page<PackageResponseDto> searchPackages(String keyword, Pageable pageable) {
        return packageRepository.searchPackages(keyword, pageable)
                .map(PackageResponseDto::fromEntity);
    }
    
    /**
     * 根据状态查找包裹
     */
    @Transactional(readOnly = true)
    public List<PackageResponseDto> findByStatus(PackageStatus status) {
        return packageRepository.findByStatus(status)
                .stream()
                .map(PackageResponseDto::fromEntity)
                .toList();
    }
    
    /**
     * 更新包裹状态
     */
    public PackageResponseDto updatePackageStatus(Long packageId, PackageStatus newStatus) {
        Package packageEntity = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("包裹不存在：" + packageId));
        
        // 检查是否可以更新状态
        if (!packageEntity.canUpdateStatus(newStatus)) {
            throw new IllegalStateException("包裹状态不允许更新：当前状态为 " + packageEntity.getStatus());
        }
        
        packageEntity.setStatus(newStatus);
        Package savedPackage = packageRepository.save(packageEntity);
        
        return PackageResponseDto.fromEntity(savedPackage);
    }
    
    /**
     * 更新包裹信息
     */
    public PackageResponseDto updatePackage(Long packageId, CreatePackageRequestDto request) {
        Package packageEntity = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("包裹不存在：" + packageId));
        
        // 只有在创建状态下才能修改包裹信息
        if (packageEntity.getStatus() != PackageStatus.CREATED) {
            throw new IllegalStateException("只有在创建状态下才能修改包裹信息");
        }
        
        // 更新包裹信息
        packageEntity.setRecipientName(request.getRecipientName());
        packageEntity.setRecipientAddress(request.getRecipientAddress());
        packageEntity.setRecipientPhone(request.getRecipientPhone());
        packageEntity.setWeight(request.getWeight());
        packageEntity.setDimensions(request.getDimensions());
        packageEntity.setDescription(request.getDescription());
        packageEntity.setDeliveryX(request.getDeliveryX());
        packageEntity.setDeliveryY(request.getDeliveryY());
        
        Package savedPackage = packageRepository.save(packageEntity);
        return PackageResponseDto.fromEntity(savedPackage);
    }
    
    /**
     * 取消包裹
     */
    public PackageResponseDto cancelPackage(Long packageId) {
        Package packageEntity = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("包裹不存在：" + packageId));
        
        // 检查是否可以取消
        if (packageEntity.getStatus() == PackageStatus.DELIVERED) {
            throw new IllegalStateException("已送达的包裹不能取消");
        }
        
        if (packageEntity.getStatus() == PackageStatus.CANCELLED) {
            throw new IllegalStateException("包裹已经被取消");
        }
        
        packageEntity.setStatus(PackageStatus.CANCELLED);
        Package savedPackage = packageRepository.save(packageEntity);
        
        return PackageResponseDto.fromEntity(savedPackage);
    }
    
    /**
     * 获取包裹统计信息
     */
    @Transactional(readOnly = true)
    public List<Object[]> getPackageStatistics() {
        return packageRepository.countPackagesByStatus();
    }
    
    /**
     * 检查用户是否拥有指定包裹
     */
    @Transactional(readOnly = true)
    public boolean isPackageOwnedByUser(Long packageId, Long userId) {
        Optional<Package> packageOpt = packageRepository.findById(packageId);
        return packageOpt.isPresent() && packageOpt.get().getUser().getId().equals(userId);
    }
}
```

### 6.5 创建PackageController

```java
package com.tutorial.ups.controller;

import com.tutorial.ups.model.dto.CreatePackageRequestDto;
import com.tutorial.ups.model.dto.PackageResponseDto;
import com.tutorial.ups.model.enums.PackageStatus;
import com.tutorial.ups.service.PackageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 包裹控制器
 */
@RestController
@RequestMapping("/api/packages")
public class PackageController {
    
    private final PackageService packageService;
    
    @Autowired
    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }
    
    /**
     * 创建包裹
     * 
     * URL: POST /api/packages?userId=1
     */
    @PostMapping
    public ResponseEntity<PackageResponseDto> createPackage(
            @RequestParam Long userId,
            @Valid @RequestBody CreatePackageRequestDto request) {
        try {
            PackageResponseDto packageDto = packageService.createPackage(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(packageDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据追踪号码查找包裹
     * 
     * URL: GET /api/packages/track/{trackingNumber}
     */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<PackageResponseDto> trackPackage(@PathVariable String trackingNumber) {
        return packageService.findByTrackingNumber(trackingNumber)
                .map(packageDto -> ResponseEntity.ok(packageDto))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取包裹详情
     * 
     * URL: GET /api/packages/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PackageResponseDto> getPackageById(@PathVariable Long id) {
        return packageService.findById(id)
                .map(packageDto -> ResponseEntity.ok(packageDto))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取用户的包裹列表
     * 
     * URL: GET /api/packages/user/{userId}?page=0&size=10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PackageResponseDto>> getUserPackages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PackageResponseDto> packages = packageService.findUserPackages(userId, pageable);
        
        return ResponseEntity.ok(packages);
    }
    
    /**
     * 获取所有包裹
     * 
     * URL: GET /api/packages?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<PackageResponseDto>> getAllPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PackageResponseDto> packages = packageService.findAllPackages(pageable);
        
        return ResponseEntity.ok(packages);
    }
    
    /**
     * 搜索包裹
     * 
     * URL: GET /api/packages/search?keyword=john&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PackageResponseDto>> searchPackages(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PackageResponseDto> packages = packageService.searchPackages(keyword, pageable);
        
        return ResponseEntity.ok(packages);
    }
    
    /**
     * 根据状态获取包裹
     * 
     * URL: GET /api/packages/by-status?status=CREATED
     */
    @GetMapping("/by-status")
    public ResponseEntity<List<PackageResponseDto>> getPackagesByStatus(@RequestParam PackageStatus status) {
        List<PackageResponseDto> packages = packageService.findByStatus(status);
        return ResponseEntity.ok(packages);
    }
    
    /**
     * 更新包裹状态
     * 
     * URL: PATCH /api/packages/{id}/status?status=PICKED_UP
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<PackageResponseDto> updatePackageStatus(
            @PathVariable Long id,
            @RequestParam PackageStatus status) {
        try {
            PackageResponseDto packageDto = packageService.updatePackageStatus(id, status);
            return ResponseEntity.ok(packageDto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 更新包裹信息
     * 
     * URL: PUT /api/packages/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PackageResponseDto> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody CreatePackageRequestDto request) {
        try {
            PackageResponseDto packageDto = packageService.updatePackage(id, request);
            return ResponseEntity.ok(packageDto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 取消包裹
     * 
     * URL: DELETE /api/packages/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<PackageResponseDto> cancelPackage(@PathVariable Long id) {
        try {
            PackageResponseDto packageDto = packageService.cancelPackage(id);
            return ResponseEntity.ok(packageDto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取包裹统计信息
     * 
     * URL: GET /api/packages/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<Object[]>> getPackageStatistics() {
        List<Object[]> statistics = packageService.getPackageStatistics();
        return ResponseEntity.ok(statistics);
    }
}
```

### 6.6 测试包裹模块

#### 使用HTTP Client测试

创建文件`test-package-api.http`：

```http
### 创建用户（为了测试包裹）
POST http://localhost:8080/api/users/register
Content-Type: application/json

{
    "username": "test_user",
    "email": "test@example.com",
    "password": "password123",
    "confirmPassword": "password123",
    "firstName": "Test",
    "lastName": "User"
}

### 创建包裹
POST http://localhost:8080/api/packages?userId=1
Content-Type: application/json

{
    "recipientName": "张三",
    "recipientAddress": "北京市朝阳区建国路1号",
    "recipientPhone": "138-0000-1234",
    "weight": 2.5,
    "dimensions": "30x20x15cm",
    "description": "书籍",
    "deliveryX": 100,
    "deliveryY": 200
}

### 追踪包裹
GET http://localhost:8080/api/packages/track/UPS20241215123456

### 获取包裹详情
GET http://localhost:8080/api/packages/1

### 获取用户的包裹列表
GET http://localhost:8080/api/packages/user/1?page=0&size=5

### 搜索包裹
GET http://localhost:8080/api/packages/search?keyword=张三

### 根据状态查找包裹
GET http://localhost:8080/api/packages/by-status?status=CREATED

### 更新包裹状态
PATCH http://localhost:8080/api/packages/1/status?status=PICKED_UP

### 更新包裹信息
PUT http://localhost:8080/api/packages/1
Content-Type: application/json

{
    "recipientName": "李四",
    "recipientAddress": "上海市浦东新区张江路100号",
    "recipientPhone": "139-0000-5678",
    "weight": 3.0,
    "dimensions": "35x25x20cm",
    "description": "电子产品",
    "deliveryX": 150,
    "deliveryY": 250
}

### 获取包裹统计信息
GET http://localhost:8080/api/packages/statistics

### 取消包裹
DELETE http://localhost:8080/api/packages/1
```

#### Postman测试步骤

1. **创建包裹**：
   ```json
   POST /api/packages?userId=1
   {
       "recipientName": "王五",
       "recipientAddress": "广州市天河区珠江新城",
       "recipientPhone": "135-0000-9999",
       "weight": 1.8,
       "dimensions": "25x18x10cm",
       "description": "文件资料",
       "deliveryX": 80,
       "deliveryY": 120
   }
   ```

2. **追踪包裹**：
   ```
   GET /api/packages/track/{追踪号码}
   ```

3. **测试状态更新**：
   ```
   PATCH /api/packages/1/status?status=IN_TRANSIT
   ```

#### 验证结果

成功的响应应该包含：
- 正确的包裹信息
- 自动生成的追踪号码
- 当前状态
- 创建时间和更新时间

---

## 7. 卡车管理模块

### 7.1 创建TruckStatus枚举

```java
package com.tutorial.ups.model.enums;

/**
 * 卡车状态枚举
 * 
 * 说明：定义卡车在物流系统中的各种工作状态
 * - IDLE: 空闲状态，可以接受新任务
 * - BUSY: 忙碌状态，正在执行配送任务
 * - EN_ROUTE: 在途状态，前往目的地
 * - AT_WAREHOUSE: 在仓库状态，装载货物
 * - MAINTENANCE: 维修状态，暂时不可用
 * - OUT_OF_SERVICE: 停止服务，长期不可用
 */
public enum TruckStatus {
    IDLE("空闲"),
    BUSY("忙碌"),
    EN_ROUTE("在途"),
    AT_WAREHOUSE("在仓库"),
    MAINTENANCE("维修中"),
    OUT_OF_SERVICE("停止服务");
    
    private final String description;
    
    TruckStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 检查卡车是否可以接受新任务
     */
    public boolean isAvailable() {
        return this == IDLE;
    }
    
    /**
     * 检查卡车是否在工作中
     */
    public boolean isWorking() {
        return this == BUSY || this == EN_ROUTE || this == AT_WAREHOUSE;
    }
}
```

### 7.2 创建Truck实体类

```java
package com.tutorial.ups.model.entity;

import com.tutorial.ups.model.enums.TruckStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡车实体类
 * 
 * 功能说明：
 * - 管理运输车辆的基本信息和实时状态
 * - 记录卡车位置、载重能力、当前任务等
 * - 支持卡车调度和路径规划
 * 
 * 业务规则：
 * - 每辆卡车有唯一的车牌号
 * - 卡车状态决定是否可以分配新任务
 * - 位置信息用于距离计算和路径优化
 */
@Entity
@Table(name = "trucks", indexes = {
    @Index(name = "idx_truck_license_plate", columnList = "licensePlate"),
    @Index(name = "idx_truck_status", columnList = "status"),
    @Index(name = "idx_truck_location", columnList = "currentX, currentY")
})
public class Truck extends BaseEntity {
    
    @NotBlank(message = "车牌号不能为空")
    @Size(max = 20, message = "车牌号长度不能超过20字符")
    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TruckStatus status = TruckStatus.IDLE;
    
    @Min(value = 0, message = "X坐标必须大于等于0")
    @Column(name = "current_x", nullable = false)
    private Integer currentX = 0;
    
    @Min(value = 0, message = "Y坐标必须大于等于0")
    @Column(name = "current_y", nullable = false)
    private Integer currentY = 0;
    
    @DecimalMin(value = "0.1", message = "载重能力必须大于0")
    @DecimalMax(value = "100.0", message = "载重能力不能超过100吨")
    @Column(name = "capacity", nullable = false, precision = 5, scale = 2)
    private Double capacity = 10.0; // 默认10吨载重
    
    @Size(max = 50, message = "司机姓名长度不能超过50字符")
    @Column(name = "driver_name", length = 50)
    private String driverName;
    
    @Pattern(regexp = "^[\\d-+()\\s]*$", message = "司机电话格式不正确")
    @Size(max = 20, message = "司机电话长度不能超过20字符")
    @Column(name = "driver_phone", length = 20)
    private String driverPhone;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // 是否激活
    
    // 一对多关系：一辆卡车可以配送多个包裹
    @OneToMany(mappedBy = "truck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Package> packages = new ArrayList<>();
    
    // 构造函数
    public Truck() {}
    
    public Truck(String licensePlate, Double capacity) {
        this.licensePlate = licensePlate;
        this.capacity = capacity;
    }
    
    // Getters and Setters
    public String getLicensePlate() {
        return licensePlate;
    }
    
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }
    
    public TruckStatus getStatus() {
        return status;
    }
    
    public void setStatus(TruckStatus status) {
        this.status = status;
    }
    
    public Integer getCurrentX() {
        return currentX;
    }
    
    public void setCurrentX(Integer currentX) {
        this.currentX = currentX;
    }
    
    public Integer getCurrentY() {
        return currentY;
    }
    
    public void setCurrentY(Integer currentY) {
        this.currentY = currentY;
    }
    
    public Double getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }
    
    public String getDriverName() {
        return driverName;
    }
    
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
    
    public String getDriverPhone() {
        return driverPhone;
    }
    
    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public List<Package> getPackages() {
        return packages;
    }
    
    public void setPackages(List<Package> packages) {
        this.packages = packages;
    }
    
    /**
     * 检查卡车是否可用（空闲且激活）
     */
    public boolean isAvailable() {
        return isActive && status.isAvailable();
    }
    
    /**
     * 更新卡车位置
     */
    public void updateLocation(Integer x, Integer y) {
        this.currentX = x;
        this.currentY = y;
    }
    
    /**
     * 计算到指定位置的距离（曼哈顿距离）
     */
    public double calculateDistanceTo(Integer x, Integer y) {
        return Math.abs(this.currentX - x) + Math.abs(this.currentY - y);
    }
    
    /**
     * 获取当前位置坐标字符串
     */
    public String getCurrentLocation() {
        return String.format("(%d, %d)", currentX, currentY);
    }
    
    /**
     * 获取当前载货数量
     */
    public int getCurrentPackageCount() {
        return packages != null ? packages.size() : 0;
    }
}
```

### 7.3 更新Package实体类（添加卡车关联）

在Package实体类中添加与Truck的关联关系：

```java
// 在Package实体类中添加以下字段

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "truck_id")
private Truck truck; // 分配的卡车

// 添加对应的getter和setter
public Truck getTruck() {
    return truck;
}

public void setTruck(Truck truck) {
    this.truck = truck;
}
```

### 7.4 创建TruckRepository

```java
package com.tutorial.ups.repository;

import com.tutorial.ups.model.entity.Truck;
import com.tutorial.ups.model.enums.TruckStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 卡车数据访问层
 */
@Repository
public interface TruckRepository extends JpaRepository<Truck, Long> {
    
    /**
     * 根据车牌号查找卡车
     */
    Optional<Truck> findByLicensePlate(String licensePlate);
    
    /**
     * 检查车牌号是否已存在
     */
    boolean existsByLicensePlate(String licensePlate);
    
    /**
     * 根据状态查找卡车
     */
    List<Truck> findByStatus(TruckStatus status);
    
    /**
     * 查找可用的卡车（空闲且激活）
     */
    @Query("SELECT t FROM Truck t WHERE t.status = 'IDLE' AND t.isActive = true")
    List<Truck> findAvailableTrucks();
    
    /**
     * 根据激活状态查找卡车
     */
    Page<Truck> findByIsActive(Boolean isActive, Pageable pageable);
    
    /**
     * 根据司机姓名查找卡车
     */
    List<Truck> findByDriverName(String driverName);
    
    /**
     * 查找指定区域内的卡车
     */
    @Query("SELECT t FROM Truck t WHERE " +
           "t.currentX BETWEEN :minX AND :maxX AND " +
           "t.currentY BETWEEN :minY AND :maxY AND " +
           "t.isActive = true")
    List<Truck> findTrucksInArea(@Param("minX") Integer minX, @Param("maxX") Integer maxX,
                                 @Param("minY") Integer minY, @Param("maxY") Integer maxY);
    
    /**
     * 查找距离指定位置最近的可用卡车
     */
    @Query(value = "SELECT t.* FROM trucks t WHERE t.status = 'IDLE' AND t.is_active = true " +
                   "ORDER BY (ABS(t.current_x - :x) + ABS(t.current_y - :y)) ASC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Truck> findNearestAvailableTrucks(@Param("x") Integer x, @Param("y") Integer y, 
                                          @Param("limit") Integer limit);
    
    /**
     * 统计各状态卡车数量
     */
    @Query("SELECT t.status, COUNT(t) FROM Truck t WHERE t.isActive = true GROUP BY t.status")
    List<Object[]> countTrucksByStatus();
    
    /**
     * 搜索卡车（车牌号或司机姓名）
     */
    @Query("SELECT t FROM Truck t WHERE " +
           "LOWER(t.licensePlate) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.driverName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Truck> searchTrucks(@Param("keyword") String keyword, Pageable pageable);
}
```

### 7.5 创建卡车相关DTO

#### 创建卡车请求DTO
```java
package com.tutorial.ups.model.dto;

import jakarta.validation.constraints.*;

/**
 * 创建/更新卡车请求DTO
 */
public class TruckRequestDto {
    
    @NotBlank(message = "车牌号不能为空")
    @Size(max = 20, message = "车牌号长度不能超过20字符")
    private String licensePlate;
    
    @DecimalMin(value = "0.1", message = "载重能力必须大于0")
    @DecimalMax(value = "100.0", message = "载重能力不能超过100吨")
    private Double capacity;
    
    @Min(value = 0, message = "X坐标必须大于等于0")
    private Integer currentX;
    
    @Min(value = 0, message = "Y坐标必须大于等于0")
    private Integer currentY;
    
    @Size(max = 50, message = "司机姓名长度不能超过50字符")
    private String driverName;
    
    @Pattern(regexp = "^[\\d-+()\\s]*$", message = "司机电话格式不正确")
    @Size(max = 20, message = "司机电话长度不能超过20字符")
    private String driverPhone;
    
    // 构造函数
    public TruckRequestDto() {}
    
    // Getters and Setters
    public String getLicensePlate() {
        return licensePlate;
    }
    
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }
    
    public Double getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }
    
    public Integer getCurrentX() {
        return currentX;
    }
    
    public void setCurrentX(Integer currentX) {
        this.currentX = currentX;
    }
    
    public Integer getCurrentY() {
        return currentY;
    }
    
    public void setCurrentY(Integer currentY) {
        this.currentY = currentY;
    }
    
    public String getDriverName() {
        return driverName;
    }
    
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
    
    public String getDriverPhone() {
        return driverPhone;
    }
    
    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }
}
```

#### 卡车响应DTO
```java
package com.tutorial.ups.model.dto;

import com.tutorial.ups.model.entity.Truck;
import com.tutorial.ups.model.enums.TruckStatus;

import java.time.LocalDateTime;

/**
 * 卡车响应DTO
 */
public class TruckResponseDto {
    
    private Long id;
    private String licensePlate;
    private TruckStatus status;
    private Integer currentX;
    private Integer currentY;
    private Double capacity;
    private String driverName;
    private String driverPhone;
    private Boolean isActive;
    private Integer currentPackageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 构造函数
    public TruckResponseDto() {}
    
    /**
     * 从Truck实体创建DTO
     */
    public static TruckResponseDto fromEntity(Truck truck) {
        TruckResponseDto dto = new TruckResponseDto();
        dto.setId(truck.getId());
        dto.setLicensePlate(truck.getLicensePlate());
        dto.setStatus(truck.getStatus());
        dto.setCurrentX(truck.getCurrentX());
        dto.setCurrentY(truck.getCurrentY());
        dto.setCapacity(truck.getCapacity());
        dto.setDriverName(truck.getDriverName());
        dto.setDriverPhone(truck.getDriverPhone());
        dto.setIsActive(truck.getIsActive());
        dto.setCurrentPackageCount(truck.getCurrentPackageCount());
        dto.setCreatedAt(truck.getCreatedAt());
        dto.setUpdatedAt(truck.getUpdatedAt());
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getLicensePlate() {
        return licensePlate;
    }
    
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }
    
    public TruckStatus getStatus() {
        return status;
    }
    
    public void setStatus(TruckStatus status) {
        this.status = status;
    }
    
    public Integer getCurrentX() {
        return currentX;
    }
    
    public void setCurrentX(Integer currentX) {
        this.currentX = currentX;
    }
    
    public Integer getCurrentY() {
        return currentY;
    }
    
    public void setCurrentY(Integer currentY) {
        this.currentY = currentY;
    }
    
    public Double getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }
    
    public String getDriverName() {
        return driverName;
    }
    
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
    
    public String getDriverPhone() {
        return driverPhone;
    }
    
    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Integer getCurrentPackageCount() {
        return currentPackageCount;
    }
    
    public void setCurrentPackageCount(Integer currentPackageCount) {
        this.currentPackageCount = currentPackageCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 获取当前位置字符串
     */
    public String getCurrentLocation() {
        return String.format("(%d, %d)", currentX, currentY);
    }
    
    /**
     * 检查卡车是否可用
     */
    public boolean isAvailable() {
        return isActive && status != null && status.isAvailable();
    }
}
```

### 7.6 创建TruckService

```java
package com.tutorial.ups.service;

import com.tutorial.ups.model.dto.TruckRequestDto;
import com.tutorial.ups.model.dto.TruckResponseDto;
import com.tutorial.ups.model.entity.Truck;
import com.tutorial.ups.model.enums.TruckStatus;
import com.tutorial.ups.repository.TruckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 卡车业务逻辑服务
 * 
 * 功能说明：
 * - 管理卡车的基本信息和状态
 * - 实现卡车调度算法
 * - 处理卡车位置更新和追踪
 * - 提供卡车查询和统计功能
 */
@Service
@Transactional
public class TruckService {
    
    private final TruckRepository truckRepository;
    
    @Autowired
    public TruckService(TruckRepository truckRepository) {
        this.truckRepository = truckRepository;
    }
    
    /**
     * 创建卡车
     */
    public TruckResponseDto createTruck(TruckRequestDto request) {
        // 检查车牌号是否已存在
        if (truckRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new IllegalArgumentException("车牌号已存在：" + request.getLicensePlate());
        }
        
        // 创建卡车实体
        Truck truck = new Truck();
        truck.setLicensePlate(request.getLicensePlate());
        truck.setCapacity(request.getCapacity() != null ? request.getCapacity() : 10.0);
        truck.setCurrentX(request.getCurrentX() != null ? request.getCurrentX() : 0);
        truck.setCurrentY(request.getCurrentY() != null ? request.getCurrentY() : 0);
        truck.setDriverName(request.getDriverName());
        truck.setDriverPhone(request.getDriverPhone());
        truck.setStatus(TruckStatus.IDLE);
        truck.setIsActive(true);
        
        Truck savedTruck = truckRepository.save(truck);
        return TruckResponseDto.fromEntity(savedTruck);
    }
    
    /**
     * 根据ID查找卡车
     */
    @Transactional(readOnly = true)
    public Optional<TruckResponseDto> findById(Long id) {
        return truckRepository.findById(id)
                .map(TruckResponseDto::fromEntity);
    }
    
    /**
     * 根据车牌号查找卡车
     */
    @Transactional(readOnly = true)
    public Optional<TruckResponseDto> findByLicensePlate(String licensePlate) {
        return truckRepository.findByLicensePlate(licensePlate)
                .map(TruckResponseDto::fromEntity);
    }
    
    /**
     * 获取所有卡车
     */
    @Transactional(readOnly = true)
    public Page<TruckResponseDto> findAllTrucks(Pageable pageable) {
        return truckRepository.findAll(pageable)
                .map(TruckResponseDto::fromEntity);
    }
    
    /**
     * 搜索卡车
     */
    @Transactional(readOnly = true)
    public Page<TruckResponseDto> searchTrucks(String keyword, Pageable pageable) {
        return truckRepository.searchTrucks(keyword, pageable)
                .map(TruckResponseDto::fromEntity);
    }
    
    /**
     * 根据状态查找卡车
     */
    @Transactional(readOnly = true)
    public List<TruckResponseDto> findByStatus(TruckStatus status) {
        return truckRepository.findByStatus(status)
                .stream()
                .map(TruckResponseDto::fromEntity)
                .toList();
    }
    
    /**
     * 获取可用卡车列表
     */
    @Transactional(readOnly = true)
    public List<TruckResponseDto> findAvailableTrucks() {
        return truckRepository.findAvailableTrucks()
                .stream()
                .map(TruckResponseDto::fromEntity)
                .toList();
    }
    
    /**
     * 查找距离指定位置最近的可用卡车
     */
    @Transactional(readOnly = true)
    public List<TruckResponseDto> findNearestAvailableTrucks(Integer x, Integer y, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 5; // 默认返回5辆最近的卡车
        }
        
        return truckRepository.findNearestAvailableTrucks(x, y, limit)
                .stream()
                .map(TruckResponseDto::fromEntity)
                .toList();
    }
    
    /**
     * 更新卡车信息
     */
    public TruckResponseDto updateTruck(Long truckId, TruckRequestDto request) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new IllegalArgumentException("卡车不存在：" + truckId));
        
        // 检查车牌号是否被其他卡车使用
        if (!truck.getLicensePlate().equals(request.getLicensePlate()) 
            && truckRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new IllegalArgumentException("车牌号已存在：" + request.getLicensePlate());
        }
        
        // 更新卡车信息
        truck.setLicensePlate(request.getLicensePlate());
        truck.setCapacity(request.getCapacity());
        truck.setDriverName(request.getDriverName());
        truck.setDriverPhone(request.getDriverPhone());
        
        // 如果提供了位置信息，更新位置
        if (request.getCurrentX() != null && request.getCurrentY() != null) {
            truck.updateLocation(request.getCurrentX(), request.getCurrentY());
        }
        
        Truck savedTruck = truckRepository.save(truck);
        return TruckResponseDto.fromEntity(savedTruck);
    }
    
    /**
     * 更新卡车状态
     */
    public TruckResponseDto updateTruckStatus(Long truckId, TruckStatus newStatus) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new IllegalArgumentException("卡车不存在：" + truckId));
        
        truck.setStatus(newStatus);
        Truck savedTruck = truckRepository.save(truck);
        
        return TruckResponseDto.fromEntity(savedTruck);
    }
    
    /**
     * 更新卡车位置
     */
    public TruckResponseDto updateTruckLocation(Long truckId, Integer x, Integer y) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new IllegalArgumentException("卡车不存在：" + truckId));
        
        truck.updateLocation(x, y);
        Truck savedTruck = truckRepository.save(truck);
        
        return TruckResponseDto.fromEntity(savedTruck);
    }
    
    /**
     * 启用/禁用卡车
     */
    public TruckResponseDto toggleTruckActive(Long truckId) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new IllegalArgumentException("卡车不存在：" + truckId));
        
        truck.setIsActive(!truck.getIsActive());
        
        // 如果禁用卡车，设置状态为停止服务
        if (!truck.getIsActive()) {
            truck.setStatus(TruckStatus.OUT_OF_SERVICE);
        } else {
            // 如果重新启用，设置为空闲状态
            truck.setStatus(TruckStatus.IDLE);
        }
        
        Truck savedTruck = truckRepository.save(truck);
        return TruckResponseDto.fromEntity(savedTruck);
    }
    
    /**
     * 删除卡车（软删除）
     */
    public void deleteTruck(Long truckId) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new IllegalArgumentException("卡车不存在：" + truckId));
        
        // 检查卡车是否有未完成的任务
        if (truck.getStatus().isWorking()) {
            throw new IllegalStateException("卡车正在工作中，无法删除");
        }
        
        truck.setIsActive(false);
        truck.setStatus(TruckStatus.OUT_OF_SERVICE);
        truckRepository.save(truck);
    }
    
    /**
     * 获取卡车统计信息
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTruckStatistics() {
        return truckRepository.countTrucksByStatus();
    }
    
    /**
     * 智能分配卡车算法
     * 根据距离和载重能力选择最适合的卡车
     */
    public Optional<TruckResponseDto> assignBestTruck(Integer destinationX, Integer destinationY, 
                                                     Double requiredCapacity) {
        List<Truck> availableTrucks = truckRepository.findAvailableTrucks();
        
        if (availableTrucks.isEmpty()) {
            return Optional.empty();
        }
        
        // 筛选载重能力足够的卡车
        List<Truck> suitableTrucks = availableTrucks.stream()
                .filter(truck -> truck.getCapacity() >= (requiredCapacity != null ? requiredCapacity : 0))
                .toList();
        
        if (suitableTrucks.isEmpty()) {
            return Optional.empty();
        }
        
        // 选择距离最近的卡车
        Truck bestTruck = suitableTrucks.stream()
                .min((t1, t2) -> Double.compare(
                    t1.calculateDistanceTo(destinationX, destinationY),
                    t2.calculateDistanceTo(destinationX, destinationY)
                ))
                .orElse(null);
        
        return bestTruck != null ? Optional.of(TruckResponseDto.fromEntity(bestTruck)) : Optional.empty();
    }
}
```

### 7.7 创建TruckController

```java
package com.tutorial.ups.controller;

import com.tutorial.ups.model.dto.TruckRequestDto;
import com.tutorial.ups.model.dto.TruckResponseDto;
import com.tutorial.ups.model.enums.TruckStatus;
import com.tutorial.ups.service.TruckService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 卡车控制器
 */
@RestController
@RequestMapping("/api/trucks")
public class TruckController {
    
    private final TruckService truckService;
    
    @Autowired
    public TruckController(TruckService truckService) {
        this.truckService = truckService;
    }
    
    /**
     * 创建卡车
     * 
     * URL: POST /api/trucks
     */
    @PostMapping
    public ResponseEntity<TruckResponseDto> createTruck(@Valid @RequestBody TruckRequestDto request) {
        try {
            TruckResponseDto truck = truckService.createTruck(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(truck);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取卡车详情
     * 
     * URL: GET /api/trucks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TruckResponseDto> getTruckById(@PathVariable Long id) {
        return truckService.findById(id)
                .map(truck -> ResponseEntity.ok(truck))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据车牌号查找卡车
     * 
     * URL: GET /api/trucks/by-license/{licensePlate}
     */
    @GetMapping("/by-license/{licensePlate}")
    public ResponseEntity<TruckResponseDto> getTruckByLicensePlate(@PathVariable String licensePlate) {
        return truckService.findByLicensePlate(licensePlate)
                .map(truck -> ResponseEntity.ok(truck))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取所有卡车
     * 
     * URL: GET /api/trucks?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<TruckResponseDto>> getAllTrucks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TruckResponseDto> trucks = truckService.findAllTrucks(pageable);
        
        return ResponseEntity.ok(trucks);
    }
    
    /**
     * 搜索卡车
     * 
     * URL: GET /api/trucks/search?keyword=京A&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<Page<TruckResponseDto>> searchTrucks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TruckResponseDto> trucks = truckService.searchTrucks(keyword, pageable);
        
        return ResponseEntity.ok(trucks);
    }
    
    /**
     * 根据状态获取卡车
     * 
     * URL: GET /api/trucks/by-status?status=IDLE
     */
    @GetMapping("/by-status")
    public ResponseEntity<List<TruckResponseDto>> getTrucksByStatus(@RequestParam TruckStatus status) {
        List<TruckResponseDto> trucks = truckService.findByStatus(status);
        return ResponseEntity.ok(trucks);
    }
    
    /**
     * 获取可用卡车
     * 
     * URL: GET /api/trucks/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<TruckResponseDto>> getAvailableTrucks() {
        List<TruckResponseDto> trucks = truckService.findAvailableTrucks();
        return ResponseEntity.ok(trucks);
    }
    
    /**
     * 查找最近的可用卡车
     * 
     * URL: GET /api/trucks/nearest?x=100&y=200&limit=5
     */
    @GetMapping("/nearest")
    public ResponseEntity<List<TruckResponseDto>> getNearestTrucks(
            @RequestParam Integer x,
            @RequestParam Integer y,
            @RequestParam(defaultValue = "5") Integer limit) {
        
        List<TruckResponseDto> trucks = truckService.findNearestAvailableTrucks(x, y, limit);
        return ResponseEntity.ok(trucks);
    }
    
    /**
     * 智能分配卡车
     * 
     * URL: GET /api/trucks/assign?x=100&y=200&capacity=5.0
     */
    @GetMapping("/assign")
    public ResponseEntity<TruckResponseDto> assignBestTruck(
            @RequestParam Integer x,
            @RequestParam Integer y,
            @RequestParam(required = false) Double capacity) {
        
        return truckService.assignBestTruck(x, y, capacity)
                .map(truck -> ResponseEntity.ok(truck))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 更新卡车信息
     * 
     * URL: PUT /api/trucks/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TruckResponseDto> updateTruck(
            @PathVariable Long id,
            @Valid @RequestBody TruckRequestDto request) {
        try {
            TruckResponseDto truck = truckService.updateTruck(id, request);
            return ResponseEntity.ok(truck);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 更新卡车状态
     * 
     * URL: PATCH /api/trucks/{id}/status?status=BUSY
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TruckResponseDto> updateTruckStatus(
            @PathVariable Long id,
            @RequestParam TruckStatus status) {
        try {
            TruckResponseDto truck = truckService.updateTruckStatus(id, status);
            return ResponseEntity.ok(truck);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 更新卡车位置
     * 
     * URL: PATCH /api/trucks/{id}/location?x=150&y=250
     */
    @PatchMapping("/{id}/location")
    public ResponseEntity<TruckResponseDto> updateTruckLocation(
            @PathVariable Long id,
            @RequestParam Integer x,
            @RequestParam Integer y) {
        try {
            TruckResponseDto truck = truckService.updateTruckLocation(id, x, y);
            return ResponseEntity.ok(truck);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 启用/禁用卡车
     * 
     * URL: PATCH /api/trucks/{id}/toggle-active
     */
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<TruckResponseDto> toggleTruckActive(@PathVariable Long id) {
        try {
            TruckResponseDto truck = truckService.toggleTruckActive(id);
            return ResponseEntity.ok(truck);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除卡车
     * 
     * URL: DELETE /api/trucks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTruck(@PathVariable Long id) {
        try {
            truckService.deleteTruck(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取卡车统计信息
     * 
     * URL: GET /api/trucks/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<Object[]>> getTruckStatistics() {
        List<Object[]> statistics = truckService.getTruckStatistics();
        return ResponseEntity.ok(statistics);
    }
}
```

### 7.8 测试卡车模块

创建文件`test-truck-api.http`：

```http
### 创建卡车
POST http://localhost:8080/api/trucks
Content-Type: application/json

{
    "licensePlate": "京A12345",
    "capacity": 15.0,
    "currentX": 0,
    "currentY": 0,
    "driverName": "张师傅",
    "driverPhone": "138-0000-1111"
}

### 获取卡车详情
GET http://localhost:8080/api/trucks/1

### 根据车牌号查找卡车
GET http://localhost:8080/api/trucks/by-license/京A12345

### 获取所有卡车
GET http://localhost:8080/api/trucks?page=0&size=5

### 搜索卡车
GET http://localhost:8080/api/trucks/search?keyword=京A

### 根据状态获取卡车
GET http://localhost:8080/api/trucks/by-status?status=IDLE

### 获取可用卡车
GET http://localhost:8080/api/trucks/available

### 查找最近的可用卡车
GET http://localhost:8080/api/trucks/nearest?x=100&y=200&limit=3

### 智能分配卡车
GET http://localhost:8080/api/trucks/assign?x=100&y=200&capacity=5.0

### 更新卡车状态
PATCH http://localhost:8080/api/trucks/1/status?status=BUSY

### 更新卡车位置
PATCH http://localhost:8080/api/trucks/1/location?x=150&y=250

### 获取卡车统计信息
GET http://localhost:8080/api/trucks/statistics

### 启用/禁用卡车
PATCH http://localhost:8080/api/trucks/1/toggle-active
```

---

## 8. 安全认证系统

现在我们来实现JWT安全认证系统，这是企业级应用的核心功能。

### 8.1 理解JWT认证

#### 什么是JWT？
JWT（JSON Web Token）是一种用于身份认证的标准。它的工作原理：

```
用户登录 -> 服务器验证 -> 生成JWT Token -> 返回给客户端
客户端存储Token -> 每次请求携带Token -> 服务器验证Token -> 允许访问
```

#### JWT的结构
JWT由三部分组成，用`.`分隔：
```
Header.Payload.Signature
```

- **Header**: 包含算法信息
- **Payload**: 包含用户信息（声明）
- **Signature**: 防止篡改的签名

### 8.2 添加安全依赖

首先在`pom.xml`中添加必要的依赖：

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

### 8.3 配置application.yml

添加JWT相关配置：

```yaml
# JWT配置
jwt:
  secret: your-very-long-secret-key-for-jwt-signing-should-be-at-least-256-bits-long
  expiration: 86400000  # 24小时（毫秒）

# 安全配置
security:
  permit-urls:
    - /api/auth/**
    - /api/users/register
    - /api/packages/track/**
    - /swagger-ui/**
    - /v3/api-docs/**
```

### 8.4 创建JWT Token Provider

```java
package com.tutorial.ups.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Token 提供者
 * 
 * 功能说明：
 * - 生成JWT令牌
 * - 验证JWT令牌
 * - 从令牌中提取用户信息
 * 
 * 安全特性：
 * - HMAC SHA-256签名算法
 * - 可配置的过期时间
 * - 安全的密钥管理
 */
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private int jwtExpirationInMs;
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    /**
     * 生成JWT令牌
     * 
     * @param username 用户名
     * @return JWT令牌
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        
        return Jwts.builder()
                .subject(username)           // 设置主题（用户名）
                .issuedAt(now)              // 设置签发时间
                .expiration(expiryDate)     // 设置过期时间
                .signWith(getSigningKey())  // 设置签名
                .compact();                 // 生成紧凑的JWT字符串
    }
    
    /**
     * 从令牌中获取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from token", e);
            return null;
        }
    }
    
    /**
     * 验证JWT令牌
     * 
     * @param authToken JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        } catch (Exception ex) {
            logger.error("JWT validation error", ex);
        }
        return false;
    }
    
    /**
     * 获取令牌过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration();
        } catch (Exception e) {
            logger.error("Error extracting expiration date from token", e);
            return null;
        }
    }
    
    /**
     * 检查令牌是否过期
     * 
     * @param token JWT令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate != null && expirationDate.before(new Date());
    }
    
    private static final org.slf4j.Logger logger = 
        org.slf4j.LoggerFactory.getLogger(JwtTokenProvider.class);
}
```

### 8.5 创建自定义用户详情服务

```java
package com.tutorial.ups.security;

import com.tutorial.ups.model.entity.User;
import com.tutorial.ups.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 自定义用户详情服务
 * 
 * 功能说明：
 * - 实现Spring Security的UserDetailsService接口
 * - 从数据库加载用户信息
 * - 转换为Spring Security需要的UserDetails对象
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * 根据用户名加载用户详情
     * 这个方法会被Spring Security自动调用
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 根据邮箱查找用户（我们使用邮箱作为用户名）
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + email));
        
        return createUserPrincipal(user);
    }
    
    /**
     * 根据用户ID加载用户详情
     * 用于JWT认证时根据用户ID获取用户信息
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + userId));
        
        return createUserPrincipal(user);
    }
    
    /**
     * 创建用户主体对象
     * 将我们的User实体转换为Spring Security的UserDetails
     */
    private UserDetails createUserPrincipal(User user) {
        // 根据用户角色创建权限列表
        List<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())          // 使用邮箱作为用户名
                .password(user.getPassword())       // 加密后的密码
                .authorities(authorities)           // 用户权限
                .accountExpired(false)              // 账户未过期
                .accountLocked(!user.getEnabled())  // 根据enabled字段判断是否锁定
                .credentialsExpired(false)          // 凭据未过期
                .disabled(!user.getEnabled())       // 根据enabled字段判断是否禁用
                .build();
    }
}
```

### 8.6 创建JWT认证过滤器

```java
package com.tutorial.ups.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 
 * 功能说明：
 * - 拦截每个HTTP请求
 * - 从请求头中提取JWT令牌
 * - 验证令牌并设置安全上下文
 * - 允许或拒绝请求
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. 从请求头中获取JWT令牌
            String jwt = getJwtFromRequest(request);
            
            // 2. 验证令牌并设置认证信息
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // 从令牌中获取用户名
                String username = tokenProvider.getUsernameFromToken(jwt);
                
                // 加载用户详情
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                
                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.getAuthorities()
                    );
                
                // 设置认证详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 设置安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }
        
        // 3. 继续过滤器链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头中提取JWT令牌
     * 
     * 支持两种格式：
     * 1. Authorization: Bearer <token>
     * 2. Authorization: <token>
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken)) {
            if (bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7); // 移除"Bearer "前缀
            } else {
                return bearerToken; // 直接返回令牌
            }
        }
        
        return null;
    }
    
    private static final org.slf4j.Logger logger = 
        org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);
}
```

### 8.7 创建认证入口点

```java
package com.tutorial.ups.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证入口点
 * 
 * 功能说明：
 * - 处理未认证用户的访问请求
 * - 返回统一的错误响应
 * - 避免重定向到登录页面
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        logger.error("Unauthorized error: {}", authException.getMessage());
        
        // 设置响应内容类型
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // 创建错误响应
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", "请先登录");
        errorResponse.put("status", 401);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getRequestURI());
        
        // 写入响应
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
    
    private static final org.slf4j.Logger logger = 
        org.slf4j.LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
}
```

### 8.8 创建安全配置

```java
package com.tutorial.ups.config;

import com.tutorial.ups.security.CustomUserDetailsService;
import com.tutorial.ups.security.JwtAuthenticationEntryPoint;
import com.tutorial.ups.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置
 * 
 * 功能说明：
 * - 配置HTTP安全规则
 * - 设置JWT认证过滤器
 * - 定义公开和受保护的URL
 * - 配置密码加密器
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用方法级别的安全控制
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * 密码加密器
     * BCrypt是目前最安全的密码加密算法之一
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 认证管理器
     * 用于处理用户登录认证
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * 认证提供者
     * 配置如何验证用户凭据
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * 安全过滤器链
     * 这是Spring Security的核心配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（跨站请求伪造）保护，因为我们使用JWT
            .csrf(csrf -> csrf.disable())
            
            // 配置异常处理
            .exceptionHandling(exception -> 
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            
            // 设置会话管理策略为无状态（因为使用JWT）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置URL访问权限
            .authorizeHttpRequests(authz -> authz
                // 公开访问的URL（不需要认证）
                .requestMatchers(
                    "/api/auth/**",           // 认证相关接口
                    "/api/users/register",    // 用户注册
                    "/api/packages/track/**", // 包裹追踪（公开）
                    "/swagger-ui/**",         // Swagger文档
                    "/v3/api-docs/**",        // API文档
                    "/error"                  // 错误页面
                ).permitAll()
                
                // 管理员专用接口
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 司机专用接口
                .requestMatchers("/api/driver/**").hasRole("DRIVER")
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            );
        
        // 添加JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### 8.9 创建认证控制器

```java
package com.tutorial.ups.controller;

import com.tutorial.ups.model.dto.LoginRequestDto;
import com.tutorial.ups.model.dto.UserResponseDto;
import com.tutorial.ups.model.entity.User;
import com.tutorial.ups.security.JwtTokenProvider;
import com.tutorial.ups.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 
 * 功能说明：
 * - 处理用户登录请求
 * - 生成和返回JWT令牌
 * - 提供令牌验证接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    /**
     * 用户登录
     * 
     * URL: POST /api/auth/login
     * 请求体: {"email": "user@example.com", "password": "password123"}
     * 返回: {"token": "eyJ...", "type": "Bearer", "user": {...}}
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            // 1. 创建认证令牌
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );
            
            // 2. 设置安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 3. 生成JWT令牌
            String jwt = tokenProvider.generateToken(authentication.getName());
            
            // 4. 获取用户信息
            User user = userService.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            UserResponseDto userDto = UserResponseDto.fromEntity(user);
            
            // 5. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("type", "Bearer");
            response.put("user", userDto);
            response.put("expiresIn", 86400); // 24小时（秒）
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid credentials");
            errorResponse.put("message", "邮箱或密码错误");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 验证令牌
     * 
     * URL: POST /api/auth/validate
     * 请求头: Authorization: Bearer <token>
     * 返回: {"valid": true, "user": {...}}
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            // 移除"Bearer "前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 验证令牌
            if (tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                User user = userService.findByEmail(username)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("user", UserResponseDto.fromEntity(user));
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "令牌无效或已过期");
                
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "令牌验证失败");
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取当前用户信息
     * 
     * URL: GET /api/auth/me
     * 请求头: Authorization: Bearer <token>
     * 返回: 当前登录用户的信息
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return ResponseEntity.ok(UserResponseDto.fromEntity(user));
    }
    
    /**
     * 刷新令牌
     * 
     * URL: POST /api/auth/refresh
     * 请求头: Authorization: Bearer <token>
     * 返回: 新的JWT令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            // 移除"Bearer "前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 验证现有令牌
            if (tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                
                // 生成新令牌
                String newToken = tokenProvider.generateToken(username);
                
                Map<String, Object> response = new HashMap<>();
                response.put("token", newToken);
                response.put("type", "Bearer");
                response.put("expiresIn", 86400);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Invalid token");
                response.put("message", "令牌无效，请重新登录");
                
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Token refresh failed");
            response.put("message", "令牌刷新失败");
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
```

### 8.10 测试认证系统

创建文件`test-auth-api.http`：

```http
### 注册新用户
POST http://localhost:8080/api/users/register
Content-Type: application/json

{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "confirmPassword": "password123",
    "firstName": "Test",
    "lastName": "User"
}

### 用户登录
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "email": "test@example.com",
    "password": "password123"
}

### 验证令牌（需要先登录获取token）
POST http://localhost:8080/api/auth/validate
Authorization: Bearer YOUR_JWT_TOKEN_HERE

### 获取当前用户信息
GET http://localhost:8080/api/auth/me
Authorization: Bearer YOUR_JWT_TOKEN_HERE

### 刷新令牌
POST http://localhost:8080/api/auth/refresh
Authorization: Bearer YOUR_JWT_TOKEN_HERE

### 访问受保护的接口（需要认证）
GET http://localhost:8080/api/users
Authorization: Bearer YOUR_JWT_TOKEN_HERE

### 尝试不带token访问受保护接口（应该返回401）
GET http://localhost:8080/api/users
```

#### Postman测试流程

1. **注册用户**：使用注册接口创建新用户
2. **登录获取Token**：使用登录接口获取JWT令牌
3. **复制Token**：从登录响应中复制token值
4. **访问受保护接口**：在Authorization头中添加`Bearer <token>`
5. **验证权限**：确认可以正常访问需要认证的接口

### 8.11 权限控制

现在我们可以在控制器方法上添加权限控制：

```java
// 在UserController中添加权限控制
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/all")
public ResponseEntity<Page<UserResponseDto>> getAllUsersForAdmin(Pageable pageable) {
    // 只有管理员可以访问
}

@PreAuthorize("hasRole('ADMIN') or @userService.isPackageOwnedByUser(#packageId, authentication.principal.id)")
@GetMapping("/packages/{packageId}")
public ResponseEntity<PackageResponseDto> getPackage(@PathVariable Long packageId) {
    // 管理员或包裹拥有者可以访问
}
```

---

到此为止，我们已经完成了用户模块、包裹模块、卡车管理模块和安全认证系统的完整开发。接下来我将继续完善API测试部分和高级功能。

这个教程提供了完整的代码示例、详细的解释，以及测试方法。每个步骤都有充分的说明，适合Java后端开发新手循序渐进地学习。

## 9. API测试详解

现在我们来详细学习如何测试我们开发的API接口。这是后端开发中非常重要的一环。

### 9.1 测试工具选择

#### IntelliJ IDEA HTTP Client（推荐）
- **优点**：集成在IDE中，方便调试
- **文件格式**：`.http`文件
- **支持**：变量、环境、脚本等

#### Postman
- **优点**：功能强大，界面友好
- **适用**：团队协作，复杂测试场景
- **支持**：集合管理、自动化测试

#### curl命令
- **优点**：简单快速，适合脚本
- **适用**：CI/CD集成，命令行操作

### 9.2 IntelliJ IDEA HTTP Client详解

#### 创建HTTP文件
在项目根目录创建`api-tests`文件夹，然后创建不同的测试文件：

```
api-tests/
├── auth-tests.http          # 认证测试
├── user-tests.http          # 用户测试
├── package-tests.http       # 包裹测试
├── truck-tests.http         # 卡车测试
└── http-client.env.json     # 环境变量配置
```

#### 环境变量配置
创建`http-client.env.json`：

```json
{
  "development": {
    "baseUrl": "http://localhost:8080",
    "authToken": ""
  },
  "staging": {
    "baseUrl": "https://staging.ups-tutorial.com",
    "authToken": ""
  },
  "production": {
    "baseUrl": "https://api.ups-tutorial.com",
    "authToken": ""
  }
}
```

#### 完整的认证测试文件
创建`api-tests/auth-tests.http`：

```http
### 设置环境变量
# @name baseUrl
GET {{baseUrl}}/health

### 1. 用户注册
# @name registerUser
POST {{baseUrl}}/api/users/register
Content-Type: application/json

{
  "username": "demo_user",
  "email": "demo@tutorial.com",
  "password": "Demo123456",
  "confirmPassword": "Demo123456",
  "firstName": "Demo",
  "lastName": "User",
  "phone": "138-0000-0001",
  "address": "北京市朝阳区测试地址123号"
}

### 2. 用户登录
# @name loginUser
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "email": "demo@tutorial.com",
  "password": "Demo123456"
}

> {%
  // 登录成功后保存token
  if (response.status === 200) {
    client.global.set("authToken", response.body.token);
    console.log("登录成功，Token已保存");
  }
%}

### 3. 验证Token
POST {{baseUrl}}/api/auth/validate
Authorization: Bearer {{authToken}}

### 4. 获取当前用户信息
GET {{baseUrl}}/api/auth/me
Authorization: Bearer {{authToken}}

### 5. 刷新Token
POST {{baseUrl}}/api/auth/refresh
Authorization: Bearer {{authToken}}

### 6. 测试无效Token（应该返回401）
GET {{baseUrl}}/api/users
Authorization: Bearer invalid_token_here

### 7. 测试不带Token访问（应该返回401）
GET {{baseUrl}}/api/users

### 8. 测试错误的登录信息
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "email": "wrong@email.com",
  "password": "wrongpassword"
}
```

#### 用户管理测试文件
创建`api-tests/user-tests.http`：

```http
### 前置：确保已登录
@authToken = {{authToken}}

### 1. 获取所有用户（需要认证）
GET {{baseUrl}}/api/users?page=0&size=5&sort=createdAt,desc
Authorization: Bearer {{authToken}}

### 2. 搜索用户
GET {{baseUrl}}/api/users/search?keyword=demo
Authorization: Bearer {{authToken}}

### 3. 根据角色查找用户
GET {{baseUrl}}/api/users/by-role?role=USER
Authorization: Bearer {{authToken}}

### 4. 获取用户详情
GET {{baseUrl}}/api/users/1
Authorization: Bearer {{authToken}}

### 5. 更新用户信息
PUT {{baseUrl}}/api/users/1
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "username": "demo_user_updated",
  "email": "demo@tutorial.com",
  "firstName": "Demo Updated",
  "lastName": "User",
  "phone": "138-0000-0002",
  "address": "北京市海淀区更新地址456号"
}

### 6. 创建管理员用户
POST {{baseUrl}}/api/users/register
Content-Type: application/json

{
  "username": "admin_user",
  "email": "admin@tutorial.com",
  "password": "Admin123456",
  "confirmPassword": "Admin123456",
  "firstName": "Admin",
  "lastName": "User"
}

### 7. 启用/禁用用户
PATCH {{baseUrl}}/api/users/2/toggle-enabled
Authorization: Bearer {{authToken}}

### 8. 测试数据验证（应该返回400错误）
POST {{baseUrl}}/api/users/register
Content-Type: application/json

{
  "username": "ab",
  "email": "invalid-email",
  "password": "123",
  "confirmPassword": "456"
}
```

#### 包裹管理测试文件
创建`api-tests/package-tests.http`：

```http
### 获取认证用户ID
# @name getCurrentUser
GET {{baseUrl}}/api/auth/me
Authorization: Bearer {{authToken}}

> {%
  if (response.status === 200) {
    client.global.set("currentUserId", response.body.id);
    console.log("当前用户ID: " + response.body.id);
  }
%}

### 1. 创建包裹
# @name createPackage
POST {{baseUrl}}/api/packages?userId={{currentUserId}}
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "recipientName": "张三",
  "recipientAddress": "上海市浦东新区张江高科技园区123号",
  "recipientPhone": "139-0000-1111",
  "weight": 2.5,
  "dimensions": "30x20x15cm",
  "description": "电子产品 - 笔记本电脑",
  "deliveryX": 200,
  "deliveryY": 300
}

> {%
  if (response.status === 201) {
    client.global.set("packageId", response.body.id);
    client.global.set("trackingNumber", response.body.trackingNumber);
    console.log("包裹创建成功，ID: " + response.body.id);
    console.log("追踪号码: " + response.body.trackingNumber);
  }
%}

### 2. 通过追踪号码查询包裹
GET {{baseUrl}}/api/packages/track/{{trackingNumber}}

### 3. 获取包裹详情
GET {{baseUrl}}/api/packages/{{packageId}}
Authorization: Bearer {{authToken}}

### 4. 获取用户的所有包裹
GET {{baseUrl}}/api/packages/user/{{currentUserId}}?page=0&size=10
Authorization: Bearer {{authToken}}

### 5. 搜索包裹
GET {{baseUrl}}/api/packages/search?keyword=张三
Authorization: Bearer {{authToken}}

### 6. 根据状态查找包裹
GET {{baseUrl}}/api/packages/by-status?status=CREATED
Authorization: Bearer {{authToken}}

### 7. 更新包裹状态
PATCH {{baseUrl}}/api/packages/{{packageId}}/status?status=PICKED_UP
Authorization: Bearer {{authToken}}

### 8. 再次更新包裹状态
PATCH {{baseUrl}}/api/packages/{{packageId}}/status?status=IN_TRANSIT
Authorization: Bearer {{authToken}}

### 9. 更新包裹信息
PUT {{baseUrl}}/api/packages/{{packageId}}
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "recipientName": "李四",
  "recipientAddress": "广州市天河区珠江新城456号",
  "recipientPhone": "135-0000-2222",
  "weight": 3.0,
  "dimensions": "35x25x20cm",
  "description": "电子产品 - 平板电脑",
  "deliveryX": 250,
  "deliveryY": 350
}

### 10. 获取包裹统计信息
GET {{baseUrl}}/api/packages/statistics
Authorization: Bearer {{authToken}}

### 11. 创建多个测试包裹
POST {{baseUrl}}/api/packages?userId={{currentUserId}}
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "recipientName": "王五",
  "recipientAddress": "深圳市南山区科技园789号",
  "recipientPhone": "159-0000-3333",
  "weight": 1.8,
  "dimensions": "25x18x12cm",
  "description": "书籍资料",
  "deliveryX": 180,
  "deliveryY": 280
}

### 12. 测试无效的包裹数据（应该返回400）
POST {{baseUrl}}/api/packages?userId={{currentUserId}}
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "recipientName": "",
  "recipientAddress": "",
  "weight": -1,
  "deliveryX": -10,
  "deliveryY": -20
}
```

#### 卡车管理测试文件
创建`api-tests/truck-tests.http`：

```http
### 1. 创建卡车
# @name createTruck
POST {{baseUrl}}/api/trucks
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "licensePlate": "京A88888",
  "capacity": 15.0,
  "currentX": 50,
  "currentY": 100,
  "driverName": "张师傅",
  "driverPhone": "138-1111-2222"
}

> {%
  if (response.status === 201) {
    client.global.set("truckId", response.body.id);
    console.log("卡车创建成功，ID: " + response.body.id);
  }
%}

### 2. 创建更多测试卡车
POST {{baseUrl}}/api/trucks
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "licensePlate": "京B99999",
  "capacity": 20.0,
  "currentX": 100,
  "currentY": 150,
  "driverName": "李师傅",
  "driverPhone": "139-2222-3333"
}

### 3. 获取卡车详情
GET {{baseUrl}}/api/trucks/{{truckId}}
Authorization: Bearer {{authToken}}

### 4. 根据车牌号查找卡车
GET {{baseUrl}}/api/trucks/by-license/京A88888
Authorization: Bearer {{authToken}}

### 5. 获取所有卡车
GET {{baseUrl}}/api/trucks?page=0&size=10&sort=createdAt,desc
Authorization: Bearer {{authToken}}

### 6. 搜索卡车
GET {{baseUrl}}/api/trucks/search?keyword=京A
Authorization: Bearer {{authToken}}

### 7. 根据状态获取卡车
GET {{baseUrl}}/api/trucks/by-status?status=IDLE
Authorization: Bearer {{authToken}}

### 8. 获取可用卡车
GET {{baseUrl}}/api/trucks/available
Authorization: Bearer {{authToken}}

### 9. 查找最近的可用卡车
GET {{baseUrl}}/api/trucks/nearest?x=200&y=300&limit=3
Authorization: Bearer {{authToken}}

### 10. 智能分配卡车
GET {{baseUrl}}/api/trucks/assign?x=200&y=300&capacity=5.0
Authorization: Bearer {{authToken}}

### 11. 更新卡车状态
PATCH {{baseUrl}}/api/trucks/{{truckId}}/status?status=BUSY
Authorization: Bearer {{authToken}}

### 12. 更新卡车位置
PATCH {{baseUrl}}/api/trucks/{{truckId}}/location?x=220&y=320
Authorization: Bearer {{authToken}}

### 13. 更新卡车信息
PUT {{baseUrl}}/api/trucks/{{truckId}}
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "licensePlate": "京A88888",
  "capacity": 18.0,
  "currentX": 220,
  "currentY": 320,
  "driverName": "张师傅（高级）",
  "driverPhone": "138-1111-2222"
}

### 14. 获取卡车统计信息
GET {{baseUrl}}/api/trucks/statistics
Authorization: Bearer {{authToken}}

### 15. 启用/禁用卡车
PATCH {{baseUrl}}/api/trucks/{{truckId}}/toggle-active
Authorization: Bearer {{authToken}}

### 16. 恢复卡车状态
PATCH {{baseUrl}}/api/trucks/{{truckId}}/status?status=IDLE
Authorization: Bearer {{authToken}}

### 17. 测试无效数据（应该返回400）
POST {{baseUrl}}/api/trucks
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "licensePlate": "",
  "capacity": -5.0,
  "currentX": -10,
  "currentY": -20
}
```

### 9.3 Postman测试指南

#### 创建Postman集合
1. 打开Postman
2. 点击"New" -> "Collection"
3. 命名为"UPS Tutorial System"
4. 创建文件夹结构：
   ```
   UPS Tutorial System/
   ├── 1. Authentication/
   ├── 2. User Management/
   ├── 3. Package Management/
   └── 4. Truck Management/
   ```

#### 设置环境变量
1. 点击右上角的齿轮图标 -> "Manage Environments"
2. 点击"Add"创建新环境："Development"
3. 添加变量：
   ```
   baseUrl: http://localhost:8080
   authToken: (空，登录后自动设置)
   userId: (空，登录后自动设置)
   packageId: (空，创建包裹后自动设置)
   truckId: (空，创建卡车后自动设置)
   ```

#### 示例请求配置

**登录请求**：
```
POST {{baseUrl}}/api/auth/login
Headers:
  Content-Type: application/json
Body (raw JSON):
{
  "email": "demo@tutorial.com",
  "password": "Demo123456"
}

Tests脚本:
if (pm.response.code === 200) {
    const response = pm.response.json();
    pm.environment.set("authToken", response.token);
    pm.environment.set("userId", response.user.id);
    console.log("登录成功，Token已保存");
}
```

**创建包裹请求**：
```
POST {{baseUrl}}/api/packages?userId={{userId}}
Headers:
  Authorization: Bearer {{authToken}}
  Content-Type: application/json
Body (raw JSON):
{
  "recipientName": "测试收件人",
  "recipientAddress": "测试地址123号",
  "recipientPhone": "138-0000-0000",
  "weight": 2.0,
  "deliveryX": 100,
  "deliveryY": 200
}

Tests脚本:
if (pm.response.code === 201) {
    const response = pm.response.json();
    pm.environment.set("packageId", response.id);
    pm.environment.set("trackingNumber", response.trackingNumber);
}
```

### 9.4 自动化测试脚本

#### 创建测试套件
可以在HTTP文件中添加自动化测试脚本：

```http
### 完整的工作流测试
# @name completeWorkflow

### 1. 注册用户
POST {{baseUrl}}/api/users/register
Content-Type: application/json

{
  "username": "workflow_test",
  "email": "workflow@test.com",
  "password": "Test123456",
  "confirmPassword": "Test123456"
}

> {%
  client.test("用户注册成功", function() {
    client.assert(response.status === 201, "状态码应该是201");
    client.assert(response.body.username === "workflow_test", "用户名应该正确");
  });
%}

### 2. 用户登录
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "email": "workflow@test.com",
  "password": "Test123456"
}

> {%
  client.test("登录成功", function() {
    client.assert(response.status === 200, "状态码应该是200");
    client.assert(response.body.token !== undefined, "应该返回token");
    client.global.set("workflowToken", response.body.token);
    client.global.set("workflowUserId", response.body.user.id);
  });
%}

### 3. 创建包裹
POST {{baseUrl}}/api/packages?userId={{workflowUserId}}
Authorization: Bearer {{workflowToken}}
Content-Type: application/json

{
  "recipientName": "工作流测试",
  "recipientAddress": "测试地址",
  "weight": 1.0,
  "deliveryX": 100,
  "deliveryY": 100
}

> {%
  client.test("包裹创建成功", function() {
    client.assert(response.status === 201, "状态码应该是201");
    client.assert(response.body.trackingNumber !== undefined, "应该生成追踪号");
    client.global.set("workflowPackageId", response.body.id);
    client.global.set("workflowTrackingNumber", response.body.trackingNumber);
  });
%}

### 4. 创建卡车
POST {{baseUrl}}/api/trucks
Authorization: Bearer {{workflowToken}}
Content-Type: application/json

{
  "licensePlate": "测试A001",
  "capacity": 10.0,
  "currentX": 0,
  "currentY": 0
}

> {%
  client.test("卡车创建成功", function() {
    client.assert(response.status === 201, "状态码应该是201");
    client.global.set("workflowTruckId", response.body.id);
  });
%}

### 5. 分配卡车
GET {{baseUrl}}/api/trucks/assign?x=100&y=100&capacity=1.0
Authorization: Bearer {{workflowToken}}

> {%
  client.test("卡车分配成功", function() {
    client.assert(response.status === 200, "状态码应该是200");
    client.assert(response.body !== null, "应该找到可用卡车");
  });
%}

### 6. 更新包裹状态为已取件
PATCH {{baseUrl}}/api/packages/{{workflowPackageId}}/status?status=PICKED_UP
Authorization: Bearer {{workflowToken}}

> {%
  client.test("包裹状态更新成功", function() {
    client.assert(response.status === 200, "状态码应该是200");
    client.assert(response.body.status === "PICKED_UP", "状态应该是PICKED_UP");
  });
%}

### 7. 追踪包裹
GET {{baseUrl}}/api/packages/track/{{workflowTrackingNumber}}

> {%
  client.test("包裹追踪成功", function() {
    client.assert(response.status === 200, "状态码应该是200");
    client.assert(response.body.status === "PICKED_UP", "状态应该是PICKED_UP");
  });
%}
```

### 9.5 错误处理测试

#### 测试各种错误情况
```http
### 错误处理测试套件

### 1. 测试400错误 - 无效数据
POST {{baseUrl}}/api/users/register
Content-Type: application/json

{
  "username": "ab",
  "email": "invalid-email",
  "password": "123"
}

> {%
  client.test("应该返回400错误", function() {
    client.assert(response.status === 400, "状态码应该是400");
  });
%}

### 2. 测试401错误 - 未认证
GET {{baseUrl}}/api/users

> {%
  client.test("应该返回401错误", function() {
    client.assert(response.status === 401, "状态码应该是401");
  });
%}

### 3. 测试404错误 - 资源不存在
GET {{baseUrl}}/api/users/99999
Authorization: Bearer {{authToken}}

> {%
  client.test("应该返回404错误", function() {
    client.assert(response.status === 404, "状态码应该是404");
  });
%}

### 4. 测试409错误 - 重复数据
POST {{baseUrl}}/api/users/register
Content-Type: application/json

{
  "username": "demo_user",
  "email": "demo@tutorial.com",
  "password": "Demo123456",
  "confirmPassword": "Demo123456"
}

> {%
  client.test("应该返回400错误（用户已存在）", function() {
    client.assert(response.status === 400, "状态码应该是400");
  });
%}
```

### 9.6 性能测试基础

#### 响应时间测试
```http
### 性能测试

### 1. 测试登录响应时间
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "email": "demo@tutorial.com",
  "password": "Demo123456"
}

> {%
  const responseTime = response.responseTime;
  client.test("响应时间应该小于500ms", function() {
    client.assert(responseTime < 500, `响应时间 ${responseTime}ms 应该小于500ms`);
  });
  
  console.log(`登录响应时间: ${responseTime}ms`);
%}

### 2. 测试查询响应时间
GET {{baseUrl}}/api/packages?page=0&size=100
Authorization: Bearer {{authToken}}

> {%
  const responseTime = response.responseTime;
  client.test("大量数据查询响应时间应该小于1000ms", function() {
    client.assert(responseTime < 1000, `响应时间 ${responseTime}ms 应该小于1000ms`);
  });
  
  console.log(`查询响应时间: ${responseTime}ms`);
%}
```

### 9.7 测试最佳实践

#### 1. 测试命名规范
```http
### [模块名] - [功能] - [预期结果]
### 用户管理 - 注册新用户 - 成功返回201
### 包裹管理 - 无效追踪号查询 - 返回404
```

#### 2. 测试数据管理
```http
### 使用一致的测试数据
# 测试用户
@testEmail = test_{{$timestamp}}@example.com
@testUsername = test_user_{{$timestamp}}

### 注册测试用户
POST {{baseUrl}}/api/users/register
Content-Type: application/json

{
  "username": "{{testUsername}}",
  "email": "{{testEmail}}",
  "password": "Test123456",
  "confirmPassword": "Test123456"
}
```

#### 3. 测试清理
```http
### 清理测试数据
DELETE {{baseUrl}}/api/users/{{testUserId}}
Authorization: Bearer {{authToken}}

> {%
  client.test("测试数据清理成功", function() {
    client.assert(response.status === 204, "状态码应该是204");
  });
%}
```

#### 4. 测试文档化
```http
/**
 * 用户注册测试套件
 * 
 * 测试内容：
 * 1. 正常注册流程
 * 2. 数据验证错误
 * 3. 重复邮箱错误
 * 4. 密码确认不匹配错误
 * 
 * 前置条件：无
 * 后置条件：清理测试用户
 */

### 1. 正常注册
POST {{baseUrl}}/api/users/register
Content-Type: application/json

{
  "username": "normal_user",
  "email": "normal@test.com",
  "password": "Normal123456",
  "confirmPassword": "Normal123456"
}
```

---

## 10. 高级功能

现在我们来实现一些企业级应用中常见的高级功能。

### 10.1 Redis缓存集成

#### 添加Redis依赖
确保`pom.xml`中有Redis依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

#### 配置Redis
在`application.yml`中添加Redis配置：

```yaml
spring:
  # Redis配置
  redis:
    host: localhost
    port: 6379
    password:  # 如果有密码
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
```

#### 创建Redis配置类
```java
package com.tutorial.ups.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 */
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
```

#### 创建缓存服务
```java
package com.tutorial.ups.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务
 * 
 * 功能说明：
 * - 提供统一的缓存操作接口
 * - 支持过期时间设置
 * - 处理缓存异常
 */
@Service
public class CacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }
    
    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    
    /**
     * 获取缓存
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 获取缓存并指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        return value != null ? (T) value : null;
    }
    
    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }
    
    /**
     * 检查缓存是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
    
    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }
    
    /**
     * 获取过期时间
     */
    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }
}
```

#### 在用户服务中集成缓存
```java
// 在UserService中添加缓存

@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheService cacheService;
    
    private static final String USER_CACHE_PREFIX = "user:";
    private static final long CACHE_TIMEOUT = 30; // 30分钟
    
    @Autowired
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      CacheService cacheService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cacheService = cacheService;
    }
    
    /**
     * 根据ID查找用户（带缓存）
     */
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> findById(Long id) {
        String cacheKey = USER_CACHE_PREFIX + id;
        
        // 先从缓存获取
        UserResponseDto cachedUser = cacheService.get(cacheKey, UserResponseDto.class);
        if (cachedUser != null) {
            logger.debug("从缓存获取用户: {}", id);
            return Optional.of(cachedUser);
        }
        
        // 缓存未命中，从数据库查询
        Optional<UserResponseDto> userOpt = userRepository.findById(id)
                .map(UserResponseDto::fromEntity);
        
        // 如果找到用户，放入缓存
        if (userOpt.isPresent()) {
            cacheService.set(cacheKey, userOpt.get(), CACHE_TIMEOUT, TimeUnit.MINUTES);
            logger.debug("用户信息已缓存: {}", id);
        }
        
        return userOpt;
    }
    
    /**
     * 更新用户时清除缓存
     */
    public UserResponseDto updateUser(Long userId, RegisterRequestDto request) {
        // 更新用户逻辑...
        UserResponseDto updatedUser = // ... 更新逻辑
        
        // 清除缓存
        String cacheKey = USER_CACHE_PREFIX + userId;
        cacheService.delete(cacheKey);
        logger.debug("用户缓存已清除: {}", userId);
        
        return updatedUser;
    }
    
    private static final org.slf4j.Logger logger = 
        org.slf4j.LoggerFactory.getLogger(UserService.class);
}
```

### 10.2 全局异常处理

#### 创建全局异常处理器
```java
package com.tutorial.ups.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * 功能说明：
 * - 统一处理应用程序中的异常
 * - 返回标准化的错误响应
 * - 记录错误日志
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理数据验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("输入数据验证失败")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(errors)
                .build();
        
        logger.warn("数据验证失败: {}", errors);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理业务逻辑异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        logger.warn("业务逻辑异常: {}", ex.getMessage());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        logger.warn("状态冲突异常: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        logger.warn("资源未找到: {}", ex.getMessage());
        
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("服务器内部错误，请稍后重试")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        logger.error("系统异常: ", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    private static final org.slf4j.Logger logger = 
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
}
```

#### 创建错误响应实体
```java
package com.tutorial.ups.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 错误响应实体
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, Object> details;
    
    // 构造函数
    public ErrorResponse() {}
    
    private ErrorResponse(Builder builder) {
        this.timestamp = builder.timestamp;
        this.status = builder.status;
        this.error = builder.error;
        this.message = builder.message;
        this.path = builder.path;
        this.details = builder.details;
    }
    
    // Builder模式
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, Object> details;
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder status(int status) {
            this.status = status;
            return this;
        }
        
        public Builder error(String error) {
            this.error = error;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder path(String path) {
            this.path = path;
            return this;
        }
        
        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }
        
        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
```

#### 自定义异常类
```java
package com.tutorial.ups.exception;

/**
 * 资源未找到异常
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s 未找到，%s: %s", resource, field, value));
    }
}
```

### 10.3 API文档集成（Swagger）

#### 添加Swagger依赖
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

#### 配置Swagger
```java
package com.tutorial.ups.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger API文档配置
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UPS Tutorial System API")
                        .description("物流管理系统API文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Tutorial Team")
                                .email("support@tutorial.com")
                                .url("https://tutorial.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }
    
    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}
```

#### 添加API文档注解
```java
// 在Controller中添加Swagger注解

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户相关的API接口")
public class UserController {
    
    @Operation(
        summary = "用户注册",
        description = "创建新的用户账户",
        responses = {
            @ApiResponse(responseCode = "201", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "输入数据无效")
        }
    )
    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(
            @Valid @RequestBody 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "用户注册信息",
                required = true
            ) RegisterRequestDto request) {
        // 实现逻辑...
    }
    
    @Operation(
        summary = "获取用户信息",
        description = "根据用户ID获取用户详细信息",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long id) {
        // 实现逻辑...
    }
}
```

### 10.4 日志管理

#### 配置日志
在`application.yml`中配置日志：

```yaml
logging:
  level:
    com.tutorial.ups: DEBUG
    org.springframework.security: INFO
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/ups-tutorial.log
    max-size: 100MB
    max-history: 30
```

#### 创建日志切面
```java
package com.tutorial.ups.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 日志切面
 * 
 * 功能说明：
 * - 自动记录方法调用日志
 * - 记录方法执行时间
 * - 记录异常信息
 */
@Aspect
@Component
public class LoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    
    /**
     * 定义切点：所有Controller方法
     */
    @Pointcut("execution(* com.tutorial.ups.controller..*(..))")
    public void controllerPointcut() {}
    
    /**
     * 定义切点：所有Service方法
     */
    @Pointcut("execution(* com.tutorial.ups.service..*(..))")
    public void servicePointcut() {}
    
    /**
     * 方法执行前记录日志
     */
    @Before("controllerPointcut() || servicePointcut()")
    public void logBefore(JoinPoint joinPoint) {
        logger.debug("调用方法: {}.{}({})",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                joinPoint.getArgs());
    }
    
    /**
     * 方法执行后记录日志
     */
    @AfterReturning(pointcut = "controllerPointcut() || servicePointcut()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        logger.debug("方法返回: {}.{} -> {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                result != null ? result.getClass().getSimpleName() : "null");
    }
    
    /**
     * 方法抛出异常时记录日志
     */
    @AfterThrowing(pointcut = "controllerPointcut() || servicePointcut()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        logger.error("方法异常: {}.{} -> {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                exception.getMessage(), exception);
    }
    
    /**
     * 记录方法执行时间
     */
    @Around("controllerPointcut()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            logger.info("API执行时间: {}.{} -> {}ms",
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    executionTime);
            
            return result;
        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            logger.error("API执行异常: {}.{} -> {}ms, 异常: {}",
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    executionTime,
                    throwable.getMessage());
            
            throw throwable;
        }
    }
}
```

### 10.5 健康检查

#### 添加Actuator依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### 配置健康检查
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  health:
    redis:
      enabled: true
    db:
      enabled: true
```

#### 自定义健康检查
```java
package com.tutorial.ups.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 自定义Redis健康检查
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Health health() {
        try {
            String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            
            if ("PONG".equals(result)) {
                return Health.up()
                        .withDetail("redis", "Available")
                        .withDetail("status", "UP")
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis", "Unavailable")
                        .withDetail("status", "DOWN")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

---

## 总结

🎉 **恭喜！** 你已经完成了一个完整的Java后端开发教程。

### 你学到了什么

#### 核心技能
- **Spring Boot框架**：从零搭建企业级应用
- **数据库设计**：JPA实体关系映射，PostgreSQL使用
- **API开发**：RESTful API设计与实现
- **安全认证**：JWT Token认证与权限控制
- **测试技能**：API测试，自动化测试脚本

#### 项目架构
- **分层架构**：Controller -> Service -> Repository模式
- **DTO模式**：数据传输对象，保护内部结构
- **异常处理**：统一的错误处理机制
- **缓存集成**：Redis缓存提升性能
- **文档生成**：Swagger API文档

#### 开发工具
- **IntelliJ IDEA**：专业Java开发环境
- **HTTP Client**：API测试与调试
- **Postman**：接口测试与团队协作
- **Maven**：项目构建与依赖管理

### 项目成果

你已经成功构建了一个包含以下模块的物流管理系统：

1. **用户管理系统**
   - 用户注册、登录、信息管理
   - 角色权限控制
   - 数据验证与安全

2. **包裹追踪系统**
   - 包裹创建、状态更新
   - 追踪号码生成
   - 查询与搜索功能

3. **卡车管理系统**
   - 车辆注册、状态管理
   - 位置追踪、智能调度
   - 距离计算算法

4. **安全认证系统**
   - JWT Token认证
   - 权限控制
   - 会话管理

### 下一步建议

#### 进阶学习方向
1. **微服务架构**：Spring Cloud、服务注册发现
2. **消息队列**：RabbitMQ、Apache Kafka
3. **容器化部署**：Docker、Kubernetes
4. **监控与运维**：Prometheus、Grafana
5. **前端开发**：React、Vue.js集成

#### 实践项目
1. **扩展现有功能**：添加订单管理、支付集成
2. **性能优化**：数据库优化、缓存策略
3. **部署上线**：云服务器部署、域名配置
4. **团队协作**：Git工作流、代码审查

### 开发最佳实践回顾

1. **代码质量**
   - 统一的命名规范
   - 完善的注释文档
   - 合理的异常处理

2. **测试驱动**
   - 完整的API测试
   - 自动化测试脚本
   - 错误情况覆盖

3. **安全考虑**
   - 数据验证
   - 权限控制
   - 敏感信息保护

4. **性能优化**
   - 数据库索引
   - 缓存策略
   - 分页查询

### 常见问题解答

**Q: 如何在生产环境部署？**
A: 使用Docker容器化，配置环境变量，使用云服务数据库

**Q: 如何处理大量并发？**
A: 使用连接池、缓存、负载均衡、数据库优化

**Q: 如何保证数据安全？**
A: HTTPS传输、数据加密、定期备份、权限控制

**Q: 如何监控应用状态？**
A: 使用Actuator健康检查、日志监控、性能指标收集

### 学习资源推荐

#### 官方文档
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Spring Security参考指南](https://spring.io/projects/spring-security)
- [JPA规范文档](https://spring.io/projects/spring-data-jpa)

#### 在线教程
- Spring Boot实战教程
- Java并发编程指南
- RESTful API设计最佳实践

#### 开源项目
- 研究优秀的开源项目代码
- 参与开源项目贡献
- 构建个人项目组合

---

**祝贺你完成了这个完整的Java后端开发教程！** 🚀

现在你已经具备了开发企业级Java后端应用的基础技能。继续实践和学习，你将成为一名优秀的后端开发工程师！

记住：**实践是最好的老师**。多动手写代码，多思考业务逻辑，多与其他开发者交流学习。

**编程之路，始于足下！** 💪