# Java Spring Boot 测试完全指南
*基于Mini-UPS项目的实战教程*

## 📚 目录

1. [测试基础概念](#1-测试基础概念)
2. [项目测试工具箱](#2-项目测试工具箱)
3. [JUnit 5 详解](#3-junit-5-详解)
4. [Spring Test 详解](#4-spring-test-详解)
5. [测试金字塔结构](#5-测试金字塔结构)
6. [实战：如何运行测试](#6-实战如何运行测试)
7. [测试代码编写指南](#7-测试代码编写指南)
8. [常见问题与解决方案](#8-常见问题与解决方案)
9. [测试最佳实践](#9-测试最佳实践)

---

## 1. 测试基础概念

### 什么是测试？
测试就是验证你的代码是否按预期工作。想象一下：
- 你写了一个加法函数 `add(2, 3)`
- 测试就是验证它是否真的返回 `5`
- 如果返回 `6`，那说明代码有问题

### 为什么要测试？
1. **发现bug**：在用户发现之前找出问题
2. **防止回归**：修改代码时不会破坏原有功能
3. **文档作用**：测试代码展示了如何使用你的代码
4. **重构信心**：有了测试，你可以放心重构代码

### 测试的AAA模式
每个测试都应该遵循AAA模式：
- **Arrange（准备）**：设置测试数据和环境
- **Act（执行）**：调用被测试的方法
- **Assert（断言）**：验证结果是否符合预期

---

## 2. 项目测试工具箱

### 核心依赖详解

#### 🔧 spring-boot-starter-test
这是Spring Boot测试的"瑞士军刀"，包含：
- **JUnit 5**：Java标准测试框架
- **Mockito**：创建"假对象"的工具
- **AssertJ**：更友好的断言语法
- **Spring Test**：Spring官方测试支持

#### 🔒 spring-security-test
测试安全相关功能：
```java
@WithMockUser(username = "admin", roles = {"ADMIN"})
@Test
void testAdminEndpoint() {
    // 模拟一个管理员用户登录
}
```

#### 🐳 Testcontainers
**超级重要**！它能启动真实的Docker容器用于测试：
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");
```

#### 🔧 Maven插件配置
```xml
<!-- 单元测试插件 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
        </includes>
    </configuration>
</plugin>

<!-- 集成测试插件 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*IntegrationTest.java</include>
        </includes>
    </configuration>
</plugin>
```

---

## 3. JUnit 5 详解

### 什么是JUnit 5？
JUnit 5是Java世界最流行的测试框架，它提供了编写和运行测试的基础设施。JUnit 5是一个全新的版本，相比JUnit 4有重大改进。

### 🏗️ JUnit 5架构
```
JUnit 5 = JUnit Platform + JUnit Jupiter + JUnit Vintage
```

- **JUnit Platform**：测试引擎的基础平台
- **JUnit Jupiter**：JUnit 5的核心，提供新的编程和扩展模型
- **JUnit Vintage**：向后兼容JUnit 3和JUnit 4

### 🔧 核心注解详解

#### @Test - 标记测试方法
```java
@Test
void shouldCalculateSum() {
    // 测试逻辑
}
```

#### @DisplayName - 为测试提供可读的名称
```java
@Test
@DisplayName("当输入有效邮箱时，验证应该通过")
void shouldValidateEmailWhenInputIsValid() {
    // 测试逻辑
}
```

#### @BeforeEach 和 @AfterEach - 在每个测试前后执行
```java
@BeforeEach
void setUp() {
    // 每个测试方法执行前运行
    System.out.println("准备测试数据");
}

@AfterEach
void tearDown() {
    // 每个测试方法执行后运行
    System.out.println("清理测试数据");
}
```

#### @BeforeAll 和 @AfterAll - 在所有测试前后执行
```java
@BeforeAll
static void setUpAll() {
    // 所有测试方法执行前运行一次
    System.out.println("初始化测试环境");
}

@AfterAll
static void tearDownAll() {
    // 所有测试方法执行后运行一次
    System.out.println("清理测试环境");
}
```

#### @ParameterizedTest - 参数化测试
```java
@ParameterizedTest
@ValueSource(strings = {"hello", "world", "test"})
void shouldNotBeEmpty(String input) {
    assertThat(input).isNotEmpty();
}

@ParameterizedTest
@CsvSource({
    "1, 1, 2",
    "2, 3, 5",
    "5, 7, 12"
})
void shouldCalculateSum(int a, int b, int expected) {
    assertThat(a + b).isEqualTo(expected);
}
```

#### @RepeatedTest - 重复测试
```java
@RepeatedTest(5)
void shouldGenerateRandomNumber() {
    int random = new Random().nextInt(100);
    assertThat(random).isBetween(0, 99);
}
```

#### @Nested - 嵌套测试
```java
@DisplayName("用户服务测试")
class UserServiceTest {
    
    @Nested
    @DisplayName("用户注册测试")
    class RegistrationTests {
        
        @Test
        @DisplayName("有效输入应该注册成功")
        void shouldRegisterWithValidInput() {
            // 测试逻辑
        }
        
        @Test
        @DisplayName("无效邮箱应该抛出异常")
        void shouldThrowExceptionWithInvalidEmail() {
            // 测试逻辑
        }
    }
    
    @Nested
    @DisplayName("用户登录测试")
    class LoginTests {
        
        @Test
        @DisplayName("正确密码应该登录成功")
        void shouldLoginWithCorrectPassword() {
            // 测试逻辑
        }
    }
}
```

#### @Tag - 测试标签
```java
@Test
@Tag("fast")
void quickTest() {
    // 快速测试
}

@Test
@Tag("slow")
void slowTest() {
    // 慢速测试
}
```

#### @Disabled - 禁用测试
```java
@Test
@Disabled("此测试暂时禁用，等待修复")
void disabledTest() {
    // 被禁用的测试
}
```

### 🔍 断言详解

#### 基本断言
```java
@Test
void testBasicAssertions() {
    // 相等性断言
    assertEquals(5, 2 + 3);
    assertEquals("Hello", "Hello");
    
    // 布尔断言
    assertTrue(5 > 3);
    assertFalse(5 < 3);
    
    // 空值断言
    assertNull(null);
    assertNotNull("not null");
    
    // 数组断言
    assertArrayEquals(new int[]{1, 2, 3}, new int[]{1, 2, 3});
}
```

#### 组合断言
```java
@Test
void testGroupedAssertions() {
    User user = new User("john", "john@example.com");
    
    // 所有断言都会执行，即使前面的失败了
    assertAll("user properties",
        () -> assertEquals("john", user.getUsername()),
        () -> assertEquals("john@example.com", user.getEmail()),
        () -> assertTrue(user.isActive())
    );
}
```

#### 异常断言
```java
@Test
void testExceptionAssertions() {
    // 断言抛出特定异常
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
        throw new IllegalArgumentException("参数错误");
    });
    
    assertEquals("参数错误", exception.getMessage());
    
    // 断言不抛出异常
    assertDoesNotThrow(() -> {
        // 不应该抛出异常的代码
    });
}
```

#### 超时断言
```java
@Test
void testTimeoutAssertions() {
    // 断言在指定时间内完成
    assertTimeout(Duration.ofSeconds(2), () -> {
        // 应该在2秒内完成的代码
        Thread.sleep(1000);
    });
    
    // 如果超时，立即中断
    assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
        // 应该在1秒内完成的代码
    });
}
```

### 🔄 测试生命周期
```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestLifecycleDemo {
    
    @BeforeAll
    void setUpAll() {
        System.out.println("1. 所有测试开始前执行一次");
    }
    
    @BeforeEach
    void setUp() {
        System.out.println("2. 每个测试方法前执行");
    }
    
    @Test
    void test1() {
        System.out.println("3. 执行测试1");
    }
    
    @Test
    void test2() {
        System.out.println("3. 执行测试2");
    }
    
    @AfterEach
    void tearDown() {
        System.out.println("4. 每个测试方法后执行");
    }
    
    @AfterAll
    void tearDownAll() {
        System.out.println("5. 所有测试结束后执行一次");
    }
}
```

### 📊 动态测试
```java
@TestFactory
Stream<DynamicTest> dynamicTests() {
    return Stream.of("apple", "banana", "orange")
        .map(fruit -> dynamicTest("测试 " + fruit, () -> {
            assertThat(fruit).isNotEmpty();
        }));
}
```

### 🎯 条件测试
```java
@Test
@EnabledOnOs(OS.LINUX)
void testOnLinux() {
    // 只在Linux上运行
}

@Test
@EnabledOnJre(JRE.JAVA_17)
void testOnJava17() {
    // 只在Java 17上运行
}

@Test
@EnabledIfEnvironmentVariable(named = "ENV", matches = "test")
void testInTestEnvironment() {
    // 只在测试环境运行
}
```

---

## 4. Spring Test 详解

### 什么是Spring Test？
Spring Test是Spring Framework提供的测试支持模块，它简化了Spring应用的测试编写，提供了多种测试注解和工具。

### 🏗️ Spring Test架构
```
Spring Test = Spring Test Core + Spring Boot Test + Spring Security Test
```

### 🎯 核心测试注解

#### @SpringBootTest - 完整的Spring Boot测试
```java
@SpringBootTest
class FullIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testWithFullContext() {
        // 完整的Spring上下文已加载
    }
}
```

**配置选项**：
```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.datasource.url=jdbc:h2:mem:testdb"}
)
```

#### @WebMvcTest - Web层测试
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void shouldReturnUsers() throws Exception {
        // 只加载Web层相关的Bean
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());
    }
}
```

#### @DataJpaTest - 数据访问层测试
```java
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldFindUserByEmail() {
        // 只加载JPA相关的Bean
        User user = new User("john", "john@example.com");
        entityManager.persistAndFlush(user);
        
        Optional<User> found = userRepository.findByEmail("john@example.com");
        assertThat(found).isPresent();
    }
}
```

#### @JsonTest - JSON序列化测试
```java
@JsonTest
class UserDtoJsonTest {
    
    @Autowired
    private JacksonTester<UserDto> json;
    
    @Test
    void shouldSerializeUserDto() throws Exception {
        UserDto user = new UserDto(1L, "john", "john@example.com");
        
        assertThat(json.write(user)).isEqualToJson("user.json");
        assertThat(json.write(user)).hasJsonPathStringValue("@.username");
    }
}
```

### 🔧 测试配置和Profile

#### @ActiveProfiles - 激活测试配置
```java
@SpringBootTest
@ActiveProfiles("test")
class ProfileTest {
    // 使用test profile的配置
}
```

#### @TestConfiguration - 测试专用配置
```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        // 测试用的弱密码编码器，提高测试速度
        return new BCryptPasswordEncoder(4);
    }
}
```

#### @Import - 导入配置
```java
@SpringBootTest
@Import(TestConfig.class)
class ConfigTest {
    // 导入测试配置
}
```

### 🎭 Mock和Spy

#### @MockBean - Spring Boot的Mock Bean
```java
@SpringBootTest
class MockBeanTest {
    
    @MockBean
    private UserService userService;
    
    @Test
    void testWithMockBean() {
        when(userService.findById(1L)).thenReturn(new UserDto());
        // userService在Spring上下文中被Mock替换
    }
}
```

#### @SpyBean - Spring Boot的Spy Bean
```java
@SpringBootTest
class SpyBeanTest {
    
    @SpyBean
    private UserService userService;
    
    @Test
    void testWithSpyBean() {
        // 部分方法使用真实实现，部分使用Mock
        doReturn(new UserDto()).when(userService).findById(1L);
    }
}
```

### 🌐 Web测试工具

#### MockMvc - 模拟HTTP请求
```java
@WebMvcTest(UserController.class)
class MockMvcTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldHandleGetRequest() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.username").value("john"));
    }
    
    @Test
    void shouldHandlePostRequest() throws Exception {
        UserDto user = new UserDto(1L, "john", "john@example.com");
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(user)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/users/1"));
    }
}
```

#### TestRestTemplate - 真实HTTP客户端
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestTemplateTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldMakeRealHttpCall() {
        ResponseEntity<UserDto> response = restTemplate.getForEntity(
            "/api/users/1", UserDto.class);
            
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("john");
    }
}
```

#### WebTestClient - 响应式Web测试
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebTestClientTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldTestReactiveEndpoint() {
        webTestClient.get()
            .uri("/api/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(UserDto.class)
            .value(user -> assertThat(user.getUsername()).isEqualTo("john"));
    }
}
```

### 🗄️ 数据库测试

#### @Sql - 执行SQL脚本
```java
@SpringBootTest
@Sql("/test-data.sql")
class SqlTest {
    
    @Test
    @Sql(statements = "INSERT INTO users (username, email) VALUES ('test', 'test@example.com')")
    void testWithSqlData() {
        // 测试前先执行SQL
    }
}
```

#### @Transactional - 事务回滚
```java
@SpringBootTest
@Transactional
class TransactionalTest {
    
    @Test
    void testWithTransactionRollback() {
        // 测试结束后自动回滚数据库更改
    }
}
```

#### @Rollback - 控制回滚行为
```java
@SpringBootTest
@Transactional
class RollbackTest {
    
    @Test
    @Rollback(false)
    void testWithoutRollback() {
        // 测试结束后不回滚
    }
}
```

### 🔐 安全测试

#### @WithMockUser - 模拟用户
```java
@WebMvcTest(UserController.class)
class SecurityTest {
    
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldAllowAdminAccess() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldDenyUserAccess() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden());
    }
}
```

#### @WithUserDetails - 使用真实用户
```java
@SpringBootTest
@WithUserDetails("john@example.com")
class UserDetailsTest {
    
    @Test
    void testWithRealUser() {
        // 使用真实用户数据进行测试
    }
}
```

### 🎯 测试切片详解

#### Web层测试切片
```java
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class WebLayerTest {
    // 只加载Web层相关的Bean
    // 包括：Controllers, ControllerAdvice, JsonComponent, 
    // WebMvcConfigurer, HandlerMethodArgumentResolver等
}
```

#### 数据层测试切片
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DataLayerTest {
    // 只加载JPA相关的Bean
    // 包括：Repository, EntityManager, TestEntityManager等
}
```

#### 缓存测试切片
```java
@DataRedisTest
class CacheLayerTest {
    // 只加载Redis相关的Bean
}
```

### 🔄 测试执行顺序

#### @TestMethodOrder - 测试方法排序
```java
@TestMethodOrder(OrderAnnotation.class)
class OrderedTest {
    
    @Test
    @Order(1)
    void firstTest() {
        // 第一个执行
    }
    
    @Test
    @Order(2)
    void secondTest() {
        // 第二个执行
    }
}
```

### 📊 测试上下文管理

#### @DirtiesContext - 重置上下文
```java
@SpringBootTest
class ContextTest {
    
    @Test
    @DirtiesContext
    void testThatDirtiesContext() {
        // 这个测试会污染Spring上下文
        // 测试后会重新创建上下文
    }
}
```

---

## 5. 测试金字塔结构

### 📊 测试金字塔理论
```
        /\
       /  \    集成测试 (少量、慢、全面)
      /____\
     /      \
    /        \  切片测试 (中等、中速、专注)
   /          \
  /__________\
 /            \
/              \ 单元测试 (大量、快、独立)
```

### 第一层：单元测试 (Unit Tests)
**特点**：快速、独立、只测试一个类

**示例**：测试密码加密工具类
```java
@ExtendWith(MockitoExtension.class)
class PasswordUtilTest {
    
    @Test
    void shouldEncryptPassword() {
        // Arrange
        String plainPassword = "password123";
        
        // Act
        String encrypted = PasswordUtil.encrypt(plainPassword);
        
        // Assert
        assertThat(encrypted).isNotEqualTo(plainPassword);
        assertThat(encrypted).isNotEmpty();
    }
}
```

### 第二层：切片测试 (Slice Tests)
**特点**：只加载Spring的一部分，专注测试某一层

#### Web层测试 (@WebMvcTest)
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void shouldReturnUserById() throws Exception {
        // Arrange
        UserDto user = new UserDto(1L, "john", "john@example.com");
        when(userService.findById(1L)).thenReturn(user);
        
        // Act & Assert
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("john"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }
}
```

#### 数据层测试 (@DataJpaTest)
```java
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldFindUserByEmail() {
        // Arrange
        User user = new User("john", "john@example.com");
        userRepository.save(user);
        
        // Act
        Optional<User> found = userRepository.findByEmail("john@example.com");
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john");
    }
}
```

### 第三层：集成测试 (Integration Tests)
**特点**：加载完整应用，测试端到端流程

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateAndRetrieveUser() {
        // Arrange
        CreateUserDto createUser = new CreateUserDto("john", "john@example.com");
        
        // Act
        ResponseEntity<UserDto> response = restTemplate.postForEntity(
            "/api/users", createUser, UserDto.class);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUsername()).isEqualTo("john");
    }
}
```

---

## 6. 实战：如何运行测试

### 🏃‍♂️ 运行命令

#### 1. 只运行单元测试（推荐日常开发）
```bash
# 切换到backend目录
cd /Users/hongxichen/Desktop/mini-ups/backend

