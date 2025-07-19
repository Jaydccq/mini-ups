# Mini-UPS 系统 CRUD 操作完整指南

## 目录
- [第一部分：后端 CRUD 实现详解](#第一部分后端-crud-实现详解)
  - [1. 架构设计概述](#1-架构设计概述)
  - [2. 数据模型层 (Entity Layer)](#2-数据模型层-entity-layer)
  - [3. 数据访问层 (Repository Layer)](#3-数据访问层-repository-layer)
  - [4. 业务逻辑层 (Service Layer)](#4-业务逻辑层-service-layer)
  - [5. 表现层 (Controller Layer)](#5-表现层-controller-layer)
- [第二部分：前端集成与完整流程](#第二部分前端集成与完整流程)
  - [6. 前端数据访问模式](#6-前端数据访问模式)
  - [7. 完整CRUD流程示例](#7-完整crud流程示例)
  - [8. 最佳实践与性能优化](#8-最佳实践与性能优化)

---

# 第一部分：后端 CRUD 实现详解

## 1. 架构设计概述

### 1.1 分层架构模式

Mini-UPS 系统采用经典的分层架构模式，确保代码的可维护性和可扩展性：

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                         │
│              (表现层 - REST API 端点)                       │
│  - HTTP 请求处理  - 参数验证  - 响应格式化  - 权限控制        │
├─────────────────────────────────────────────────────────────┤
│                     Service Layer                          │
│              (业务逻辑层 - 核心业务处理)                     │
│  - 业务规则验证  - 事务管理  - 业务流程编排  - 异常处理       │
├─────────────────────────────────────────────────────────────┤
│                   Repository Layer                         │
│              (数据访问层 - 数据持久化)                       │
│  - 数据库操作  - 查询优化  - 缓存管理  - 数据验证             │
├─────────────────────────────────────────────────────────────┤
│                     Entity Layer                           │
│              (数据模型层 - 领域模型)                         │
│  - 实体定义  - 关系映射  - 数据验证  - 业务方法               │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 核心设计原则

1. **单一职责原则 (SRP)**: 每一层只负责特定的功能
2. **依赖倒置原则 (DIP)**: 高层模块不依赖低层模块的具体实现
3. **开闭原则 (OCP)**: 对扩展开放，对修改关闭
4. **接口隔离原则 (ISP)**: 使用专门的接口，避免依赖不需要的方法

## 2. 数据模型层 (Entity Layer)

### 2.1 基础实体设计

#### BaseEntity - 通用基础类
```java
// 文件位置: backend/src/main/java/com/miniups/model/entity/BaseEntity.java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // 主键，自增策略
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;    // 创建时间，自动设置
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;    // 更新时间，自动更新
    
    @Version
    private Long version;               // 乐观锁版本号
}
```

**设计要点：**
- `@MappedSuperclass`: 标记为映射超类，子类继承字段但不创建表
- `@EntityListeners`: 启用 JPA 审计功能
- `@CreatedDate/@LastModifiedDate`: 自动管理时间戳
- `@Version`: 实现乐观锁，防止并发修改冲突

### 2.2 核心实体设计

#### User 实体 - 用户管理
```java
// 文件位置: backend/src/main/java/com/miniups/model/entity/User.java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_username", columnList = "username")
})
public class User extends BaseEntity {
    
    // 基本信息字段
    @NotBlank @Size(min = 3, max = 50)
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @NotBlank @Email @Size(max = 100)
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @NotBlank @Size(min = 8, max = 255)
    @Column(name = "password", nullable = false)
    private String password;
    
    // 个人信息字段
    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;
    
    // 系统字段
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    // 关联关系
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Shipment> shipments = new ArrayList<>();
}
```

**关键特性：**
- **唯一性约束**: username 和 email 字段确保系统中的唯一性
- **数据验证**: 使用 Bean Validation 注解进行字段验证
- **索引优化**: 在常用查询字段上创建数据库索引
- **关联关系**: 与 Shipment 建立一对多关系
- **枚举类型**: 使用 UserRole 枚举确保角色类型安全

#### Shipment 实体 - 运输订单
```java
// 文件位置: backend/src/main/java/com/miniups/model/entity/Shipment.java
@Entity
@Table(name = "shipments", indexes = {
    @Index(name = "idx_shipment_shipment_id", columnList = "shipment_id"),
    @Index(name = "idx_shipment_tracking_id", columnList = "ups_tracking_id"),
    @Index(name = "idx_shipment_status", columnList = "status"),
    @Index(name = "idx_shipment_user", columnList = "user_id")
})
public class Shipment extends BaseEntity {
    
    // 订单标识
    @NotBlank @Size(max = 100)
    @Column(name = "shipment_id", nullable = false, unique = true, length = 100)
    private String shipmentId;          // Amazon 系统订单ID
    
    @Size(max = 100)
    @Column(name = "ups_tracking_id", unique = true, length = 100)
    private String upsTrackingId;       // UPS 生成的追踪号
    
    // 状态管理
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.CREATED;
    
    // 地理坐标
    @NotNull @Column(name = "origin_x", nullable = false)
    private Integer originX;            // 起点X坐标
    
    @NotNull @Column(name = "origin_y", nullable = false)
    private Integer originY;            // 起点Y坐标
    
    @NotNull @Column(name = "dest_x", nullable = false)
    private Integer destX;              // 终点X坐标
    
    @NotNull @Column(name = "dest_y", nullable = false)
    private Integer destY;              // 终点Y坐标
    
    // 业务字段
    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;
    
    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;
    
    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;                  // 关联用户
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id")
    private Truck truck;                // 关联卡车
    
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Package> packages = new ArrayList<>();
    
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShipmentStatusHistory> statusHistory = new ArrayList<>();
    
    // 业务方法
    public boolean canChangeAddress() {
        return status == ShipmentStatus.CREATED || 
               status == ShipmentStatus.PICKED_UP || 
               status == ShipmentStatus.IN_TRANSIT;
    }
    
    public void updateStatus(ShipmentStatus newStatus) {
        this.status = newStatus;
        // 自动添加状态历史记录
        ShipmentStatusHistory history = new ShipmentStatusHistory();
        history.setShipment(this);
        history.setStatus(newStatus);
        history.setTimestamp(LocalDateTime.now());
        this.statusHistory.add(history);
    }
}
```

**设计亮点：**
- **多重标识**: shipmentId (外部) 和 upsTrackingId (内部) 双重标识
- **状态机设计**: 使用枚举管理订单状态，确保状态转换的合法性
- **审计追踪**: 通过 statusHistory 记录所有状态变更
- **业务方法**: 在实体中封装业务逻辑，如地址变更权限检查
- **级联操作**: 配置适当的级联策略管理关联数据

## 3. 数据访问层 (Repository Layer)

### 3.1 Repository 接口设计

#### UserRepository - 用户数据访问
```java
// 文件位置: backend/src/main/java/com/miniups/repository/UserRepository.java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 基于方法名的查询 - Spring Data JPA 自动实现
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    // 存在性检查 - 性能优化，避免加载完整对象
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

#### ShipmentRepository - 订单数据访问
```java
// 文件位置: backend/src/main/java/com/miniups/repository/ShipmentRepository.java
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    
    // 单个结果查询
    Optional<Shipment> findByShipmentId(String shipmentId);
    Optional<Shipment> findByUpsTrackingId(String upsTrackingId);
    
    // 列表查询
    List<Shipment> findByUserId(Long userId);
    List<Shipment> findByStatus(ShipmentStatus status);
    List<Shipment> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 分页查询
    Page<Shipment> findByUserId(Long userId, Pageable pageable);
    
    // 自定义 JPQL 查询
    @Query("SELECT s FROM Shipment s WHERE s.user.id = :userId AND s.status = :status")
    List<Shipment> findByUserIdAndStatus(
        @Param("userId") Long userId, 
        @Param("status") ShipmentStatus status
    );
    
    // 统计查询
    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status = :status")
    long countByStatus(@Param("status") ShipmentStatus status);
    
    // 存在性检查
    boolean existsByUpsTrackingId(String upsTrackingId);
}
```

**Repository 设计要点：**
- **继承 JpaRepository**: 获得完整的 CRUD 操作支持
- **方法命名规范**: 遵循 Spring Data JPA 的命名约定
- **返回类型优化**: 使用 Optional 避免空指针异常
- **性能考虑**: exists 方法比 find 方法更高效
- **自定义查询**: 使用 @Query 注解处理复杂查询
- **分页支持**: 处理大数据量的查询结果

### 3.2 继承的标准 CRUD 方法

通过继承 `JpaRepository<T, ID>`，每个 Repository 自动获得以下方法：

#### CREATE 操作
```java
// 保存单个实体
<S extends T> S save(S entity);

// 批量保存
<S extends T> List<S> saveAll(Iterable<S> entities);

// 保存并立即刷新到数据库
<S extends T> S saveAndFlush(S entity);
```

#### READ 操作
```java
// 根据ID查找
Optional<T> findById(ID id);

// 查找所有
List<T> findAll();

// 分页查找
Page<T> findAll(Pageable pageable);

// 排序查找
List<T> findAll(Sort sort);

// 根据ID列表查找
List<T> findAllById(Iterable<ID> ids);

// 检查存在性
boolean existsById(ID id);

// 统计总数
long count();
```

#### UPDATE 操作
```java
// save() 方法同时支持创建和更新
// - 如果实体 ID 为 null，执行 INSERT
// - 如果实体 ID 不为 null，执行 UPDATE
<S extends T> S save(S entity);
```

#### DELETE 操作
```java
// 根据ID删除
void deleteById(ID id);

// 删除实体
void delete(T entity);

// 批量删除
void deleteAll(Iterable<? extends T> entities);

// 删除所有
void deleteAll();

// 根据ID批量删除
void deleteAllById(Iterable<? extends ID> ids);
```

## 4. 业务逻辑层 (Service Layer)

### 4.1 UserService - 用户管理服务

```java
// 文件位置: backend/src/main/java/com/miniups/service/UserService.java
@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // CREATE 操作 - 创建新用户
    public UserDto createUser(CreateUserDto createRequest) {
        logger.info("创建新用户: username={}, email={}, role={}", 
                   createRequest.getUsername(), createRequest.getEmail(), createRequest.getRole());
        
        // 1. 业务验证
        if (userRepository.existsByUsername(createRequest.getUsername())) {
            throw new UserAlreadyExistsException("用户名", createRequest.getUsername());
        }
        
        if (userRepository.existsByEmail(createRequest.getEmail())) {
            throw new UserAlreadyExistsException("邮箱", createRequest.getEmail());
        }
        
        try {
            // 2. 创建实体
            User user = createUserFromCreateRequest(createRequest);
            
            // 3. 持久化
            User savedUser = userRepository.save(user);
            
            // 4. 返回DTO
            UserDto userDto = UserDto.fromEntity(savedUser);
            
            logger.info("用户创建成功: id={}, username={}, role={}", 
                       savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
            return userDto;
            
        } catch (Exception e) {
            logger.error("创建用户时出现异常: username={}", createRequest.getUsername(), e);
            throw new RuntimeException("创建用户失败，请稍后重试");
        }
    }
    
    // READ 操作 - 查询用户信息
    @Transactional(readOnly = true)
    public UserDto getCurrentUserInfo(String username) {
        logger.debug("Getting user information: username={}", username);
        
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            logger.error("User not found: username={}", username);
            throw new UserNotFoundException(username);
        }
        
        User user = userOptional.get();
        UserDto userDto = UserDto.fromEntity(user);
        
        logger.debug("Successfully retrieved user information: username={}, id={}", 
                    username, user.getId());
        return userDto;
    }
    
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        logger.debug("Get user information by ID: userId={}", userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User not found: userId={}", userId);
            throw new UserNotFoundException("ID: " + userId);
        }
        
        User user = userOptional.get();
        UserDto userDto = UserDto.fromEntity(user);
        
        logger.debug("Successfully retrieved user information: userId={}, username={}", 
                    userId, user.getUsername());
        return userDto;
    }
    
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        logger.debug("获取所有用户列表");
        
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = users.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
        
        logger.debug("获取用户列表成功，共{}个用户", userDtos.size());
        return userDtos;
    }
    
    // UPDATE 操作 - 更新用户信息
    public UserDto updateUser(Long userId, UpdateUserDto updateRequest) {
        logger.info("Update user information: userId={}", userId);
        
        // 1. 查找现有用户
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User not found: userId={}", userId);
            throw new UserNotFoundException("ID: " + userId);
        }
        
        User user = userOptional.get();
        
        try {
            // 2. 业务验证
            if (updateRequest.getEmail() != null && 
                !updateRequest.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(updateRequest.getEmail())) {
                    logger.warn("Email already in use: email={}", updateRequest.getEmail());
                    throw new UserAlreadyExistsException("邮箱", updateRequest.getEmail());
                }
                user.setEmail(updateRequest.getEmail());
            }
            
            // 3. 更新字段
            if (updateRequest.getFirstName() != null) {
                user.setFirstName(updateRequest.getFirstName());
            }
            if (updateRequest.getLastName() != null) {
                user.setLastName(updateRequest.getLastName());
            }
            if (updateRequest.getPhone() != null) {
                user.setPhone(updateRequest.getPhone());
            }
            if (updateRequest.getAddress() != null) {
                user.setAddress(updateRequest.getAddress());
            }
            
            // 管理员专用字段
            if (updateRequest.getRole() != null) {
                user.setRole(updateRequest.getRole());
            }
            if (updateRequest.getEnabled() != null) {
                user.setEnabled(updateRequest.getEnabled());
            }
            
            // 4. 保存更新
            User updatedUser = userRepository.save(user);
            UserDto userDto = UserDto.fromEntity(updatedUser);
            
            logger.info("用户信息更新成功: userId={}, username={}", userId, user.getUsername());
            return userDto;
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Update user information时出现异常: userId={}", userId, e);
            throw new RuntimeException("Update user information失败，请稍后重试");
        }
    }
    
    // DELETE 操作 - 软删除用户
    public void deleteUser(Long userId) {
        logger.info("删除用户: userId={}", userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User not found: userId={}", userId);
            throw new UserNotFoundException("ID: " + userId);
        }
        
        User user = userOptional.get();
        
        // 软删除 - 禁用账户而不是物理删除
        user.setEnabled(false);
        userRepository.save(user);
        
        logger.info("用户已禁用: userId={}, username={}", userId, user.getUsername());
    }
    
    // 辅助方法
    private User createUserFromCreateRequest(CreateUserDto createRequest) {
        User user = new User();
        user.setUsername(createRequest.getUsername());
        user.setEmail(createRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createRequest.getPassword()));
        user.setFirstName(createRequest.getFirstName());
        user.setLastName(createRequest.getLastName());
        user.setPhone(createRequest.getPhone());
        user.setAddress(createRequest.getAddress());
        user.setRole(createRequest.getRole());
        user.setEnabled(createRequest.getEnabled());
        return user;
    }
}
```

**Service 层设计要点：**
- **事务管理**: 使用 `@Transactional` 确保数据一致性
- **异常处理**: 统一的异常处理和日志记录
- **DTO 转换**: 实体与 DTO 之间的转换，隐藏内部实现
- **业务验证**: 在持久化前进行业务规则验证
- **只读优化**: 查询操作使用 `@Transactional(readOnly = true)`
- **软删除**: 使用状态字段而非物理删除

## 5. 表现层 (Controller Layer)

### 5.1 UserController - 用户管理控制器

```java
// 文件位置: backend/src/main/java/com/miniups/controller/UserController.java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    // CREATE 操作 - 创建用户 (管理员功能)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDto createRequest) {
        try {
            logger.info("Admin creating new user: username={}, email={}, role={}", 
                       createRequest.getUsername(), createRequest.getEmail(), createRequest.getRole());
            
            UserDto createdUser = userService.createUser(createRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("user", createdUser);
            
            logger.info("User created successfully: id={}, username={}", 
                       createdUser.getId(), createdUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            logger.error("Failed to create user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Creation failed", e.getMessage()));
        } catch (Exception e) {
            logger.error("Exception occurred while creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("System error", "Failed to create user"));
        }
    }
    
    // READ 操作 - 获取当前用户信息
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUserProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.debug("Get user profile: username={}", username);
            
            UserDto userProfile = userService.getCurrentUserInfo(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Get user profile successful");
            response.put("user", userProfile);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Get user profile failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("System error", "Get user profile failed"));
        }
    }
    
    // READ 操作 - 获取指定用户信息
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            logger.debug("Get user information: userId={}", userId);
            
            UserDto user = userService.getUserById(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Get user information successful");
            response.put("user", user);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Get user information failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Query failed", e.getMessage()));
        } catch (Exception e) {
            logger.error("Get user information exception occurred", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("System error", "Get user information failed"));
        }
    }
    
    // READ 操作 - 获取用户列表 (管理员功能)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(@RequestParam(required = false) UserRole role) {
        try {
            logger.debug("Get user list: role={}", role);
            
            List<UserDto> users;
            if (role != null) {
                users = userService.getUsersByRole(role);
            } else {
                users = userService.getAllUsers();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Get user list successful");
            response.put("users", users);
            response.put("total", users.size());
            
            logger.debug("Get user list successful, total {} users", users.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Get user list exception occurred", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("System error", "Get user list failed"));
        }
    }
    
    // UPDATE 操作 - 更新当前用户信息
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateCurrentUserProfile(@Valid @RequestBody UpdateUserDto updateRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("Update user profile: username={}", username);
            
            // 获取当前用户信息
            UserDto currentUser = userService.getCurrentUserInfo(username);
            
            // 确保普通用户无法修改管理员字段
            UpdateUserDto userSafeRequest = updateRequest.forUserSelfUpdate();
            
            // 更新用户信息
            UserDto updatedUser = userService.updateUser(currentUser.getId(), userSafeRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User profile update successful");
            response.put("user", updatedUser);
            
            logger.info("User profile update successful: username={}", username);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Update user profile failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Update failed", e.getMessage()));
        } catch (Exception e) {
            logger.error("Update user profile exception occurred", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("System error", "Update user profile failed"));
        }
    }
    
    // UPDATE 操作 - 更新用户信息 (管理员功能)
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long userId,
                                        @Valid @RequestBody UpdateUserDto updateRequest) {
        try {
            logger.info("Admin updating user information: userId={}", userId);

            UserDto updatedUser = userService.updateUser(userId, updateRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User information updated successfully");
            response.put("user", updatedUser);

            logger.info("User information updated successfully: userId={}, username={}", 
                       userId, updatedUser.getUsername());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("Failed to update user information: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Update failed", e.getMessage()));
        } catch (Exception e) {
            logger.error("Exception occurred while updating user information", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("System error", "Failed to update user information"));
        }
    }
    
    // DELETE 操作 - 禁用用户 (管理员功能)
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            logger.info("Admin disabling user: userId={}", userId);

            userService.deleteUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User has been disabled");

            logger.info("User disabled successfully: userId={}", userId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("Failed to disable user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Disable failed", e.getMessage()));
        } catch (Exception e) {
            logger.error("Exception occurred while disabling user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("System error", "Failed to disable user"));
        }
    }
    
    // 辅助方法 - 创建统一错误响应
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
```

**Controller 层设计要点：**
- **RESTful 设计**: 遵循 REST 架构风格的 URL 设计
- **HTTP 状态码**: 合理使用 HTTP 状态码表示请求结果
- **权限控制**: 使用 `@PreAuthorize` 进行方法级权限控制
- **参数验证**: 使用 `@Valid` 进行请求参数验证
- **统一响应**: 标准化的响应格式，便于前端处理
- **异常处理**: 分层的异常处理，提供友好的错误信息
- **日志记录**: 详细的操作日志，便于问题排查

---

## API 端点总结

### 用户管理 API 端点

| HTTP 方法 | 端点路径 | 功能描述 | 权限要求 | CRUD 操作 |
|----------|----------|----------|----------|-----------|
| POST | `/api/users` | 创建新用户 | ADMIN | CREATE |
| GET | `/api/users/profile` | 获取当前用户信息 | 认证用户 | READ |
| PUT | `/api/users/profile` | 更新当前用户信息 | 认证用户 | UPDATE |
| GET | `/api/users/{id}` | 获取指定用户信息 | ADMIN 或本人 | READ |
| GET | `/api/users` | 获取用户列表 | ADMIN | READ |
| PUT | `/api/users/{id}` | 更新用户信息 | ADMIN | UPDATE |
| DELETE | `/api/users/{id}` | 禁用用户 | ADMIN | DELETE |
| POST | `/api/users/{id}/enable` | 启用用户 | ADMIN | UPDATE |

**第一部分总结:**

本部分详细介绍了 Mini-UPS 系统后端 CRUD 操作的完整实现，从数据模型设计到 API 端点，展示了分层架构的优势和最佳实践。每一层都有明确的职责，通过依赖注入实现松耦合，确保系统的可维护性和可扩展性。

---

# 第二部分：前端集成与完整流程

## 6. 前端数据访问模式

### 6.1 TypeScript 类型定义

#### 核心数据类型
```typescript
// 文件位置: frontend/src/types/index.ts

