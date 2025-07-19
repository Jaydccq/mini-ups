/**
 * 用户数据访问接口
 * 
 * 功能说明：
 * - 继承JpaRepository提供标准CRUD操作（增删改查）
 * - 定义用户相关的自定义查询方法
 * - 支持按用户名、邮箱查找和验证重复性
 * 
 * 自定义查询方法：
 * - findByUsername: 根据用户名查找用户（登录验证）
 * - findByEmail: 根据邮箱查找用户（登录验证、重置密码）
 * - existsByUsername: 检查用户名是否已存在（注册验证）
 * - existsByEmail: 检查邮箱是否已存在（注册验证）
 * 
 * 继承的标准方法：
 * - save(): 保存或更新用户
 * - findById(): 根据ID查找用户
 * - findAll(): 查询所有用户
 * - deleteById(): 根据ID删除用户
 * - count(): 统计用户数量
 * 
 * 性能优化：
 * - 基于方法名的查询会自动利用数据库索引
 * - 返回Optional避免null指针异常
 * - exists方法比find方法性能更好（用于验证存在性）
 * 
 * 使用场景：
 * - 用户注册和登录验证
 * - 用户信息管理
 * - 权限验证和用户查询
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.repository;

import com.miniups.model.entity.User;
import com.miniups.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    Page<User> findByRole(UserRole role, Pageable pageable);
    
    List<User> findByRole(UserRole role);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") UserRole role);
    
    boolean existsByEmailAndIdNot(String email, Long id);
}