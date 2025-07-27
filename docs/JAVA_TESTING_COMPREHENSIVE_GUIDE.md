# Java 测试开发完整指南 - 基于 Mini-UPS 项目实践

> 本指南基于 Mini-UPS 项目的实际测试架构，提供从基础到高级的 Java 测试开发教程

## 目录

1. [测试架构概览](#测试架构概览)
2. [环境配置与依赖管理](#环境配置与依赖管理)
3. [单元测试 (Unit Tests)](#单元测试-unit-tests)
4. [集成测试 (Integration Tests)](#集成测试-integration-tests)
5. [控制器测试 (Controller Tests)](#控制器测试-controller-tests)
6. [安全测试 (Security Tests)](#安全测试-security-tests)
7. [测试数据管理](#测试数据管理)
8. [测试配置与Profile](#测试配置与profile)
9. [代码覆盖率与质量](#代码覆盖率与质量)
10. [最佳实践与常见陷阱](#最佳实践与常见陷阱)

---

## 测试架构概览

### 🏗️ 分层测试架构

Mini-UPS 项目采用了完整的分层测试架构，基于实际项目结构：

```
📁 src/test/java/com/miniups/
├── 📁 config/                    # 测试配置层
│   ├── TestConfig.java           # 全局测试配置 (Mock外部服务)
│   ├── TestRabbitConfig.java     # RabbitMQ测试配置
│   └── ExceptionMetricsConfigTest.java # 异常监控配置测试
├── 📁 controller/                # API控制器测试
│   ├── UserControllerTest.java           # 用户管理API测试
│   ├── AuthControllerTest.java           # 认证API测试
│   ├── TrackingControllerIntegrationTest.java # 追踪API集成测试
│   ├── TruckManagementControllerTest.java     # 卡车管理API测试
│   ├── WorldSimulatorControllerTest.java      # 外部服务集成测试
│   └── 📁 security/              # API安全测试
│       ├── UserControllerSecurityTest.java    # 用户权限控制测试
│       ├── TruckControllerSecurityTest.java   # 卡车权限控制测试
│       └── AdminControllerSecurityTest.java   # 管理员权限测试
├── 📁 service/                   # 业务逻辑层测试
│   ├── UserServiceTest.java              # 用户服务测试
│   ├── AuthServiceTest.java              # 认证服务测试
│   ├── TruckManagementServiceTest.java   # 卡车管理服务测试
│   ├── TrackingServiceTest.java          # 追踪服务测试
│   ├── AmazonIntegrationServiceTest.java # Amazon集成服务测试
│   ├── WorldSimulatorServiceTest.java    # 世界模拟器服务测试
│   └── AdminServiceTest.java             # 管理员服务测试
├── 📁 repository/                # 数据访问层测试
│   └── ShipmentRepositoryIntegrationTest.java # JPA数据库集成测试
├── 📁 security/                  # 安全组件测试
│   ├── JwtTokenProviderTest.java         # JWT令牌提供者单元测试
│   ├── CustomUserDetailsServiceTest.java # 用户详情服务测试
│   ├── JwtSecurityTest.java              # JWT安全集成测试
│   └── SecurityIntegrationTest.java      # 完整安全集成测试
├── 📁 concurrency/               # 并发性能测试
│   ├── ConcurrencyTestBase.java          # 并发测试基类
│   ├── ConcurrentUserRegistrationTest.java   # 并发用户注册测试
│   ├── ConcurrentTruckAssignmentTest.java     # 并发卡车分配测试
│   ├── ConcurrentOrderProcessingTest.java     # 并发订单处理测试
│   ├── ConcurrentTrackingNumberGenerationTest.java # 并发编号生成测试
│   └── PerformanceBenchmarkTest.java     # 性能基准测试
├── 📁 exception/                 # 异常处理测试
│   └── GlobalExceptionHandlerTest.java  # 全局异常处理器测试
├── 📁 util/                      # 测试工具类
│   └── TestDataFactory.java             # 测试数据工厂
└── MiniUpsApplicationTests.java  # 应用程序启动测试
```

### 🎯 测试分类与策略

| 测试类型 | 运行频率 | 目标 | 工具 |
|---------|---------|-----|-----|
| **单元测试** | 每次提交 | 业务逻辑验证 | JUnit 5, Mockito |
| **集成测试** | CI/CD | 组件协作验证 | TestContainers, Spring Boot Test |
| **安全测试** | 每次发布 | 权限与认证验证 | Spring Security Test |
| **API测试** | 每次提交 | 接口契约验证 | MockMvc, WireMock |

---

## 环境配置与依赖管理

### 🚀 环境准备与验证指令

#### **前置条件检查**
```bash
# 检查Java版本 (需要17+)
java -version

# 检查Maven版本 (需要3.6+) 
mvn -version

# 检查Docker版本 (TestContainers需要)
docker --version
docker compose version

# 进入项目后端目录
cd backend

# 清理之前的构建
mvn clean
```

#### **项目依赖验证**
```bash
# 验证Maven依赖
mvn dependency:tree

# 检查依赖冲突
mvn dependency:analyze

# 更新依赖到最新版本
mvn versions:display-dependency-updates
```

### 📦 Maven 依赖配置

```xml
<!-- pom.xml 中的测试依赖 -->
<dependencies>
    <!-- Spring Boot 测试启动器 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Security 测试 -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- TestContainers 集成测试 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- H2 内存数据库 (快速单元测试) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- WireMock 外部服务模拟 -->
    <dependency>
        <groupId>com.github.tomakehurst</groupId>
        <artifactId>wiremock-jre8</artifactId>
        <version>2.35.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 🔧 Maven 插件配置

```xml
<build>
    <plugins>
        <!-- Surefire: 单元测试 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.2</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IntegrationTest.java</exclude>
                </excludes>
            </configuration>
        </plugin>
        
        <!-- Failsafe: 集成测试 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.2.2</version>
            <configuration>
                <includes>
                    <include>**/*IntegrationTest.java</include>
                    <include>**/*IT.java</include>
                </includes>
            </configuration>
        </plugin>
        
        <!-- JaCoCo: 代码覆盖率 -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## 单元测试 (Unit Tests)

### 🎯 服务层测试示例

服务层是业务逻辑的核心，需要重点测试：

```java
package com.miniups.service;

import com.miniups.exception.UserNotFoundException;
import com.miniups.model.dto.user.UpdateUserDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import com.miniups.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 业务逻辑测试
 * 重点测试：业务规则、异常处理、数据转换
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserService 业务逻辑测试")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean  // 模拟数据访问层
    private UserRepository userRepository;

    @MockBean  // 模拟密码编码器
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        // 使用工厂类创建测试数据
        testUser = TestDataFactory.createTestUser();
        testUser.setId(1L);
        
        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setUsername("testuser");
        testUserDto.setEmail("test@example.com");
        testUserDto.setRole(UserRole.USER);
        testUserDto.setEnabled(true);
    }

    @Test
    @DisplayName("根据用户名获取用户信息 - 成功场景")
    void getCurrentUserInfo_Success() {
        // Given (准备阶段)
        when(userRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(testUser));

        // When (执行阶段)
        UserDto result = userService.getCurrentUserInfo("testuser");

        // Then (验证阶段)
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.isEnabled());

        // 验证方法调用
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("根据用户名获取用户信息 - 用户不存在")
    void getCurrentUserInfo_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            userService.getCurrentUserInfo("nonexistent");
        });

        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("更新用户信息 - 邮箱冲突")
    void updateUser_EmailExists() {
        // Given
        UpdateUserDto updateDto = new UpdateUserDto();
        updateDto.setEmail("admin@example.com"); // 已存在的邮箱

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndIdNot("admin@example.com", 1L))
            .thenReturn(true);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.updateUser(1L, updateDto);
        });

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1))
            .existsByEmailAndIdNot("admin@example.com", 1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("测试空值处理")
    void testNullHandling() {
        // 验证参数校验
        assertThrows(IllegalArgumentException.class, () -> {
            userService.getCurrentUserInfo(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            userService.getCurrentUserInfo("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(null);
        });
    }
}
```

### 📋 单元测试关键要点

1. **AAA 模式**: Arrange (准备) → Act (执行) → Assert (验证)
2. **Mock 使用**: 用 `@MockBean` 模拟依赖，用 `@InjectMocks` 注入被测试类
3. **边界测试**: 测试空值、无效输入、边界条件
4. **异常测试**: 验证异常抛出和错误处理
5. **验证调用**: 使用 `verify()` 确认方法被正确调用

### 🚀 单元测试执行指令

#### **基础单元测试运行**
```bash
# 运行所有单元测试 (最快，开发时常用)
mvn test

# 运行特定测试类
mvn test -Dtest=UserServiceTest

# 运行特定测试方法
mvn test -Dtest=UserServiceTest#getCurrentUserInfo_Success

# 运行多个测试类
mvn test -Dtest=UserServiceTest,AuthServiceTest

# 运行包下所有测试
mvn test -Dtest=com.miniups.service.*Test
```

#### **调试和分析**
```bash
# 调试单个测试 (会等待调试器连接到5005端口)
mvn test -Dtest=UserServiceTest -Dmaven.surefire.debug

# 查看详细测试输出
mvn test -Dtest=UserServiceTest -X

# 显示测试执行时间
mvn test -Dtest=UserServiceTest -Dsurefire.printSummary=true

# 跳过测试编译错误继续运行
mvn test -Dmaven.test.failure.ignore=true
```

#### **服务层专项测试**
```bash
# 运行所有服务层测试
mvn test -Dtest=*ServiceTest

# 运行业务逻辑核心测试
mvn test -Dtest=UserServiceTest,AuthServiceTest,TruckManagementServiceTest

# 测试异常处理场景
mvn test -Dtest=*ServiceTest -Dgroups=exception
```

---

## 集成测试 (Integration Tests)

### 🔗 数据库集成测试

使用 TestContainers 进行真实数据库测试：

```java
package com.miniups.repository;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.UserRole;
import com.miniups.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shipment Repository 集成测试
 * 使用真实 PostgreSQL 数据库测试复杂查询
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ShipmentRepository 数据访问测试")
class ShipmentRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShipmentRepository shipmentRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createTestUser();
        entityManager.persistAndFlush(testUser);
    }

    @Test
    @DisplayName("根据状态查询订单")
    void findByStatus_ShouldReturnCorrectShipments() {
        // Given
        Shipment createdShipment = TestDataFactory.createTestShipment(
            testUser, "UPS123456789", ShipmentStatus.CREATED
        );
        Shipment deliveredShipment = TestDataFactory.createTestShipment(
            testUser, "UPS987654321", ShipmentStatus.DELIVERED
        );
        
        entityManager.persistAndFlush(createdShipment);
        entityManager.persistAndFlush(deliveredShipment);

        // When
        List<Shipment> createdShipments = shipmentRepository
            .findByStatus(ShipmentStatus.CREATED);
        List<Shipment> deliveredShipments = shipmentRepository
            .findByStatus(ShipmentStatus.DELIVERED);

        // Then
        assertThat(createdShipments).hasSize(1);
        assertThat(createdShipments.get(0).getUpsTrackingId())
            .isEqualTo("UPS123456789");
        
        assertThat(deliveredShipments).hasSize(1);
        assertThat(deliveredShipments.get(0).getUpsTrackingId())
            .isEqualTo("UPS987654321");
    }

    @Test
    @DisplayName("复杂查询：根据用户和状态查询")
    void findByUserAndStatus_ShouldReturnUserSpecificShipments() {
        // Given
        User anotherUser = TestDataFactory.createTestUser(
            "anotheruser", "another@example.com", UserRole.USER
        );
        entityManager.persistAndFlush(anotherUser);

        Shipment userShipment = TestDataFactory.createTestShipment(
            testUser, "UPS111111111", ShipmentStatus.CREATED
        );
        Shipment anotherUserShipment = TestDataFactory.createTestShipment(
            anotherUser, "UPS222222222", ShipmentStatus.CREATED
        );
        
        entityManager.persistAndFlush(userShipment);
        entityManager.persistAndFlush(anotherUserShipment);

        // When
        List<Shipment> userShipments = shipmentRepository
            .findByUserAndStatus(testUser, ShipmentStatus.CREATED);

        // Then
        assertThat(userShipments).hasSize(1);
        assertThat(userShipments.get(0).getUser().getUsername())
            .isEqualTo(testUser.getUsername());
        assertThat(userShipments.get(0).getUpsTrackingId())
            .isEqualTo("UPS111111111");
    }
}
```

### 🏠 集成测试基类

为了避免重复配置，创建基类：

```java
package com.miniups.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 集成测试基类
 * 提供真实数据库环境和统一配置
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgreSQLContainer = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("integration_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        
        // 禁用真实外部服务
        registry.add("app.world-simulator.enabled", () -> false);
        registry.add("app.amazon-integration.enabled", () -> false);
    }
}
```

### 🚀 集成测试执行指令

#### **基础集成测试运行**
```bash
# 运行所有集成测试 (需要Docker)
mvn verify

# 或者明确指定
mvn integration-test

# 运行特定集成测试
mvn verify -Dit.test=ShipmentRepositoryIntegrationTest

# 运行所有数据库相关集成测试
mvn verify -Dit.test=*RepositoryIntegrationTest
```

#### **TestContainers集成测试**
```bash
# 确保Docker正在运行
docker info

# 运行数据库集成测试 (会自动启动PostgreSQL容器)
mvn verify -Dit.test=*IntegrationTest

# 查看TestContainers日志
mvn verify -Dit.test=ShipmentRepositoryIntegrationTest -X

# 指定特定数据库版本测试
mvn verify -Dit.test=*IntegrationTest -Dtestcontainers.postgresql.version=15-alpine
```

#### **集成测试环境管理**
```bash
# 使用H2内存数据库进行快速集成测试
mvn verify -Dspring.profiles.active=test,h2

# 运行带外部服务的完整集成测试
mvn verify -Dspring.profiles.active=test,integration

# 清理TestContainers缓存
docker system prune -f
docker volume prune -f
```

#### **数据库集成测试故障排除**
```bash
# 检查PostgreSQL容器状态
docker ps | grep postgres

# 查看容器日志
docker logs $(docker ps -q --filter ancestor=postgres:15-alpine)

# 重新拉取测试镜像
docker pull postgres:15-alpine

# 使用本地PostgreSQL (如果TestContainers有问题)
mvn verify -Dspring.datasource.url=jdbc:postgresql://localhost:5432/test_db
```

---

## 控制器测试 (Controller Tests)

### 🌐 Web层测试

使用 `@WebMvcTest` 测试 REST API：

```java
package com.miniups.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.model.dto.user.UpdateUserDto;
import com.miniups.model.dto.user.UserDto;
import com.miniups.model.enums.UserRole;
import com.miniups.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController API 测试
 * 重点测试：HTTP状态码、JSON响应、权限控制
 */
@WebMvcTest(UserController.class)
@DisplayName("UserController API 测试")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto testUserDto;
    private UpdateUserDto updateUserDto;

    @BeforeEach
    void setUp() {
        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setUsername("testuser");
        testUserDto.setEmail("test@example.com");
        testUserDto.setRole(UserRole.USER);
        testUserDto.setEnabled(true);

        updateUserDto = new UpdateUserDto();
        updateUserDto.setEmail("updated@example.com");
        updateUserDto.setPhoneNumber("1234567890");
    }

    @Test
    @DisplayName("获取当前用户资料 - 成功")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getCurrentUserProfile_Success() throws Exception {
        // Given
        when(userService.getCurrentUserInfo("testuser"))
            .thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(get("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.message").value("Get user profile successful"))
                .andExpected(jsonPath("$.user.id").value(1))
                .andExpected(jsonPath("$.user.username").value("testuser"))
                .andExpected(jsonPath("$.user.email").value("test@example.com"))
                .andExpected(jsonPath("$.user.role").value("USER"));

        verify(userService, times(1)).getCurrentUserInfo("testuser");
    }

    @Test
    @DisplayName("获取当前用户资料 - 未认证")
    void getCurrentUserProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpected(status().isUnauthorized());

        verify(userService, never()).getCurrentUserInfo(anyString());
    }

    @Test
    @DisplayName("更新用户资料 - 数据验证失败")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void updateCurrentUserProfile_ValidationFailed() throws Exception {
        // Given - 无效邮箱格式
        UpdateUserDto invalidDto = new UpdateUserDto();
        invalidDto.setEmail("invalid-email");

        // When & Then
        mockMvc.perform(put("/api/users/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpected(status().isBadRequest());

        verify(userService, never()).getCurrentUserInfo(anyString());
        verify(userService, never()).updateUser(anyLong(), any(UpdateUserDto.class));
    }

    @Test
    @DisplayName("权限测试 - 普通用户访问管理员功能")
    @WithMockUser(username = "user", roles = {"USER"})
    void securityTest_UserAccessAdminFunction() throws Exception {
        mockMvc.perform(delete("/api/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpected(status().isForbidden());

        verify(userService, never()).deleteUser(anyLong());
    }

    @Test
    @DisplayName("CSRF 保护测试")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void csrfProtectionTest() throws Exception {
        // 不带CSRF token的请求应该被拒绝
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserDto)))
                .andDo(print())
                .andExpected(status().isForbidden());

        verify(userService, never()).updateUser(anyLong(), any(UpdateUserDto.class));
    }
}
```

### 📊 完整API集成测试

```java
package com.miniups.controller;

import com.miniups.config.BaseIntegrationTest;
import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.UserRepository;
import com.miniups.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 完整API集成测试
 * 测试真实数据库 + 完整Spring容器
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("用户管理完整流程测试")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createTestUser(
            "integrationuser", "integration@example.com", 
            UserRole.USER
        );
        testUser.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("完整用户注册登录流程")
    void completeUserRegistrationAndLoginFlow() throws Exception {
        // 1. 注册新用户
        String registerJson = """
            {
                "username": "newuser",
                "email": "newuser@example.com",
                "password": "newpassword123",
                "firstName": "New",
                "lastName": "User"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists());

        // 2. 登录
        String loginJson = """
            {
                "username": "newuser",
                "password": "newpassword123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.username").value("newuser"));
    }
}
```

### 🚀 控制器测试执行指令

#### **Web层测试运行**
```bash
# 运行所有控制器测试
mvn test -Dtest=*ControllerTest

# 运行特定控制器测试
mvn test -Dtest=UserControllerTest

# 运行API安全相关测试
mvn test -Dtest=*SecurityTest

# 运行MockMvc集成测试
mvn test -Dtest=*ControllerIntegrationTest
```

#### **API测试调试**
```bash
# 带详细HTTP请求日志运行控制器测试
mvn test -Dtest=UserControllerTest -Dlogging.level.org.springframework.web=DEBUG

# 查看MockMvc详细输出
mvn test -Dtest=UserControllerTest -Dlogging.level.org.springframework.test.web.servlet=DEBUG

# 运行特定API端点测试
mvn test -Dtest=UserControllerTest#getCurrentUserProfile_Success
```

#### **权限和认证测试**
```bash
# 运行所有权限控制测试
mvn test -Dtest=*ControllerSecurityTest

# 测试不同角色访问权限
mvn test -Dtest=UserControllerSecurityTest

# CSRF保护测试
mvn test -Dtest=*ControllerTest -Dspring.security.csrf.enabled=true
```

#### **完整API流程测试**
```bash
# 运行端到端API集成测试
mvn verify -Dit.test=*ControllerIntegrationTest

# 测试完整用户注册登录流程
mvn verify -Dit.test=UserControllerIntegrationTest#completeUserRegistrationAndLoginFlow

# 带真实数据库的API测试
mvn verify -Dit.test=*ControllerIntegrationTest -Dspring.profiles.active=test,integration
```

---

## 安全测试 (Security Tests)

### 🔒 JWT 令牌测试

```java
package com.miniups.security;

import com.miniups.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * JWT 令牌提供者测试
 * 测试令牌生成、验证、过期等核心功能
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JWT 令牌安全测试")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    
    private final String testUsername = "testuser";
    private final UserRole testRole = UserRole.USER;
    private final String testSecret = "testSecretKeytestSecretKeytestSecretKey"; // 32+ chars
    private final long testExpiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        jwtTokenProvider.setSecretKey(testSecret);
        jwtTokenProvider.setJwtExpiration(testExpiration);
        jwtTokenProvider.init(); // 初始化密钥
    }

    @Test
    @DisplayName("生成有效JWT令牌")
    void generateToken_ShouldCreateValidToken() {
        // When
        String token = jwtTokenProvider.generateToken(testUsername, testRole);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("从令牌中提取用户名")
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        // Given
        String token = jwtTokenProvider.generateToken(testUsername, testRole);

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(testUsername);
    }

    @Test
    @DisplayName("从令牌中提取用户角色")
    void getRoleFromToken_ShouldReturnCorrectRole() {
        // Given
        String token = jwtTokenProvider.generateToken(testUsername, testRole);

        // When
        UserRole extractedRole = jwtTokenProvider.getRoleFromToken(token);

        // Then
        assertThat(extractedRole).isEqualTo(testRole);
    }

    @Test
    @DisplayName("验证过期令牌")
    void validateToken_ShouldReturnFalseForExpiredToken() {
        // Given - 创建已过期的令牌
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider();
        expiredTokenProvider.setSecretKey(testSecret);
        expiredTokenProvider.setJwtExpiration(-1000); // 负数表示已过期
        expiredTokenProvider.init();
        
        String expiredToken = expiredTokenProvider.generateToken(testUsername, testRole);

        // When & Then
        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("验证无效格式令牌")
    void validateToken_ShouldReturnFalseForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThat(jwtTokenProvider.validateToken(invalidToken)).isFalse();
    }

    @Test
    @DisplayName("验证令牌过期时间设置")
    void generateToken_ShouldSetCorrectExpiration() {
        // Given
        String token = jwtTokenProvider.generateToken(testUsername, testRole);

        // When
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();

        // Then
        assertThat(expiration).isAfter(now);
        assertThat(expiration.getTime() - now.getTime())
            .isBetween(testExpiration - 5000, testExpiration + 5000); // 允许5秒误差
    }
}
```

### 🛡️ 端点安全测试

```java
package com.miniups.controller.security;

import com.miniups.config.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户控制器安全测试
 * 验证不同角色的访问权限
 */
@AutoConfigureMockMvc
@DisplayName("用户控制器安全访问测试")
class UserControllerSecurityTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("未认证用户访问受保护资源")
    void accessProtectedResource_WithoutAuthentication_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER角色访问自己的资源")
    @WithMockUser(username = "user", roles = {"USER"})
    void userAccessOwnResource_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER角色访问管理员资源")
    @WithMockUser(username = "user", roles = {"USER"})
    void userAccessAdminResource_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN角色访问管理员资源")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminAccessAdminResource_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DRIVER角色访问权限测试")
    @WithMockUser(username = "driver", roles = {"DRIVER"})
    void driverAccessPermissions() throws Exception {
        // DRIVER可以访问自己的资料
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk());
        
        // DRIVER不能访问用户管理功能
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
        
        // DRIVER可以访问卡车相关功能
        mockMvc.perform(get("/api/trucks"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("角色继承测试")
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER", "DRIVER"})
    void roleInheritanceTest() throws Exception {
        // ADMIN继承所有权限
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/trucks"))
                .andExpect(status().isOk());
    }
}
```

### 🚀 安全测试执行指令

#### **JWT令牌测试**
```bash
# 运行JWT安全组件测试
mvn test -Dtest=JwtTokenProviderTest

# 运行认证流程测试
mvn test -Dtest=AuthServiceTest

# 测试令牌生成和验证
mvn test -Dtest=JwtTokenProviderTest#generateToken_ShouldCreateValidToken
```

#### **端点安全测试**
```bash
# 运行所有安全访问控制测试
mvn test -Dtest=*SecurityTest

# 测试角色权限控制
mvn test -Dtest=UserControllerSecurityTest

# 测试未认证访问拒绝
mvn test -Dtest=*SecurityTest#*WithoutAuthentication*
```

#### **认证和授权集成测试**
```bash
# 运行完整安全集成测试
mvn verify -Dit.test=SecurityIntegrationTest

# 测试Spring Security配置
mvn test -Dtest=*SecurityTest -Dspring.profiles.active=test,security

# 调试安全过滤器链
mvn test -Dtest=SecurityIntegrationTest -Dlogging.level.org.springframework.security=DEBUG
```

#### **并发安全测试**
```bash
# 运行并发安全测试
mvn test -Dtest=*ConcurrencyTest

# 测试并发用户注册
mvn test -Dtest=ConcurrentUserRegistrationTest

# 性能和安全压力测试
mvn test -Dtest=PerformanceBenchmarkTest -DthreadCount=50
```

---

## 测试数据管理

### 🏭 测试数据工厂

以下是项目实际使用的测试数据工厂，提供完整的测试数据创建方法：

```java
package com.miniups.util;

import com.miniups.model.entity.*;
import com.miniups.model.enums.*;
import com.miniups.model.dto.amazon.AmazonOrderDto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 测试数据工厂 - 创建测试实体和DTO
 * 
 * 设计原则：
 * 1. 提供一致的测试数据创建方法
 * 2. 支持默认值和自定义配置
 * 3. 包含边界值和异常情况的测试数据
 * 4. 考虑JPA ID生成策略的兼容性
 */
public class TestDataFactory {

    // =========================== 用户相关 ===========================

    /**
     * 创建默认测试用户
     * 返回具有基本属性的USER角色用户
     */
    public static User createTestUser() {
        return createTestUser("testuser", "test@example.com", UserRole.USER);
    }

    /**
     * 创建自定义测试用户
     * 
     * @param username 用户名
     * @param email    邮箱地址
     * @param role     用户角色
     * @return 配置好的User实体
     */
    public static User createTestUser(String username, String email, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword123"); // 在测试中使用固定密码
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * 创建管理员用户
     */
    public static User createTestAdmin() {
        return createTestUser("admin", "admin@example.com", UserRole.ADMIN);
    }

    /**
     * 创建司机用户
     */
    public static User createTestDriver() {
        return createTestUser("driver", "driver@example.com", UserRole.DRIVER);
    }

    // =========================== 订单相关 ===========================

    /**
     * 创建默认测试订单
     */
    public static Shipment createTestShipment() {
        return createTestShipment(createTestUser(), "UPS123456789", ShipmentStatus.CREATED);
    }

    /**
     * 创建自定义测试订单
     * 
     * @param user           订单所属用户
     * @param trackingNumber UPS追踪号
     * @param status         订单状态
     * @return 配置好的Shipment实体
     */
    public static Shipment createTestShipment(User user, String trackingNumber, ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setUpsTrackingId(trackingNumber);
        shipment.setUser(user);
        shipment.setStatus(status);
        shipment.setWeight(BigDecimal.valueOf(5.0));
        shipment.setOriginX(10);
        shipment.setOriginY(20);
        shipment.setDestX(15);
        shipment.setDestY(25);
        shipment.setDeliveryAddress("123 Main St");
        shipment.setDeliveryCity("Test City");
        shipment.setDeliveryZipCode("12345");
        shipment.setCreatedAt(LocalDateTime.now());
        return shipment;
    }

    // =========================== 卡车相关 ===========================

    /**
     * 创建默认测试卡车
     */
    public static Truck createTestTruck() {
        return createTestTruck(1L, "TRUCK001", TruckStatus.IDLE);
    }

    /**
     * 创建自定义测试卡车
     * 
     * 注意：ID参数是为了兼容性保留，实际使用中不会设置ID
     * JPA会自动生成ID，手动设置ID可能导致持久化问题
     */
    public static Truck createTestTruck(Long id, String licensePlate, TruckStatus status) {
        Truck truck = new Truck();
        // 不设置ID - 让JPA自动生成
        truck.setLicensePlate(licensePlate);
        truck.setStatus(status);
        truck.setCurrentX(10);
        truck.setCurrentY(20);
        truck.setCapacity(1000);
        truck.setCurrentLoad(0.0);
        truck.setCreatedAt(LocalDateTime.now());
        return truck;
    }

    /**
     * 创建重型卡车（大容量）
     */
    public static Truck createTestHeavyDutyTruck() {
        Truck truck = createTestTruck(2L, "HEAVY001", TruckStatus.IDLE);
        truck.setCapacity(2000); // 双倍容量
        return truck;
    }

    // =========================== 复合对象相关 ===========================

    /**
     * 创建订单状态历史记录
     */
    public static ShipmentStatusHistory createTestStatusHistory(Shipment shipment, ShipmentStatus status, String notes) {
        ShipmentStatusHistory history = new ShipmentStatusHistory();
        history.setShipment(shipment);
        history.setStatus(status);
        history.setNotes(notes);
        history.setTimestamp(LocalDateTime.now());
        return history;
    }

    /**
     * 创建Amazon订单DTO
     * 用于测试外部系统集成
     */
    public static AmazonOrderDto createTestAmazonOrder() {
        AmazonOrderDto order = new AmazonOrderDto();
        order.setOrderId("AMZ123456");
        order.setCustomerEmail("customer@example.com");
        order.setTotalWeight(BigDecimal.valueOf(5.0));
        
        // 设置配送地址
        AmazonOrderDto.ShippingAddressDto address = new AmazonOrderDto.ShippingAddressDto();
        address.setStreet("123 Customer St");
        address.setCity("Customer City");
        address.setState("NY");
        address.setZipCode("54321");
        address.setX(15);
        address.setY(25);
        order.setShippingAddress(address);
        
        return order;
    }

    // =========================== 特殊场景测试数据 ===========================

    /**
     * 创建重量超限的订单（用于容量测试）
     */
    public static Shipment createHeavyShipment(User user) {
        Shipment shipment = createTestShipment(user, "UPS999888777", ShipmentStatus.CREATED);
        shipment.setWeight(BigDecimal.valueOf(1500.0)); // 超过普通卡车容量
        return shipment;
    }

    /**
     * 批量创建卡车车队
     * 注意：ID参数保留用于兼容性，实际不设置ID
     */
    public static Truck[] createTestFleet(int size) {
        Truck[] fleet = new Truck[size];
        for (int i = 0; i < size; i++) {
            fleet[i] = createTestTruck((long) (i + 1), "TRUCK" + String.format("%03d", i + 1), 
                                     i % 2 == 0 ? TruckStatus.IDLE : TruckStatus.DELIVERING);
            // 设置不同位置用于距离计算测试
            fleet[i].setCurrentX(10 + i);
            fleet[i].setCurrentY(20 + i);
        }
        return fleet;
    }

    /**
     * 创建位于特定坐标的订单（用于距离计算测试）
     */
    public static Shipment createShipmentAtLocation(User user, String trackingNumber, 
                                                   double pickupLat, double pickupLon, 
                                                   double deliveryLat, double deliveryLon) {
        Shipment shipment = createTestShipment(user, trackingNumber, ShipmentStatus.CREATED);
        shipment.setOriginX((int) pickupLat);
        shipment.setOriginY((int) pickupLon);
        shipment.setDestX((int) deliveryLat);
        shipment.setDestY((int) deliveryLon);
        return shipment;
    }

    /**
     * 创建位于特定位置的卡车（用于距离计算测试）
     */
    public static Truck createTruckAtLocation(Long id, String licensePlate, 
                                            double lat, double lon, TruckStatus status) {
        Truck truck = createTestTruck(id, licensePlate, status);
        truck.setCurrentX((int) lat);
        truck.setCurrentY((int) lon);
        return truck;
    }

    // =========================== 边界值和异常测试数据 ===========================

    /**
     * 创建无效的追踪号列表（用于负面测试）
     */
    public static String[] createInvalidTrackingNumbers() {
        return new String[]{
            null,
            "",
            "INVALID123",
            "ups123456789", // 小写
            "UPS12345", // 太短
            "UPS1234567890123", // 太长
            "ABC123456789", // 错误前缀
            "UPS12345678A" // 包含字母
        };
    }

    /**
     * 创建有效的追踪号列表（用于正向测试）
     */
    public static String[] createValidTrackingNumbers() {
        return new String[]{
            "UPS123456789",
            "UPS000000001",
            "UPS999999999",
            "UPS555444333"
        };
    }

    /**
     * 创建包含完整状态变更历史的订单
     */
    public static Shipment createShipmentWithFullHistory(User user, String trackingNumber) {
        Shipment shipment = createTestShipment(user, trackingNumber, ShipmentStatus.DELIVERED);
        // 实际的状态历史通常由服务层管理
        // 这里主要用于测试状态历史功能
        return shipment;
    }

    /**
     * 防止实例化的私有构造函数
     */
    private TestDataFactory() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }
}
```

### 📊 测试数据Builder模式

对于复杂的测试数据，使用Builder模式：

```java
package com.miniups.util;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.User;
import com.miniups.model.enums.ShipmentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单测试数据构建器
 * 提供链式调用构建复杂测试数据
 */
public class ShipmentTestBuilder {
    
    private Shipment shipment;

    public ShipmentTestBuilder() {
        this.shipment = new Shipment();
        // 设置默认值
        this.shipment.setUpsTrackingId("UPS123456789");
        this.shipment.setStatus(ShipmentStatus.CREATED);
        this.shipment.setWeight(BigDecimal.valueOf(5.0));
        this.shipment.setOriginX(10);
        this.shipment.setOriginY(20);
        this.shipment.setDestX(15);
        this.shipment.setDestY(25);
        this.shipment.setDeliveryAddress("123 Test Street");
        this.shipment.setDeliveryCity("Test City");
        this.shipment.setDeliveryZipCode("12345");
        this.shipment.setCreatedAt(LocalDateTime.now());
    }

    public static ShipmentTestBuilder aShipment() {
        return new ShipmentTestBuilder();
    }

    public ShipmentTestBuilder withTrackingId(String trackingId) {
        this.shipment.setUpsTrackingId(trackingId);
        return this;
    }

    public ShipmentTestBuilder withUser(User user) {
        this.shipment.setUser(user);
        return this;
    }

    public ShipmentTestBuilder withStatus(ShipmentStatus status) {
        this.shipment.setStatus(status);
        return this;
    }

    public ShipmentTestBuilder withWeight(double weight) {
        this.shipment.setWeight(BigDecimal.valueOf(weight));
        return this;
    }

    public ShipmentTestBuilder withOrigin(int x, int y) {
        this.shipment.setOriginX(x);
        this.shipment.setOriginY(y);
        return this;
    }

    public ShipmentTestBuilder withDestination(int x, int y) {
        this.shipment.setDestX(x);
        this.shipment.setDestY(y);
        return this;
    }

    public ShipmentTestBuilder withAddress(String address, String city, String zipCode) {
        this.shipment.setDeliveryAddress(address);
        this.shipment.setDeliveryCity(city);
        this.shipment.setDeliveryZipCode(zipCode);
        return this;
    }

    public ShipmentTestBuilder createdAt(LocalDateTime createdAt) {
        this.shipment.setCreatedAt(createdAt);
        return this;
    }

    public Shipment build() {
        return this.shipment;
    }
}

// 使用示例:
/*
Shipment heavyShipment = ShipmentTestBuilder.aShipment()
    .withUser(testUser)
    .withTrackingId("UPS999888777")
    .withWeight(1500.0)
    .withStatus(ShipmentStatus.PICKUP_SCHEDULED)
    .withOrigin(100, 200)
    .withDestination(150, 250)
    .build();
*/
```

### 🚀 测试数据管理执行指令

#### **测试数据工厂使用**
```bash
# 运行使用测试数据工厂的测试
mvn test -Dtest=*ServiceTest

# 验证测试数据一致性
mvn test -Dtest=TestDataFactoryTest

# 测试数据Builder模式使用
mvn test -Dtest=*BuilderTest
```

#### **测试数据清理**
```bash
# 运行带数据清理的测试
mvn test -Dtest=*IntegrationTest -Dspring.jpa.hibernate.ddl-auto=create-drop

# 手动触发测试数据清理
mvn test -Dtest=DataCleanupTest

# 验证测试数据隔离
mvn test -Dtest=*IsolationTest
```

#### **大数据量测试**
```bash
# 运行大数据量性能测试
mvn test -Dtest=*PerformanceTest -DdataSize=10000

# 测试批量数据处理
mvn test -Dtest=*BatchTest

# 内存使用测试
mvn test -Dtest=*MemoryTest -Xmx512m
```

---

## 测试配置与Profile

### ⚙️ 测试配置类

以下是项目实际使用的测试配置，包含所有关键的Mock服务：

```java
package com.miniups.config;

import com.miniups.service.WorldSimulatorService;
import com.miniups.service.AmazonIntegrationService;
import com.miniups.service.EventPublisherService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Mini-UPS 应用程序测试配置
 * 提供Mock Bean和测试专用配置
 * 
 * 主要功能：
 * 1. Mock所有外部服务依赖
 * 2. 配置测试专用的Bean (如快速密码编码器)
 * 3. 防止测试期间的真实外部调用
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Mock 世界模拟器服务
     * 防止测试中建立真实TCP连接
     * 在所有使用 @SpringBootTest 的测试中自动生效
     */
    @MockBean
    private WorldSimulatorService worldSimulatorService;

    /**
     * Mock Amazon集成服务
     * 防止测试中调用真实Amazon HTTP API
     * 包括订单同步、状态更新等操作
     */
    @MockBean
    private AmazonIntegrationService amazonIntegrationService;

    /**
     * Mock 事件发布服务
     * 防止测试中发送真实的RabbitMQ消息
     * 用于测试异步事件处理逻辑
     */
    @MockBean
    private EventPublisherService eventPublisherService;

    /**
     * 测试专用密码编码器
     * 使用BCrypt强度4而不是默认的10
     * 
     * 性能提升：
     * - 默认强度10: ~100ms/操作
     * - 测试强度4:  ~6ms/操作
     * - 测试执行速度提升约15倍
     * 
     * @return 快速密码编码器实例
     */
    @Bean
    @Primary
    @Profile("test")
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(4);
    }
}
```

### 📝 测试配置文件

`src/test/resources/application-test.yml`:

```yaml
# 测试环境配置
spring:
  profiles:
    active: test
  
  # 数据库配置 (将被TestContainers覆盖)
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.H2Dialect
  
  # Redis配置 (嵌入式)
  redis:
    host: localhost
    port: 6379
    password: 
    database: 1  # 使用不同的数据库避免冲突
  
  # 日志配置
  logging:
    level:
      com.miniups: DEBUG
      org.springframework.security: DEBUG
      org.springframework.web: DEBUG
      org.hibernate.SQL: DEBUG
      org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# 应用配置
app:
  # 禁用外部服务
  world-simulator:
    enabled: false
    host: localhost
    port: 12345
  
  amazon-integration:
    enabled: false
    api-url: http://localhost:8080
  
  # JWT配置 (测试用)
  jwt:
    secret: testSecretKeyForTestingPurposesOnly123456789
    expiration: 3600000  # 1小时

# 测试专用配置
test:
  containers:
    postgresql:
      image: postgres:15-alpine
      database: test_db
      username: test
      password: test
  
  performance:
    timeout: 30s
    max-concurrent-tests: 4
```

### 🚀 测试配置执行指令

#### **Profile配置测试**
```bash
# 使用测试配置运行
mvn test -Dspring.profiles.active=test

# 使用本地测试配置
mvn test -Dspring.profiles.active=local,test

# 使用H2内存数据库测试
mvn test -Dspring.datasource.url=jdbc:h2:mem:testdb

# 使用不同环境配置测试
mvn test -Dspring.profiles.active=test,integration
```

#### **测试配置验证**
```bash
# 验证测试配置加载
mvn test -Dtest=TestConfigTest

# 查看激活的Profile
mvn test -Dspring.profiles.active=test -Dlogging.level.org.springframework.core.env=DEBUG

# 测试配置属性绑定
mvn test -Dtest=ConfigurationPropertiesTest
```

#### **Mock服务配置**
```bash
# 运行Mock外部服务测试
mvn test -Dtest=*ServiceTest -Dspring.profiles.active=test,mock

# 验证Mock配置生效
mvn test -Dtest=MockConfigurationTest

# 外部服务集成测试
mvn verify -Dit.test=*IntegrationTest -Dspring.profiles.active=test,external
```

---

## 代码覆盖率与质量

### 📊 JaCoCo 代码覆盖率

配置 JaCoCo 生成详细的覆盖率报告：

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <excludes>
            <!-- 排除配置类 -->
            <exclude>**/config/**</exclude>
            <!-- 排除实体类 -->
            <exclude>**/model/entity/**</exclude>
            <!-- 排除DTO -->
            <exclude>**/model/dto/**</exclude>
            <!-- 排除枚举 -->
            <exclude>**/model/enums/**</exclude>
            <!-- 排除应用启动类 -->
            <exclude>**/MiniUpsApplication.class</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>INSTRUCTION</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum> <!-- 80%覆盖率 -->
                            </limit>
                            <limit>
                                <counter>CLASS</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.85</minimum> <!-- 85%类覆盖率 -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 📈 覆盖率分析命令

```bash
# 运行测试并生成覆盖率报告
mvn clean test jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html

# 检查覆盖率是否达标
mvn jacoco:check

# 生成聚合报告 (如果有多模块)
mvn jacoco:report-aggregate
```

### 🎯 覆盖率目标

| 层级 | 目标覆盖率 | 说明 |
|------|-----------|------|
| **服务层** | 90%+ | 核心业务逻辑必须充分测试 |
| **控制器层** | 85%+ | API端点和错误处理 |
| **安全层** | 95%+ | 安全相关代码需要最高覆盖率 |
| **工具类** | 80%+ | 公共工具方法测试 |
| **整体项目** | 80%+ | 整体目标覆盖率 |

---

## 最佳实践与常见陷阱

### ✅ 最佳实践

#### 1. **测试命名规范**

```java
// ✅ 好的命名
@Test
@DisplayName("用户注册时邮箱已存在应该抛出异常")
void register_shouldThrowException_whenEmailAlreadyExists() { }

// ❌ 不好的命名
@Test
void test1() { }

@Test
void testRegister() { }
```

#### 2. **AAA 模式**

```java
@Test
void updateUser_shouldUpdateUserInfo_whenDataIsValid() {
    // Arrange (准备)
    User existingUser = TestDataFactory.createTestUser();
    UpdateUserDto updateDto = new UpdateUserDto();
    updateDto.setEmail("new@example.com");
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(existingUser);
    
    // Act (执行)
    UserDto result = userService.updateUser(1L, updateDto);
    
    // Assert (验证)
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo("new@example.com");
    verify(userRepository).save(any(User.class));
}
```

#### 3. **独立性原则**

```java
// ✅ 每个测试独立
@BeforeEach
void setUp() {
    // 每个测试前重置状态
    testUser = TestDataFactory.createTestUser();
}

@AfterEach
void tearDown() {
    // 清理测试数据 (如果需要)
    userRepository.deleteAll();
}
```

#### 4. **边界值测试**

```java
@Test
@DisplayName("测试边界值和异常情况")
void validateTrackingNumber_shouldHandleBoundaryValues() {
    // 测试null值
    assertThrows(IllegalArgumentException.class, () -> 
        trackingService.validateTrackingNumber(null));
    
    // 测试空字符串
    assertThrows(IllegalArgumentException.class, () -> 
        trackingService.validateTrackingNumber(""));
    
    // 测试最小长度
    assertThrows(IllegalArgumentException.class, () -> 
        trackingService.validateTrackingNumber("UPS12345"));
    
    // 测试最大长度
    assertThrows(IllegalArgumentException.class, () -> 
        trackingService.validateTrackingNumber("UPS1234567890123"));
    
    // 测试有效值
    assertDoesNotThrow(() -> 
        trackingService.validateTrackingNumber("UPS123456789"));
}
```

### ❌ 常见陷阱

#### 1. **过度依赖集成测试**

```java
// ❌ 为简单逻辑写集成测试
@SpringBootTest
@AutoConfigureMockMvc
class UserUtilsIntegrationTest {
    @Test
    void testFormatUsername() {
        String result = UserUtils.formatUsername("  testUser  ");
        assertEquals("testuser", result);
    }
}

// ✅ 简单逻辑用单元测试
class UserUtilsTest {
    @Test
    void formatUsername_shouldTrimAndLowercase() {
        String result = UserUtils.formatUsername("  testUser  ");
        assertThat(result).isEqualTo("testuser");
    }
}
```

#### 2. **测试数据耦合**

```java
// ❌ 硬编码测试数据
@Test
void testGetUser() {
    // 依赖特定ID存在
    User user = userService.getUserById(123L);
    assertEquals("john@example.com", user.getEmail());
}

// ✅ 创建独立测试数据
@Test
void testGetUser() {
    User testUser = TestDataFactory.createTestUser();
    testUser = userRepository.save(testUser);
    
    User foundUser = userService.getUserById(testUser.getId());
    assertThat(foundUser.getEmail()).isEqualTo(testUser.getEmail());
}
```

#### 3. **Mock 过度使用**

```java
// ❌ 过度Mock导致测试失去意义
@Test
void calculateShippingCost_shouldReturnCost() {
    when(shipmentCalculator.getBaseRate()).thenReturn(10.0);
    when(shipmentCalculator.getWeightMultiplier()).thenReturn(0.5);
    when(shipmentCalculator.getDistanceMultiplier()).thenReturn(0.1);
    when(shipmentCalculator.calculateCost(any(), any())).thenReturn(15.0);
    
    // 实际上只是在测试Mock返回值
    double cost = shippingService.calculateCost(shipment);
    assertEquals(15.0, cost);
}

// ✅ 测试真实逻辑
@Test
void calculateShippingCost_shouldCalculateCorrectly() {
    Shipment shipment = TestDataFactory.createTestShipment();
    shipment.setWeight(BigDecimal.valueOf(10.0));
    shipment.setOriginX(0);
    shipment.setOriginY(0);
    shipment.setDestX(10);
    shipment.setDestY(10);
    
    double cost = shippingService.calculateCost(shipment);
    
    // 验证实际计算逻辑
    assertThat(cost).isGreaterThan(0);
    assertThat(cost).isEqualTo(expectedCost, within(0.01));
}
```

#### 4. **忽略异步和并发测试**

```java
// ✅ 异步操作测试
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void processShipmentAsync_shouldCompleteSuccessfully() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    
    shipmentService.processShipmentAsync(shipment.getId(), result -> {
        assertThat(result.isSuccess()).isTrue();
        latch.countDown();
    });
    
    assertTrue(latch.await(3, TimeUnit.SECONDS));
}

// ✅ 并发安全测试
@Test
void concurrent_userRegistration_shouldHandleRaceCondition() throws InterruptedException {
    String email = "concurrent@example.com";
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    
    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try {
                CreateUserDto dto = new CreateUserDto();
                dto.setUsername("user" + Thread.currentThread().getId());
                dto.setEmail(email);
                dto.setPassword("password123");
                
                userService.createUser(dto);
                successCount.incrementAndGet();
            } catch (Exception e) {
                // 预期会有重复邮箱异常
            } finally {
                latch.countDown();
            }
        }).start();
    }
    
    latch.await();
    
    // 只有一个线程应该成功
    assertThat(successCount.get()).isEqualTo(1);
}
```

### 🚀 代码覆盖率执行指令

#### **JaCoCo覆盖率分析**
```bash
# 运行测试并生成覆盖率报告
mvn clean test jacoco:report

# 查看覆盖率报告 (在浏览器中打开)
open target/site/jacoco/index.html
# Windows: start target/site/jacoco/index.html
# Linux: xdg-open target/site/jacoco/index.html

# 检查覆盖率是否达到设定目标
mvn jacoco:check

# 生成聚合报告 (如果有多模块)
mvn jacoco:report-aggregate
```

#### **覆盖率质量控制**
```bash
# 设置覆盖率门槛运行测试
mvn test jacoco:report jacoco:check -Djacoco.minimum.coverage=0.80

# 生成详细覆盖率分析
mvn clean verify jacoco:report -Djacoco.output.file=target/jacoco-detailed.exec

# 排除特定包的覆盖率统计
mvn test jacoco:report -Djacoco.excludes="**/config/**,**/dto/**"
```

#### **测试报告生成**
```bash
# 生成Surefire测试报告
mvn surefire-report:report

# 查看测试报告
open target/site/surefire-report.html

# 生成Failsafe集成测试报告
mvn failsafe:report

# 生成综合测试报告
mvn site
```

### 🚀 测试执行策略

#### **开发阶段策略**
```bash
# 1. 快速单元测试 (开发时常用)
mvn test -Dtest="*Test" -Dmaven.test.skip.exec=false

# 2. 特定功能测试
mvn test -Dtest="User*Test"

# 3. 失败重试测试
mvn test -Dsurefire.rerunFailingTestsCount=2
```

#### **提交前验证**
```bash
# 完整测试套件
mvn clean verify jacoco:report

# 快速验证 (跳过集成测试)
mvn clean test jacoco:report jacoco:check

# 并行测试执行
mvn clean verify -T 4C
```

#### **CI/CD策略**
```bash
# 生产级测试执行
mvn clean verify jacoco:report surefire-report:report failsafe:report

# 设置测试超时
mvn test -Dsurefire.timeout=300

# 内存优化测试
mvn test -Dmaven.surefire.maxMemSize=2048m -XX:MaxPermSize=512m
```

#### **调试和故障排除**
```bash
# 调试模式运行测试
mvn test -Dmaven.surefire.debug -Dtest="UserServiceTest#getCurrentUserInfo_Success"

# 查看失败测试详细信息
mvn test -Dtest="FailingTest" -X

# 跳过特定测试
mvn test -Dtest='!FailingTest'

# 重新运行失败的测试
mvn test -Dsurefire.rerunFailingTestsCount=3
```

---

## 🔄 高级测试主题 - 并发测试

### 🚀 并发测试架构

Mini-UPS项目包含了一套完整的并发测试框架，专门测试高并发场景下的数据一致性和性能：

#### **并发测试的重要性**

在分布式系统中，并发问题往往是最难发现和调试的问题：
- **数据竞争条件**：多个线程同时修改共享数据
- **死锁问题**：资源互相等待导致程序挂起
- **性能瓶颈**：高并发下的系统性能表现
- **数据一致性**：并发写入时的数据完整性

#### **实际并发测试示例**

以下是项目中实际使用的并发用户注册测试：

```java
/**
 * 用户注册并发测试示例
 * 测试场景：50个线程同时注册相同用户名，验证唯一性约束
 */
@Test
@DisplayName("并发用户注册 - 相同用户名冲突处理")
void testConcurrentUserRegistration_SameUsernameConflict() {
    // Given - 测试参数
    String duplicateUsername = "duplicate_user_" + System.currentTimeMillis();
    int threadCount = 50;
    AtomicInteger successCounter = new AtomicInteger(0);
    AtomicInteger conflictCounter = new AtomicInteger(0);

    // When - 并发执行测试
    ConcurrencyTestResult result = executeConcurrencyTest(() -> {
        try {
            // 所有线程尝试注册相同的用户名
            RegisterRequestDto request = createRegisterRequest(duplicateUsername, 
                duplicateUsername + "_" + Thread.currentThread().getId() + "@example.com");
            
            authService.register(request);
            successCounter.incrementAndGet();
            return true;
        } catch (Exception e) {
            conflictCounter.incrementAndGet();
            return false;
        }
    }, threadCount, 2, 30);

    // Then - 验证结果
    // 应该只有一个注册成功，其他都应该失败
    assertThat(successCounter.get()).isEqualTo(1);
    assertThat(conflictCounter.get()).isEqualTo(result.getFailureCount());
    assertThat(result.getFailureRate()).isGreaterThan(90.0);
    
    System.out.println("成功注册数: " + successCounter.get());
    System.out.println("冲突失败数: " + conflictCounter.get());
}
```

#### **并发测试基类架构**

```java
/**
 * 并发测试基类 - 提供通用的并发测试工具
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class ConcurrencyTestBase {

    /**
     * 执行并发测试并收集性能指标
     */
    protected ConcurrencyTestResult executeConcurrencyTest(
            Callable<Boolean> operation, 
            int threadCount, 
            int operationsPerThread, 
            int timeoutSeconds) {
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // 控制统一开始
        CountDownLatch endLatch = new CountDownLatch(threadCount); // 等待全部完成
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 启动所有工作线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待统一开始信号
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (operation.call()) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // 发出开始信号
        
        // 等待所有线程完成
        endLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        
        return new ConcurrencyTestResult(/* 收集的指标 */);
    }
}
```

### 🚀 并发测试执行指令

#### **并发测试运行**
```bash
# 运行所有并发测试
mvn test -Dtest=*ConcurrentTest

# 运行特定并发测试
mvn test -Dtest=ConcurrentUserRegistrationTest

# 运行性能基准测试
mvn test -Dtest=PerformanceBenchmarkTest

# 并发测试调试（降低线程数）
mvn test -Dtest=ConcurrentUserRegistrationTest -DthreadCount=5
```

#### **并发测试分析**
```bash
# 查看并发测试结果和指标
mvn test -Dtest=*ConcurrentTest -Dshow.performance.metrics=true

# 生成并发性能报告
mvn test -Dtest=*ConcurrentTest surefire-report:report

# 检查并发死锁和资源泄露
mvn test -Dtest=*ConcurrentTest -XX:+PrintGCDetails -XX:+PrintConcurrentLocks
```

#### **并发问题调试**
```bash
# 启用详细并发日志
mvn test -Dtest=ConcurrentUserRegistrationTest \
    -Dlogging.level.com.miniups=DEBUG \
    -Dspring.jpa.show-sql=true

# 模拟高并发场景
mvn test -Dtest=ConcurrentUserRegistrationTest \
    -DthreadCount=100 -DoperationsPerThread=20

# 内存和线程监控
mvn test -Dtest=*ConcurrentTest -Xmx1G -XX:+HeapDumpOnOutOfMemoryError
```

### 📊 并发测试最佳实践

#### **1. 测试设计原则**
```java
// ✅ 好的并发测试设计
@Test
void testConcurrentOperation() {
    // 明确的测试目标
    String sharedResource = "shared_resource_" + System.currentTimeMillis();
    
    // 可控的并发参数
    int threadCount = 20;
    int operationsPerThread = 5;
    
    // 原子性结果统计
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicReference<Exception> firstException = new AtomicReference<>();
    
    // 使用基类提供的并发测试框架
    ConcurrencyTestResult result = executeConcurrencyTest(() -> {
        try {
            // 实际的业务操作
            businessService.performOperation(sharedResource);
            successCount.incrementAndGet();
            return true;
        } catch (Exception e) {
            firstException.compareAndSet(null, e);
            return false;
        }
    }, threadCount, operationsPerThread, 30);
    
    // 明确的断言和结果验证
    assertThat(result.getSuccessRate()).isGreaterThan(expectedSuccessRate);
    if (firstException.get() != null) {
        System.err.println("首个异常: " + firstException.get().getMessage());
    }
}
```

#### **2. 避免并发测试陷阱**
```java
// ❌ 错误的并发测试
@Test
void badConcurrentTest() {
    // 问题1: 没有同步控制，线程启动时间不一致
    for (int i = 0; i < 10; i++) {
        new Thread(() -> {
            businessService.doSomething(); // 不知道什么时候执行
        }).start();
    }
    
    // 问题2: 没有等待机制，测试可能提前结束
    Thread.sleep(1000); // 硬编码等待时间
    
    // 问题3: 无法验证结果
    // 没有结果收集机制
}

// ✅ 正确的并发测试
@Test  
void goodConcurrentTest() throws InterruptedException {
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(10);
    AtomicInteger results = new AtomicInteger(0);
    
    for (int i = 0; i < 10; i++) {
        new Thread(() -> {
            try {
                startLatch.await(); // 等待统一开始
                businessService.doSomething();
                results.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown(); // 标记完成
            }
        }).start();
    }
    
    startLatch.countDown(); // 发出开始信号
    assertTrue(endLatch.await(10, TimeUnit.SECONDS)); // 等待完成
    
    // 验证结果
    assertThat(results.get()).isEqualTo(expectedValue);
}
```

#### **3. 性能基准测试**
```java
@Test
@DisplayName("高负载性能基准测试")
void performanceBenchmarkTest() {
    // 预热阶段 - 避免JIT编译影响
    for (int i = 0; i < 100; i++) {
        businessService.warmupOperation();
    }
    
    // 基准测试参数
    int[] threadCounts = {1, 5, 10, 20, 50, 100};
    
    for (int threadCount : threadCounts) {
        long startTime = System.nanoTime();
        
        ConcurrencyTestResult result = executeConcurrencyTest(() -> {
            businessService.benchmarkOperation();
            return true;
        }, threadCount, 100, 60);
        
        long endTime = System.nanoTime();
        double throughput = result.getTotalOperations() / ((endTime - startTime) / 1_000_000_000.0);
        
        System.out.println(String.format(
            "线程数: %d, 吞吐量: %.2f ops/sec, 成功率: %.2f%%", 
            threadCount, throughput, result.getSuccessRate()
        ));
        
        // 性能回归检测
        if (threadCount <= 10) {
            assertThat(result.getSuccessRate()).isGreaterThan(95.0);
        }
    }
}
```

---

## 🎯 新手完整执行指南

### **第一步：环境准备**
```bash
# 1. 验证环境
java -version    # 确保Java 17+
mvn -version     # 确保Maven 3.6+
docker --version # 确保Docker可用

# 2. 进入项目目录
cd /Users/hongxichen/Desktop/mini-ups/backend

# 3. 清理环境
mvn clean
```

### **第二步：基础测试运行**
```bash
# 1. 首次运行：快速单元测试
mvn test -Dtest="*ServiceTest"

# 2. 如果成功，运行所有单元测试
mvn test

# 3. 查看测试结果
ls -la target/surefire-reports/
```

### **第三步：集成测试**
```bash
# 1. 确保Docker运行
docker info

# 2. 运行集成测试 (会自动启动数据库容器)
mvn verify -Dit.test="*IntegrationTest"

# 3. 如果失败，检查Docker和网络
docker ps
docker logs $(docker ps -q --filter ancestor=postgres:15-alpine)
```

### **第四步：生成测试报告**
```bash
# 1. 生成覆盖率报告
mvn clean test jacoco:report

# 2. 在浏览器中查看报告
open target/site/jacoco/index.html

# 3. 检查覆盖率是否达标
mvn jacoco:check
```

### **第五步：完整测试流程**
```bash
# 完整的生产级测试流程
mvn clean verify jacoco:report surefire-report:report

# 查看所有报告
echo "覆盖率报告: target/site/jacoco/index.html"
echo "测试报告: target/site/surefire-report.html"
echo "集成测试报告: target/site/failsafe-report.html"
```

### **常见问题解决**

#### **问题1：Docker连接失败**
```bash
# 检查Docker状态
docker info

# 重启Docker服务
# macOS: 重启Docker Desktop
# Linux: sudo systemctl restart docker

# 清理Docker资源
docker system prune -f
```

#### **问题2：端口占用**
```bash
# 检查端口占用
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis

# 杀死占用进程
kill -9 $(lsof -t -i:5432)
```

#### **问题3：内存不足**
```bash
# 增加Maven内存
export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"

# 或在pom.xml中配置
mvn test -Dmaven.surefire.maxMemSize=2048m
```

#### **问题4：测试运行缓慢**
```bash
# 并行执行
mvn test -T 4

# 只运行快速测试
mvn test -Dtest="*Test" -Dgroups="fast"

# 跳过集成测试
mvn test -DskipITs=true
```

### **新手学习路径**

#### **第1周：基础测试**
```bash
# Day 1-2: 单元测试
mvn test -Dtest="UserServiceTest"
mvn test -Dtest="AuthServiceTest"

# Day 3-4: 控制器测试  
mvn test -Dtest="UserControllerTest"

# Day 5-7: 安全测试
mvn test -Dtest="*SecurityTest"
```

#### **第2周：集成测试**
```bash
# Day 1-3: 数据库集成测试
mvn verify -Dit.test="*RepositoryIntegrationTest"

# Day 4-5: API集成测试
mvn verify -Dit.test="*ControllerIntegrationTest"

# Day 6-7: 端到端测试
mvn verify -Dit.test="*IntegrationTest"
```

#### **第3周：高级特性**
```bash
# Day 1-2: 并发测试
mvn test -Dtest="*ConcurrencyTest"

# Day 3-4: 性能测试
mvn test -Dtest="*PerformanceTest"

# Day 5-7: 覆盖率和质量
mvn clean verify jacoco:report jacoco:check
```

### **实用脚本创建**

#### **quick-test.sh**
```bash
#!/bin/bash
echo "🚀 Running quick unit tests..."
cd backend
mvn test -Dtest='!*IntegrationTest' -q
echo "✅ Quick tests completed!"
```

#### **full-test.sh**
```bash
#!/bin/bash
echo "🧪 Running full test suite..."
cd backend
mvn clean verify jacoco:report
echo "📊 Opening coverage report..."
open target/site/jacoco/index.html
echo "✅ Full tests completed!"
```

#### **debug-test.sh**
```bash
#!/bin/bash
if [ -z "$1" ]; then
    echo "Usage: ./debug-test.sh TestClassName"
    exit 1
fi
echo "🔍 Debugging test: $1"
cd backend
mvn test -Dtest=$1 -Dmaven.surefire.debug
```

```bash
# 给脚本执行权限
chmod +x quick-test.sh full-test.sh debug-test.sh

# 使用脚本
./quick-test.sh
./full-test.sh  
./debug-test.sh UserServiceTest
```

---

## 总结

这个基于 Mini-UPS 项目的完整 Java 测试指南涵盖了企业级测试开发的方方面面：

### 🏗️ **架构与设计**
1. **完整的测试架构** - 基于实际项目的分层测试策略
2. **模块化测试设计** - 服务层、控制层、数据层、安全层的完整覆盖
3. **并发测试框架** - 专门针对高并发场景的测试基础设施

### 🛠️ **工具与配置**
4. **现代化工具栈** - Maven插件、TestContainers、JaCoCo、Mockito等
5. **测试环境配置** - Profile管理、Mock服务配置、外部依赖隔离
6. **测试数据管理** - 工厂模式、Builder模式、边界值测试数据

### 💡 **实践与示例**
7. **真实代码示例** - 直接来自生产项目的测试实现
8. **详细执行指令** - 每个测试场景的具体运行命令
9. **问题排查指南** - 常见问题的诊断和解决方案

### 🚀 **高级主题**
10. **并发测试专题** - 竞争条件、数据一致性、性能基准测试
11. **安全测试深度覆盖** - JWT、权限控制、API安全
12. **集成测试最佳实践** - TestContainers、真实数据库环境

### 📊 **质量保障**
13. **代码覆盖率策略** - JaCoCo配置、覆盖率目标、质量门禁
14. **性能监控集成** - 测试性能指标收集和分析
15. **CI/CD集成指导** - 持续集成环境下的测试执行策略

### 🎯 **学习路径**
16. **新手入门指南** - 从零开始的三周学习计划
17. **渐进式学习** - 从基础到高级的知识体系
18. **实用脚本提供** - 开箱即用的测试执行脚本

### 💎 **核心价值观**

**测试即文档**: 每个测试都应该清楚地表达业务逻辑和预期行为
**测试即信心**: 良好的测试覆盖让重构和功能迭代更安全
**测试即反馈**: 快速的测试执行为开发提供即时反馈
**测试即质量**: 测试不仅验证功能，更保证系统的健壮性

### 🔑 **关键成功因素**

1. **分层清晰**: 不同层次的测试有明确的职责和边界
2. **数据隔离**: 测试间相互独立，避免数据污染
3. **环境一致**: 开发、测试、生产环境的一致性
4. **持续改进**: 定期重构测试代码，保持可维护性
5. **团队约定**: 统一的测试标准和最佳实践

通过遵循这个指南，你将能够：

✅ **构建企业级测试套件** - 从单元测试到性能测试的全覆盖
✅ **掌握现代测试工具** - 熟练使用Spring Boot Test生态
✅ **处理复杂测试场景** - 并发、安全、集成等高级测试技术
✅ **建立质量保障体系** - 代码覆盖率、CI/CD集成、自动化测试
✅ **提升开发效率** - 通过测试驱动开发加快迭代速度

**记住**: 测试不是开发的负担，而是软件质量的守护者和开发效率的助推器。投资于测试就是投资于软件的未来！

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"id": "1", "content": "Analyze existing test code structure and architecture", "status": "completed", "priority": "high"}, {"id": "2", "content": "Validate test effectiveness and completeness using zen code review", "status": "completed", "priority": "high"}, {"id": "3", "content": "Create comprehensive Java testing tutorial based on project structure", "status": "completed", "priority": "medium"}]