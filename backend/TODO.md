# MyBatis 迁移进度跟踪

## 项目状态
**迁移类型**: Spring Data JPA → MyBatis
**开始时间**: 2025-10-31
**当前进度**: 70% 完成

---

## ✅ 已完成的工作

### 1. 配置文件更新
- [x] `pom.xml` - 移除JPA依赖，添加MyBatis + PageHelper
- [x] `pom.xml` - 配置Lombok注解处理器 (maven-compiler-plugin 3.11.0)
- [x] `application.yml` - 添加MyBatis配置
- [x] `MiniUpsApplication.java` - 添加@MapperScan注解

### 2. 数据库Schema
- [x] `schema.sql` - 创建完整的数据库表结构

### 3. Entity层迁移 (12个文件)
- [x] `BaseEntity.java` - 移除JPA注解，保留基础字段
- [x] `User.java` - 移除@Entity等注解
- [x] `Shipment.java` - 添加外键字段(userId, truckId)
- [x] `ShipmentPackage.java` - 移除JPA注解
- [x] `ShipmentStatusHistory.java` - 移除JPA注解
- [x] `Truck.java` - 移除JPA注解
- [x] `TruckLocationHistory.java` - 移除JPA注解
- [x] `Driver.java` - 添加外键字段(truckId)
- [x] `AuditLog.java` - 移除JPA注解
- [x] `OutboxEvent.java` - 移除JPA注解
- [x] `CommunicationLog.java` - 移除JPA注解
- [x] `AddressChange.java` - 移除JPA注解
- [x] `TrackingSequence.java` - 移除JPA注解
- [x] `LeafAlloc.java` - 保留Lombok，移除JPA

### 4. Repository层迁移 (10个文件)
- [x] `UserRepository.java` - 转换为MyBatis Mapper
- [x] `ShipmentRepository.java` - 转换为MyBatis Mapper
- [x] `TruckRepository.java` - 转换为MyBatis Mapper
- [x] `DriverRepository.java` - 转换为MyBatis Mapper
- [x] `AuditLogRepository.java` - 转换为MyBatis Mapper
- [x] `OutboxEventRepository.java` - 转换为MyBatis Mapper
- [x] `CommunicationLogRepository.java` - 转换为MyBatis Mapper
- [x] `ShipmentStatusHistoryRepository.java` - 转换为MyBatis Mapper
- [x] `TrackingSequenceRepository.java` - 转换为MyBatis Mapper
- [x] `LeafAllocRepository.java` - 转换为MyBatis Mapper

### 5. Service层迁移 (部分完成)
- [x] `AuthService.java` - 已完成所有JPA模式转换
  - ✓ Optional → null检查
  - ✓ .orElseThrow() → null + 异常
  - ✓ save() → insert()/update()
  - ✓ 移除Optional import
- [x] `DataInitializer.java` - 已修复save()调用

### 6. 配置类
- [x] `JpaConfig.java` - 已删除(备份为.bak)

---

## 🔄 进行中的工作

### Service层剩余迁移 (13个文件)

#### 高优先级 - 核心服务
1. **AdminService.java** - 🔄 进行中
   - [ ] 修复 `findAll(Pageable)` → PageHelper分页
   - [ ] 修复 `Page<T>` 返回类型 → 自定义分页对象
   - 错误行: 218, 255, 319, 384

2. **AmazonIntegrationService.java**
   - [ ] 修复 `save()` → `insert()`/`update()` (6处)
   - [ ] 修复 `Optional<Shipment>` → null检查 (5处)
   - [ ] 修复 `Optional<User>` → null检查 (1处)
   - 错误行: 146, 165, 218, 308, 316, 342, 386, 401, 524, 537, 621, 638

3. **TrackingService.java**
   - [ ] 修复JPA查询方法
   - [ ] 修复save()调用

4. **TruckManagementService.java**
   - [ ] 修复truck相关的JPA操作

#### 中优先级 - 日志和通信服务
5. **CommunicationLogService.java**
   - [ ] 修复 `save()` → `insert()` (4处)
   - [ ] 修复 `findTop50ByOrderByCreatedAtDesc()` → MyBatis查询
   - 错误行: 60, 70, 82, 90, 99

6. **EventPublisherService.java**
   - [ ] 修复outbox事件保存逻辑

7. **AnalyticsConsumer.java**
   - [ ] 修复消费者中的JPA操作

#### 低优先级 - 工具类和其他
8. **DriverService.java**
   - [ ] 修复driver相关JPA操作

9. **LeafSegmentIdGenerator.java**
   - [ ] 修复ID生成器中的JPA操作

10. **CustomUserDetailsService.java**
    - [ ] 修复用户查询逻辑

11. **OpenAiEmbeddingClient.java**
    - [ ] 修复RAG相关的JPA操作

12. **LeafAlloc.java** (Entity)
    - [ ] 检查是否有额外的JPA引用需要移除

13. **RabbitMQConfig.java**
    - [ ] 检查Lombok @Slf4j是否正常工作