// 用户类型定义
export interface User {
  id: number
  username: string
  email: string
  firstName?: string
  lastName?: string
  phone?: string
  address?: string
  role: UserRole
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
  DRIVER = 'DRIVER',
  OPERATOR = 'OPERATOR'
}

// 认证类型
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  firstName?: string
  lastName?: string
  phone?: string
  address?: string
}

export interface AuthResponse {
  token: string
  type: string
  user: User
}

// 运输订单类型
export interface Shipment {
  id: number
  shipmentId: string
  upsTrackingId?: string
  amazonOrderId?: string
  status: ShipmentStatus
  originX: number
  originY: number
  destX: number
  destY: number
  weight?: number
  estimatedDelivery?: string
  actualDelivery?: string
  pickupTime?: string
  worldId?: number
  user?: User
  truck?: Truck
  packages: Package[]
  statusHistory: ShipmentStatusHistory[]
  addressChanges: AddressChange[]
  createdAt: string
  updatedAt: string
}

export enum ShipmentStatus {
  CREATED = 'CREATED',
  TRUCK_DISPATCHED = 'TRUCK_DISPATCHED',
  PICKED_UP = 'PICKED_UP',
  IN_TRANSIT = 'IN_TRANSIT',
  OUT_FOR_DELIVERY = 'OUT_FOR_DELIVERY',
  DELIVERED = 'DELIVERED',
  EXCEPTION = 'EXCEPTION',
  RETURNED = 'RETURNED'
}

