package com.miniups.service.id;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 双缓冲段管理器
 * 
 * 实现美团Leaf-Segment的双Buffer机制：
 * - Buffer0 和 Buffer1 轮换使用
 * - 当前Buffer使用到75%时，异步预加载下一个Buffer
 * - 确保ID生成的连续性和高性能
 * 
 * 核心设计：
 * - 双Buffer确保无缝切换
 * - 异步预加载避免阻塞
 * - 线程安全的原子操作
 * 
 * @author Mini-UPS Team
 */
public class SegmentBuffer {
    
    /**
     * 业务标识
     */
    private final String bizTag;
    
    /**
     * 双缓冲区
     */
    private final Segment[] segments = new Segment[]{new Segment(), new Segment()};
    
    /**
     * 当前使用的缓冲区索引 (0 或 1)
     */
    private volatile int currentIndex = 0;
    
    /**
     * 下一个缓冲区是否正在加载
     */
    private final AtomicBoolean nextSegmentLoading = new AtomicBoolean(false);
    
    /**
     * 下一个缓冲区是否已准备就绪
     */
    private final AtomicBoolean nextSegmentReady = new AtomicBoolean(false);
    
    /**
     * 初始化状态
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    /**
     * 线程安全的切换锁
     */
    private final Object switchLock = new Object();
    
    public SegmentBuffer(String bizTag) {
        this.bizTag = bizTag;
    }
    
    /**
     * 初始化第一个段
     * 
     * @param start 起始值
     * @param end 结束值
     */
    public void initFirstSegment(long start, long end) {
        segments[0].init(bizTag, start, end);
        currentIndex = 0;
        nextSegmentReady.set(false);
        initialized.set(true);
    }
    
    /**
     * 预加载下一个段
     * 
     * @param start 起始值  
     * @param end 结束值
     */
    public void preloadNextSegment(long start, long end) {
        int nextIndex = getNextIndex();
        segments[nextIndex].init(bizTag, start, end);
        nextSegmentReady.set(true);
        nextSegmentLoading.set(false);
    }
    
    /**
     * 获取下一个ID
     * 
     * @return 下一个可用ID，失败返回-1
     */
    public long nextId() {
        if (!initialized.get()) {
            return -1;
        }
        
        Segment currentSegment = getCurrentSegment();
        
        // 检查是否需要预加载下一个段
        if (currentSegment.shouldPreload() && !nextSegmentLoading.get() && !nextSegmentReady.get()) {
            nextSegmentLoading.set(true);
            // 触发异步预加载（调用方需要处理）
        }
        
        long nextId = currentSegment.nextId();
        
        // 如果当前段已用尽，尝试切换到下一个段
        if (nextId == -1) {
            return switchToNextSegment();
        }
        
        return nextId;
    }
    
    /**
     * 切换到下一个段
     * 
     * @return 新段的第一个ID，失败返回-1
     */
    private long switchToNextSegment() {
        synchronized (switchLock) {
            // 双重检查，可能在等待锁期间已经被其他线程切换了
            Segment currentSegment = getCurrentSegment();
            long nextId = currentSegment.nextId();
            if (nextId != -1) {
                return nextId;
            }
            
            // 检查下一个段是否准备就绪
            if (!nextSegmentReady.get()) {
                return -1; // 下一个段还未准备好
            }
            
            // 切换到下一个段
            currentIndex = getNextIndex();
            nextSegmentReady.set(false);
            
            // 从新段获取ID
            return getCurrentSegment().nextId();
        }
    }
    
    /**
     * 获取当前段
     */
    public Segment getCurrentSegment() {
        return segments[currentIndex];
    }
    
    /**
     * 获取下一个段
     */
    public Segment getNextSegment() {
        return segments[getNextIndex()];
    }
    
    /**
     * 获取下一个索引
     */
    private int getNextIndex() {
        return 1 - currentIndex;
    }
    
    /**
     * 检查是否需要预加载
     * 
     * @return true表示需要预加载下一个段
     */
    public boolean needsPreload() {
        return initialized.get() && 
               getCurrentSegment().shouldPreload() && 
               !nextSegmentLoading.get() && 
               !nextSegmentReady.get();
    }
    
    /**
     * 标记正在加载下一个段
     */
    public boolean markLoading() {
        return nextSegmentLoading.compareAndSet(false, true);
    }
    
    /**
     * 取消加载状态（用于预加载失败时复位）
     */
    public void cancelLoading() {
        nextSegmentLoading.set(false);
    }
    
    /**
     * 获取缓冲区状态信息
     */
    public SegmentBufferStatus getStatus() {
        Segment current = getCurrentSegment();
        Segment next = getNextSegment();
        
        return new SegmentBufferStatus(
            bizTag,
            currentIndex,
            current.toString(),
            nextSegmentReady.get() ? next.toString() : "Not ready",
            nextSegmentLoading.get(),
            nextSegmentReady.get(),
            initialized.get()
        );
    }
    
    /**
     * 缓冲区状态信息
     */
    public static class SegmentBufferStatus {
        private final String bizTag;
        private final int currentIndex;
        private final String currentSegmentInfo;
        private final String nextSegmentInfo;
        private final boolean loading;
        private final boolean nextReady;
        private final boolean initialized;
        
        public SegmentBufferStatus(String bizTag, int currentIndex, String currentSegmentInfo, 
                                 String nextSegmentInfo, boolean loading, boolean nextReady, boolean initialized) {
            this.bizTag = bizTag;
            this.currentIndex = currentIndex;
            this.currentSegmentInfo = currentSegmentInfo;
            this.nextSegmentInfo = nextSegmentInfo;
            this.loading = loading;
            this.nextReady = nextReady;
            this.initialized = initialized;
        }
        
        // Getters
        public String getBizTag() { return bizTag; }
        public int getCurrentIndex() { return currentIndex; }
        public String getCurrentSegmentInfo() { return currentSegmentInfo; }
        public String getNextSegmentInfo() { return nextSegmentInfo; }
        public boolean isLoading() { return loading; }
        public boolean isNextReady() { return nextReady; }
        public boolean isInitialized() { return initialized; }
        
        @Override
        public String toString() {
            return String.format("SegmentBuffer{bizTag='%s', currentIndex=%d, current=[%s], next=[%s], loading=%b, nextReady=%b, initialized=%b}",
                bizTag, currentIndex, currentSegmentInfo, nextSegmentInfo, loading, nextReady, initialized);
        }
    }
    
    public String getBizTag() {
        return bizTag;
    }
    
    public boolean isInitialized() {
        return initialized.get();
    }
}
