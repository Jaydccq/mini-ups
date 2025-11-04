# MyBatis 完全新手指南

## 目录
1. [什么是MyBatis](#1-什么是mybatis)
2. [MyBatis vs JPA/Hibernate](#2-mybatis-vs-jpahibernate)
3. [核心概念](#3-核心概念)
4. [环境搭建](#4-环境搭建)
5. [基础使用](#5-基础使用)
6. [XML映射详解](#6-xml映射详解)
7. [注解方式](#7-注解方式)
8. [动态SQL](#8-动态sql)
9. [结果映射](#9-结果映射)
10. [关联查询](#10-关联查询)
11. [高级特性](#11-高级特性)
12. [最佳实践](#12-最佳实践)
13. [常见问题](#13-常见问题)

---

## 1. 什么是MyBatis

### 1.1 简介
MyBatis是一个优秀的**半自动化持久层框架**，它支持：
- 自定义SQL
- 存储过程
- 高级映射

MyBatis **免除了几乎所有的JDBC代码**以及设置参数和获取结果集的工作。

### 1.2 为什么选择MyBatis？

**优点：**
- ✅ **SQL控制灵活**：你可以完全控制SQL，适合复杂查询和性能优化
- ✅ **学习曲线平缓**：对于熟悉SQL的开发者非常友好
- ✅ **性能优异**：没有多余的抽象层，直接操作SQL
- ✅ **易于维护**：SQL集中管理，便于优化和审查
- ✅ **灵活映射**：支持复杂的对象映射关系

**适用场景：**
- 复杂的查询逻辑
- 需要精细的SQL优化
- 数据库设计优先的项目
- 报表和分析类应用

---

## 2. MyBatis vs JPA/Hibernate

| 特性 | MyBatis | JPA/Hibernate |
|------|---------|---------------|
| **类型** | 半自动ORM | 全自动ORM |
| **SQL控制** | 完全控制 | 自动生成 |
| **学习曲线** | 较平缓 | 较陡峭 |
| **复杂查询** | 容易 | 困难（需HQL/JPQL） |
| **性能优化** | 容易 | 需要深入理解 |
| **开发效率** | 中等 | 高（简单CRUD） |
| **灵活性** | 高 | 低 |
| **数据库移植性** | 低 | 高 |

**简单记忆：**
- 想要**控制**SQL → MyBatis
- 想要**自动化** → JPA/Hibernate

---

## 3. 核心概念

### 3.1 核心组件

```
SqlSessionFactoryBuilder
    ↓ 构建
SqlSessionFactory
    ↓ 打开
SqlSession
    ↓ 获取
Mapper接口
    ↓ 执行
数据库操作
```

#### 3.1.1 SqlSessionFactory
- **作用**：创建SqlSession的工厂
- **生命周期**：应用级别（单例）
- **创建**：通过配置文件或Java代码创建

#### 3.1.2 SqlSession
- **作用**：执行数据库操作的会话
- **生命周期**：请求级别（用完即关）
- **特点**：**不是线程安全的**

#### 3.1.3 Mapper接口
- **作用**：定义数据库操作方法
- **实现**：MyBatis自动生成代理实现
- **获取**：通过SqlSession获取

### 3.2 配置文件

MyBatis有两类重要的配置文件：

#### 3.2.1 主配置文件（mybatis-config.xml）
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
  PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
  <!-- 全局配置 -->
  <settings>
    <setting name="mapUnderscoreToCamelCase" value="true"/>
  </settings>

  <!-- 环境配置 -->
  <environments default="development">
    <environment id="development">
      <transactionManager type="JDBC"/>
      <dataSource type="POOLED">
        <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/test"/>
        <property name="username" value="root"/>
        <property name="password" value="password"/>
      </dataSource>
    </environment>
  </environments>

  <!-- 映射器 -->
  <mappers>
    <mapper resource="mapper/UserMapper.xml"/>
  </mappers>
</configuration>
```

#### 3.2.2 映射文件（Mapper.xml）
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">
  <select id="selectUser" resultType="com.example.entity.User">
    SELECT * FROM users WHERE id = #{id}
  </select>
</mapper>
```

---

## 4. 环境搭建

### 4.1 Maven依赖（Spring Boot项目）

```xml
<dependencies>
    <!-- MyBatis Spring Boot Starter -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>

    <!-- 数据库驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- 连接池（可选但推荐） -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
    </dependency>
</dependencies>
```

### 4.2 Spring Boot配置（application.yml）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/miniups?useSSL=false&serverTimezone=UTC
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  # Mapper XML文件位置
  mapper-locations: classpath:mapper/**/*.xml
  # 类型别名包
  type-aliases-package: com.miniups.model.entity
  # MyBatis配置
  configuration:
    # 下划线转驼峰
    map-underscore-to-camel-case: true
    # 打印SQL日志（开发环境）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 二级缓存
    cache-enabled: true
    # 延迟加载
    lazy-loading-enabled: true
    # 积极的延迟加载
    aggressive-lazy-loading: false

# 日志配置
logging:
  level:
    com.miniups.repository: DEBUG
```

### 4.3 项目结构

```
src/main/java/com/miniups/
├── model/
│   └── entity/
│       └── User.java           # 实体类
├── repository/                  # Mapper接口
│   └── UserRepository.java
└── service/
    └── UserService.java         # 业务逻辑

src/main/resources/
├── mapper/                      # Mapper XML文件
│   └── UserMapper.xml
└── application.yml              # Spring Boot配置
```

---

## 5. 基础使用

### 5.1 创建实体类

```java
package com.miniups.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：users
 */
@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private Integer age;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**命名规范：**
- 数据库字段：`user_name`（下划线命名）
- Java字段：`userName`（驼峰命名）
- MyBatis会自动转换（开启`map-underscore-to-camel-case`）

### 5.2 创建Mapper接口

```java
package com.miniups.repository;

import com.miniups.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户数据访问接口
 */
@Mapper  // 标记为MyBatis Mapper
public interface UserRepository {

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户对象，不存在返回null
     */
    User selectById(@Param("id") Long id);

    /**
     * 查询所有用户
     * @return 用户列表
     */
    List<User> selectAll();

    /**
     * 插入用户
     * @param user 用户对象
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 更新用户
     * @param user 用户对象
     * @return 影响行数
     */
    int update(User user);

    /**
     * 删除用户
     * @param id 用户ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
```

**重要注解：**
- `@Mapper`：标记接口为MyBatis Mapper
- `@Param`：指定参数名称（多参数时必须）

### 5.3 创建Mapper XML

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace必须对应Mapper接口的全限定名 -->
<mapper namespace="com.miniups.repository.UserRepository">

    <!-- 结果映射（可选，字段名相同时不需要） -->
    <resultMap id="BaseResultMap" type="User">
        <id property="id" column="id"/>
        <result property="username" column="username"/>
        <result property="email" column="email"/>
        <result property="password" column="password"/>
        <result property="age" column="age"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <!-- 查询：根据ID查询 -->
    <select id="selectById" resultMap="BaseResultMap">
        SELECT id, username, email, password, age, created_at, updated_at
        FROM users
        WHERE id = #{id}
    </select>

    <!-- 查询：查询所有 -->
    <select id="selectAll" resultMap="BaseResultMap">
        SELECT id, username, email, password, age, created_at, updated_at
        FROM users
        ORDER BY created_at DESC
    </select>

    <!-- 插入 -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (username, email, password, age, created_at, updated_at)
        VALUES (#{username}, #{email}, #{password}, #{age}, NOW(), NOW())
    </insert>

    <!-- 更新 -->
    <update id="update">
        UPDATE users
        SET username = #{username},
            email = #{email},
            password = #{password},
            age = #{age},
            updated_at = NOW()
        WHERE id = #{id}
    </update>

    <!-- 删除 -->
    <delete id="deleteById">
        DELETE FROM users WHERE id = #{id}
    </delete>

</mapper>
```

**XML标签说明：**
- `<mapper>`：根标签，namespace对应Mapper接口
- `<select>`：查询语句，id对应接口方法名
- `<insert>`：插入语句
- `<update>`：更新语句
- `<delete>`：删除语句
- `<resultMap>`：结果映射配置

### 5.4 Service层使用

```java
package com.miniups.service;

import com.miniups.model.entity.User;
import com.miniups.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor  // Lombok自动生成构造器注入
public class UserService {

    private final UserRepository userRepository;

    /**
     * 根据ID查询用户
     */
    public User getUserById(Long id) {
        return userRepository.selectById(id);
    }

    /**
     * 查询所有用户
     */
    public List<User> getAllUsers() {
        return userRepository.selectAll();
    }

    /**
     * 创建用户
     */
    @Transactional
    public User createUser(User user) {
        userRepository.insert(user);
        // insert后，user.id会自动填充（因为useGeneratedKeys=true）
        return user;
    }

    /**
     * 更新用户
     */
    @Transactional
    public boolean updateUser(User user) {
        return userRepository.update(user) > 0;
    }

    /**
     * 删除用户
     */
    @Transactional
    public boolean deleteUser(Long id) {
        return userRepository.deleteById(id) > 0;
    }
}
```

---

## 6. XML映射详解

### 6.1 参数传递

#### 6.1.1 单个参数

```java
// Mapper接口
User selectById(Long id);
```

```xml
<!-- Mapper XML -->
<select id="selectById" resultType="User">
    SELECT * FROM users WHERE id = #{id}
</select>
```

**说明：**
- 单个参数可以直接使用，不需要`@Param`
- `#{id}`：参数占位符，MyBatis会自动处理SQL注入

#### 6.1.2 多个参数（必须使用@Param）

```java
// Mapper接口
List<User> selectByUsernameAndAge(@Param("username") String username,
                                   @Param("age") Integer age);
```

```xml
<!-- Mapper XML -->
<select id="selectByUsernameAndAge" resultType="User">
    SELECT * FROM users
    WHERE username = #{username} AND age = #{age}
</select>
```

#### 6.1.3 对象参数

```java
// Mapper接口
int insert(User user);
```

```xml
<!-- Mapper XML -->
<insert id="insert">
    INSERT INTO users (username, email, age)
    VALUES (#{username}, #{email}, #{age})
</insert>
```

**说明：**
- `#{username}`会调用`user.getUsername()`

#### 6.1.4 Map参数

```java
// Mapper接口
List<User> selectByMap(Map<String, Object> params);
```

```xml
<!-- Mapper XML -->
<select id="selectByMap" resultType="User">
    SELECT * FROM users
    WHERE username = #{username} AND age = #{age}
</select>
```

```java
// 使用示例
Map<String, Object> params = new HashMap<>();
params.put("username", "John");
params.put("age", 25);
List<User> users = userRepository.selectByMap(params);
```

### 6.2 参数符号：#{} vs ${}

| 特性 | #{} | ${} |
|------|-----|-----|
| **SQL处理** | 预编译（PreparedStatement） | 字符串替换 |
| **SQL注入** | 安全 | 不安全 |
| **使用场景** | 参数值 | 动态表名、列名 |
| **类型处理** | 自动类型转换 | 无类型转换 |

```xml
<!-- 正确：使用#{} -->
<select id="selectById" resultType="User">
    SELECT * FROM users WHERE id = #{id}
</select>
<!-- 生成SQL：SELECT * FROM users WHERE id = ? -->

<!-- 危险：使用${} -->
<select id="selectById" resultType="User">
    SELECT * FROM users WHERE id = ${id}
</select>
<!-- 生成SQL：SELECT * FROM users WHERE id = 123 -->
<!-- 如果id="1 OR 1=1"，会导致SQL注入！ -->
```

**${}的合法使用场景：**
```xml
<!-- 动态表名 -->
<select id="selectFromTable" resultType="User">
    SELECT * FROM ${tableName} WHERE id = #{id}
</select>

<!-- 动态排序 -->
<select id="selectWithOrder" resultType="User">
    SELECT * FROM users ORDER BY ${orderColumn} ${orderDirection}
</select>
```

### 6.3 结果映射

#### 6.3.1 自动映射（字段名相同）

```xml
<!-- 如果数据库字段和Java字段名相同，可以直接使用resultType -->
<select id="selectById" resultType="User">
    SELECT id, username, email FROM users WHERE id = #{id}
</select>
```

#### 6.3.2 手动映射（字段名不同）

```xml
<!-- 方式1：使用别名 -->
<select id="selectById" resultType="User">
    SELECT
        id,
        user_name AS username,
        user_email AS email
    FROM users
    WHERE id = #{id}
</select>

<!-- 方式2：使用resultMap -->
<resultMap id="UserResultMap" type="User">
    <id property="id" column="id"/>
    <result property="username" column="user_name"/>
    <result property="email" column="user_email"/>
</resultMap>

<select id="selectById" resultMap="UserResultMap">
    SELECT id, user_name, user_email FROM users WHERE id = #{id}
</select>
```

#### 6.3.3 复杂结果映射

```java
// 用户实体包含地址对象
@Data
public class User {
    private Long id;
    private String username;
    private Address address;  // 嵌套对象
}

@Data
public class Address {
    private String province;
    private String city;
    private String street;
}
```

```xml
<resultMap id="UserWithAddressMap" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>
    <!-- 嵌套结果映射 -->
    <association property="address" javaType="Address">
        <result property="province" column="province"/>
        <result property="city" column="city"/>
        <result property="street" column="street"/>
    </association>
</resultMap>

<select id="selectUserWithAddress" resultMap="UserWithAddressMap">
    SELECT
        u.id, u.username,
        a.province, a.city, a.street
    FROM users u
    LEFT JOIN addresses a ON u.id = a.user_id
    WHERE u.id = #{id}
</select>
```

### 6.4 获取自增主键

```xml
<!-- 方式1：useGeneratedKeys（推荐） -->
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO users (username, email) VALUES (#{username}, #{email})
</insert>

<!-- 方式2：selectKey（适用于不支持自增的数据库） -->
<insert id="insert">
    <selectKey keyProperty="id" resultType="long" order="AFTER">
        SELECT LAST_INSERT_ID()
    </selectKey>
    INSERT INTO users (username, email) VALUES (#{username}, #{email})
</insert>
```

```java
// 使用示例
User user = new User();
user.setUsername("John");
user.setEmail("john@example.com");

userRepository.insert(user);
System.out.println("新增用户ID：" + user.getId());  // ID已自动填充
```

---

## 7. 注解方式

### 7.1 基本注解

```java
package com.miniups.repository;

import com.miniups.model.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserAnnotationRepository {

    /**
     * 查询
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    User selectById(@Param("id") Long id);

    /**
     * 查询所有
     */
    @Select("SELECT * FROM users ORDER BY created_at DESC")
    List<User> selectAll();

    /**
     * 插入
     */
    @Insert("INSERT INTO users (username, email, password) " +
            "VALUES (#{username}, #{email}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 更新
     */
    @Update("UPDATE users SET username = #{username}, email = #{email} " +
            "WHERE id = #{id}")
    int update(User user);

    /**
     * 删除
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
```

### 7.2 结果映射注解

```java
@Results(id = "userResultMap", value = {
    @Result(property = "id", column = "id", id = true),
    @Result(property = "username", column = "user_name"),
    @Result(property = "email", column = "user_email")
})
@Select("SELECT id, user_name, user_email FROM users WHERE id = #{id}")
User selectById(@Param("id") Long id);

// 复用结果映射
@ResultMap("userResultMap")
@Select("SELECT id, user_name, user_email FROM users")
List<User> selectAll();
```

### 7.3 动态SQL注解

```java
/**
 * 使用Provider实现动态SQL
 */
@SelectProvider(type = UserSqlProvider.class, method = "selectByCondition")
List<User> selectByCondition(@Param("username") String username,
                              @Param("age") Integer age);

/**
 * SQL Provider类
 */
class UserSqlProvider {
    public String selectByCondition(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");

        if (params.get("username") != null) {
            sql.append(" AND username = #{username}");
        }
        if (params.get("age") != null) {
            sql.append(" AND age = #{age}");
        }

        return sql.toString();
    }
}
```

### 7.4 XML vs 注解对比

| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **XML** | SQL集中管理<br>支持复杂SQL<br>易于维护 | 文件多 | 复杂查询<br>大型项目 |
| **注解** | 代码简洁<br>无需额外文件 | 复杂SQL难写<br>难以格式化 | 简单CRUD<br>小型项目 |

**推荐做法：**
- 简单SQL → 注解
- 复杂SQL → XML
- **混合使用**是可以的

---

## 8. 动态SQL

动态SQL是MyBatis的强大特性，可以根据条件动态生成SQL。

### 8.1 if标签

```xml
<select id="selectByCondition" resultType="User">
    SELECT * FROM users
    WHERE 1=1
    <if test="username != null and username != ''">
        AND username = #{username}
    </if>
    <if test="age != null">
        AND age = #{age}
    </if>
    <if test="email != null and email != ''">
        AND email = #{email}
    </if>
</select>
```

**test属性支持的表达式：**
- `username != null`：判空
- `username != ''`：判断空字符串
- `age > 18`：数值比较
- `list != null and list.size() > 0`：集合判断

### 8.2 where标签

`<where>`标签会自动处理第一个`AND`或`OR`：

```xml
<select id="selectByCondition" resultType="User">
    SELECT * FROM users
    <where>
        <if test="username != null and username != ''">
            AND username = #{username}
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
        <if test="email != null and email != ''">
            AND email = #{email}
        </if>
    </where>
</select>
```

**效果：**
- 如果所有条件都不满足，`<where>`不会生成任何内容
- 如果有条件满足，`<where>`会生成`WHERE`，并自动去掉第一个`AND`

### 8.3 set标签

用于动态UPDATE语句：

```xml
<update id="updateSelective">
    UPDATE users
    <set>
        <if test="username != null">username = #{username},</if>
        <if test="email != null">email = #{email},</if>
        <if test="age != null">age = #{age},</if>
        updated_at = NOW()
    </set>
    WHERE id = #{id}
</update>
```

**效果：**
- 自动添加`SET`关键字
- 自动去掉最后一个多余的逗号

### 8.4 choose-when-otherwise

类似于Java的`switch-case`：

```xml
<select id="selectByCondition" resultType="User">
    SELECT * FROM users
    WHERE 1=1
    <choose>
        <when test="id != null">
            AND id = #{id}
        </when>
        <when test="username != null">
            AND username = #{username}
        </when>
        <otherwise>
            AND status = 1
        </otherwise>
    </choose>
</select>
```

### 8.5 foreach标签

#### 8.5.1 IN查询

```java
// Mapper接口
List<User> selectByIds(@Param("ids") List<Long> ids);
```

```xml
<!-- Mapper XML -->
<select id="selectByIds" resultType="User">
    SELECT * FROM users
    WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

**生成的SQL：**
```sql
SELECT * FROM users WHERE id IN (1, 2, 3, 4, 5)
```

#### 8.5.2 批量插入

```java
// Mapper接口
int batchInsert(@Param("users") List<User> users);
```

```xml
<!-- Mapper XML -->
<insert id="batchInsert">
    INSERT INTO users (username, email, age)
    VALUES
    <foreach collection="users" item="user" separator=",">
        (#{user.username}, #{user.email}, #{user.age})
    </foreach>
</insert>
```

**生成的SQL：**
```sql
INSERT INTO users (username, email, age)
VALUES
    ('John', 'john@example.com', 25),
    ('Jane', 'jane@example.com', 30),
    ('Bob', 'bob@example.com', 28)
```

#### 8.5.3 批量更新

```xml
<update id="batchUpdate">
    <foreach collection="users" item="user" separator=";">
        UPDATE users
        SET username = #{user.username}, email = #{user.email}
        WHERE id = #{user.id}
    </foreach>
</update>
```

**foreach属性说明：**
- `collection`：集合参数名
- `item`：集合中的元素变量名
- `index`：索引变量名（可选）
- `open`：开始字符
- `close`：结束字符
- `separator`：分隔符

### 8.6 trim标签

`<trim>`是`<where>`和`<set>`的通用版本：

```xml
<select id="selectByCondition" resultType="User">
    SELECT * FROM users
    <trim prefix="WHERE" prefixOverrides="AND |OR ">
        <if test="username != null">
            AND username = #{username}
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </trim>
</select>
```

**属性说明：**
- `prefix`：添加的前缀
- `suffix`：添加的后缀
- `prefixOverrides`：需要去掉的前缀
- `suffixOverrides`：需要去掉的后缀

### 8.7 SQL片段复用

```xml
<!-- 定义可复用的SQL片段 -->
<sql id="userColumns">
    id, username, email, age, created_at, updated_at
</sql>

<sql id="userWhere">
    <where>
        <if test="username != null">AND username = #{username}</if>
        <if test="age != null">AND age = #{age}</if>
    </where>
</sql>

<!-- 使用SQL片段 -->
<select id="selectByCondition" resultType="User">
    SELECT <include refid="userColumns"/>
    FROM users
    <include refid="userWhere"/>
</select>

<select id="countByCondition" resultType="int">
    SELECT COUNT(*)
    FROM users
    <include refid="userWhere"/>
</select>
```

---

## 9. 结果映射

### 9.1 基本结果映射

```xml
<resultMap id="UserResultMap" type="User">
    <!-- id标签用于主键 -->
    <id property="id" column="id"/>

    <!-- result标签用于普通字段 -->
    <result property="username" column="username"/>
    <result property="email" column="email"/>
    <result property="age" column="age"/>
    <result property="createdAt" column="created_at"/>
    <result property="updatedAt" column="updated_at"/>
</resultMap>
```

**标签说明：**
- `<id>`：映射主键字段（性能更好）
- `<result>`：映射普通字段
- `property`：Java对象属性名
- `column`：数据库列名

### 9.2 嵌套对象映射（一对一）

#### 9.2.1 实体类

```java
@Data
public class User {
    private Long id;
    private String username;
    private UserProfile profile;  // 一对一关联
}

@Data
public class UserProfile {
    private Long id;
    private Long userId;
    private String nickname;
    private String avatar;
    private String bio;
}
```

#### 9.2.2 方式1：嵌套结果映射

```xml
<resultMap id="UserWithProfileMap" type="User">
    <id property="id" column="user_id"/>
    <result property="username" column="username"/>

    <!-- association用于一对一关联 -->
    <association property="profile" javaType="UserProfile">
        <id property="id" column="profile_id"/>
        <result property="userId" column="user_id"/>
        <result property="nickname" column="nickname"/>
        <result property="avatar" column="avatar"/>
        <result property="bio" column="bio"/>
    </association>
</resultMap>

<select id="selectUserWithProfile" resultMap="UserWithProfileMap">
    SELECT
        u.id AS user_id,
        u.username,
        p.id AS profile_id,
        p.user_id,
        p.nickname,
        p.avatar,
        p.bio
    FROM users u
    LEFT JOIN user_profiles p ON u.id = p.user_id
    WHERE u.id = #{id}
</select>
```

#### 9.2.3 方式2：嵌套查询

```xml
<resultMap id="UserWithProfileMap2" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>

    <!-- 嵌套查询：调用另一个select -->
    <association property="profile"
                 column="id"
                 select="selectProfileByUserId"/>
</resultMap>

<select id="selectUserWithProfile2" resultMap="UserWithProfileMap2">
    SELECT id, username FROM users WHERE id = #{id}
</select>

<select id="selectProfileByUserId" resultType="UserProfile">
    SELECT id, user_id, nickname, avatar, bio
    FROM user_profiles
    WHERE user_id = #{id}
</select>
```

**两种方式对比：**

| 方式 | 优点 | 缺点 |
|------|------|------|
| **嵌套结果** | 一次查询，性能好 | SQL复杂 |
| **嵌套查询** | SQL简单，易维护 | N+1问题 |

### 9.3 集合映射（一对多）

#### 9.3.1 实体类

```java
@Data
public class User {
    private Long id;
    private String username;
    private List<Order> orders;  // 一对多关联
}

@Data
public class Order {
    private Long id;
    private Long userId;
    private String orderNo;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
```

#### 9.3.2 结果映射

```xml
<resultMap id="UserWithOrdersMap" type="User">
    <id property="id" column="user_id"/>
    <result property="username" column="username"/>

    <!-- collection用于一对多关联 -->
    <collection property="orders" ofType="Order">
        <id property="id" column="order_id"/>
        <result property="userId" column="user_id"/>
        <result property="orderNo" column="order_no"/>
        <result property="amount" column="amount"/>
        <result property="createdAt" column="created_at"/>
    </collection>
</resultMap>

<select id="selectUserWithOrders" resultMap="UserWithOrdersMap">
    SELECT
        u.id AS user_id,
        u.username,
        o.id AS order_id,
        o.user_id,
        o.order_no,
        o.amount,
        o.created_at
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.id = #{id}
</select>
```

### 9.4 多对多映射

#### 9.4.1 实体类

```java
@Data
public class User {
    private Long id;
    private String username;
    private List<Role> roles;  // 多对多关联
}

@Data
public class Role {
    private Long id;
    private String name;
    private String code;
}
```

#### 9.4.2 结果映射

```xml
<resultMap id="UserWithRolesMap" type="User">
    <id property="id" column="user_id"/>
    <result property="username" column="username"/>

    <collection property="roles" ofType="Role">
        <id property="id" column="role_id"/>
        <result property="name" column="role_name"/>
        <result property="code" column="role_code"/>
    </collection>
</resultMap>

<select id="selectUserWithRoles" resultMap="UserWithRolesMap">
    SELECT
        u.id AS user_id,
        u.username,
        r.id AS role_id,
        r.name AS role_name,
        r.code AS role_code
    FROM users u
    LEFT JOIN user_roles ur ON u.id = ur.user_id
    LEFT JOIN roles r ON ur.role_id = r.id
    WHERE u.id = #{id}
</select>
```

### 9.5 鉴别器（discriminator）

根据某个字段值决定使用哪个结果映射：

```xml
<resultMap id="VehicleResultMap" type="Vehicle">
    <id property="id" column="id"/>
    <result property="type" column="type"/>

    <!-- 根据type字段值选择不同的映射 -->
    <discriminator javaType="string" column="type">
        <case value="CAR" resultMap="CarResultMap"/>
        <case value="TRUCK" resultMap="TruckResultMap"/>
    </discriminator>
</resultMap>

<resultMap id="CarResultMap" type="Car" extends="VehicleResultMap">
    <result property="doorCount" column="door_count"/>
</resultMap>

<resultMap id="TruckResultMap" type="Truck" extends="VehicleResultMap">
    <result property="payload" column="payload"/>
</resultMap>
```

---

## 10. 关联查询

### 10.1 一对一关联

#### 示例：用户和身份证

```java
@Data
public class User {
    private Long id;
    private String username;
    private IdCard idCard;
}

@Data
public class IdCard {
    private Long id;
    private Long userId;
    private String idNumber;
    private String address;
}
```

```xml
<resultMap id="UserWithIdCardMap" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>
    <association property="idCard" javaType="IdCard">
        <id property="id" column="card_id"/>
        <result property="userId" column="user_id"/>
        <result property="idNumber" column="id_number"/>
        <result property="address" column="address"/>
    </association>
</resultMap>

<select id="selectUserWithIdCard" resultMap="UserWithIdCardMap">
    SELECT
        u.id, u.username,
        c.id AS card_id, c.user_id, c.id_number, c.address
    FROM users u
    LEFT JOIN id_cards c ON u.id = c.user_id
    WHERE u.id = #{id}
</select>
```

### 10.2 一对多关联

#### 示例：用户和订单

```java
@Data
public class User {
    private Long id;
    private String username;
    private List<Order> orders;
}
```

```xml
<resultMap id="UserWithOrdersMap" type="User">
    <id property="id" column="user_id"/>
    <result property="username" column="username"/>
    <collection property="orders" ofType="Order">
        <id property="id" column="order_id"/>
        <result property="orderNo" column="order_no"/>
        <result property="amount" column="amount"/>
    </collection>
</resultMap>

<select id="selectUserWithOrders" resultMap="UserWithOrdersMap">
    SELECT
        u.id AS user_id, u.username,
        o.id AS order_id, o.order_no, o.amount
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.id = #{id}
</select>
```

### 10.3 多对多关联

#### 示例：学生和课程

```java
@Data
public class Student {
    private Long id;
    private String name;
    private List<Course> courses;
}

@Data
public class Course {
    private Long id;
    private String name;
    private String code;
}
```

```xml
<resultMap id="StudentWithCoursesMap" type="Student">
    <id property="id" column="student_id"/>
    <result property="name" column="student_name"/>
    <collection property="courses" ofType="Course">
        <id property="id" column="course_id"/>
        <result property="name" column="course_name"/>
        <result property="code" column="course_code"/>
    </collection>
</resultMap>

<select id="selectStudentWithCourses" resultMap="StudentWithCoursesMap">
    SELECT
        s.id AS student_id, s.name AS student_name,
        c.id AS course_id, c.name AS course_name, c.code AS course_code
    FROM students s
    LEFT JOIN student_courses sc ON s.id = sc.student_id
    LEFT JOIN courses c ON sc.course_id = c.id
    WHERE s.id = #{id}
</select>
```

### 10.4 延迟加载

延迟加载可以避免不必要的数据查询，提高性能。

#### 10.4.1 配置延迟加载

```yaml
mybatis:
  configuration:
    # 开启延迟加载
    lazy-loading-enabled: true
    # 关闭积极加载
    aggressive-lazy-loading: false
```

#### 10.4.2 使用延迟加载

```xml
<resultMap id="UserLazyMap" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>

    <!-- fetchType="lazy"：延迟加载 -->
    <collection property="orders"
                column="id"
                select="selectOrdersByUserId"
                fetchType="lazy"/>
</resultMap>

<select id="selectUserLazy" resultMap="UserLazyMap">
    SELECT id, username FROM users WHERE id = #{id}
</select>

<select id="selectOrdersByUserId" resultType="Order">
    SELECT * FROM orders WHERE user_id = #{id}
</select>
```

```java
// 使用示例
User user = userRepository.selectUserLazy(1L);
System.out.println(user.getUsername());  // 只查询user表

// 只有访问orders时才会查询orders表
List<Order> orders = user.getOrders();  // 此时才执行查询
```

**fetchType选项：**
- `lazy`：延迟加载
- `eager`：立即加载

---

## 11. 高级特性

### 11.1 缓存

#### 11.1.1 一级缓存（SqlSession级别）

一级缓存默认开启，作用域为SqlSession。

```java
@Test
public void testFirstLevelCache() {
    SqlSession session = sqlSessionFactory.openSession();
    UserRepository mapper = session.getMapper(UserRepository.class);

    // 第一次查询，访问数据库
    User user1 = mapper.selectById(1L);

    // 第二次查询，直接从缓存读取，不访问数据库
    User user2 = mapper.selectById(1L);

    System.out.println(user1 == user2);  // true

    session.close();
}
```

**一级缓存失效情况：**
1. SqlSession关闭
2. 执行了增删改操作
3. 手动清空缓存：`session.clearCache()`
4. 查询不同的数据

#### 11.1.2 二级缓存（Mapper级别）

二级缓存需要手动开启，作用域为Mapper。

**步骤1：开启全局二级缓存**
```yaml
mybatis:
  configuration:
    cache-enabled: true
```

**步骤2：在Mapper XML中配置缓存**
```xml
<mapper namespace="com.miniups.repository.UserRepository">
    <!-- 开启二级缓存 -->
    <cache
        eviction="LRU"
        flushInterval="60000"
        size="512"
        readOnly="true"/>

    <!-- 其他配置... -->
</mapper>
```

**cache属性说明：**
- `eviction`：缓存回收策略
  - `LRU`：最近最少使用（默认）
  - `FIFO`：先进先出
  - `SOFT`：软引用
  - `WEAK`：弱引用
- `flushInterval`：刷新间隔（毫秒）
- `size`：缓存对象数量
- `readOnly`：是否只读

**步骤3：实体类实现Serializable**
```java
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    // ...
}
```

#### 11.1.3 自定义缓存

可以集成Redis等缓存：

```xml
<cache type="com.miniups.cache.RedisCache"/>
```

```java
public class RedisCache implements Cache {
    private final String id;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCache(String id) {
        this.id = id;
        this.redisTemplate = SpringContextHolder.getBean("redisTemplate");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void putObject(Object key, Object value) {
        redisTemplate.opsForValue().set(key.toString(), value, 1, TimeUnit.HOURS);
    }

    @Override
    public Object getObject(Object key) {
        return redisTemplate.opsForValue().get(key.toString());
    }

    // 实现其他方法...
}
```

### 11.2 分页

#### 11.2.1 使用PageHelper插件

**步骤1：添加依赖**
```xml
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>2.1.0</version>
</dependency>
```

**步骤2：使用分页**
```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public PageInfo<User> getUsersByPage(int pageNum, int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);

        // 执行查询
        List<User> users = userRepository.selectAll();

        // 封装分页信息
        return new PageInfo<>(users);
    }
}
```

**步骤3：返回分页数据**
```java
@GetMapping("/users")
public Result<PageInfo<User>> getUsers(
    @RequestParam(defaultValue = "1") int pageNum,
    @RequestParam(defaultValue = "10") int pageSize) {

    PageInfo<User> pageInfo = userService.getUsersByPage(pageNum, pageSize);
    return Result.success(pageInfo);
}
```

**PageInfo包含的信息：**
```json
{
  "total": 100,           // 总记录数
  "list": [...],          // 当前页数据
  "pageNum": 1,           // 当前页码
  "pageSize": 10,         // 每页大小
  "pages": 10,            // 总页数
  "isFirstPage": true,    // 是否第一页
  "isLastPage": false,    // 是否最后一页
  "hasPreviousPage": false,
  "hasNextPage": true
}
```

#### 11.2.2 手动分页

```xml
<select id="selectByPage" resultType="User">
    SELECT * FROM users
    ORDER BY created_at DESC
    LIMIT #{offset}, #{limit}
</select>
```

```java
public interface UserRepository {
    List<User> selectByPage(@Param("offset") int offset,
                            @Param("limit") int limit);
}

// 使用
int pageNum = 1;
int pageSize = 10;
int offset = (pageNum - 1) * pageSize;
List<User> users = userRepository.selectByPage(offset, pageSize);
```

### 11.3 类型处理器（TypeHandler）

用于Java类型和JDBC类型之间的转换。

#### 11.3.1 自定义TypeHandler

```java
/**
 * 将List<String>存储为JSON字符串
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ListTypeHandler extends BaseTypeHandler<List<String>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                     List<String> parameter,
                                     JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName)
        throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex)
        throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex)
        throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
```

#### 11.3.2 注册TypeHandler

```yaml
mybatis:
  type-handlers-package: com.miniups.handler
```

或在XML中使用：
```xml
<result property="tags" column="tags"
        typeHandler="com.miniups.handler.ListTypeHandler"/>
```

### 11.4 插件（Interceptor）

MyBatis允许在SQL执行的不同阶段进行拦截。

#### 11.4.1 自定义插件

```java
/**
 * 性能监控插件
 */
@Intercepts({
    @Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
    ),
    @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class}
    )
})
@Component
public class PerformanceInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(
        PerformanceInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            return invocation.proceed();
        } finally {
            long end = System.currentTimeMillis();
            long time = end - start;

            if (time > 1000) {  // 慢查询阈值：1秒
                MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
                log.warn("慢查询: {} 耗时: {}ms", ms.getId(), time);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以配置属性
    }
}
```

#### 11.4.2 拦截点

MyBatis提供了4个拦截点：

1. **Executor**：执行器（增删改查）
2. **ParameterHandler**：参数处理
3. **ResultSetHandler**：结果集处理
4. **StatementHandler**：SQL语句处理

---

## 12. 最佳实践

### 12.1 命名规范

#### 12.1.1 Mapper接口

```java
// 推荐
public interface UserRepository { }
public interface UserMapper { }

// 不推荐
public interface UserDao { }
public interface IUserRepository { }
```

#### 12.1.2 方法命名

```java
// 查询
User selectById(Long id);
List<User> selectAll();
List<User> selectByUsername(String username);
int countByStatus(Integer status);

// 插入
int insert(User user);
int batchInsert(List<User> users);

// 更新
int update(User user);
int updateSelective(User user);  // 选择性更新

// 删除
int deleteById(Long id);
int deleteByIds(List<Long> ids);
```

### 12.2 返回值规范

| 操作类型 | 返回值类型 | 说明 |
|---------|-----------|------|
| 单个查询 | 实体对象 | 不存在返回null |
| 列表查询 | List<实体> | 空列表返回empty list |
| 统计查询 | int/long | 返回数量 |
| 插入 | int | 返回影响行数 |
| 更新 | int | 返回影响行数 |
| 删除 | int | 返回影响行数 |

### 12.3 参数传递

```java
// ✅ 推荐：单个参数无需@Param
User selectById(Long id);

// ✅ 推荐：多个参数使用@Param
List<User> selectByUsernameAndAge(@Param("username") String username,
                                   @Param("age") Integer age);

// ✅ 推荐：复杂参数使用对象
List<User> selectByCondition(UserQueryDTO query);

// ❌ 不推荐：多个参数不使用@Param
List<User> selectByUsernameAndAge(String username, Integer age);
```

### 12.4 SQL编写

#### 12.4.1 明确指定查询字段

```xml
<!-- ✅ 推荐 -->
<select id="selectById" resultType="User">
    SELECT id, username, email, age FROM users WHERE id = #{id}
</select>

<!-- ❌ 不推荐 -->
<select id="selectById" resultType="User">
    SELECT * FROM users WHERE id = #{id}
</select>
```

#### 12.4.2 使用别名处理字段映射

```xml
<!-- ✅ 推荐 -->
<select id="selectById" resultType="User">
    SELECT
        id,
        user_name AS username,
        user_email AS email
    FROM users
    WHERE id = #{id}
</select>
```

#### 12.4.3 使用resultMap处理复杂映射

```xml
<!-- ✅ 推荐：复杂映射使用resultMap -->
<resultMap id="UserResultMap" type="User">
    <id property="id" column="id"/>
    <result property="username" column="user_name"/>
    <result property="email" column="user_email"/>
</resultMap>
```

### 12.5 动态SQL

```xml
<!-- ✅ 推荐：使用<where>标签 -->
<select id="selectByCondition" resultType="User">
    SELECT * FROM users
    <where>
        <if test="username != null">AND username = #{username}</if>
        <if test="age != null">AND age = #{age}</if>
    </where>
</select>

<!-- ❌ 不推荐：手动处理WHERE -->
<select id="selectByCondition" resultType="User">
    SELECT * FROM users WHERE 1=1
    <if test="username != null">AND username = #{username}</if>
    <if test="age != null">AND age = #{age}</if>
</select>
```

### 12.6 批量操作

```xml
<!-- ✅ 推荐：使用批量插入 -->
<insert id="batchInsert">
    INSERT INTO users (username, email) VALUES
    <foreach collection="users" item="user" separator=",">
        (#{user.username}, #{user.email})
    </foreach>
</insert>

<!-- ❌ 不推荐：循环调用单条插入 -->
for (User user : users) {
    userRepository.insert(user);  // 性能差
}
```

### 12.7 事务管理

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // ✅ 推荐：使用@Transactional
    @Transactional(rollbackFor = Exception.class)
    public void createUserWithProfile(User user, UserProfile profile) {
        userRepository.insert(user);
        profileRepository.insert(profile);
    }

    // ✅ 推荐：只读事务
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.selectById(id);
    }
}
```

### 12.8 异常处理

```java
@Service
public class UserService {

    public User getUserById(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Transactional
    public void updateUser(User user) {
        int rows = userRepository.update(user);
        if (rows == 0) {
            throw new BusinessException("更新失败");
        }
    }
}
```

### 12.9 性能优化

#### 12.9.1 避免N+1问题

```xml
<!-- ❌ 不推荐：会产生N+1查询 -->
<resultMap id="UserWithOrdersMap" type="User">
    <collection property="orders"
                column="id"
                select="selectOrdersByUserId"/>
</resultMap>

<!-- ✅ 推荐：使用JOIN一次查询 -->
<resultMap id="UserWithOrdersMap" type="User">
    <collection property="orders" ofType="Order">
        <id property="id" column="order_id"/>
        <result property="orderNo" column="order_no"/>
    </collection>
</resultMap>

<select id="selectUserWithOrders" resultMap="UserWithOrdersMap">
    SELECT u.*, o.id AS order_id, o.order_no
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.id = #{id}
</select>
```

#### 12.9.2 使用合适的查询方式

```java
// ✅ 推荐：只需要数量时用COUNT
int count = userRepository.countByStatus(1);

// ❌ 不推荐：查询全部数据再统计
List<User> users = userRepository.selectByStatus(1);
int count = users.size();

// ✅ 推荐：分页查询
PageInfo<User> page = userService.getUsersByPage(1, 10);

// ❌ 不推荐：查询全部数据再分页
List<User> allUsers = userRepository.selectAll();
List<User> page = allUsers.subList(0, 10);
```

#### 12.9.3 合理使用缓存

```xml
<!-- 频繁查询的数据开启二级缓存 -->
<cache eviction="LRU" flushInterval="60000" size="512"/>

<!-- 实时性要求高的数据不使用缓存 -->
<select id="selectById" useCache="false">
    SELECT * FROM users WHERE id = #{id}
</select>
```

### 12.10 日志配置

```yaml
# 开发环境：打印SQL
logging:
  level:
    com.miniups.repository: DEBUG

mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 生产环境：关闭SQL日志
logging:
  level:
    com.miniups.repository: INFO

mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

---

## 13. 常见问题

### 13.1 找不到Mapper

**问题：**
```
org.apache.ibatis.binding.BindingException: Invalid bound statement (not found)
```

**原因和解决方案：**

1. **XML文件路径不对**
   ```yaml
   # 检查配置
   mybatis:
     mapper-locations: classpath:mapper/**/*.xml  # 确保路径正确
   ```

2. **namespace不匹配**
   ```xml
   <!-- 确保namespace是Mapper接口的全限定名 -->
   <mapper namespace="com.miniups.repository.UserRepository">
   ```

3. **方法id不匹配**
   ```xml
   <!-- 确保id与接口方法名一致 -->
   <select id="selectById" resultType="User">
   ```

4. **XML文件没有编译到target目录**
   ```xml
   <!-- pom.xml添加资源配置 -->
   <build>
       <resources>
           <resource>
               <directory>src/main/resources</directory>
               <includes>
                   <include>**/*.xml</include>
               </includes>
           </resource>
       </resources>
   </build>
   ```

### 13.2 参数传递错误

**问题：**
```
org.apache.ibatis.binding.BindingException: Parameter 'xxx' not found
```

**解决方案：**
```java
// ✅ 多个参数必须使用@Param
List<User> selectByUsernameAndAge(@Param("username") String username,
                                   @Param("age") Integer age);

// ❌ 不使用@Param会报错
List<User> selectByUsernameAndAge(String username, Integer age);
```

### 13.3 结果映射失败

**问题：**
查询返回null或某些字段为null

**原因和解决方案：**

1. **字段名不匹配**
   ```yaml
   # 开启驼峰转换
   mybatis:
     configuration:
       map-underscore-to-camel-case: true
   ```

2. **使用别名**
   ```xml
   <select id="selectById" resultType="User">
       SELECT
           id,
           user_name AS username,
           user_email AS email
       FROM users
       WHERE id = #{id}
   </select>
   ```

3. **使用resultMap**
   ```xml
   <resultMap id="UserResultMap" type="User">
       <result property="username" column="user_name"/>
       <result property="email" column="user_email"/>
   </resultMap>
   ```

### 13.4 事务不生效

**问题：**
数据没有回滚

**原因和解决方案：**

1. **方法不是public**
   ```java
   // ✅ 必须是public方法
   @Transactional
   public void createUser(User user) { }

   // ❌ private方法事务不生效
   @Transactional
   private void createUser(User user) { }
   ```

2. **自调用问题**
   ```java
   @Service
   public class UserService {
       // ❌ 自调用，事务不生效
       public void methodA() {
           this.methodB();  // 直接调用，绕过代理
       }

       @Transactional
       public void methodB() { }

       // ✅ 通过注入自己或使用AopContext
       @Autowired
       private UserService self;

       public void methodA() {
           self.methodB();  // 通过代理调用
       }
   }
   ```

3. **异常类型不匹配**
   ```java
   // ✅ 指定回滚的异常类型
   @Transactional(rollbackFor = Exception.class)
   public void createUser(User user) { }
   ```

### 13.5 SQL注入风险

**问题：**
使用${}导致SQL注入

**解决方案：**
```xml
<!-- ✅ 推荐：使用#{} -->
<select id="selectById" resultType="User">
    SELECT * FROM users WHERE id = #{id}
</select>

<!-- ❌ 危险：使用${} -->
<select id="selectById" resultType="User">
    SELECT * FROM users WHERE id = ${id}
</select>

<!-- ✅ ${}的合法使用：动态表名（确保参数安全） -->
<select id="selectFromTable" resultType="User">
    SELECT * FROM ${tableName} WHERE id = #{id}
</select>
```

### 13.6 性能问题

#### 13.6.1 慢查询

**排查方法：**
```yaml
# 开启SQL日志
logging:
  level:
    com.miniups.repository: DEBUG
```

**优化方案：**
- 添加索引
- 优化SQL
- 使用分页
- 避免SELECT *
- 使用缓存

#### 13.6.2 N+1问题

```xml
<!-- ❌ 问题：会产生N+1查询 -->
<select id="selectAll" resultMap="UserWithOrdersMap">
    SELECT * FROM users
</select>

<resultMap id="UserWithOrdersMap" type="User">
    <collection property="orders"
                column="id"
                select="selectOrdersByUserId"/>
</resultMap>

<!-- ✅ 解决：使用JOIN -->
<select id="selectAll" resultMap="UserWithOrdersMap">
    SELECT u.*, o.id AS order_id, o.order_no
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
</select>
```

### 13.7 类型转换问题

**问题：**
```
org.apache.ibatis.type.TypeException: Could not set parameter
```

**解决方案：**
```java
// 使用正确的类型
// ✅ 数据库BIGINT → Java Long
private Long id;

// ❌ 数据库BIGINT → Java Integer（可能溢出）
private Integer id;

// ✅ 数据库DATETIME → Java LocalDateTime
private LocalDateTime createdAt;

// ✅ 数据库VARCHAR → Java String
private String username;
```

### 13.8 批量操作失败

**问题：**
批量插入/更新失败

**解决方案：**
```yaml
# 开启批量操作支持
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test?rewriteBatchedStatements=true
```

```xml
<!-- 使用foreach实现批量插入 -->
<insert id="batchInsert">
    INSERT INTO users (username, email) VALUES
    <foreach collection="users" item="user" separator=",">
        (#{user.username}, #{user.email})
    </foreach>
</insert>
```

---

## 总结

### 学习路径

1. **基础阶段**
   - 理解MyBatis的核心概念
   - 掌握简单的CRUD操作
   - 熟悉XML配置

2. **进阶阶段**
   - 掌握动态SQL
   - 理解结果映射
   - 学习关联查询

3. **高级阶段**
   - 使用缓存
   - 编写插件
   - 性能优化

### 学习建议

1. **多实践**：通过实际项目练习
2. **看源码**：理解MyBatis的工作原理
3. **对比学习**：与JPA对比，理解各自优势
4. **关注性能**：学会SQL优化和性能调优

### 推荐资源

- [MyBatis官方文档](https://mybatis.org/mybatis-3/zh/index.html)
- [MyBatis-Spring-Boot-Starter](https://github.com/mybatis/spring-boot-starter)
- [PageHelper分页插件](https://github.com/pagehelper/Mybatis-PageHelper)
- [MyBatis-Plus](https://baomidou.com/)（增强工具）

---

## 附录：你的项目示例

基于你的项目（mini-ups），这里是一些实际的使用示例：

### A.1 用户管理

```java
// Repository
@Mapper
public interface UserRepository {
    User selectById(@Param("id") Long id);
    List<User> selectAll();
    int insert(User user);
    int update(User user);
}
```

```xml
<!-- UserMapper.xml -->
<mapper namespace="com.miniups.repository.UserRepository">
    <select id="selectById" resultType="User">
        SELECT * FROM users WHERE id = #{id}
    </select>
</mapper>
```

### A.2 物流追踪

```java
@Mapper
public interface ShipmentRepository {
    Shipment selectByTrackingNumber(@Param("trackingNumber") String trackingNumber);
    List<Shipment> selectByStatus(@Param("status") String status);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
```

### A.3 复杂查询示例

```xml
<!-- 动态查询物流信息 -->
<select id="selectByCondition" resultType="Shipment">
    SELECT * FROM shipments
    <where>
        <if test="trackingNumber != null">
            AND tracking_number = #{trackingNumber}
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
        <if test="startDate != null">
            AND created_at &gt;= #{startDate}
        </if>
        <if test="endDate != null">
            AND created_at &lt;= #{endDate}
        </if>
    </where>
    ORDER BY created_at DESC
</select>
```

---

**祝你学习愉快！有任何问题随时问我。**
