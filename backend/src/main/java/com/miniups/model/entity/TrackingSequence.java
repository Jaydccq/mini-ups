package com.miniups.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 追踪号序列实体
 * 
 * 基于美团Leaf-Segment模式的序列管理实体
 * 支持高并发分布式ID生成
 * 
 * 数据库表：tracking_sequences
 * 
 * @author Mini-UPS Team
 */
@Entity
@Table(name = "tracking_sequences")
public class TrackingSequence {
    
    /**
     * 业务标识，主键
     * 用于区分不同业务的ID生成需求
     */
    @Id
    @Column(name = "biz_tag", length = 128, nullable = false)
    private String bizTag;
    
    /**
     * 当前已分配出去的最大ID
     * 每次分配新段时，这个值会增加step
     */
    @Column(name = "max_id", nullable = false)
    private Long maxId = 0L;
    
    /**
     * 每次分配的步长
     * 决定每次批量分配多少个ID
     */
    @Column(name = "step", nullable = false)
    private Integer step = 1000;
    
    /**
     * 业务描述
     */
    @Column(name = "description")
    private String description;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 默认构造函数
     */
    public TrackingSequence() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 构造函数
     */
    public TrackingSequence(String bizTag, Integer step, String description) {
        this();
        this.bizTag = bizTag;
        this.step = step;
        this.description = description;
    }
    
    /**
     * 更新前置处理
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 创建前置处理
     */
    @PrePersist  
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }
    
    /**
     * 计算下一个段的起始和结束值
     * 
     * @return 长度为2的数组，[0]为起始值，[1]为结束值
     */
    public long[] calculateNextSegment() {
        long start = maxId + 1;
        long end = maxId + step;
        return new long[]{start, end};
    }
    
    /**
     * 应用新的段分配
     * 
     * @param newMaxId 新的最大ID值
     */
    public void applySegmentAllocation(long newMaxId) {
        this.maxId = newMaxId;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public String getBizTag() {
        return bizTag;
    }
    
    public void setBizTag(String bizTag) {
        this.bizTag = bizTag;
    }
    
    public Long getMaxId() {
        return maxId;
    }
    
    public void setMaxId(Long maxId) {
        this.maxId = maxId;
    }
    
    public Integer getStep() {
        return step;
    }
    
    public void setStep(Integer step) {
        this.step = step;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    @Override
    public String toString() {
        return String.format("TrackingSequence{bizTag='%s', maxId=%d, step=%d, description='%s', updatedAt=%s}",
            bizTag, maxId, step, description, updatedAt);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        TrackingSequence that = (TrackingSequence) o;
        return bizTag != null ? bizTag.equals(that.bizTag) : that.bizTag == null;
    }
    
    @Override
    public int hashCode() {
        return bizTag != null ? bizTag.hashCode() : 0;
    }
}