# 使用Java 17运行单元测试
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn test
```

#### 2. 运行所有测试（包括集成测试）
```bash
# 运行完整测试套件
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn verify
```

#### 3. 运行特定测试类
```bash
# 运行单个测试类
mvn test -Dtest=UserServiceTest

# 运行多个测试类
mvn test -Dtest=UserServiceTest,AuthServiceTest
```

#### 4. 运行特定测试方法
```bash
# 运行类中的特定方法
mvn test -Dtest=UserServiceTest#testFindById
```

### 📊 测试报告
运行测试后，查看报告：
```bash
# 测试报告位置
open target/surefire-reports/index.html

# 代码覆盖率报告（JaCoCo）
open target/site/jacoco/index.html
```

---

## 7. 测试代码编写指南

### 🔧 编写单元测试

#### 步骤1：创建测试类
```java
// 测试类命名：被测试类名 + Test
// 文件位置：src/test/java/com/miniups/service/UserServiceTest.java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    // 测试方法
}
```

#### 步骤2：编写测试方法
```java
@Test
@DisplayName("根据ID查找用户应该返回正确的用户")
void shouldReturnUserWhenFindById() {
    // Arrange
    Long userId = 1L;
    User expectedUser = new User("john", "john@example.com");
    when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));
    
    // Act
    UserDto actualUser = userService.findById(userId);
    
    // Assert
    assertThat(actualUser).isNotNull();
    assertThat(actualUser.getUsername()).isEqualTo("john");
    assertThat(actualUser.getEmail()).isEqualTo("john@example.com");
    
    // 验证调用次数
    verify(userRepository, times(1)).findById(userId);
}