// API 响应类型
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  errors?: string[]
}
```

### 6.2 API 服务层设计

#### AuthService - 认证服务
```typescript
// 文件位置: frontend/src/services/authService.ts

class AuthService {
  private baseURL = '/api/auth'
  
  // CREATE 操作 - 用户注册
  async register(registerData: RegisterRequest): Promise<AuthResponse> {
    try {
      const response = await fetch(`${this.baseURL}/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(registerData),
      })
      
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '注册失败')
      }
      
      // 存储 JWT token
      if (result.token) {
        localStorage.setItem('token', result.token)
        localStorage.setItem('user', JSON.stringify(result.user))
      }
      
      return result
    } catch (error) {
      console.error('注册错误:', error)
      throw error
    }
  }
  
  // READ 操作 - 用户登录
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await fetch(`${this.baseURL}/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          usernameOrEmail: credentials.username,
          password: credentials.password,
        }),
      })
      
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '登录失败')
      }
      
      // 存储认证信息
      if (result.token) {
        localStorage.setItem('token', result.token)
        localStorage.setItem('user', JSON.stringify(result.user))
      }
      
      return result
    } catch (error) {
      console.error('登录错误:', error)
      throw error
    }
  }
  
  // READ 操作 - 获取当前用户信息
  async getCurrentUser(): Promise<User> {
    try {
      const response = await this.authenticatedFetch(`${this.baseURL}/me`)
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '获取用户信息失败')
      }
      
      return result.user
    } catch (error) {
      console.error('获取用户信息错误:', error)
      throw error
    }
  }
  
  // UPDATE 操作 - 修改密码
  async changePassword(passwordData: {
    currentPassword: string
    newPassword: string
    confirmPassword: string
  }): Promise<void> {
    try {
      const response = await this.authenticatedFetch(`${this.baseURL}/change-password`, {
        method: 'POST',
        body: JSON.stringify(passwordData),
      })
      
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '修改密码失败')
      }
    } catch (error) {
      console.error('修改密码错误:', error)
      throw error
    }
  }
  
  // 辅助方法 - 带认证的请求
  private async authenticatedFetch(url: string, options: RequestInit = {}): Promise<Response> {
    const token = this.getToken()
    
    return fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers,
      },
    })
  }
  
  // 令牌管理
  getToken(): string | null {
    return localStorage.getItem('token')
  }
  
  isAuthenticated(): boolean {
    const token = this.getToken()
    if (!token) return false
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      return payload.exp * 1000 > Date.now()
    } catch {
      return false
    }
  }
  
  logout(): void {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'
  }
}

