# 🏃‍♂️ Java测试实战练习

## 练习0：环境验证（必做）

### 验证测试环境是否正常工作

```bash
# 1. 进入项目目录
cd /Users/hongxichen/Desktop/mini-ups/backend

# 2. 运行现有测试
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn test

# 3. 如果看到"BUILD SUCCESS"，说明环境正常
```

**预期输出**：
```
[INFO] Tests run: X, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 练习1：第一个单元测试（初级）

### 目标：为字符串工具类编写测试

在 `src/test/java/com/miniups/util/` 目录下创建 `StringUtilsTest.java`：

```java
package com.miniups.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

@DisplayName("字符串工具类测试")
class StringUtilsTest {
    
    @Test
    @DisplayName("应该正确判断字符串是否为空")
    void shouldCheckIfStringIsEmpty() {
        // TODO: 请完成这个测试
        // 1. 测试 null 字符串
        // 2. 测试 "" 空字符串
        // 3. 测试 "   " 空白字符串
        // 4. 测试 "hello" 正常字符串
        
        // 提示：使用 assertThat(result).isTrue() 或 assertThat(result).isFalse()
    }
    
    @Test
    @DisplayName("应该正确去除字符串前后空白")
    void shouldTrimWhitespace() {
        // TODO: 请完成这个测试
        // 测试 "  hello  " 应该返回 "hello"
        
        // 提示：使用 assertThat(result).isEqualTo("hello")
    }
}
```

### 运行测试
```bash
mvn test -Dtest=StringUtilsTest
```

---

## 练习2：Service层单元测试（中级）

### 目标：为UserService编写Mock测试

创建 `src/test/java/com/miniups/service/UserServiceTest.java`：

```java
package com.miniups.service;

import com.miniups.model.entity.User;
import com.miniups.model.dto.UserDto;
import com.miniups.repository.UserRepository;
import com.miniups.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("应该根据ID返回用户")
    void shouldReturnUserById() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // Act
        UserDto result = userService.findById(userId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(userRepository, times(1)).findById(userId);
    }
    
    @Test
    @DisplayName("查找不存在的用户应该抛出异常")
    void shouldThrowExceptionWhenUserNotFound() {
        // TODO: 请完成这个测试
        // 1. 准备一个不存在的用户ID
        // 2. 让 userRepository.findById() 返回 Optional.empty()
        // 3. 调用 userService.findById()
        // 4. 验证抛出 UserNotFoundException
        
        // 提示：使用 assertThatThrownBy(() -> userService.findById(999L))
    }
}
```

### 运行测试
```bash
mvn test -Dtest=UserServiceTest
```

---

## 练习3：Controller层测试（中级）

### 目标：测试REST API端点

创建 `src/test/java/com/miniups/controller/UserControllerTest.java`：

```java
package com.miniups.controller;

import com.miniups.model.dto.UserDto;
import com.miniups.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(UserController.class)
@DisplayName("用户控制器测试")
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser
    @DisplayName("应该返回用户信息")
    void shouldReturnUserById() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDto userDto = new UserDto(userId, "testuser", "test@example.com");
        when(userService.findById(userId)).thenReturn(userDto);
        
        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }
    
    @Test
    @WithMockUser
    @DisplayName("查找不存在的用户应该返回404")
    void shouldReturn404WhenUserNotFound() throws Exception {
        // TODO: 请完成这个测试
        // 1. 让 userService.findById() 抛出 UserNotFoundException
        // 2. 发送 GET 请求到 /api/users/999
        // 3. 验证返回 404 状态码
        
        // 提示：使用 when(userService.findById(999L)).thenThrow(new UserNotFoundException("User not found"));
    }
}
```

### 运行测试
```bash
mvn test -Dtest=UserControllerTest
```

---

## 练习4：数据库测试（中级）

### 目标：测试Repository层

创建 `src/test/java/com/miniups/repository/UserRepositoryTest.java`：

```java
package com.miniups.repository;

import com.miniups.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("用户仓库测试")
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    @DisplayName("应该根据邮箱查找用户")
    void shouldFindUserByEmail() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        entityManager.persistAndFlush(user);
        
        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
    
    @Test
    @DisplayName("邮箱不存在应该返回空")
    void shouldReturnEmptyWhenEmailNotFound() {
        // TODO: 请完成这个测试
        // 1. 查找一个不存在的邮箱
        // 2. 验证返回 Optional.empty()
        
        // 提示：使用 assertThat(result).isEmpty()
    }
}
```

### 运行测试
```bash
mvn test -Dtest=UserRepositoryTest
```

---

## 练习5：集成测试（高级）

### 目标：端到端测试完整流程

创建 `src/test/java/com/miniups/integration/UserIntegrationTest.java`：

```java
package com.miniups.integration;

import com.miniups.model.dto.CreateUserDto;
import com.miniups.model.dto.UserDto;
import com.miniups.model.entity.User;
import com.miniups.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("用户集成测试")
class UserIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("应该创建用户并保存到数据库")
    void shouldCreateUserEndToEnd() throws Exception {
        // Arrange
        CreateUserDto createUserDto = new CreateUserDto("newuser", "newuser@example.com", "password");
        
        // Act
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserDto))
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.email").value("newuser@example.com"));
        
        // Assert - 验证数据库中确实创建了用户
        Optional<User> savedUser = userRepository.findByEmail("newuser@example.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).isEqualTo("newuser");
    }
}
```

### 运行测试
```bash
mvn test -Dtest=UserIntegrationTest
```

---

## 🎯 进阶挑战

### 挑战1：测试异常处理
编写测试验证当服务层抛出异常时，控制器能正确处理并返回适当的HTTP状态码。

### 挑战2：测试安全性
编写测试验证：
- 未认证用户无法访问受保护的端点
- 普通用户无法访问管理员端点
- JWT令牌过期时的处理

### 挑战3：测试数据库事务
编写测试验证：
- 事务回滚机制
- 并发访问处理
- 数据一致性

### 挑战4：性能测试
编写测试验证：
- 大量数据处理的性能
- 数据库查询优化
- 内存使用情况

---

## 📊 测试完成度检查

### 基础测试（必须完成）
- [ ] 练习0：环境验证
- [ ] 练习1：第一个单元测试
- [ ] 练习2：Service层单元测试

### 进阶测试（推荐完成）
- [ ] 练习3：Controller层测试
- [ ] 练习4：数据库测试
- [ ] 练习5：集成测试

### 高级挑战（可选）
- [ ] 挑战1：测试异常处理
- [ ] 挑战2：测试安全性
- [ ] 挑战3：测试数据库事务
- [ ] 挑战4：性能测试

---

## 🏆 恭喜！

完成所有练习后，你已经掌握了：
- 单元测试的基本写法
- Mock对象的使用
- Web层测试技巧
- 数据库测试方法
- 集成测试实践

**继续加油，成为测试专家！** 🚀

---

## 🆘 遇到问题？

### 常见错误及解决方案

1. **编译错误**：检查import语句和类路径
2. **测试失败**：检查断言条件和预期值
3. **Mock不工作**：确保使用了正确的Mockito注解
4. **数据库连接问题**：确保Testcontainers正常启动

### 获取帮助
- 查看 `JAVA_TESTING_GUIDE.md` 详细说明
- 参考项目中现有的测试用例
- 搜索Spring Boot测试文档