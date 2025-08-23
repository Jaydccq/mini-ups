package com.miniups.worldsim.model.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * Base entity class providing common fields for all World Simulator entities.
 * 
 * This abstract base class implements the common pattern for entity classes
 * in the World Simulator, providing:
 * - Primary key generation using PostgreSQL sequences
 * - Automatic audit timestamps for creation and modification
 * - Optimistic locking with version field
 * - Common equals/hashCode/toString implementations
 * 
 * All domain entities should extend this class to ensure consistency
 * across the data model and proper audit trail maintenance.
 * 
 * @author World Simulator Team
 * @version 1.0.0
 * @since 2025-01-13
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Protected constructor for JPA and subclasses.
     */
    protected BaseEntity() {
        // Default constructor for JPA
    }

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

    /**
     * Indicates whether this entity has been persisted (has a non-null ID).
     * 
     * @return true if the entity has been persisted, false otherwise
     */
    public boolean isPersistent() {
        return id != null;
    }

    /**
     * Indicates whether this entity is transient (has a null ID).
     * 
     * @return true if the entity is transient, false otherwise
     */
    public boolean isTransient() {
        return id == null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        BaseEntity that = (BaseEntity) obj;
        
        // If both entities are transient, they are not equal unless they are the same instance
        if (isTransient() || that.isTransient()) {
            return false;
        }
        
        // If both entities are persistent, compare by ID
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        // Use a constant hash code for transient entities to ensure they work properly in collections
        // This is safe because transient entities should not be stored in hash-based collections
        // across persistence operations
        return isTransient() ? getClass().hashCode() : id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s{id=%d, version=%d, createdAt=%s, updatedAt=%s}", 
            getClass().getSimpleName(),
            id,
            version,
            createdAt,
            updatedAt);
    }
}