export const authService = new AuthService()
```

#### UserService - 用户管理服务
```typescript
// 文件位置: frontend/src/services/userService.ts

class UserService {
  private baseURL = '/api/users'
  
  // READ 操作 - 获取用户资料
  async getUserProfile(): Promise<User> {
    try {
      const response = await this.authenticatedFetch(`${this.baseURL}/profile`)
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '获取用户资料失败')
      }
      
      return result.user
    } catch (error) {
      console.error('获取用户资料错误:', error)
      throw error
    }
  }
  
  // UPDATE 操作 - 更新用户资料
  async updateUserProfile(userData: {
    firstName?: string
    lastName?: string
    phone?: string
    address?: string
    email?: string
  }): Promise<User> {
    try {
      const response = await this.authenticatedFetch(`${this.baseURL}/profile`, {
        method: 'PUT',
        body: JSON.stringify(userData),
      })
      
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '更新用户资料失败')
      }
      
      // 更新本地存储的用户信息
      localStorage.setItem('user', JSON.stringify(result.user))
      
      return result.user
    } catch (error) {
      console.error('更新用户资料错误:', error)
      throw error
    }
  }
  
  // READ 操作 - 获取用户列表 (管理员)
  async getAllUsers(role?: UserRole): Promise<User[]> {
    try {
      const url = role ? `${this.baseURL}?role=${role}` : this.baseURL
      const response = await this.authenticatedFetch(url)
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '获取用户列表失败')
      }
      
      return result.users
    } catch (error) {
      console.error('获取用户列表错误:', error)
      throw error
    }
  }
  
  // CREATE 操作 - 创建用户 (管理员)
  async createUser(userData: {
    username: string
    email: string
    password: string
    firstName?: string
    lastName?: string
    phone?: string
    address?: string
    role: UserRole
    enabled: boolean
  }): Promise<User> {
    try {
      const response = await this.authenticatedFetch(this.baseURL, {
        method: 'POST',
        body: JSON.stringify(userData),
      })
      
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '创建用户失败')
      }
      
      return result.user
    } catch (error) {
      console.error('创建用户错误:', error)
      throw error
    }
  }
  
  // UPDATE 操作 - 更新用户 (管理员)
  async updateUser(userId: number, userData: Partial<User>): Promise<User> {
    try {
      const response = await this.authenticatedFetch(`${this.baseURL}/${userId}`, {
        method: 'PUT',
        body: JSON.stringify(userData),
      })
      
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '更新用户失败')
      }
      
      return result.user
    } catch (error) {
      console.error('更新用户错误:', error)
      throw error
    }
  }
  
  // DELETE 操作 - 禁用用户 (管理员)
  async deleteUser(userId: number): Promise<void> {
    try {
      const response = await this.authenticatedFetch(`${this.baseURL}/${userId}`, {
        method: 'DELETE',
      })
      
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '禁用用户失败')
      }
    } catch (error) {
      console.error('禁用用户错误:', error)
      throw error
    }
  }
  
  // UPDATE 操作 - 启用用户 (管理员)
  async enableUser(userId: number): Promise<void> {
    try {
      const response = await this.authenticatedFetch(`${this.baseURL}/${userId}/enable`, {
        method: 'POST',
      })
      
      const result = await response.json()
      
      if (!response.ok) {
        throw new Error(result.message || '启用用户失败')
      }
    } catch (error) {
      console.error('启用用户错误:', error)
      throw error
    }
  }
  
  // 辅助方法
  private async authenticatedFetch(url: string, options: RequestInit = {}): Promise<Response> {
    const token = localStorage.getItem('token')
    
    return fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers,
      },
    })
  }
}

