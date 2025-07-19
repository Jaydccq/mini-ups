# 🚀 JUnit 5 & Spring Test 速查表

## 📋 JUnit 5 核心注解

### 基础注解
| 注解 | 用途 | 示例 |
|------|------|------|
| `@Test` | 标记测试方法 | `@Test void shouldWork() {}` |
| `@DisplayName` | 测试显示名称 | `@DisplayName("应该返回用户")` |
| `@BeforeEach` | 每个测试前执行 | `@BeforeEach void setUp() {}` |
| `@AfterEach` | 每个测试后执行 | `@AfterEach void tearDown() {}` |
| `@BeforeAll` | 所有测试前执行一次 | `@BeforeAll static void init() {}` |
| `@AfterAll` | 所有测试后执行一次 | `@AfterAll static void cleanup() {}` |
| `@Disabled` | 禁用测试 | `@Disabled("待修复")` |

### 高级注解
| 注解 | 用途 | 示例 |
|------|------|------|
| `@Nested` | 嵌套测试类 | `@Nested class LoginTests {}` |
| `@Tag` | 测试标签 | `@Tag("slow")` |
| `@ParameterizedTest` | 参数化测试 | `@ParameterizedTest @ValueSource(...)` |
| `@RepeatedTest` | 重复测试 | `@RepeatedTest(5)` |
| `@TestFactory` | 动态测试 | `@TestFactory Stream<DynamicTest>` |
| `@TestMethodOrder` | 测试顺序 | `@TestMethodOrder(OrderAnnotation.class)` |

## 📋 JUnit 5 断言速查

### 基本断言
```java
// 相等性断言
assertEquals(expected, actual);
assertNotEquals(expected, actual);

// 布尔断言
assertTrue(condition);
assertFalse(condition);

// 空值断言
assertNull(object);
assertNotNull(object);

// 数组断言
assertArrayEquals(expected, actual);
```

### 高级断言
```java
// 异常断言
assertThrows(Exception.class, () -> {
    // 会抛出异常的代码
});

// 超时断言
assertTimeout(Duration.ofSeconds(2), () -> {
    // 应该在2秒内完成
});

// 组合断言
assertAll("user validation",
    () -> assertEquals("john", user.getName()),
    () -> assertNotNull(user.getEmail())
);
```

## 📋 Spring Test 注解速查

### 核心测试注解
| 注解 | 用途 | 加载内容 | 适用场景 |
|------|------|----------|----------|
| `@SpringBootTest` | 完整集成测试 | 完整Spring上下文 | 端到端测试 |
| `@WebMvcTest` | Web层测试 | 仅Web层组件 | Controller测试 |
| `@DataJpaTest` | 数据层测试 | 仅JPA组件 | Repository测试 |
| `@JsonTest` | JSON测试 | 仅JSON序列化 | DTO序列化测试 |
| `@RestClientTest` | REST客户端测试 | 仅REST客户端 | 外部API调用测试 |

### Mock注解
| 注解 | 用途 | 使用场景 |
|------|------|----------|
| `@MockBean` | Spring Boot Mock Bean | 替换Spring容器中的Bean |
| `@SpyBean` | Spring Boot Spy Bean | 部分Mock Spring Bean |
| `@Mock` | Mockito Mock | 纯单元测试 |
| `@InjectMocks` | 注入Mock对象 | 创建被测试对象 |

### 配置注解
| 注解 | 用途 | 示例 |
|------|------|------|
| `@ActiveProfiles` | 激活配置文件 | `@ActiveProfiles("test")` |
| `@TestConfiguration` | 测试配置类 | `@TestConfiguration class TestConfig {}` |
| `@Import` | 导入配置 | `@Import(TestConfig.class)` |
| `@TestPropertySource` | 测试属性 | `@TestPropertySource("test.properties")` |

## 📋 安全测试注解

| 注解 | 用途 | 示例 |
|------|------|------|
| `@WithMockUser` | 模拟用户 | `@WithMockUser(roles = "ADMIN")` |
| `@WithUserDetails` | 使用真实用户 | `@WithUserDetails("admin@example.com")` |
| `@WithAnonymousUser` | 匿名用户 | `@WithAnonymousUser` |

## 📋 数据库测试注解

| 注解 | 用途 | 示例 |
|------|------|------|
| `@Transactional` | 事务回滚 | `@Transactional` |
| `@Rollback` | 控制回滚 | `@Rollback(false)` |
| `@Sql` | 执行SQL | `@Sql("/test-data.sql")` |
| `@DirtiesContext` | 重置上下文 | `@DirtiesContext` |

