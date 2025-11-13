/**
 * Base Entity Abstract Class
 *
 * Function Description:
 * - Provides common fields and behavior for all database entities
 * - Manages entity creation time, update time, and version control
 *
 * Common Fields:
 * - id: Primary key with auto-increment strategy
 * - createdAt: Creation time
 * - updatedAt: Update time
 * - version: Optimistic lock version number to prevent concurrent modification conflicts
 *
 * Design Advantages:
 * - Avoids repetitive definition of common fields in each entity
 * - Unified audit log recording
 * - Supports optimistic locking mechanism to ensure data consistency
 *
 * Usage:
 * - All business entity classes inherit from this base class
 *
 *

 */
package com.miniups.model.entity;

import java.time.LocalDateTime;

public abstract class BaseEntity {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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