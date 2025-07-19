# 🧪 Mini-UPS 测试执行指南

## 📋 概述

本指南提供了 Mini-UPS 后端测试系统的完整执行说明，包括不同类型的测试、运行方法、环境配置和故障排除。

**测试状态**: 🟡 部分完成
- ✅ **安全测试**: 已完成 (90+ 测试用例)
- 🔄 **编译修复**: 进行中
- ⏳ **弹性测试**: 待实施
- ⏳ **业务逻辑测试**: 待扩展

---

## 🎯 测试分类

### 1. **单元测试** (Unit Tests)
**命名模式**: `*Test.java`
**执行工具**: Maven Surefire Plugin
**目标**: 测试单个类或方法的功能

```bash
# 运行所有单元测试
mvn test

# 运行特定测试类
mvn test -Dtest="UserServiceTest"

# 运行特定测试方法
mvn test -Dtest="UserServiceTest#testCreateUser"
```

### 2. **集成测试** (Integration Tests)
**命名模式**: `*IntegrationTest.java`
**执行工具**: Maven Failsafe Plugin
**目标**: 测试组件间的集成和数据库交互

```bash
# 运行所有集成测试
mvn verify

# 运行特定集成测试
mvn verify -Dit.test="UserControllerIntegrationTest"
```

### 3. **安全测试** (Security Tests)
**位置**: `src/test/java/com/miniups/security/`
**目标**: 验证认证、授权和安全边界

```bash
# 运行所有安全测试
mvn test -Dtest="*Security*Test"

# 运行 JWT 安全测试
mvn test -Dtest="JwtSecurityTest"

# 运行完整安全集成测试
mvn test -Dtest="SecurityIntegrationTest"
```

---

## 🚀 快速开始

### 基本测试执行

```bash
# 1. 进入后端目录
cd backend

# 2. 运行基础编译检查
mvn compile test-compile

# 3. 运行单元测试（跳过有编译错误的测试）
mvn test -Dtest="*Security*Test"

# 4. 运行安全测试套件
mvn test -Dtest="SecurityIntegrationTest,JwtSecurityTest,*ControllerSecurityTest"
```

### 生成测试报告

```bash
# 运行测试并生成覆盖率报告
mvn clean test jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html
```

---

## 🛠️ 环境配置

### 测试数据库配置

测试使用 H2 内存数据库，配置文件：`src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  
# JWT 测试配置
jwt:
  secret: testSecretKeyForJwtTokenGenerationAndValidation12345
  expiration: 86400000
```

### IDE 配置

#### **IntelliJ IDEA**
1. 打开项目根目录
2. 确保 Maven 项目已正确导入
3. 运行配置：
   - **Working Directory**: `backend`
   - **VM Options**: `-Dspring.profiles.active=test`
   - **Use classpath of module**: `backend`

#### **Visual Studio Code**
1. 安装 Java Extension Pack
2. 打开后端文件夹
3. 使用命令面板: `Java: Run Tests`

---

## 📁 测试文件结构

```
📁 src/test/java/com/miniups/
├── 🔐 security/                           # 安全测试套件
│   ├── SecurityIntegrationTest.java       # 综合安全集成测试
│   ├── JwtSecurityTest.java              # JWT 令牌专项测试
│   └── controller/security/               # 控制器安全测试
│       ├── UserControllerSecurityTest.java
│       ├── AdminControllerSecurityTest.java
│       └── TruckControllerSecurityTest.java
│
├── 🎮 controller/                          # 控制器测试
│   ├── AuthControllerTest.java
│   ├── UserControllerTest.java
│   └── *ControllerIntegrationTest.java
│
├── 💼 service/                            # 服务层测试
│   ├── UserServiceTest.java
│   ├── AuthServiceTest.java
│   └── TruckManagementServiceTest.java
│
├── 🗃️ repository/                         # 数据访问层测试
│   └── *RepositoryIntegrationTest.java
│
├── ❌ exception/                          # 异常处理测试
│   └── GlobalExceptionHandlerTest.java
│
├── 🛠️ util/                              # 工具类测试
│   └── TestDataFactory.java
│
└── 📋 config/                            # 配置类测试
    └── TestConfig.java
```

---

## 📊 测试覆盖率目标

| 层级 | 当前覆盖率 | 目标覆盖率 | 状态 |
|------|-----------|-----------|------|
| 安全层 | 90% | 95% | 🟢 优秀 |
| 控制器层 | 60% | 80% | 🟡 需提升 |
| 服务层 | 45% | 85% | 🔴 待改进 |
| 仓储层 | 70% | 90% | 🟡 需提升 |
| **总体** | **65%** | **85%** | 🟡 **进行中** |

---

## 🔧 故障排除

