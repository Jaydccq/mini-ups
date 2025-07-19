/**
 * 基础实体抽象类
 * 
 * 功能说明：
 * - 为所有数据库实体提供通用字段和行为
 * - 自动管理实体的创建时间、更新时间、版本控制
 * - 使用JPA审计功能自动填充时间戳
 * 
 * 通用字段：
 * - id: 主键，自增长策略
 * - createdAt: 创建时间，插入时自动设置，不可更新
 * - updatedAt: 更新时间，每次修改时自动更新
 * - version: 乐观锁版本号，防止并发修改冲突
 * 
 * 设计优势：
 * - 避免在每个实体中重复定义通用字段
 * - 统一的审计日志记录
 * - 支持乐观锁机制保证数据一致性
 * - 自动化的时间戳管理
 * 
 * 使用方式：
 * - 所有业务实体类继承此基类
 * - 需要在主类上添加@EnableJpaAuditing启用审计功能
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
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