export const userService = new UserService()
```

### 6.3 React Hooks 数据管理

#### useAuth Hook - 认证状态管理
```typescript
// 文件位置: frontend/src/hooks/useAuth.ts

import { useState, useEffect, useContext, createContext } from 'react'
import { authService } from '../services/authService'
import { User } from '../types'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (username: string, password: string) => Promise<void>
  register: (userData: RegisterRequest) => Promise<void>
  logout: () => void
  updateUser: (userData: Partial<User>) => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  
  useEffect(() => {
    // 初始化时检查认证状态
    const initAuth = async () => {
      try {
        if (authService.isAuthenticated()) {
          const userData = await authService.getCurrentUser()
          setUser(userData)
        }
      } catch (error) {
        console.error('初始化认证失败:', error)
        authService.logout()
      } finally {
        setIsLoading(false)
      }
    }
    
    initAuth()
  }, [])
  
  const login = async (username: string, password: string) => {
    try {
      setIsLoading(true)
      const response = await authService.login({ username, password })
      setUser(response.user)
    } catch (error) {
      throw error
    } finally {
      setIsLoading(false)
    }
  }
  
  const register = async (userData: RegisterRequest) => {
    try {
      setIsLoading(true)
      const response = await authService.register(userData)
      setUser(response.user)
    } catch (error) {
      throw error
    } finally {
      setIsLoading(false)
    }
  }
  
  const logout = () => {
    authService.logout()
    setUser(null)
  }
  
  const updateUser = (userData: Partial<User>) => {
    setUser(prev => prev ? { ...prev, ...userData } : null)
  }
  
  const value = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    register,
    logout,
    updateUser,
  }
  
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
```

#### useUsers Hook - 用户管理
```typescript
// 文件位置: frontend/src/hooks/useUsers.ts

import { useState, useEffect } from 'react'
import { userService } from '../services/userService'
import { User, UserRole } from '../types'