@Test
@DisplayName("查找不存在的用户应该抛出异常")
void shouldThrowExceptionWhenUserNotFound() {
    // Arrange
    Long userId = 999L;
    when(userRepository.findById(userId)).thenReturn(Optional.empty());
    
    // Act & Assert
    assertThatThrownBy(() -> userService.findById(userId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("User not found with id: 999");
}
```

### 🌐 编写Web层测试

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthService authService;
    
    @Test
    @WithMockUser
    void shouldRegisterUser() throws Exception {
        // Arrange
        RegisterRequestDto request = new RegisterRequestDto("john", "john@example.com", "password");
        UserDto response = new UserDto(1L, "john", "john@example.com");
        when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request))
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("john"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }
    
    // 工具方法：将对象转换为JSON字符串
    private String asJsonString(Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }
}
```

### 🗄️ 编写数据层测试

```java
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldFindUserByEmail() {
        // Arrange
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        userRepository.save(user);
        
        // Act
        Optional<User> found = userRepository.findByEmail("john@example.com");
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john");
    }
    
    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        
        // Assert
        assertThat(found).isEmpty();
    }
}
```

### 🔗 编写集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldCreateUserEndToEnd() {
        // Arrange
        CreateUserDto createRequest = new CreateUserDto("john", "john@example.com", "password");
        
        // Act
        ResponseEntity<UserDto> response = restTemplate.postForEntity(
            "/api/users", createRequest, UserDto.class);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUsername()).isEqualTo("john");
        
        // 验证数据库中确实创建了用户
        Optional<User> savedUser = userRepository.findByEmail("john@example.com");
        assertThat(savedUser).isPresent();
    }
}
```

---

## 8. 常见问题与解决方案

### ❌ 问题1：测试运行太慢
**症状**：`mvn test` 需要很长时间
**原因**：过度使用 `@SpringBootTest`
**解决方案**：
```java
// ❌ 错误：为了测试Controller就加载整个应用
@SpringBootTest
class UserControllerTest {
    // ...
}

