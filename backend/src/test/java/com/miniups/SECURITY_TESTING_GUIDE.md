# 🔒 Mini-UPS 安全测试指南

## 📋 概述

本指南提供了 Mini-UPS 后端系统的完整安全测试框架，确保所有安全控制机制都经过严格验证，防止未授权访问和权限提升漏洞。

## 🎯 测试目标

### 1. **认证验证**
- JWT 令牌生成和验证机制
- 令牌过期和刷新处理
- 恶意令牌检测和拒绝

### 2. **授权控制**
- 角色基础访问控制 (RBAC)
- 端点级权限验证
- 数据隔离和个人信息保护

### 3. **权限边界**
- 防止权限提升攻击
- 角色权限严格分离
- 管理员功能保护

## 🏗️ 测试架构

### 测试分层结构
```
📁 src/test/java/com/miniups/security/
├── 🔐 SecurityIntegrationTest.java          # 综合安全集成测试
├── 🎫 JwtSecurityTest.java                  # JWT 令牌专项测试
└── 📁 controller/security/
    ├── 👤 UserControllerSecurityTest.java   # 用户管理安全测试
    ├── 👑 AdminControllerSecurityTest.java  # 管理员功能安全测试
    └── 🚛 TruckControllerSecurityTest.java  # 卡车管理安全测试
```

## 🔑 安全配置分析

### 端点权限配置
根据 `SecurityConfig.java` 的配置：

#### **🌐 公共端点** (无需认证)
```
/api/auth/**              # 认证端点
/api/public/**            # 公共 API
/api/tracking/**          # 包裹追踪
/api/webhooks/**          # Amazon 集成 webhook
/actuator/health          # 健康检查
/api-docs/**              # API 文档
```

#### **🔒 受保护端点** (需要认证)
```
/api/users/profile        # 个人资料 (用户自己)
/api/users/{id}          # 用户信息 (ADMIN 或用户自己)
其他 /api/** 端点         # 一般认证用户
```

#### **👑 管理员端点** (需要 ADMIN 角色)
```
/api/admin/**            # 所有管理员功能
/api/users               # 用户管理 (列表、创建、更新、删除)
/api/trucks/{id}/assign  # 卡车分配
/api/trucks/{id}/release # 卡车释放
```

#### **🚛 运营端点** (需要 ADMIN 或 OPERATOR 角色)
```
/api/trucks              # 卡车查看
/api/trucks/statistics   # 车队统计
/api/trucks/{id}/status  # 卡车状态更新
/api/trucks/nearest      # 最近卡车查找
```

## 🧪 测试套件详解

### 1. **SecurityIntegrationTest.java**
**目的**: 综合测试整个安全配置的正确性

**覆盖范围**:
- ✅ 公共端点无需认证访问验证
- ✅ 受保护端点认证要求验证
- ✅ 角色权限边界测试
- ✅ JWT 令牌验证测试
- ✅ CORS 配置验证

**关键测试场景**:
```java
@Test
@DisplayName("未认证访问 - 受保护端点应返回 401 Unauthorized")
void testProtectedEndpoints_ShouldReturn401WhenUnauthenticated()

@Test
@DisplayName("管理员权限 - 普通用户访问管理员端点应返回 403 Forbidden")
void testAdminEndpoints_ShouldReturn403WhenAccessedByNormalUser()
```

### 2. **JwtSecurityTest.java**
**目的**: 专门测试 JWT 令牌机制的安全性

**覆盖范围**:
- ✅ JWT 令牌生成和验证
- ✅ 令牌过期检测
- ✅ 恶意令牌拒绝
- ✅ 令牌解析安全性
- ✅ 性能和边界条件

**关键测试场景**:
```java
@Test
@DisplayName("JWT 安全 - 使用错误密钥签名的令牌应验证失败")
void testValidateToken_ShouldFailForTokenWithWrongSignature()

@Test
@DisplayName("JWT 安全 - 令牌不应包含敏感信息")
void testToken_ShouldNotContainSensitiveInformation()
```

### 3. **UserControllerSecurityTest.java**
**目的**: 测试用户管理端点的权限控制

**覆盖范围**:
- ✅ 个人资料访问控制
- ✅ 用户数据隔离验证
- ✅ 管理员用户管理权限
- ✅ 权限提升攻击防护

**关键测试场景**:
```java
@Test
@DisplayName("权限提升防护 - 普通用户不能将自己升级为管理员")
void testPrivilegeEscalation_ShouldPreventUserFromBecomingAdmin()

@Test
@DisplayName("数据隔离 - 用户不能查看其他用户的详细信息")
void testGetUserById_ShouldDenyUserFromViewingOthersInfo()
```

### 4. **AdminControllerSecurityTest.java**
**目的**: 测试管理员功能的严格权限控制

**覆盖范围**:
- ✅ 管理员仪表盘访问控制
- ✅ 车队管理权限验证
- ✅ 系统管理功能保护
- ✅ 类级别安全注解验证

**关键测试场景**:
```java
@Test
@DisplayName("类级别安全 - 所有管理员端点都应要求 ADMIN 角色")
void testClassLevelSecurity_AllEndpointsShouldRequireAdminRole()
```

### 5. **TruckControllerSecurityTest.java**
**目的**: 测试卡车管理的角色权限边界

