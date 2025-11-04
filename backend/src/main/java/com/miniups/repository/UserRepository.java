/**
 * User Data Access Mapper Interface
 *
 * Function Description:
 * - MyBatis Mapper interface for user data access
 * - Defines user-related CRUD and custom query methods
 * - Supports lookup and duplicate verification by username and email
 *
 * Custom Query Methods:
 * - findByUsername: Find user by username (login verification)
 * - findByEmail: Find user by email (login verification, password reset)
 * - existsByUsername: Check if username already exists (registration verification)
 * - existsByEmail: Check if email already exists (registration verification)
 *
 * Standard Methods:
 * - insert(): Insert user
 * - update(): Update user
 * - selectById(): Find user by ID
 * - selectAll(): Query all users
 * - deleteById(): Delete user by ID
 *
 * Performance Optimization:
 * - Queries utilize database indexes
 * - Parameterized queries prevent SQL injection
 *
 * Usage Scenarios:
 * - User registration and login verification
 * - User information management
 * - Permission verification and user queries
 *
 *

 */
package com.miniups.repository;

import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import com.miniups.model.enums.AuthProvider;
import org.apache.ibatis.annotations.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Mapper
public interface UserRepository {

    // Basic CRUD operations
    @Insert("INSERT INTO users (username, email, password, first_name, last_name, phone, address, role, enabled, auth_provider, provider_id, created_at, updated_at, version) " +
            "VALUES (#{username}, #{email}, #{password}, #{firstName}, #{lastName}, #{phone}, #{address}, #{role}, #{enabled}, #{authProvider}, #{providerId}, NOW(), NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE users SET username = #{username}, email = #{email}, password = #{password}, " +
            "first_name = #{firstName}, last_name = #{lastName}, phone = #{phone}, address = #{address}, " +
            "role = #{role}, enabled = #{enabled}, auth_provider = #{authProvider}, provider_id = #{providerId}, " +
            "updated_at = NOW(), version = version + 1 WHERE id = #{id} AND version = #{version}")
    int update(User user);

    @Select("SELECT * FROM users WHERE id = #{id}")
    User selectById(Long id);

    @Select("SELECT * FROM users")
    List<User> selectAll();

    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Long id);

    // Custom query methods
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(String email);

    @Select("SELECT * FROM users WHERE phone = #{phone}")
    User findByPhone(String phone);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username}")
    boolean existsByUsername(String username);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email}")
    boolean existsByEmail(String email);

    @Select("SELECT * FROM users WHERE role = #{role}")
    List<User> findByRole(@Param("role") UserRole role);

    @Select("SELECT COUNT(*) FROM users WHERE role = #{role}")
    long countByRole(@Param("role") UserRole role);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email} AND id != #{id}")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Long id);

    // OAuth2 related queries
    @Select("SELECT * FROM users WHERE auth_provider = #{authProvider} AND provider_id = #{providerId}")
    User findByAuthProviderAndProviderId(@Param("authProvider") AuthProvider authProvider, @Param("providerId") String providerId);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE auth_provider = #{authProvider} AND provider_id = #{providerId}")
    boolean existsByAuthProviderAndProviderId(@Param("authProvider") AuthProvider authProvider, @Param("providerId") String providerId);

    @Select("SELECT * FROM users WHERE auth_provider = #{authProvider}")
    List<User> findByAuthProvider(@Param("authProvider") AuthProvider authProvider);

    @Select("SELECT COUNT(*) FROM users WHERE auth_provider = #{authProvider}")
    long countByAuthProvider(@Param("authProvider") AuthProvider authProvider);

    @Select("SELECT COUNT(*) FROM users")
    long count();

    // Additional methods for service layer compatibility
    @Select("SELECT * FROM users")
    List<User> findAll();

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    // Pageable methods - will work with PageHelper interceptor
    @Select("SELECT * FROM users")
    Page<User> findAll(Pageable pageable);

    @Select("SELECT * FROM users WHERE role = #{role}")
    Page<User> findByRole(@Param("role") UserRole role, Pageable pageable);
}