// ✅ 正确：只加载Web层
@WebMvcTest(UserController.class)
class UserControllerTest {
    // ...
}
```

### ❌ 问题2：测试互相影响
**症状**：单独运行测试通过，一起运行就失败
**原因**：测试之间共享状态
**解决方案**：
```java
// ✅ 在集成测试类上加上@Transactional
@SpringBootTest
@Transactional
class UserIntegrationTest {
    // 每个测试方法结束后自动回滚数据库更改
}

// ✅ 在测试方法前后清理状态
@BeforeEach
void setUp() {
    // 清理测试数据
    userRepository.deleteAll();
}
```

### ❌ 问题3：无法测试需要登录的API
**症状**：测试返回401 Unauthorized
**解决方案**：
```java
// 方法1：使用@WithMockUser（适用于@WebMvcTest）
@Test
@WithMockUser(username = "admin", roles = {"ADMIN"})
void shouldAccessAdminEndpoint() {
    // ...
}

// 方法2：生成真实JWT（适用于@SpringBootTest）
@Test
void shouldAccessProtectedEndpoint() {
    String token = jwtTokenProvider.generateToken("admin");
    
    mockMvc.perform(get("/api/admin/users")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
}
```

### ❌ 问题4：Mock对象没有生效
**症状**：测试中的Mock对象返回null
**解决方案**：
```java
// ✅ 确保使用了正确的注解
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void test() {
        // 必须定义Mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        
        // 然后才能调用
        userService.findById(1L);
    }
}
```

### ❌ 问题5：Testcontainers启动失败
**症状**：测试启动时Docker容器无法启动
**解决方案**：
1. 确保Docker已安装并运行
2. 检查Docker权限
3. 使用正确的镜像版本：
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");
```