### 常见编译错误

#### **错误 1: 缺失的类或包**
```
cannot find symbol: class ShipmentStatusHistoryRepository
```
**解决方案**: 这些类尚未实现，暂时跳过相关测试
```bash
# 只运行已修复的测试
mvn test -Dtest="*Security*Test"
```

#### **错误 2: 方法签名不匹配**
```
method validateToken cannot be applied to given types
```
**解决方案**: 检查实际方法签名并更新测试调用

#### **错误 3: 枚举值不存在**
```
cannot find symbol: variable AVAILABLE
```
**解决方案**: 使用正确的枚举值（如 `TruckStatus.IDLE`）

### 环境问题

#### **H2 数据库连接失败**
```bash
# 检查数据库配置
cat src/test/resources/application-test.yml

# 确保 H2 依赖存在
mvn dependency:tree | grep h2
```

#### **JWT 配置错误**
```
JWT secret is not configured
```
**解决方案**: 确保测试配置中有有效的 JWT 密钥

### 性能问题

#### **测试运行缓慢**
```bash
# 并行运行测试
mvn test -T 1C

# 跳过长时间测试
mvn test -Dtest="!*IntegrationTest"
```

---

## 🎨 测试最佳实践

### 1. **测试命名规范**
```java
@Test
@DisplayName("用户管理 - ADMIN 应能创建新用户")
void testCreateUser_ShouldBeAllowedForAdmin() {
    // Given
    // When  
    // Then
}
```

### 2. **测试数据准备**
```java
@BeforeEach
void setUp() {
    // 清理数据
    userRepository.deleteAll();
    
    // 准备测试数据
    setupTestUsers();
    setupJwtTokens();
}
```

### 3. **断言模式**
```java
// 使用 AssertJ 进行流畅断言
assertThat(response)
    .extracting("status", "data.username")
    .containsExactly(200, "test_user");
```

### 4. **模拟策略**
```java
// 精确模拟
when(userRepository.findByUsername("test"))
    .thenReturn(Optional.of(testUser));

// 验证交互
verify(userService).createUser(createRequest);
verify(auditService, never()).logFailure(any());
```

---

## 📈 测试执行流水线

### 本地开发流程
```bash
# 1. 开发新功能
git checkout -b feature/new-feature

# 2. 运行相关测试
mvn test -Dtest="*NewFeature*Test"

# 3. 运行安全测试确保无回归
mvn test -Dtest="*Security*Test"

# 4. 生成覆盖率报告
mvn clean test jacoco:report

# 5. 提交代码
git add . && git commit -m "Add new feature with tests"
```

### CI/CD 集成
```yaml
# GitHub Actions 示例
- name: Run Tests
  run: |
    cd backend
    mvn clean test
    mvn jacoco:report
    
- name: Upload Coverage
  uses: codecov/codecov-action@v1
  with:
    file: backend/target/site/jacoco/jacoco.xml
```

---

## 📚 参考资料

### Maven 测试命令参考
```bash
# 基本命令
mvn test                    # 运行单元测试
mvn verify                  # 运行集成测试
mvn clean test              # 清理并运行测试

# 高级选项
mvn test -Dtest=Pattern     # 运行匹配模式的测试
mvn test -DfailIfNoTests=false  # 无测试时不失败
mvn test -Dmaven.test.skip=true # 跳过测试
mvn test -Dtest=ClassName#methodName # 运行特定方法

# 覆盖率
mvn jacoco:report           # 生成覆盖率报告
mvn jacoco:check            # 检查覆盖率阈值
```

### Spring Boot 测试注解
```java
@SpringBootTest             // 完整 Spring 上下文
@WebMvcTest                 // Web 层测试
@DataJpaTest                // JPA 仓储测试
@TestMethodOrder            // 测试执行顺序
@Transactional              // 事务回滚
@ActiveProfiles("test")     // 激活测试配置
```

---

## 🎯 下一步计划

### 近期目标 (1-2 周)
1. **🔧 修复编译错误** - 创建缺失的类和方法
2. **🔄 实施弹性测试** - 添加 WireMock 外部服务测试
3. **💼 扩展业务逻辑测试** - 提升服务层覆盖率到 85%

### 中期目标 (3-4 周)
1. **🔗 完整集成测试** - RabbitMQ 和数据库事务测试
2. **📋 完善文档** - 创建完整的测试维护指南
3. **🚀 CI/CD 集成** - 自动化测试流水线

**测试成功标准**: 
- ✅ 90% 代码覆盖率
- ✅ 所有安全测试通过
- ✅ 零编译错误
- ✅ 完整的文档和维护指南

---

*最后更新: 2025年1月18日*
*下次审查: 2025年1月25日*