package com.miniups.service.id;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成段结构
 * 
 * 基于美团Leaf-Segment模式设计
 * 每个Segment代表一个ID范围，支持线程安全的原子递增
 * 
 * 核心特性：
 * - 线程安全的原子计数器
 * - 支持预加载和缓冲
 * - 高性能内存操作
 * 
 * @author Mini-UPS Team
 */
public class Segment {
    
    /**
     * 当前段的起始值（包含）
     */
    private volatile long start;
    
    /**
     * 当前段的结束值（包含）
     */
    private volatile long end;
    
    /**
     * 原子计数器，当前已分配到的位置
     */
    private final AtomicLong currentPos = new AtomicLong(-1);
    
    /**
     * 段长度（end - start + 1）
     */
    private volatile long step;
    
    /**
     * 业务标识
     */
    private volatile String bizTag;
    
    /**
     * 创建时间戳
     */
    private volatile long createTime;
    
    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;
    
    public Segment() {
        this.createTime = System.currentTimeMillis();
    }
    
    /**
     * 初始化段信息
     * 
     * @param bizTag 业务标识
     * @param start 起始值
     * @param end 结束值
     */
    public void init(String bizTag, long start, long end) {
        this.bizTag = bizTag;
        this.start = start;
        this.end = end;
        this.step = end - start + 1;
        this.currentPos.set(start - 1); // 设置为start-1，这样第一次increment返回start
        this.initialized = true;
    }
    
    /**
     * 获取下一个ID
     * 
     * @return 下一个可用的ID，如果段已用尽则返回-1
     */
    public long nextId() {
        if (!initialized) {
            return -1;
        }
        
        long current = currentPos.incrementAndGet();
        
        // 检查是否超出范围
        if (current > end) {
            return -1; // 段已用尽
        }
        
        return current;
    }
    
    /**
     * 检查段是否已用尽
     * 
     * @return true表示已用尽
     */
    public boolean isExhausted() {
        return currentPos.get() >= end;
    }
    
    /**
     * 获取剩余可用ID数量
     * 
     * @return 剩余数量
     */
    public long getRemaining() {
        if (!initialized) {
            return 0;
        }
        
        long current = currentPos.get();
        return Math.max(0, end - current);
    }
    
    /**
     * 获取使用率（百分比）
     * 
     * @return 0.0-100.0的使用率
     */
    public double getUsagePercentage() {
        if (!initialized || step == 0) {
            return 0.0;
        }
        
        long current = currentPos.get();
        long used = Math.max(0, current - start + 1);
        return (double) used / step * 100.0;
    }
    
    /**
     * 检查是否应该触发预加载下一个段
     * 默认当使用率达到75%时触发
     * 
     * @return true表示应该预加载
     */
    public boolean shouldPreload() {
        return getUsagePercentage() >= 75.0;
    }
    
    /**
     * 检查是否应该触发预加载下一个段（可配置阈值）
     * 
     * @param threshold 预加载阈值（0.0-100.0）
     * @return true表示应该预加载
     */
    public boolean shouldPreload(double threshold) {
        return getUsagePercentage() >= threshold;
    }
    
    /**
     * 重置段（用于复用对象）
     */
    public void reset() {
        this.start = 0;
        this.end = 0;
        this.currentPos.set(-1);
        this.step = 0;
        this.bizTag = null;
        this.initialized = false;
        this.createTime = System.currentTimeMillis();
    }
    
    // Getters and Setters
    
    public long getStart() {
        return start;
    }
    
    public long getEnd() {
        return end;
    }
    
    public long getCurrentPos() {
        return currentPos.get();
    }
    
    public long getStep() {
        return step;
    }
    
    public String getBizTag() {
        return bizTag;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public String toString() {
        return String.format("Segment{bizTag='%s', start=%d, end=%d, current=%d, remaining=%d, usage=%.1f%%}", 
            bizTag, start, end, currentPos.get(), getRemaining(), getUsagePercentage());
    }
}