---

## 📝 迁移模式参考

### 常见转换模式

#### 1. Optional转换
```java
// JPA (旧)
Optional<User> user = userRepository.findByUsername(username);
User u = user.orElseThrow(() -> new UserNotFoundException());

// MyBatis (新)
User user = userRepository.findByUsername(username);
if (user == null) {
    throw new UserNotFoundException();
}
```

#### 2. save()转换
```java
// JPA (旧)
User savedUser = userRepository.save(user);

// MyBatis (新)
if (user.getId() == null) {
    userRepository.insert(user);
} else {
    userRepository.update(user);
}
User savedUser = user; // insert/update已修改原对象
```

#### 3. 分页转换
```java
// JPA (旧)
Page<User> users = userRepository.findAll(pageable);

// MyBatis (新)
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

PageHelper.startPage(pageable.getPageNumber() + 1, pageable.getPageSize());
List<User> userList = userRepository.findAll();
PageInfo<User> pageInfo = new PageInfo<>(userList);

// 构建自定义分页响应
Map<String, Object> response = new HashMap<>();
response.put("content", pageInfo.getList());
response.put("totalElements", pageInfo.getTotal());
response.put("totalPages", pageInfo.getPages());
response.put("number", pageInfo.getPageNum() - 1);
response.put("size", pageInfo.getPageSize());
```

#### 4. 自定义查询方法
```java
// JPA (旧)
List<Shipment> findByStatus(ShipmentStatus status);

// MyBatis (新)
@Select("SELECT * FROM shipments WHERE status = #{status}")
List<Shipment> findByStatus(@Param("status") ShipmentStatus status);
```

---

## 🐛 已解决的问题

### 1. Lombok注解处理器配置
**问题**: `log` 变量找不到符号
**原因**: maven-compiler-plugin未配置Lombok注解处理器
**解决**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
    </configuration>
</plugin>
```
移除了annotationProcessorPaths，让Maven自动发现Lombok依赖

### 2. 编译器版本兼容性
**问题**: `java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag`
**原因**: maven-compiler-plugin 3.13.0与Lombok不兼容
**解决**: 降级到3.11.0

### 3. 重复的maven-compiler-plugin
**问题**: Maven警告重复的plugin配置
**原因**: pom.xml中有两个maven-compiler-plugin定义
**解决**: 合并为单一配置

### 4. Hibernate异常类引用
**问题**: `ConstraintViolationException`找不到(AuthService.java)
**原因**: 移除Hibernate依赖后，异常类不存在
**解决**: 改用`java.sql.SQLException`并解析PostgreSQL错误消息

---

## 📋 待办事项

### 立即执行
- [ ] 修复AdminService.java (当前进行中)
- [ ] 修复AmazonIntegrationService.java
- [ ] 修复CommunicationLogService.java
- [ ] 修复剩余10个Service文件

### 后续工作
- [ ] 更新测试文件 (转换JPA测试为MyBatis测试)
- [ ] 运行完整编译验证
- [ ] 运行测试套件
- [ ] 更新MYBATIS_MIGRATION_GUIDE.md文档

### 可选优化
- [ ] 为复杂查询创建XML Mapper文件
- [ ] 优化分页查询性能
- [ ] 添加MyBatis ResultMap配置

---

## 📚 相关文档

- [MYBATIS_MIGRATION_GUIDE.md](./MYBATIS_MIGRATION_GUIDE.md) - 详细迁移指南
- [MIGRATION_STATUS.md](./MIGRATION_STATUS.md) - 迁移状态记录
- [schema.sql](./src/main/resources/schema.sql) - 数据库表结构

---

## 🔍 注意事项

1. **外键字段**: 所有关系都通过外键ID字段管理，不再使用JPA的@ManyToOne
2. **事务管理**: 继续使用Spring的@Transactional
3. **验证注解**: 保留Jakarta Validation注解(@NotBlank, @Email等)
4. **Lombok注解**: 保留所有Lombok注解(@Data, @Slf4j等)
5. **枚举类型**: 在schema.sql中使用VARCHAR存储，MyBatis自动映射

---

## 🔥 当前状态

**编译进度**: 85% 完成
**主要问题**: Lombok注解处理器问题导致部分类的getter/setter未生成

### 剩余编译错误（约50个）

#### 主要问题文件：
1. **OutboxEvent.java** - @Data注解未生成getter/setter（影响OutboxPollerService）
2. **KafkaMessagingProperties.java** - TopicDefinition的@Data未生成方法（影响KafkaConfig）
3. **RabbitMQConfig.java** - @Slf4j未生成log变量
4. **KafkaConfig.java** - @Slf4j未生成log变量

#### 需要添加的Repository方法：
- OutboxEventRepository: findEventsReadyForProcessing(), claimEventsForProcessing()

### 临时解决方案
考虑为问题类手动添加getter/setter作为临时方案，然后再彻底解决Lombok配置问题。

---

**最后更新**: 2025-10-31 23:20
**更新人**: Claude Code
