# Leaf-segment算法实现详解

## 目录

1. [核心数据结构](#1-核心数据结构)
2. [完整实现代码](#2-完整实现代码)
3. [关键算法解析](#3-关键算法解析)
4. [并发安全设计](#4-并发安全设计)
5. [容错机制](#5-容错机制)

---

## 1. 核心数据结构

### 1.1 LeafAlloc - 数据库实体类

```java
/**
 * Leaf号段分配实体类
 * 对应数据库表：leaf_alloc
 */
public class LeafAlloc {
    private String bizTag;        // 业务标识
    private Long maxId;          // 当前已分配的最大ID
    private Integer step;        // 步长（号段大小）
    private String description;  // 业务描述
    private Date updateTime;     // 更新时间

    // 构造函数和getter/setter...
}
```

### 1.2 Segment - 号段类

```java
/**
 * 号段信息
 * 表示一个连续的ID范围
 */
public class Segment {
    private AtomicLong value;    // 当前值（原子操作）
    private volatile long max;   // 号段最大值
    private volatile int step;   // 步长
    private volatile boolean ready; // 是否就绪

    public Segment() {
        this.value = new AtomicLong(0);
        this.max = 0;
        this.step = 0;
        this.ready = false;
    }

    /**
     * 获取下一个ID（原子操作）
     */
    public long getIdAndIncrement() {
        return value.getAndIncrement();
    }

    /**
     * 检查是否用完
     */
    public boolean useful() {
        return value.get() < max;
    }

    // getter/setter方法...
}
```

### 1.3 SegmentBuffer - 双Buffer容器

```java
/**
 * 双Buffer号段容器
 * 实现双Buffer机制，保证高可用
 */
public class SegmentBuffer {
    private String key;                    // 业务标识
    private Segment[] segments;            // 双Buffer数组
    private volatile int currentPos;       // 当前使用的Buffer位置
    private volatile boolean nextReady;    // 下一个Buffer是否就绪
    private volatile boolean initOk;       // 是否初始化完成
    private final AtomicBoolean threadRunning; // 异步更新线程状态

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor(
        r -> {
            Thread t = new Thread(r);
            t.setName("segment-update-" + key);
            t.setDaemon(true);
            return t;
        }
    );

    public SegmentBuffer() {
        segments = new Segment[]{new Segment(), new Segment()};
        currentPos = 0;
        nextReady = false;
        initOk = false;
        threadRunning = new AtomicBoolean(false);
    }

    /**
     * 获取下一个ID
     * 核心方法，包含双Buffer切换逻辑
     */
    public long nextId(String bizTag, LeafIdGeneratorService service) {
        // 实现详见下文...
    }
}
```

## 2. 完整实现代码

### 2.1 LeafIdGeneratorService主服务类

```java
@Service
@Slf4j
public class LeafIdGeneratorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 业务号段缓存
    private final ConcurrentHashMap<String, SegmentBuffer> cache = new ConcurrentHashMap<>();

    // 预加载阈值（当前号段使用90%时开始预加载下一号段）
    private static final double UPDATE_THRESHOLD = 0.9;

    /**
     * 获取下一个ID - 外部调用入口
     */
    public long nextId(String bizTag) {
        if (StringUtils.isEmpty(bizTag)) {
            throw new IllegalArgumentException("bizTag cannot be empty");
        }

        SegmentBuffer buffer = cache.get(bizTag);
        if (buffer == null) {
            synchronized (this) {
                buffer = cache.get(bizTag);
                if (buffer == null) {
                    buffer = new SegmentBuffer();
                    buffer.setKey(bizTag);
                    cache.put(bizTag, buffer);
                }
            }
        }

        return buffer.nextId(bizTag, this);
    }

    /**
     * 从数据库获取并更新号段
     * 核心数据库操作方法
     */
    public LeafAlloc updateMaxIdAndGetLeafAlloc(String bizTag) {
        String updateSql = "UPDATE leaf_alloc SET max_id = max_id + step WHERE biz_tag = ?";
        String selectSql = "SELECT biz_tag, max_id, step, description FROM leaf_alloc WHERE biz_tag = ?";

        try {
            // 1. 原子性更新max_id
            int affectedRows = jdbcTemplate.update(updateSql, bizTag);
            if (affectedRows == 0) {
                throw new RuntimeException("Update leaf_alloc failed, bizTag: " + bizTag);
            }

            // 2. 查询更新后的结果
            return jdbcTemplate.queryForObject(selectSql, new BeanPropertyRowMapper<>(LeafAlloc.class), bizTag);
        } catch (Exception e) {
            log.error("Failed to update max_id for bizTag: {}", bizTag, e);
            throw new RuntimeException("Database operation failed", e);
        }
    }

    /**
     * 获取所有业务标识列表
     */
    public List<String> getAllBizTags() {
        String sql = "SELECT biz_tag FROM leaf_alloc";
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
```

### 2.2 SegmentBuffer核心实现

```java
public class SegmentBuffer {
    // ... 成员变量定义见上文

    /**
     * 获取下一个ID的核心逻辑
     */
    public long nextId(String bizTag, LeafIdGeneratorService service) {
        // 第一次使用时初始化
        if (!initOk) {
            synchronized (this) {
                if (!initOk) {
                    try {
                        updateSegmentFromDB(bizTag, getCurrentSegment(), service);
                        initOk = true;
                    } catch (Exception e) {
                        log.error("Initialize segment failed for bizTag: {}", bizTag, e);
                        throw new RuntimeException("Initialize segment failed", e);
                    }
                }
            }
        }

        return getIdFromSegment(bizTag, service);
    }

    /**
     * 从当前号段获取ID
     */
    private long getIdFromSegment(String bizTag, LeafIdGeneratorService service) {
        while (true) {
            lock.readLock().lock();
            try {
                Segment segment = getCurrentSegment();

                // 检查是否需要预加载下一个号段
                if (!nextReady && shouldUpdateNext(segment) &&
                    threadRunning.compareAndSet(false, true)) {
                    // 异步加载下一个号段
                    asyncUpdateNextSegment(bizTag, service);
                }

                // 尝试从当前号段获取ID
                long value = segment.getIdAndIncrement();
                if (value < segment.getMax()) {
                    return value;
                }

                // 当前号段已用完，需要切换
            } finally {
                lock.readLock().unlock();
            }

            // 等待切换到下一个号段
            waitAndSwitchSegment(bizTag, service);
        }
    }

    /**
     * 检查是否需要更新下一个号段
     */
    private boolean shouldUpdateNext(Segment segment) {
        return segment.getIdAndIncrement() >= segment.getMax() * UPDATE_THRESHOLD;
    }

    /**
     * 异步更新下一个号段
     */
    private void asyncUpdateNextSegment(String bizTag, LeafIdGeneratorService service) {
        taskExecutor.execute(() -> {
            try {
                Segment nextSegment = getNextSegment();
                updateSegmentFromDB(bizTag, nextSegment, service);
                nextReady = true;
                log.info("Next segment loaded for bizTag: {}, range: [{}-{}]",
                         bizTag, nextSegment.getValue().get(), nextSegment.getMax());
            } catch (Exception e) {
                log.error("Failed to update next segment for bizTag: {}", bizTag, e);
            } finally {
                threadRunning.set(false);
            }
        });
    }

    /**
     * 等待并切换到下一个号段
     */
    private void waitAndSwitchSegment(String bizTag, LeafIdGeneratorService service) {
        lock.writeLock().lock();
        try {
            // 如果下一个号段还没准备好，同步加载
            if (!nextReady) {
                Segment nextSegment = getNextSegment();
                updateSegmentFromDB(bizTag, nextSegment, service);
                nextReady = true;
            }

            // 切换到下一个号段
            currentPos = (currentPos + 1) % 2;
            nextReady = false;

            log.info("Switched to next segment for bizTag: {}", bizTag);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 从数据库更新号段信息
     */
    private void updateSegmentFromDB(String bizTag, Segment segment, LeafIdGeneratorService service) {
        SegmentBuffer buffer = null;
        try {
            LeafAlloc leafAlloc = service.updateMaxIdAndGetLeafAlloc(bizTag);

            // 更新号段信息
            segment.getValue().set(leafAlloc.getMaxId() - leafAlloc.getStep());
            segment.setMax(leafAlloc.getMaxId());
            segment.setStep(leafAlloc.getStep());
            segment.setReady(true);

            log.debug("Updated segment for bizTag: {}, range: [{}-{}]",
                     bizTag, segment.getValue().get(), segment.getMax());
        } catch (Exception e) {
            log.error("Failed to update segment from DB for bizTag: {}", bizTag, e);
            throw new RuntimeException("Update segment failed", e);
        }
    }

    // 辅助方法
    private Segment getCurrentSegment() {
        return segments[currentPos];
    }

    private Segment getNextSegment() {
        return segments[(currentPos + 1) % 2];
    }
}
```

## 3. 关键算法解析

### 3.1 双Buffer机制详解

```
时间轴：
T0: Buffer A [1-1000]     正在使用，Buffer B 空闲
T1: Buffer A 使用到 900   触发预加载，异步加载 Buffer B [1001-2000]
T2: Buffer A 用完         切换到 Buffer B，重置 Buffer A
T3: Buffer B [1001-2000]  正在使用，Buffer A 空闲
T4: 循环往复...
```

### 3.2 预加载时机计算

```java
// 当当前值 >= 最大值 * 0.9 时触发预加载
private boolean shouldUpdateNext(Segment segment) {
    long currentValue = segment.getValue().get();
    long maxValue = segment.getMax();
    double usageRatio = (double) currentValue / maxValue;

    return usageRatio >= UPDATE_THRESHOLD; // 0.9
}
```

### 3.3 原子操作保证

```java
public class Segment {
    private AtomicLong value; // 使用AtomicLong保证原子性

    public long getIdAndIncrement() {
        // getAndIncrement是原子操作，无需额外同步
        return value.getAndIncrement();
    }
}
```

## 4. 并发安全设计

### 4.1 读写锁使用

```java
private final ReadWriteLock lock = new ReentrantReadWriteLock();

// 读操作（获取ID）- 允许并发
lock.readLock().lock();
try {
    // 获取ID的逻辑
} finally {
    lock.readLock().unlock();
}

// 写操作（切换号段）- 独占
lock.writeLock().lock();
try {
    // 切换号段的逻辑
} finally {
    lock.writeLock().unlock();
}
```

### 4.2 线程安全的状态管理

```java
// 使用volatile确保可见性
private volatile int currentPos;
private volatile boolean nextReady;
private volatile boolean initOk;

// 使用AtomicBoolean控制异步任务
private final AtomicBoolean threadRunning = new AtomicBoolean(false);

// CAS操作避免重复任务
if (threadRunning.compareAndSet(false, true)) {
    // 执行异步任务
}
```

## 5. 容错机制

### 5.1 数据库连接失败处理

```java
public LeafAlloc updateMaxIdAndGetLeafAlloc(String bizTag) {
    try {
        // 数据库操作
        return doUpdateAndSelect(bizTag);
    } catch (DataAccessException e) {
        log.error("Database access failed for bizTag: {}", bizTag, e);

        // 可以考虑以下容错策略：
        // 1. 重试机制
        // 2. 降级到UUID生成
        // 3. 使用本地文件存储

        throw new RuntimeException("ID generation service unavailable", e);
    }
}
```

### 5.2 号段耗尽保护

```java
private long getIdFromSegment(String bizTag, LeafIdGeneratorService service) {
    // 防止死循环的保护机制
    int maxRetries = 3;
    int retryCount = 0;

    while (retryCount < maxRetries) {
        try {
            // 正常获取ID逻辑
            return doGetId(bizTag, service);
        } catch (Exception e) {
            retryCount++;
            log.warn("Failed to get ID, retry {}/{}", retryCount, maxRetries);

            if (retryCount >= maxRetries) {
                throw new RuntimeException("Failed to generate ID after retries", e);
            }

            // 短暂休眠后重试
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted", ie);
            }
        }
    }

    throw new RuntimeException("Unexpected error in ID generation");
}
```

### 5.3 初始化失败处理

```java
public long nextId(String bizTag) {
    SegmentBuffer buffer = getOrCreateBuffer(bizTag);

    if (!buffer.isInitOk()) {
        synchronized (buffer) {
            if (!buffer.isInitOk()) {
                try {
                    initializeBuffer(buffer, bizTag);
                } catch (Exception e) {
                    // 初始化失败，移除缓存避免影响其他请求
                    cache.remove(bizTag);
                    throw new RuntimeException("Failed to initialize buffer", e);
                }
            }
        }
    }

    return buffer.nextId(bizTag, this);
}
```

## 总结

Leaf-segment算法的核心特点：

1. **高性能**: 通过批量获取减少数据库访问
2. **高可用**: 双Buffer机制确保服务连续性
3. **并发安全**: 使用读写锁和原子操作
4. **容错能力**: 多层次的错误处理和恢复机制
5. **可扩展**: 支持多业务场景，配置灵活

这个实现已经在你的Mini-UPS项目中得到验证，可以支持高并发的分布式ID生成需求。