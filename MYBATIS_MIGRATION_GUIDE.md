# MyBatis 迁移指南

本文档提供了从 Spring Data JPA 迁移到 MyBatis 的完整指南。

## 已完成的迁移步骤

### 1. 依赖更新 (✅ 已完成)

**backend/pom.xml 变更:**
- ❌ 移除: `spring-boot-starter-data-jpa`
- ✅ 添加: `mybatis-spring-boot-starter` (3.0.3)
- ✅ 添加: `pagehelper-spring-boot-starter` (2.1.0)

### 2. 配置文件更新 (✅ 已完成)

**backend/src/main/resources/application.yml:**

```yaml
# 移除了 JPA 配置
# jpa:
#   hibernate:
#     ddl-auto: update

# 添加了 MyBatis 配置
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.miniups.model.entity
  configuration:
    map-underscore-to-camel-case: true
    use-generated-keys: true
    default-fetch-size: 100
    default-statement-timeout: 30
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

pagehelper:
  helper-dialect: postgresql
  reasonable: true
  support-methods-arguments: true
  params: count=countSql
```

### 3. Entity 类迁移 (✅ User 已完成示例)

**变更前 (JPA):**
```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Column(name = "username")
    private String username;
    // ...
}
```

**变更后 (MyBatis):**
```java
public class User extends BaseEntity {
    private String username;  // 字段名会通过 map-underscore-to-camel-case 自动映射
    // ...
}
```

**需要移除的注解:**
- `@Entity`
- `@Table`
- `@Column`
- `@Id`
- `@GeneratedValue`
- `@ManyToOne`, `@OneToMany`, `@ManyToMany`
- `@JoinColumn`
- `@Enumerated`

### 4. Repository 迁移 (✅ UserRepository 已完成示例)

**变更前 (JPA):**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

**变更后 (MyBatis):**
```java
@Mapper
public interface UserRepository {
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username}")
    boolean existsByUsername(String username);

    @Insert("INSERT INTO users (...) VALUES (...)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE users SET ... WHERE id = #{id}")
    int update(User user);
}
```

### 5. 数据库 Schema (✅ 已完成)

创建了 `backend/src/main/resources/schema.sql`，包含所有表结构定义。

## 需要手动完成的迁移任务

### Task 1: 迁移所有 Entity 类

需要迁移以下 Entity (按照 User.java 的模式):

1. ✅ `BaseEntity.java` - 已完成
2. ✅ `User.java` - 已完成
3. ⬜ `Shipment.java`
4. ⬜ `Truck.java`
5. ⬜ `Driver.java`
6. ⬜ `ShipmentPackage.java`
7. ⬜ `AddressChange.java`
8. ⬜ `ShipmentStatusHistory.java`
9. ⬜ `TruckLocationHistory.java`
10. ⬜ `CommunicationLog.java`
11. ⬜ `AuditLog.java`
12. ⬜ `OutboxEvent.java`
13. ⬜ `TrackingSequence.java`
14. ⬜ `LeafAlloc.java`

**迁移步骤:**
```bash
# 对每个 Entity 文件执行以下操作：
1. 移除所有 JPA 注解 (@Entity, @Table, @Column 等)
2. 保留 validation 注解 (@NotNull, @Size, @Email 等)
3. 移除关联关系注解 (@ManyToOne, @OneToMany 等)
4. 关联关系改为手动查询管理
```

### Task 2: 迁移所有 Repository 接口

需要迁移以下 Repository:

1. ✅ `UserRepository.java` - 已完成
2. ⬜ `ShipmentRepository.java`
3. ⬜ `TruckRepository.java`
4. ⬜ `DriverRepository.java`
5. ⬜ `AuditLogRepository.java`
6. ⬜ `OutboxEventRepository.java`
7. ⬜ `CommunicationLogRepository.java`
8. ⬜ `ShipmentStatusHistoryRepository.java`
9. ⬜ `TrackingSequenceRepository.java`
10. ⬜ `LeafAllocRepository.java`

**迁移步骤:**
```java
// 1. 改变继承关系
// Before:
public interface UserRepository extends JpaRepository<User, Long> { }

// After:
@Mapper
public interface UserRepository { }

// 2. 添加基本 CRUD 方法
@Insert("INSERT INTO users (...) VALUES (...)")
@Options(useGeneratedKeys = true, keyProperty = "id")
int insert(User user);

@Update("UPDATE users SET ... WHERE id = #{id}")
int update(User user);

@Select("SELECT * FROM users WHERE id = #{id}")
User selectById(Long id);

@Delete("DELETE FROM users WHERE id = #{id}")
int deleteById(Long id);

// 3. 转换查询方法
// Before: Optional<User> findByUsername(String username)
// After: User findByUsername(String username)  // 返回 null 而不是 Optional

// 4. 转换存在性检查
// Before: boolean existsByUsername(String username)
// After: @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username}")
//        boolean existsByUsername(String username)
```

### Task 3: 创建 Mapper XML 文件 (可选，用于复杂查询)

