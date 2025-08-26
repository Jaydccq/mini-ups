package com.miniups.service.id;

import com.miniups.repository.TrackingSequenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;

/**
 * 基于美团Leaf-Segment的高性能ID生成器
 * 
 * 核心特性：
 * - 双缓冲机制确保无缝ID生成
 * - 异步预加载避免阻塞主流程
 * - 支持多业务类型的ID生成
 * - QPS可达5万+（美团实测数据）
 * 
 * 工作原理：
 * 1. 应用启动时初始化第一个段
 * 2. ID生成过程中当使用率达到75%时异步预加载下一个段
 * 3. 当前段用尽时无缝切换到已预加载的段
 * 4. 所有操作都在内存中完成，只有段切换时才访问数据库
 * 
 * @author Mini-UPS Team
 */
@Service
public class LeafSegmentIdGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(LeafSegmentIdGenerator.class);
    
    /**
     * 业务缓冲区映射
     * Key: 业务标识（如"tracking_number"）
     * Value: 对应的双缓冲管理器
     */
    private final ConcurrentHashMap<String, SegmentBuffer> bufferMap = new ConcurrentHashMap<>();
    
    /**
     * 缓冲区创建锁（避免synchronized(this)的潜在风险）
     */
    private final Object bufferCreationLock = new Object();
    
    /**
     * 12位数字最大值常量
     */
    private static final long TWELVE_DIGIT_MAX = 1_000_000_000_000L; // 10^12
    
    @Autowired
    private TrackingSequenceRepository sequenceRepository;
    
    @Autowired
    @Qualifier("applicationTaskExecutor")
    private Executor taskExecutor;
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    
    /**
     * 默认业务标识
     */
    private static final String DEFAULT_BIZ_TAG = "tracking_number";
    
    /**
     * 初始化方法
     * 系统启动时确保追踪号序列已准备就绪
     */
    @PostConstruct
    public void init() {
        try {
            // 确保追踪号序列已初始化
            initializeBizTagIfNeeded(DEFAULT_BIZ_TAG, 2000, "UPS追踪号序列生成");
            
            logger.info("LeafSegmentIdGenerator initialized successfully with bizTag: {}", DEFAULT_BIZ_TAG);
        } catch (Exception e) {
            logger.error("Failed to initialize LeafSegmentIdGenerator", e);
        }
    }
    
    /**
     * 生成追踪号
     * 
     * @return 格式化的追踪号，格式：UPS + 12位序列号（无时间戳）
     */
    public String generateTrackingNumber() {
        return generateFormattedId(DEFAULT_BIZ_TAG, "UPS", false);
    }
    
    /**
     * 生成指定业务的格式化ID
     * 
     * @param bizTag 业务标识
     * @param prefix 前缀（如"UPS"）
     * @param includeTimestamp 是否包含时间戳
     * @return 格式化的ID
     */
    public String generateFormattedId(String bizTag, String prefix, boolean includeTimestamp) {
        long id = generateId(bizTag);
        if (id == -1) {
            throw new RuntimeException("Failed to generate ID for bizTag: " + bizTag);
        }
        
        StringBuilder sb = new StringBuilder(prefix);
        
        if (includeTimestamp) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            sb.append(timestamp);
        }
        
        // 添加12位序列号，防止溢出
        if (id >= TWELVE_DIGIT_MAX) {
            logger.warn("Generated ID {} for bizTag {} exceeds 12 digits, applying modulo", id, bizTag);
        }
        long safeId = id % TWELVE_DIGIT_MAX;
        sb.append(String.format("%012d", safeId));
        
        return sb.toString();
    }
    
    /**
     * 生成原始ID
     * 
     * @param bizTag 业务标识
     * @return 原始ID，失败返回-1
     */
    public long generateId(String bizTag) {
        SegmentBuffer buffer = getOrCreateBuffer(bizTag);
        
        // 检查是否需要预加载
        if (buffer.needsPreload()) {
            if (buffer.markLoading()) {
                // 异步预加载下一个段
                preloadNextSegmentAsync(buffer);
            }
        }
        
        long id = buffer.nextId();

        // 如果获取失败，可能是段用尽但下一个段还未准备好
        if (id == -1) {
            logger.warn("Failed to get ID from buffer for bizTag: {}, buffer status: {}", 
                       bizTag, buffer.getStatus());
            
            // 尝试同步加载下一个段作为应急措施
            if (loadNextSegmentSync(buffer)) {
                id = buffer.nextId();
            }
        }
        
        if (id != -1) {
            recordGenerated(bizTag);
        }
        return id;
    }
    
    /**
     * 获取或创建缓冲区
     * 
     * @param bizTag 业务标识
     * @return 对应的缓冲区
     */
    private SegmentBuffer getOrCreateBuffer(String bizTag) {
        SegmentBuffer buffer = bufferMap.get(bizTag);
        if (buffer != null && buffer.isInitialized()) {
            return buffer;
        }
        
        // 双重检查锁定模式 - 使用专用锁对象
        synchronized (bufferCreationLock) {
            buffer = bufferMap.get(bizTag);
            if (buffer != null && buffer.isInitialized()) {
                return buffer;
            }
            
            // 创建新的缓冲区并初始化
            buffer = new SegmentBuffer(bizTag);
            
            // 确保数据库中有对应的序列记录
            initializeBizTagIfNeeded(bizTag, 2000, "Auto-created sequence for " + bizTag);
            
            // 加载第一个段
            if (loadNextSegmentSync(buffer)) {
                bufferMap.put(bizTag, buffer);
                logger.info("Created and initialized new buffer for bizTag: {}", bizTag);
                // 注册Gauge（段剩余量）
                registerGauges(bizTag, buffer);
            } else {
                throw new RuntimeException("Failed to initialize buffer for bizTag: " + bizTag);
            }
            
            return buffer;
        }
    }
    
    /**
     * 异步预加载下一个段
     * 
     * @param buffer 缓冲区
     */
    public void preloadNextSegmentAsync(SegmentBuffer buffer) {
        CompletableFuture.runAsync(() -> {
            try {
                loadNextSegmentSync(buffer);
                logger.debug("Async preloaded next segment for bizTag: {}", buffer.getBizTag());
            } catch (Exception e) {
                logger.error("Failed to preload next segment for bizTag: {}", buffer.getBizTag(), e);
                // 预加载失败时复位加载状态，避免卡死
                buffer.cancelLoading();
            }
        }, taskExecutor);
    }
    
    /**
     * 同步加载下一个段
     * 
     * @param buffer 缓冲区
     * @return 加载成功返回true
     */
    private boolean loadNextSegmentSync(SegmentBuffer buffer) {
        long startTimeNanos = System.nanoTime(); // 重命名避免变量遮蔽
        boolean success = false;
        try {
            TrackingSequenceRepository.SegmentInfo segmentInfo = 
                sequenceRepository.getNextSegment(buffer.getBizTag());
            
            if (segmentInfo == null) {
                logger.error("No sequence found for bizTag: {}", buffer.getBizTag());
                return false;
            }
            
            long end = segmentInfo.getMaxId();
            int step = segmentInfo.getStep();
            long segmentStart = end - step + 1; // 重命名避免变量遮蔽
            
            if (!buffer.isInitialized()) {
                // 初始化第一个段
                buffer.initFirstSegment(segmentStart, end);
                logger.info("Initialized first segment for bizTag: {} with range [{}, {}]", 
                           buffer.getBizTag(), segmentStart, end);
            } else {
                // 预加载下一个段
                buffer.preloadNextSegment(segmentStart, end);
                logger.debug("Preloaded next segment for bizTag: {} with range [{}, {}]", 
                           buffer.getBizTag(), segmentStart, end);
            }
            success = true;
            recordPreloadSuccess(buffer.getBizTag());
            return true;
            
        } catch (Exception e) {
            logger.error("Error loading segment for bizTag: {}", buffer.getBizTag(), e);
            return false;
        } finally {
            recordPreloadMetrics(buffer.getBizTag(), System.nanoTime() - startTimeNanos, success);
        }
    }
    
    /**
     * 如果需要则初始化业务标识
     * 
     * @param bizTag 业务标识
     * @param step 步长
     * @param description 描述
     */
    private void initializeBizTagIfNeeded(String bizTag, int step, String description) {
        // 直接使用幂等的INSERT ... ON CONFLICT，避免竞态条件
        int result = sequenceRepository.initializeSequence(bizTag, step, description);
        if (result > 0) {
            logger.info("Initialized new sequence for bizTag: {} with step: {}", bizTag, step);
        }
    }
    
    /**
     * 获取指定业务的缓冲区状态
     * 
     * @param bizTag 业务标识
     * @return 缓冲区状态，如果不存在返回null
     */
    public SegmentBuffer.SegmentBufferStatus getBufferStatus(String bizTag) {
        SegmentBuffer buffer = bufferMap.get(bizTag);
        return buffer != null ? buffer.getStatus() : null;
    }
    
    /**
     * 获取所有缓冲区状态
     * 
     * @return 所有缓冲区的状态信息
     */
    public ConcurrentHashMap<String, SegmentBuffer.SegmentBufferStatus> getAllBufferStatuses() {
        ConcurrentHashMap<String, SegmentBuffer.SegmentBufferStatus> statuses = new ConcurrentHashMap<>();
        
        bufferMap.forEach((bizTag, buffer) -> {
            statuses.put(bizTag, buffer.getStatus());
        });
        
        return statuses;
    }
    
    /**
     * 动态调整步长
     * 
     * @param bizTag 业务标识
     * @param newStep 新步长
     * @return 调整成功返回true
     */
    public boolean adjustStep(String bizTag, int newStep) {
        try {
            int result = sequenceRepository.updateStep(bizTag, newStep);
            if (result > 0) {
                logger.info("Adjusted step for bizTag: {} to: {}", bizTag, newStep);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to adjust step for bizTag: {}", bizTag, e);
            return false;
        }
    }
    
    /**
     * 清理指定业务的缓冲区（主要用于测试）
     * 
     * @param bizTag 业务标识
     */
    public void clearBuffer(String bizTag) {
        bufferMap.remove(bizTag);
        logger.info("Cleared buffer for bizTag: {}", bizTag);
    }
    
    /**
     * 获取缓冲区数量
     * 
     * @return 当前缓冲区数量
     */
    public int getBufferCount() {
        return bufferMap.size();
    }
    
    private void registerGauges(String bizTag, SegmentBuffer buffer) {
        if (meterRegistry == null) return;
        try {
            Gauge.builder("leaf.segment.remaining", buffer, b -> (double) b.getCurrentSegment().getRemaining())
                .description("Remaining IDs in current segment")
                .tag("biz_tag", bizTag)
                .register(meterRegistry);
        } catch (Exception ignore) { }
    }
    
    private void recordPreloadMetrics(String bizTag, long nanos, boolean success) {
        if (meterRegistry == null) return;
        try {
            Timer.builder("leaf.segment.preload")
                .description("Segment preload duration")
                .tag("biz_tag", bizTag)
                .register(meterRegistry)
                .record(nanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            if (!success) {
                Counter.builder("leaf.segment.preload.failures")
                    .description("Segment preload failures")
                    .tag("biz_tag", bizTag)
                    .register(meterRegistry)
                    .increment();
            }
        } catch (Exception ignore) { }
    }

    private void recordPreloadSuccess(String bizTag) {
        if (meterRegistry == null) return;
        try {
            Counter.builder("leaf.segment.preload.successes")
                .description("Segment preload successes")
                .tag("biz_tag", bizTag)
                .register(meterRegistry)
                .increment();
        } catch (Exception ignore) { }
    }

    private void recordGenerated(String bizTag) {
        if (meterRegistry == null) return;
        try {
            Counter.builder("leaf.segment.generated.total")
                .description("Total generated IDs")
                .tag("biz_tag", bizTag)
                .register(meterRegistry)
                .increment();
        } catch (Exception ignore) { }
    }
}