---

## 9. 测试最佳实践

### ✅ 命名规范
```java
// 测试类命名
UserServiceTest.java          // 单元测试
UserControllerTest.java       // 切片测试
UserIntegrationTest.java      // 集成测试

// 测试方法命名
@Test
void shouldReturnUserWhenValidId() { } // 应该...当...

@Test
void shouldThrowExceptionWhenInvalidId() { } // 应该...当...
```

### ✅ 测试数据管理
```java
// 使用工厂方法创建测试数据
public class TestDataFactory {
    
    public static User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }
    
    public static CreateUserDto createUserDto(String username, String email) {
        return new CreateUserDto(username, email, "password");
    }
}

// 在测试中使用
@Test
void shouldCreateUser() {
    User user = TestDataFactory.createUser("john", "john@example.com");
    // 测试逻辑
}
```

### ✅ 断言技巧
```java
// ✅ 使用AssertJ的流式断言
assertThat(users)
    .hasSize(3)
    .extracting(User::getUsername)
    .containsExactly("john", "jane", "bob");

// ✅ 对异常进行详细断言
assertThatThrownBy(() -> userService.findById(999L))
    .isInstanceOf(UserNotFoundException.class)
    .hasMessage("User not found with id: 999")
    .hasNoCause();
```

### ✅ 测试覆盖率
```bash
# 运行测试并生成覆盖率报告
mvn clean test jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html
```