对于复杂查询，建议使用 XML 文件而不是注解。已创建了 `UserMapper.xml` 作为示例。

**创建步骤:**
```bash
# 在 backend/src/main/resources/mapper/ 目录下创建 XML 文件
backend/src/main/resources/mapper/
├── UserMapper.xml      # ✅ 已创建
├── ShipmentMapper.xml  # ⬜ 待创建
├── TruckMapper.xml     # ⬜ 待创建
└── ...
```

**XML 示例结构:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.miniups.repository.ShipmentRepository">

    <resultMap id="ShipmentResultMap" type="com.miniups.model.entity.Shipment">
        <id property="id" column="id"/>
        <!-- 其他字段映射 -->
    </resultMap>

    <select id="findByStatusWithPagination" resultMap="ShipmentResultMap">
        SELECT * FROM shipments WHERE status = #{status}
    </select>

</mapper>
```

### Task 4: 更新 Service 层代码

**主要变更点:**

#### 4.1 Optional 处理
```java
// Before (JPA):
Optional<User> userOptional = userRepository.findById(id);
if (userOptional.isEmpty()) {
    throw new UserNotFoundException();
}
User user = userOptional.get();

// After (MyBatis):
User user = userRepository.selectById(id);
if (user == null) {
    throw new UserNotFoundException();
}
```

#### 4.2 保存/更新操作
```java
// Before (JPA):
User savedUser = userRepository.save(user);  // 自动判断是插入还是更新

// After (MyBatis):
if (user.getId() == null) {
    userRepository.insert(user);  // 插入
} else {
    userRepository.update(user);  // 更新
}
```

#### 4.3 分页查询
```java
// Before (JPA):
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

Page<User> users = userRepository.findByRole(role, pageable);
long total = users.getTotalElements();
List<User> content = users.getContent();

// After (MyBatis with PageHelper):
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

PageHelper.startPage(pageNum, pageSize);
List<User> users = userRepository.findByRole(role);
PageInfo<User> pageInfo = new PageInfo<>(users);
long total = pageInfo.getTotal();
List<User> content = pageInfo.getList();
```

#### 4.4 关联查询
```java
// Before (JPA - 自动级联):
User user = userRepository.findById(id).get();
List<Shipment> shipments = user.getShipments();  // 自动加载

// After (MyBatis - 手动查询):
User user = userRepository.selectById(id);
List<Shipment> shipments = shipmentRepository.findByUserId(id);
user.setShipments(shipments);
```

### Task 5: 更新测试代码

需要更新所有使用了 JPA 特性的测试:

1. 移除 `@DataJpaTest` 注解
2. 使用 `@MybatisTest` 或 `@SpringBootTest`
3. 更新 Optional 的使用
4. 更新 save() 为 insert() 或 update()
5. 更新分页相关测试

**测试示例:**
```java
// Before:
@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindByUsername() {
        Optional<User> user = userRepository.findByUsername("test");
        assertTrue(user.isPresent());
    }
}

// After:
@SpringBootTest
@Transactional
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindByUsername() {
        User user = userRepository.findByUsername("test");
        assertNotNull(user);
    }
}
```

### Task 6: 更新应用启动类

```java
// Before:
@SpringBootApplication
@EnableJpaAuditing  // ❌ 移除这个注解
public class MiniUpsApplication { }

// After:
@SpringBootApplication
@MapperScan("com.miniups.repository")  // ✅ 添加 Mapper 扫描
public class MiniUpsApplication { }
```

## 迁移验证清单

- [ ] 编译项目: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn clean compile`
- [ ] 运行单元测试: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn test`
- [ ] 启动应用: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn spring-boot:run`
- [ ] 测试 API 端点
- [ ] 验证数据库操作
- [ ] 检查日志中的 SQL 输出

## 常见问题

### Q1: 枚举类型如何处理？
```yaml
# 在 application.yml 中配置
mybatis:
  configuration:
    default-enum-type-handler: org.apache.ibatis.type.EnumTypeHandler
```

### Q2: 时间字段如何自动更新？
```sql
-- 在数据库层面使用触发器，或在 Mapper 中显式设置
updated_at = NOW()
```

### Q3: 乐观锁如何实现？
```sql
-- 在 UPDATE 语句中加入 version 检查
UPDATE users SET ..., version = version + 1
WHERE id = #{id} AND version = #{version}
```

### Q4: 事务如何管理？
MyBatis 完全兼容 Spring 的 `@Transactional` 注解，无需修改。

## 性能优化建议

1. **使用 XML 进行复杂查询**: 避免在注解中写长 SQL
2. **合理使用 ResultMap**: 避免字段映射错误
3. **启用 SQL 日志**: 便于调试和性能分析
4. **使用批量操作**: 利用 `<foreach>` 标签
5. **配置连接池**: HikariCP 参数已在 application.yml 中配置

## 参考资料

- MyBatis 官方文档: https://mybatis.org/mybatis-3/
- PageHelper 文档: https://github.com/pagehelper/Mybatis-PageHelper
- Spring Boot MyBatis Starter: https://mybatis.org/spring-boot-starter/