export const useUsers = (role?: UserRole) => {
  const [users, setUsers] = useState<User[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  // READ 操作 - 获取用户列表
  const fetchUsers = async () => {
    try {
      setIsLoading(true)
      setError(null)
      const userData = await userService.getAllUsers(role)
      setUsers(userData)
    } catch (err) {
      setError(err instanceof Error ? err.message : '获取用户列表失败')
    } finally {
      setIsLoading(false)
    }
  }
  
  // CREATE 操作 - 创建用户
  const createUser = async (userData: {
    username: string
    email: string
    password: string
    firstName?: string
    lastName?: string
    phone?: string
    address?: string
    role: UserRole
    enabled: boolean
  }) => {
    try {
      setError(null)
      const newUser = await userService.createUser(userData)
      setUsers(prev => [...prev, newUser])
      return newUser
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建用户失败')
      throw err
    }
  }
  
  // UPDATE 操作 - 更新用户
  const updateUser = async (userId: number, userData: Partial<User>) => {
    try {
      setError(null)
      const updatedUser = await userService.updateUser(userId, userData)
      setUsers(prev => prev.map(user => 
        user.id === userId ? updatedUser : user
      ))
      return updatedUser
    } catch (err) {
      setError(err instanceof Error ? err.message : '更新用户失败')
      throw err
    }
  }
  
  // DELETE 操作 - 禁用用户
  const deleteUser = async (userId: number) => {
    try {
      setError(null)
      await userService.deleteUser(userId)
      setUsers(prev => prev.map(user => 
        user.id === userId ? { ...user, enabled: false } : user
      ))
    } catch (err) {
      setError(err instanceof Error ? err.message : '禁用用户失败')
      throw err
    }
  }
  
  // UPDATE 操作 - 启用用户
  const enableUser = async (userId: number) => {
    try {
      setError(null)
      await userService.enableUser(userId)
      setUsers(prev => prev.map(user => 
        user.id === userId ? { ...user, enabled: true } : user
      ))
    } catch (err) {
      setError(err instanceof Error ? err.message : '启用用户失败')
      throw err
    }
  }
  
  useEffect(() => {
    fetchUsers()
  }, [role])
  
  return {
    users,
    isLoading,
    error,
    fetchUsers,
    createUser,
    updateUser,
    deleteUser,
    enableUser,
  }
}
```

## 7. 完整CRUD流程示例

### 7.1 用户注册完整流程

#### 步骤 1: 前端表单提交
```typescript
// 文件位置: frontend/src/pages/RegisterPage.tsx

const RegisterPage: React.FC = () => {
  const [formData, setFormData] = useState<RegisterRequest>({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    address: '',
  })
  const [isLoading, setIsLoading] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  
  const { register } = useAuth()
  const navigate = useNavigate()
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      setIsLoading(true)
      setErrors({})
      
      // 前端验证
      const validationErrors = validateRegistrationForm(formData)
      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors)
        return
      }
      
      // 调用注册 API
      await register(formData)
      
      // 注册成功，跳转到仪表板
      navigate('/dashboard')
      
    } catch (error) {
      console.error('注册失败:', error)
      setErrors({ submit: error instanceof Error ? error.message : '注册失败' })
    } finally {
      setIsLoading(false)
    }
  }
  
  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label htmlFor="username">用户名</label>
        <input
          id="username"
          type="text"
          value={formData.username}
          onChange={(e) => setFormData(prev => ({ ...prev, username: e.target.value }))}
          className="w-full border rounded px-3 py-2"
          required
        />
        {errors.username && <p className="text-red-500 text-sm">{errors.username}</p>}
      </div>
      
      <div>
        <label htmlFor="email">邮箱</label>
        <input
          id="email"
          type="email"
          value={formData.email}
          onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
          className="w-full border rounded px-3 py-2"
          required
        />
        {errors.email && <p className="text-red-500 text-sm">{errors.email}</p>}
      </div>
      
      <div>
        <label htmlFor="password">密码</label>
        <input
          id="password"
          type="password"
          value={formData.password}
          onChange={(e) => setFormData(prev => ({ ...prev, password: e.target.value }))}
          className="w-full border rounded px-3 py-2"
          required
        />
        {errors.password && <p className="text-red-500 text-sm">{errors.password}</p>}
      </div>
      
      {/* 其他字段 */}
      
      {errors.submit && (
        <div className="text-red-500 text-sm">{errors.submit}</div>
      )}
      
      <button
        type="submit"
        disabled={isLoading}
        className="w-full bg-blue-500 text-white py-2 px-4 rounded hover:bg-blue-600 disabled:opacity-50"
      >
        {isLoading ? '注册中...' : '注册'}
      </button>
    </form>
  )
}
```

#### 步骤 2: 前端服务层处理
```typescript
// authService.register() 方法执行流程:

1. 前端验证数据格式
2. 发送 POST 请求到 /api/auth/register
3. 设置 Content-Type: application/json
4. 发送 JSON 格式的用户数据
```

#### 步骤 3: 后端控制器接收
```java
// AuthController.registerUser() 方法:

@PostMapping("/register")
public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDto registerRequest) {
    // 1. Spring Boot 自动进行 JSON 反序列化
    // 2. @Valid 注解触发 Bean Validation 验证
    // 3. 调用 AuthService.register() 处理业务逻辑
    // 4. 返回 JSON 响应
}
```

#### 步骤 4: 业务层处理
```java
// AuthService.register() 方法:

public AuthResponseDto register(RegisterRequestDto request) {
    // 1. 业务验证 (用户名邮箱唯一性检查)
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new UserAlreadyExistsException("用户名", request.getUsername());
    }
    
    // 2. 创建用户实体
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    // ... 设置其他字段
    
    // 3. 保存到数据库
    User savedUser = userRepository.save(user);
    
    // 4. 生成 JWT 令牌
    String token = jwtTokenProvider.createToken(savedUser.getUsername(), savedUser.getRole());
    
    // 5. 返回响应
    return new AuthResponseDto(token, "Bearer", UserDto.fromEntity(savedUser));
}
```

#### 步骤 5: 数据访问层执行
```java
// UserRepository.save() 方法:

// Spring Data JPA 自动执行:
// 1. 检查实体 ID 是否为 null
// 2. 执行 INSERT SQL 语句
// 3. 设置生成的主键到实体对象
// 4. 更新审计字段 (createdAt, updatedAt)
// 5. 返回持久化后的实体

// 实际 SQL 执行:
INSERT INTO users (username, email, password, first_name, last_name, phone, address, role, enabled, created_at, updated_at, version)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
```

#### 步骤 6: 响应返回流程
```
数据库 → Repository → Service → Controller → HTTP Response → 前端 → React State 更新
```

### 7.2 用户信息更新完整流程

#### 步骤 1: 前端组件
```typescript
// 用户资料编辑组件
const UserProfileEdit: React.FC = () => {
  const { user, updateUser } = useAuth()
  const [formData, setFormData] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    phone: user?.phone || '',
    address: user?.address || '',
    email: user?.email || '',
  })
  const [isLoading, setIsLoading] = useState(false)
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      setIsLoading(true)
      
      // 调用用户服务更新资料
      const updatedUser = await userService.updateUserProfile(formData)
      
      // 更新本地认证状态
      updateUser(updatedUser)
      
      // 显示成功消息
      toast.success('用户资料更新成功!')
      
    } catch (error) {
      console.error('更新失败:', error)
      toast.error(error instanceof Error ? error.message : '更新失败')
    } finally {
      setIsLoading(false)
    }
  }
  
  return (
    <form onSubmit={handleSubmit}>
      {/* 表单字段 */}
      <button type="submit" disabled={isLoading}>
        {isLoading ? '更新中...' : '保存更改'}
      </button>
    </form>
  )
}
```

#### 步骤 2: API 调用链路
```
前端表单提交 
→ userService.updateUserProfile()
→ PUT /api/users/profile 
→ UserController.updateCurrentUserProfile()
→ UserService.updateUser()
→ UserRepository.save()
→ 数据库 UPDATE 操作
```

#### 步骤 3: 后端更新逻辑
```java
// UserService.updateUser() 详细流程:

