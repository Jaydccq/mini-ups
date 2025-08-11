/**
 * Base Entity Abstract Class
 * 
 * Function Description:
 * - Provides common fields and behavior for all database entities
 * - Automatically manages entity creation time, update time, and version control
 * - Uses JPA auditing features to automatically populate timestamps
 * 
 * Common Fields:
 * - id: Primary key with auto-increment strategy
 * - createdAt: Creation time, automatically set on insert, not updatable
 * - updatedAt: Update time, automatically updated on each modification
 * - version: Optimistic lock version number to prevent concurrent modification conflicts
 * 
 * Design Advantages:
 * - Avoids repetitive definition of common fields in each entity
 * - Unified audit log recording
 * - Supports optimistic locking mechanism to ensure data consistency
 * - Automated timestamp management
 * 
 * Usage:
 * - All business entity classes inherit from this base class
 * - Need to add @EnableJpaAuditing on the main class to enable auditing functionality
 * 
 *
 
 */
package com.miniups.model.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    // Constructors
    public BaseEntity() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}