/**
 * User Data Access Interface
 * 
 * Function Description:
 * - Inherits JpaRepository to provide standard CRUD operations (Create, Read, Update, Delete)
 * - Defines user-related custom query methods
 * - Supports lookup and duplicate verification by username and email
 * 
 * Custom Query Methods:
 * - findByUsername: Find user by username (login verification)
 * - findByEmail: Find user by email (login verification, password reset)
 * - existsByUsername: Check if username already exists (registration verification)
 * - existsByEmail: Check if email already exists (registration verification)
 * 
 * Inherited Standard Methods:
 * - save(): Save or update user
 * - findById(): Find user by ID
 * - findAll(): Query all users
 * - deleteById(): Delete user by ID
 * - count(): Count number of users
 * 
 * Performance Optimization:
 * - Query based on method names will automatically utilize database indexes
 * - Returns Optional to avoid null pointer exceptions
 * - exists methods have better performance than find methods (for existence verification)
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