public UserDto updateUser(Long userId, UpdateUserDto updateRequest) {
    // 1. 查找现有用户 (READ before UPDATE)
    Optional<User> userOptional = userRepository.findById(userId);
    if (userOptional.isEmpty()) {
        throw new UserNotFoundException("ID: " + userId);
    }
    User user = userOptional.get();
    
    // 2. 业务验证 (邮箱唯一性检查)
    if (updateRequest.getEmail() != null && 
        !updateRequest.getEmail().equals(user.getEmail())) {
        if (userRepository.existsByEmail(updateRequest.getEmail())) {
            throw new UserAlreadyExistsException("邮箱", updateRequest.getEmail());
        }
    }
    
    // 3. 选择性更新字段 (只更新非 null 字段)
    if (updateRequest.getFirstName() != null) {
        user.setFirstName(updateRequest.getFirstName());
    }
    // ... 其他字段更新
    
    // 4. 保存更新 (JPA 自动检测变更并执行 UPDATE)
    User updatedUser = userRepository.save(user);
    
    // 5. 返回 DTO
    return UserDto.fromEntity(updatedUser);
}
```

### 7.3 用户列表查询与管理

#### 管理员用户管理界面
```typescript
// 用户管理页面组件
const UserManagementPage: React.FC = () => {
  const { users, isLoading, error, createUser, updateUser, deleteUser, enableUser } = useUsers()
  const [selectedRole, setSelectedRole] = useState<UserRole | undefined>()
  const [showCreateModal, setShowCreateModal] = useState(false)
  
  const handleCreateUser = async (userData: CreateUserData) => {
    try {
      await createUser(userData)
      setShowCreateModal(false)
      toast.success('用户创建成功!')
    } catch (error) {
      toast.error('创建用户失败')
    }
  }
  
  const handleToggleUserStatus = async (user: User) => {
    try {
      if (user.enabled) {
        await deleteUser(user.id)
        toast.success('用户已禁用')
      } else {
        await enableUser(user.id)
        toast.success('用户已启用')
      }
    } catch (error) {
      toast.error('操作失败')
    }
  }
  
  if (isLoading) return <div>加载中...</div>
  if (error) return <div>错误: {error}</div>
  
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">用户管理</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
        >
          创建用户
        </button>
      </div>
      
      {/* 角色筛选 */}
      <div className="mb-4">
        <select
          value={selectedRole || ''}
          onChange={(e) => setSelectedRole(e.target.value as UserRole || undefined)}
          className="border rounded px-3 py-2"
        >
          <option value="">所有角色</option>
          <option value={UserRole.USER}>普通用户</option>
          <option value={UserRole.ADMIN}>管理员</option>
          <option value={UserRole.DRIVER}>司机</option>
          <option value={UserRole.OPERATOR}>操作员</option>
        </select>
      </div>
      
      {/* 用户列表表格 */}
      <div className="overflow-x-auto">
        <table className="min-w-full bg-white border border-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                用户信息
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                角色
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                状态
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                创建时间
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                操作
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {users.map((user) => (
              <tr key={user.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="flex-shrink-0 h-10 w-10">
                      <div className="h-10 w-10 rounded-full bg-gray-300 flex items-center justify-center">
                        {user.firstName?.[0] || user.username[0]}
                      </div>
                    </div>
                    <div className="ml-4">
                      <div className="text-sm font-medium text-gray-900">
                        {user.firstName && user.lastName 
                          ? `${user.firstName} ${user.lastName}` 
                          : user.username
                        }
                      </div>
                      <div className="text-sm text-gray-500">{user.email}</div>
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                    user.role === UserRole.ADMIN ? 'bg-red-100 text-red-800' :
                    user.role === UserRole.DRIVER ? 'bg-blue-100 text-blue-800' :
                    user.role === UserRole.OPERATOR ? 'bg-yellow-100 text-yellow-800' :
                    'bg-green-100 text-green-800'
                  }`}>
                    {user.role}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                    user.enabled ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  }`}>
                    {user.enabled ? '启用' : '禁用'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {new Date(user.createdAt).toLocaleDateString('zh-CN')}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <button
                    onClick={() => handleToggleUserStatus(user)}
                    className={`${
                      user.enabled 
                        ? 'text-red-600 hover:text-red-900' 
                        : 'text-green-600 hover:text-green-900'
                    } mr-4`}
                  >
                    {user.enabled ? '禁用' : '启用'}
                  </button>
                  <button
                    onClick={() => {/* 打开编辑模态框 */}}
                    className="text-indigo-600 hover:text-indigo-900"
                  >
                    编辑
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      
      {/* 创建用户模态框 */}
      {showCreateModal && (
        <CreateUserModal
          onClose={() => setShowCreateModal(false)}
          onSubmit={handleCreateUser}
        />
      )}
    </div>
  )
}
```

## 8. 最佳实践与性能优化

### 8.1 数据库层优化

#### 索引策略
```sql
-- 用户表索引优化
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_role ON users(role);
CREATE INDEX idx_user_enabled ON users(enabled);

-- 运输订单表索引优化
CREATE INDEX idx_shipment_shipment_id ON shipments(shipment_id);
CREATE INDEX idx_shipment_tracking_id ON shipments(ups_tracking_id);
CREATE INDEX idx_shipment_status ON shipments(status);
CREATE INDEX idx_shipment_user_status ON shipments(user_id, status);
CREATE INDEX idx_shipment_created_at ON shipments(created_at);

-- 复合索引优化常用查询
CREATE INDEX idx_shipment_user_created ON shipments(user_id, created_at DESC);
```

#### 查询优化
```java
// Repository 层查询优化示例

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    
    // 使用分页避免大量数据加载
    @Query("SELECT s FROM Shipment s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    Page<Shipment> findByUserIdOrderByCreatedAtDesc(
        @Param("userId") Long userId, 
        Pageable pageable
    );
    
    // 使用投影减少数据传输
    @Query("SELECT NEW com.miniups.model.dto.ShipmentSummaryDto(" +
           "s.id, s.shipmentId, s.upsTrackingId, s.status, s.createdAt) " +
           "FROM Shipment s WHERE s.status = :status")
    List<ShipmentSummaryDto> findShipmentSummariesByStatus(@Param("status") ShipmentStatus status);
    
    // 批量操作优化
    @Modifying
    @Query("UPDATE Shipment s SET s.status = :status WHERE s.id IN :ids")
    int updateStatusForShipments(@Param("status") ShipmentStatus status, @Param("ids") List<Long> ids);
}
```

### 8.2 缓存策略

#### Redis 缓存配置
```java
// 缓存配置类
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))  // 设置缓存过期时间
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .build();
    }
}

// Service 层缓存应用
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#username")
    public UserDto getCurrentUserInfo(String username) {
        // 方法实现
    }
    
    @CacheEvict(value = "users", key = "#username")
    public UserDto updateUser(String username, UpdateUserDto updateRequest) {
        // 方法实现
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void clearUserCache() {
        // 清除所有用户缓存
    }
}
```

### 8.3 前端性能优化

#### React 组件优化
```typescript
// 使用 React.memo 避免不必要的重渲染
const UserCard = React.memo<{ user: User; onUpdate: (user: User) => void }>(
  ({ user, onUpdate }) => {
    const handleUpdate = useCallback(() => {
      onUpdate(user)
    }, [user, onUpdate])
    
    return (
      <div className="user-card">
        <h3>{user.username}</h3>
        <p>{user.email}</p>
        <button onClick={handleUpdate}>更新</button>
      </div>
    )
  }
)

