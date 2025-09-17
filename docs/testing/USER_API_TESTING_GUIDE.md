# 用户模块API测试完整指南

## 📋 测试概览

本指南提供了Mini-UPS系统用户模块的完整API测试方案，包括：

- **用户控制器测试** (`UserControllerTest`)
- **认证控制器测试** (`AuthControllerTest`) 
- **用户服务测试** (`UserServiceTest`)
- **用户详情服务测试** (`CustomUserDetailsServiceTest`)

## 🏗️ 测试架构

### 测试分层结构

```
用户模块测试
├── 单元测试 (70%)
│   ├── UserService - 业务逻辑测试
│   ├── AuthService - 认证逻辑测试
│   └── 工具类和验证器测试
├── 集成测试 (20%)
│   ├── UserController - API端点测试
│   ├── AuthController - 认证API测试
│   └── Repository - 数据访问测试
└── 端到端测试 (10%)
    ├── 完整用户注册流程
    ├── 完整用户登录流程
    └── 用户权限验证流程
```

## 🎯 测试覆盖的API端点

### 用户管理API (`/api/users`)

| 方法 | 端点 | 权限 | 测试场景 |
|------|------|------|----------|
| GET | `/api/users/profile` | 已认证用户 | 获取当前用户资料 |
| PUT | `/api/users/profile` | 已认证用户 | 更新当前用户资料 |
| GET | `/api/users/{userId}` | 管理员/用户本人 | 获取指定用户信息 |
| GET | `/api/users` | 管理员 | 获取用户列表 |
| PUT | `/api/users/{userId}` | 管理员 | 更新用户信息 |
| DELETE | `/api/users/{userId}` | 管理员 | 禁用用户 |
| POST | `/api/users/{userId}/enable` | 管理员 | 启用用户 |
| GET | `/api/users/{userId}/public` | 公开 | 获取用户公开资料 |

### 认证API (`/api/auth`)

| 方法 | 端点 | 权限 | 测试场景 |
|------|------|------|----------|
| POST | `/api/auth/register` | 公开 | 用户注册 |
| POST | `/api/auth/login` | 公开 | 用户登录 |
| GET | `/api/auth/me` | 已认证用户 | 获取当前用户信息 |
| POST | `/api/auth/change-password` | 已认证用户 | 修改密码 |
| POST | `/api/auth/logout` | 已认证用户 | 用户退出 |
| GET | `/api/auth/validate` | 已认证用户 | 验证令牌 |
| GET | `/api/auth/check-username` | 公开 | 检查用户名可用性 |
| GET | `/api/auth/check-email` | 公开 | 检查邮箱可用性 |

## 🧪 测试类详解

### 1. UserControllerTest - 用户控制器测试

**测试范围：**
- HTTP请求处理
- 权限验证（@PreAuthorize）
- 响应格式验证
- 异常处理

**关键测试场景：**
```java
// 成功场景
@Test @WithMockUser(roles = {"USER"})
void testGetCurrentUserProfile_Success()

// 权限验证
@Test @WithMockUser(roles = {"USER"})
void testGetUserById_AccessDenied()

// 异常处理
@Test @WithMockUser(roles = {"USER"})
void testServiceException()
```

### 2. AuthControllerTest - 认证控制器测试

**测试范围：**
- 用户注册和登录
- JWT令牌处理
- 密码管理
- 输入验证

**关键测试场景：**
```java
// 注册成功
@Test
void testRegisterUser_Success()

// 登录失败
@Test
void testAuthenticateUser_InvalidCredentials()

// 密码修改
@Test @WithMockUser(roles = {"USER"})
void testChangePassword_Success()
```

### 3. UserServiceTest - 用户服务测试

**测试范围：**
- 业务逻辑验证
- 数据转换（Entity ↔ DTO）
- 异常处理
- 边界条件

**关键测试场景：**
```java
// 用户查询
@Test
void testGetCurrentUserInfo_Success()

// 用户更新
@Test
void testUpdateUser_EmailExists()

// 空值处理
@Test
void testNullHandling()
```

### 4. CustomUserDetailsServiceTest - 现有测试

**测试范围：**
- Spring Security集成
- 用户角色加载
- 认证详情

## 🚀 运行测试

### 快速运行