目标覆盖率：
- **行覆盖率**：≥80%
- **分支覆盖率**：≥70%
- **方法覆盖率**：≥85%

---

## 🚀 开始你的测试之旅

### 第一步：运行现有测试
```bash
cd /Users/hongxichen/Desktop/mini-ups/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn test
```

### 第二步：选择一个简单的类开始
推荐从工具类开始，比如：
- `StringUtils`
- `DateUtils`
- `ValidationUtils`

### 第三步：编写你的第一个测试
```java
@Test
void shouldReturnTrueWhenEmailIsValid() {
    // Arrange
    String email = "test@example.com";
    
    // Act
    boolean result = ValidationUtils.isValidEmail(email);
    
    // Assert
    assertThat(result).isTrue();
}
```

### 第四步：逐步增加复杂度
- 单元测试 → 切片测试 → 集成测试
- 简单类 → 复杂服务 → 完整流程

---

## 10. JUnit 5 & Spring Test 实战练习

### 🎯 练习1：JUnit 5 基础功能
创建一个简单的计算器测试类，练习JUnit 5的核心功能：

```java
package com.miniups.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("计算器测试")
class CalculatorTest {
    
    private Calculator calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new Calculator();
        System.out.println("准备测试环境");
    }
    
    @AfterEach
    void tearDown() {
        System.out.println("清理测试环境");
    }
    
    @Nested
    @DisplayName("加法测试")
    class AdditionTests {
        
        @Test
        @DisplayName("正数相加应该返回正确结果")
        void shouldAddPositiveNumbers() {
            assertEquals(5, calculator.add(2, 3));
        }
        
        @ParameterizedTest
        @DisplayName("参数化测试：多组数据验证加法")
        @CsvSource({
            "1, 1, 2",
            "2, 3, 5",
            "-1, 1, 0",
            "0, 0, 0"
        })
        void shouldAddNumbers(int a, int b, int expected) {
            assertEquals(expected, calculator.add(a, b));
        }
    }
    
    @Nested
    @DisplayName("除法测试")
    class DivisionTests {
        
        @Test
        @DisplayName("正常除法应该返回正确结果")
        void shouldDivideNumbers() {
            assertEquals(2.0, calculator.divide(6, 3));
        }
        
        @Test
        @DisplayName("除零应该抛出异常")
        void shouldThrowExceptionWhenDivideByZero() {
            Exception exception = assertThrows(ArithmeticException.class, () -> {
                calculator.divide(5, 0);
            });
            assertEquals("Division by zero", exception.getMessage());
        }
        
        @RepeatedTest(5)
        @DisplayName("重复测试除法运算")
        void shouldDivideRepeatedlty() {
            assertTrue(calculator.divide(10, 2) > 0);
        }
    }
    
    @Test
    @Tag("performance")
    @DisplayName("性能测试：计算应该在指定时间内完成")
    void shouldCalculateWithinTime() {
        assertTimeout(Duration.ofMillis(100), () -> {
            calculator.add(1000000, 2000000);
        });
    }
    
    @Test
    @Disabled("功能暂未实现")
    void shouldHandleComplexCalculation() {
        // 待实现的功能
    }
}
```