// 使用 useMemo 缓存计算结果
const UserList: React.FC<{ users: User[] }> = ({ users }) => {
  const sortedUsers = useMemo(() => {
    return users.sort((a, b) => 
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  }, [users])
  
  const usersByRole = useMemo(() => {
    return sortedUsers.reduce((acc, user) => {
      acc[user.role] = acc[user.role] || []
      acc[user.role].push(user)
      return acc
    }, {} as Record<UserRole, User[]>)
  }, [sortedUsers])
  
  return (
    <div>
      {Object.entries(usersByRole).map(([role, users]) => (
        <div key={role}>
          <h2>{role}</h2>
          {users.map(user => (
            <UserCard key={user.id} user={user} onUpdate={handleUserUpdate} />
          ))}
        </div>
      ))}
    </div>
  )
}
```

#### 数据获取优化
```typescript
// 使用 React Query 进行数据管理和缓存
import { useQuery, useMutation, useQueryClient } from 'react-query'

const useUsersQuery = (role?: UserRole) => {
  return useQuery(
    ['users', role],
    () => userService.getAllUsers(role),
    {
      staleTime: 5 * 60 * 1000,  // 5分钟内数据视为新鲜
      cacheTime: 10 * 60 * 1000, // 10分钟缓存时间
      refetchOnWindowFocus: false,
    }
  )
}

const useUpdateUserMutation = () => {
  const queryClient = useQueryClient()
  
  return useMutation(
    ({ userId, userData }: { userId: number; userData: Partial<User> }) =>
      userService.updateUser(userId, userData),
    {
      onSuccess: (updatedUser) => {
        // 更新相关查询的缓存
        queryClient.setQueryData(['user', updatedUser.id], updatedUser)
        queryClient.invalidateQueries(['users'])
      },
    }
  )
}
```

### 8.4 安全最佳实践

#### JWT 令牌管理
```typescript
// 安全的令牌存储和管理
class TokenManager {
  private static readonly TOKEN_KEY = 'access_token'
  private static readonly REFRESH_TOKEN_KEY = 'refresh_token'
  
  static setTokens(accessToken: string, refreshToken?: string) {
    // 使用 HttpOnly Cookie 存储刷新令牌（更安全）
    if (refreshToken) {
      document.cookie = `${this.REFRESH_TOKEN_KEY}=${refreshToken}; HttpOnly; Secure; SameSite=Strict`
    }
    
    // 访问令牌存储在内存中（会话结束时自动清除）
    sessionStorage.setItem(this.TOKEN_KEY, accessToken)
  }
  
  static getAccessToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY)
  }
  
  static isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      return payload.exp * 1000 < Date.now()
    } catch {
      return true
    }
  }
  
  static clearTokens() {
    sessionStorage.removeItem(this.TOKEN_KEY)
    document.cookie = `${this.REFRESH_TOKEN_KEY}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`
  }
}
```

#### 输入验证和清理
```typescript
// 前端输入验证
const validateUserInput = (userData: Partial<User>): Record<string, string> => {
  const errors: Record<string, string> = {}
  
  if (userData.username) {
    if (userData.username.length < 3) {
      errors.username = '用户名至少需要3个字符'
    }
    if (!/^[a-zA-Z0-9_]+$/.test(userData.username)) {
      errors.username = '用户名只能包含字母、数字和下划线'
    }
  }
  
  if (userData.email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(userData.email)) {
      errors.email = '请输入有效的邮箱地址'
    }
  }
  
  if (userData.phone) {
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(userData.phone)) {
      errors.phone = '请输入有效的手机号码'
    }
  }
  
  return errors
}

// XSS 防护 - 输入清理
const sanitizeInput = (input: string): string => {
  return input
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/\//g, '&#x2F;')
}
```

### 8.5 错误处理和监控

#### 统一错误处理
```typescript
// 全局错误处理器
class ErrorHandler {
  static handle(error: unknown): void {
    console.error('应用错误:', error)
    
    if (error instanceof Error) {
      // 根据错误类型进行不同处理
      if (error.message.includes('401') || error.message.includes('Unauthorized')) {
        // 认证错误 - 重定向到登录页
        authService.logout()
        return
      }
      
      if (error.message.includes('403') || error.message.includes('Forbidden')) {
        // 权限错误
        toast.error('您没有权限执行此操作')
        return
      }
      
      if (error.message.includes('500')) {
        // 服务器错误
        toast.error('服务器错误，请稍后重试')
        return
      }
    }
    
    // 默认错误处理
    toast.error('操作失败，请稍后重试')
  }
}

// React 错误边界
class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props)
    this.state = { hasError: false }
  }
  
  static getDerivedStateFromError(error: Error): { hasError: boolean } {
    return { hasError: true }
  }
  
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('React错误边界捕获错误:', error, errorInfo)
    // 发送错误报告到监控服务
  }
  
  render() {
    if (this.state.hasError) {
      return (
        <div className="error-fallback">
          <h2>出现了错误</h2>
          <button onClick={() => this.setState({ hasError: false })}>
            重试
          </button>
        </div>
      )
    }
    
    return this.props.children
  }
}
```

## 总结

Mini-UPS 系统的 CRUD 操作实现展示了现代 Web 应用的完整技术栈：

### 后端特色
- **分层架构**: 清晰的职责分离，便于维护和测试
- **Spring Boot 生态**: 充分利用 Spring 框架的特性
- **JPA/Hibernate**: 对象关系映射，简化数据库操作
- **安全机制**: JWT 认证，角色权限控制
- **异常处理**: 统一的错误处理和日志记录

### 前端特色
- **React + TypeScript**: 类型安全的组件开发
- **Hook 模式**: 现代 React 开发模式
- **服务层抽象**: 统一的 API 调用管理
- **状态管理**: Context API 和 React Query
- **用户体验**: 加载状态，错误处理，友好提示

### 性能优化
- **数据库索引**: 优化查询性能
- **缓存策略**: Redis 缓存热点数据
- **前端优化**: 组件缓存，懒加载，代码分割
- **网络优化**: 请求去重，批量操作

### 安全考虑
- **输入验证**: 前后端双重验证
- **权限控制**: 细粒度的访问控制
- **令牌管理**: 安全的 JWT 处理
- **XSS 防护**: 输入清理和输出编码

这个 CRUD 实现方案体现了企业级应用的开发标准，可以作为类似项目的参考模板。