```bash
# 运行所有用户模块测试
./run-user-tests.sh

# 运行特定测试类
cd backend
mvn test -Dtest=UserControllerTest
mvn test -Dtest=AuthControllerTest  
mvn test -Dtest=UserServiceTest
```

### 详细测试运行

```bash
# 运行测试并生成详细报告
mvn test -Dtest=UserControllerTest -Dsurefire.printSummary=true

# 运行测试并生成覆盖率报告
mvn test jacoco:report
open target/site/jacoco/index.html
```

## 📊 测试覆盖率目标

| 组件 | 目标覆盖率 | 当前状态 |
|------|------------|----------|
| UserController | 90%+ | ✅ 新建测试 |
| AuthController | 90%+ | ✅ 新建测试 |
| UserService | 85%+ | ✅ 新建测试 |
| Security Layer | 80%+ | ✅ 已有测试 |

## 🔧 测试配置

### 测试环境配置

**application-test.yml:**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

**TestConfig.java:**
```java
@TestConfiguration
public class TestConfig {
    @MockBean
    private WorldSimulatorService worldSimulatorService;
    
    @MockBean  
    private AmazonIntegrationService amazonIntegrationService;
}
```

### Mock配置

**主要Mock对象：**
- `UserRepository` - 数据访问模拟
- `AuthService` - 认证服务模拟
- `UserService` - 用户服务模拟
- `PasswordEncoder` - 密码编码器模拟

## 🧩 测试数据工厂

**TestDataFactory.java 提供：**
```java
// 创建测试用户
User testUser = TestDataFactory.createTestUser();
User testAdmin = TestDataFactory.createTestAdmin();

// 创建测试DTO
UserDto userDto = createUserDto();
UpdateUserDto updateDto = createUpdateUserDto();
```

## 🔐 安全测试重点

### 权限测试

```java
// 测试用户只能访问自己的资料
@Test @WithMockUser(username="user1", roles={"USER"})
void testUserCanOnlyAccessOwnProfile()

// 测试管理员可以访问所有用户
@Test @WithMockUser(roles={"ADMIN"})  
void testAdminCanAccessAllUsers()
```

### CSRF保护测试

```java
// 测试CSRF令牌要求
@Test @WithMockUser(roles={"USER"})
void testCSRFProtection() {
    // 不带CSRF令牌的请求应该失败
    mockMvc.perform(put("/api/users/profile")
        .content(json))
        .andExpect(status().isForbidden());
}
```

## 📋 测试清单

### 功能测试 ✅

- [x] 用户注册功能
- [x] 用户登录功能
- [x] 用户资料查询
- [x] 用户资料更新
- [x] 用户权限验证
- [x] 密码修改功能
- [x] 用户状态管理

### 安全测试 ✅

- [x] JWT令牌验证
- [x] 权限控制（@PreAuthorize）
- [x] CSRF保护
- [x] 输入验证
- [x] 密码强度验证

### 异常测试 ✅

- [x] 用户不存在
- [x] 邮箱已存在
- [x] 用户名已存在
- [x] 无效凭证
- [x] 权限不足
- [x] 系统异常

### 边界测试 ✅

- [x] 空值处理
- [x] 无效输入
- [x] 长度限制
- [x] 格式验证

## 🐛 常见问题解决

### 1. 编译错误

```bash
# 清理并重新编译
mvn clean compile test-compile
```

### 2. 依赖冲突

```bash
# 检查依赖树
mvn dependency:tree
```

### 3. 测试失败

```bash
# 查看详细错误信息
mvn test -Dtest=TestClassName -X
```

### 4. 覆盖率报告

```bash
# 生成覆盖率报告
mvn jacoco:report
open target/site/jacoco/index.html
```

## 📈 持续改进

### 下一步计划

1. **集成测试扩展**
   - 添加Testcontainers支持
   - 真实数据库集成测试

2. **性能测试**
   - API响应时间测试
   - 并发用户测试

3. **端到端测试**
   - 完整用户流程测试
   - 前后端集成测试

4. **测试自动化**
   - CI/CD集成
   - 自动化测试报告

## 🔗 相关资源

- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [Spring Security Test](https://docs.spring.io/spring-security/reference/servlet/test/index.html)
- [MockMvc Reference](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

---

## 📞 技术支持

如有测试相关问题，请查看：
1. 测试日志：`target/surefire-reports/`
2. 覆盖率报告：`target/site/jacoco/`
3. API文档：`http://localhost:8081/swagger-ui.html`