### 🎯 练习2：Spring Test 注解练习
创建一个用户服务的完整测试套件：

```java
package com.miniups.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// 1. 单元测试版本
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceUnitTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("应该根据ID查找用户")
    void shouldFindUserById() {
        // 使用纯Mockito进行单元测试
        User user = new User("john", "john@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        UserDto result = userService.findById(1L);
        
        assertThat(result.getUsername()).isEqualTo("john");
        verify(userRepository).findById(1L);
    }
}

// 2. Spring Boot集成测试版本
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("用户服务集成测试")
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @MockBean
    private EmailService emailService;
    
    @Test
    @DisplayName("应该创建用户并发送欢迎邮件")
    void shouldCreateUserAndSendWelcomeEmail() {
        // 使用Spring上下文进行集成测试
        CreateUserDto createUser = new CreateUserDto("jane", "jane@example.com", "password");
        
        UserDto result = userService.createUser(createUser);
        
        assertThat(result.getUsername()).isEqualTo("jane");
        verify(emailService).sendWelcomeEmail("jane@example.com");
    }
}
```

### 🎯 练习3：Web层测试练习
使用不同的Spring Test注解测试Controller：

```java
package com.miniups.controller;

// 1. 使用@WebMvcTest进行切片测试
@WebMvcTest(UserController.class)
@DisplayName("用户控制器Web层测试")
class UserControllerWebTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("应该返回用户列表")
    void shouldReturnUserList() throws Exception {
        List<UserDto> users = Arrays.asList(
            new UserDto(1L, "john", "john@example.com"),
            new UserDto(2L, "jane", "jane@example.com")
        );
        when(userService.findAll()).thenReturn(users);
        
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].username").value("john"))
            .andExpect(jsonPath("$[1].username").value("jane"));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("管理员应该能够删除用户")
    void shouldAllowAdminToDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/1")
                .with(csrf()))
            .andExpect(status().isNoContent());
            
        verify(userService).deleteUser(1L);
    }
}

// 2. 使用@SpringBootTest进行完整集成测试
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("用户控制器集成测试")
class UserControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("应该创建用户并返回201状态")
    void shouldCreateUser() {
        CreateUserDto createUser = new CreateUserDto("newuser", "newuser@example.com", "password");
        
        ResponseEntity<UserDto> response = restTemplate.postForEntity(
            "/api/users", createUser, UserDto.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUsername()).isEqualTo("newuser");
        
        // 验证数据库中确实创建了用户
        Optional<User> savedUser = userRepository.findByEmail("newuser@example.com");
        assertThat(savedUser).isPresent();
    }
}
```

### 🎯 练习4：数据层测试练习
使用@DataJpaTest测试Repository：

```java
package com.miniups.repository;

@DataJpaTest
@DisplayName("用户仓库测试")
class UserRepositoryDataTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("应该根据邮箱查找用户")
    void shouldFindUserByEmail() {
        // 准备测试数据
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        entityManager.persistAndFlush(user);
        
        // 执行查询
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        // 验证结果
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
    
    @Test
    @DisplayName("应该查找活跃用户")
    void shouldFindActiveUsers() {
        // 创建活跃用户
        User activeUser = new User();
        activeUser.setUsername("active");
        activeUser.setEmail("active@example.com");
        activeUser.setActive(true);
        entityManager.persistAndFlush(activeUser);
        
        // 创建非活跃用户
        User inactiveUser = new User();
        inactiveUser.setUsername("inactive");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setActive(false);
        entityManager.persistAndFlush(inactiveUser);
        
        // 查找活跃用户
        List<User> activeUsers = userRepository.findByActiveTrue();
        
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getUsername()).isEqualTo("active");
    }
}
```