**覆盖范围**:
- ✅ 查看权限 (ADMIN/OPERATOR)
- ✅ 管理权限 (仅 ADMIN)
- ✅ 系统级操作权限
- ✅ 数据验证和边界条件

## 🏃‍♂️ 运行测试

### Maven 命令

#### **运行所有安全测试**
```bash
# 运行所有安全相关测试
mvn test -Dtest="*Security*Test"

# 仅运行安全集成测试
mvn test -Dtest="SecurityIntegrationTest"

# 仅运行 JWT 安全测试
mvn test -Dtest="JwtSecurityTest"
```

#### **运行特定控制器安全测试**
```bash
# 用户控制器安全测试
mvn test -Dtest="UserControllerSecurityTest"

# 管理员控制器安全测试
mvn test -Dtest="AdminControllerSecurityTest"

# 卡车控制器安全测试
mvn test -Dtest="TruckControllerSecurityTest"
```

#### **生成测试覆盖率报告**
```bash
# 运行测试并生成 JaCoCo 覆盖率报告
mvn clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

### IDE 运行

#### **IntelliJ IDEA**
1. 右键点击测试类或方法
2. 选择 "Run 'TestName'" 或 "Debug 'TestName'"
3. 查看测试结果和覆盖率

#### **Eclipse**
1. 右键点击测试类
2. 选择 "Run As" → "JUnit Test"
3. 在 JUnit 视图中查看结果

## 🎯 测试结果验证

### 预期测试结果

#### **成功指标**
- ✅ 所有安全测试通过 (100% pass rate)
- ✅ 覆盖率达到 90% 以上
- ✅ 无安全漏洞被检测到
- ✅ 所有角色权限边界正确验证

#### **关键验证点**
1. **认证验证**: 未认证用户被正确拒绝 (401)
2. **授权控制**: 无权限用户被正确拒绝 (403)
3. **权限边界**: 角色权限严格分离
4. **数据保护**: 用户只能访问自己的数据
5. **管理员保护**: 管理员功能严格控制

### 测试失败排查

#### **常见失败原因**
1. **数据库配置**: H2 内存数据库未正确配置
2. **JWT 配置**: JWT 密钥或过期时间配置错误
3. **角色定义**: 用户角色枚举不匹配
4. **注解配置**: 安全注解配置错误

#### **排查步骤**
```bash
# 1. 检查测试环境配置
cat src/test/resources/application-test.yml

# 2. 查看详细测试日志
mvn test -Dtest="SecurityIntegrationTest" -X

# 3. 验证数据库连接
mvn test -Dtest="SecurityIntegrationTest" -Dspring.jpa.show-sql=true
```

## 📊 测试覆盖率目标

### 覆盖率要求

| 组件 | 目标覆盖率 | 优先级 |
|------|-----------|--------|
| SecurityConfig | 95% | 🔴 高 |
| JwtTokenProvider | 90% | 🔴 高 |
| UserController | 85% | 🟡 中 |
| AdminController | 85% | 🟡 中 |
| TruckController | 80% | 🟡 中 |

### 覆盖率检查
```bash
# 生成详细覆盖率报告
mvn clean verify jacoco:report

# 检查覆盖率阈值
mvn jacoco:check
```

## 🔧 配置和环境

### 测试环境配置

#### **测试数据库**
```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:securitytest
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

#### **JWT 测试配置**
```yaml
app:
  jwtSecret: testSecretKey
  jwtExpirationInMs: 86400000
```

#### **日志配置**
```yaml
logging:
  level:
    com.miniups.security: DEBUG
    org.springframework.security: DEBUG
```

## 🚨 安全最佳实践

### 1. **测试数据安全**
- ✅ 使用独立的测试数据库
- ✅ 测试完成后自动清理数据
- ✅ 不在测试中使用真实密码

### 2. **令牌安全**
- ✅ 测试环境使用独立的 JWT 密钥
- ✅ 验证令牌不包含敏感信息
- ✅ 测试令牌过期和撤销机制

### 3. **权限验证**
- ✅ 测试所有权限边界
- ✅ 验证负面测试场景
- ✅ 确保权限提升攻击被阻止

### 4. **输入验证**
- ✅ 测试恶意输入处理
- ✅ 验证数据格式验证
- ✅ 确保 SQL 注入防护

## 📈 持续改进

### 定期安全审计
1. **每周**: 运行完整安全测试套件
2. **每月**: 分析测试覆盖率和安全漏洞
3. **每季度**: 更新安全测试用例

### 新功能安全测试
当添加新的端点或功能时，必须:
1. ✅ 添加对应的安全测试
2. ✅ 验证权限配置正确
3. ✅ 确保测试覆盖率不下降

### 测试用例维护
- 🔄 定期更新测试数据
- 🔄 优化测试性能
- 🔄 添加新的安全场景测试

---

## 📞 支持和问题反馈

如果在运行安全测试时遇到问题，请检查:

1. **环境配置**: 确保所有依赖项正确安装
2. **数据库连接**: 验证 H2 数据库配置
3. **JWT 配置**: 检查 JWT 密钥和过期时间设置
4. **角色定义**: 确认用户角色枚举正确

**测试成功运行后，您的 Mini-UPS 系统将具备企业级的安全保护！** 🛡️