## 📋 常用测试工具

### MockMvc (Web层测试)
```java
@Autowired
private MockMvc mockMvc;

// GET请求
mockMvc.perform(get("/api/users"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.name").value("john"));

// POST请求
mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"john\"}"))
    .andExpect(status().isCreated());
```

### TestRestTemplate (集成测试)
```java
@Autowired
private TestRestTemplate restTemplate;

// GET请求
ResponseEntity<UserDto> response = restTemplate.getForEntity(
    "/api/users/1", UserDto.class);

// POST请求
ResponseEntity<UserDto> response = restTemplate.postForEntity(
    "/api/users", createUser, UserDto.class);
```

### TestEntityManager (数据层测试)
```java
@Autowired
private TestEntityManager entityManager;

// 保存并刷新
User user = new User("john", "john@example.com");
entityManager.persistAndFlush(user);

// 清除持久化上下文
entityManager.clear();
```

## 📋 参数化测试

### @ValueSource
```java
@ParameterizedTest
@ValueSource(strings = {"hello", "world"})
void testWithStrings(String input) {
    assertNotNull(input);
}
```

### @CsvSource
```java
@ParameterizedTest
@CsvSource({
    "1, 2, 3",
    "2, 3, 5",
    "3, 4, 7"
})
void testAddition(int a, int b, int expected) {
    assertEquals(expected, a + b);
}
```

### @MethodSource
```java
@ParameterizedTest
@MethodSource("userProvider")
void testWithUsers(User user) {
    assertNotNull(user.getName());
}

static Stream<User> userProvider() {
    return Stream.of(
        new User("john", "john@example.com"),
        new User("jane", "jane@example.com")
    );
}
```

## 📋 AssertJ 断言

### 基础断言
```java
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotNull();
assertThat(actual).isInstanceOf(User.class);
```

### 字符串断言
```java
assertThat(str).isNotEmpty();
assertThat(str).startsWith("Hello");
assertThat(str).contains("world");
assertThat(str).matches("\\d+");
```

### 集合断言
```java
assertThat(list).hasSize(3);
assertThat(list).contains("item1", "item2");
assertThat(list).containsExactly("item1", "item2", "item3");
assertThat(list).isEmpty();
```

### 异常断言
```java
assertThatThrownBy(() -> {
    // 会抛出异常的代码
}).isInstanceOf(IllegalArgumentException.class)
  .hasMessage("Invalid input");
```

## 📋 测试生命周期

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestLifecycleExample {
    
    @BeforeAll
    void beforeAll() {
        // 1. 所有测试前执行一次
    }
    
    @BeforeEach
    void beforeEach() {
        // 2. 每个测试前执行
    }
    
    @Test
    void test1() {
        // 3. 执行测试
    }
    
    @AfterEach
    void afterEach() {
        // 4. 每个测试后执行
    }
    
    @AfterAll
    void afterAll() {
        // 5. 所有测试后执行一次
    }
}
```

## 📋 条件测试

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

## 📋 常见测试模式

### 单元测试模式
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void shouldFindUserById() {
        // AAA模式
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // Act
        UserDto result = userService.findById(1L);
        
        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
    }
}
```

### Web层测试模式
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    @WithMockUser
    void shouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("john"));
    }
}
```

### 集成测试模式
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateUser() {
        ResponseEntity<UserDto> response = restTemplate.postForEntity(
            "/api/users", createUser, UserDto.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

## 📋 测试命名规范

### 测试类命名
- 单元测试：`ClassNameTest`
- 集成测试：`ClassNameIntegrationTest`
- Web测试：`ClassNameWebTest`

### 测试方法命名
- 模式：`should[ExpectedResult]When[Condition]`
- 示例：`shouldReturnUserWhenValidId`
- 示例：`shouldThrowExceptionWhenInvalidInput`

## 📋 快速命令

```bash
# 只运行单元测试
mvn test

# 运行所有测试
mvn verify

# 运行特定测试类
mvn test -Dtest=UserServiceTest

# 运行特定测试方法
mvn test -Dtest=UserServiceTest#shouldFindUser

# 生成测试报告
mvn test jacoco:report
```

---

## 💡 小贴士

1. **优先使用切片测试**：比完整的@SpringBootTest更快
2. **合理使用Mock**：只Mock直接依赖，避免过度Mock
3. **测试数据独立**：每个测试应该有独立的测试数据
4. **断言要具体**：使用具体的断言而不是通用的assertTrue
5. **测试要稳定**：避免依赖时间、随机数等不稳定因素

**保存这份速查表，随时查阅！** 🚀