### 🎯 练习5：完整的测试套件
综合运用所有知识：

```java
package com.miniups.integration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("用户管理完整流程测试")
class UserManagementFullTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @MockBean
    private EmailService emailService;
    
    private String adminToken;
    
    @BeforeEach
    void setUp() {
        // 设置管理员token
        adminToken = "Bearer " + generateAdminToken();
    }
    
    @Test
    @Order(1)
    @DisplayName("1. 管理员创建用户应该成功")
    void adminShouldCreateUser() {
        CreateUserDto createUser = new CreateUserDto("john", "john@example.com", "password");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", adminToken);
        HttpEntity<CreateUserDto> request = new HttpEntity<>(createUser, headers);
        
        ResponseEntity<UserDto> response = restTemplate.postForEntity(
            "/api/users", request, UserDto.class);
        
        // 验证响应
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUsername()).isEqualTo("john");
        
        // 验证数据库
        Optional<User> savedUser = userRepository.findByEmail("john@example.com");
        assertThat(savedUser).isPresent();
        
        // 验证邮件发送
        verify(emailService).sendWelcomeEmail("john@example.com");
    }
    
    @Test
    @Order(2)
    @DisplayName("2. 用户应该能够登录")
    void userShouldLogin() {
        LoginRequestDto loginRequest = new LoginRequestDto("john@example.com", "password");
        
        ResponseEntity<LoginResponseDto> response = restTemplate.postForEntity(
            "/api/auth/login", loginRequest, LoginResponseDto.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotEmpty();
    }
    
    @Test
    @Order(3)
    @DisplayName("3. 用户应该能够更新个人信息")
    void userShouldUpdateProfile() {
        String userToken = "Bearer " + generateUserToken("john@example.com");
        UpdateUserDto updateUser = new UpdateUserDto("John Doe", "john.doe@example.com");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", userToken);
        HttpEntity<UpdateUserDto> request = new HttpEntity<>(updateUser, headers);
        
        ResponseEntity<UserDto> response = restTemplate.exchange(
            "/api/users/profile", HttpMethod.PUT, request, UserDto.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo("john.doe@example.com");
    }
    
    @ParameterizedTest
    @Order(4)
    @DisplayName("4. 参数化测试：各种无效输入应该返回400")
    @ValueSource(strings = {"", "invalid-email", "@example.com", "user@"})
    void shouldRejectInvalidEmails(String invalidEmail) {
        CreateUserDto createUser = new CreateUserDto("test", invalidEmail, "password");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", adminToken);
        HttpEntity<CreateUserDto> request = new HttpEntity<>(createUser, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/users", request, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    private String generateAdminToken() {
        // 生成管理员token的逻辑
        return "admin-token";
    }
    
    private String generateUserToken(String email) {
        // 生成用户token的逻辑
        return "user-token";
    }
}
```

### 🎯 练习总结

通过这些练习，您应该掌握：

1. **JUnit 5核心功能**：
   - 生命周期注解（@BeforeEach, @AfterEach等）
   - 测试组织（@Nested, @DisplayName）
   - 参数化测试（@ParameterizedTest）
   - 异常和超时测试

2. **Spring Test注解**：
   - @SpringBootTest vs @WebMvcTest vs @DataJpaTest
   - @MockBean vs @Mock
   - @ActiveProfiles和@TestConfiguration

3. **测试策略**：
   - 单元测试：快速、独立
   - 切片测试：专注某一层
   - 集成测试：端到端验证

4. **最佳实践**：
   - 测试命名规范
   - 测试数据管理
   - 断言技巧
   - 安全测试

---

## 📝 小结

测试不是负担，而是保障代码质量的最佳实践。通过这份指南，你应该能够：

1. **理解**不同类型测试的用途
2. **运行**项目中的现有测试
3. **编写**自己的测试用例
4. **解决**常见的测试问题
5. **遵循**测试最佳实践

记住：**好的测试是最好的文档，也是重构的安全网！**

---

**Happy Testing! 🎉**

*如果你在测试过程中遇到任何问题，可以参考这份指南或者查看项目中的现有测试用例作为参考。*