import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.sql.*;

/**
 * Leaf-Segment 集成测试 - 测试实际项目中的数据库集成版本
 * 
 * 这个测试模拟了与项目中 LeafSegmentIdGenerator 相同的工作原理，
 * 包括数据库交互和双缓冲机制。
 * 
 * 预期性能目标：
 * - QPS > 10,000 (考虑数据库I/O)
 * - 100% 唯一性保证
 * - 低延迟响应
 * 
 * @author Mini-UPS Team
 */
public class LeafSegmentIntegrationTest {
    
    private static final int WARMUP_SECONDS = 3;
    private static final int TEST_DURATION_SECONDS = 10;
    private static final int[] THREAD_COUNTS = {10, 20, 50, 100, 200};
    
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5431/ups_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "abc123";
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Leaf-Segment 集成测试 (带数据库) ===\n");
        
        LeafSegmentIntegrationTest tester = new LeafSegmentIntegrationTest();
        
        // 检查数据库连接
        if (!tester.checkDatabaseConnection()) {
            System.err.println("❌ 数据库连接失败，请确保PostgreSQL已启动并且数据库存在");
            System.err.println("连接信息: " + JDBC_URL);
            System.err.println("用户: " + DB_USER);
            return;
        }
        
        System.out.println("✅ 数据库连接成功\n");
        
        // 初始化数据库表
        tester.initializeDatabase();
        
        // 运行不同线程数的测试
        for (int threadCount : THREAD_COUNTS) {
            System.out.println("🧵 测试线程数: " + threadCount);
            tester.runIntegrationTest(threadCount);
            System.out.println();
            
            // 让系统稍作休息
            Thread.sleep(1000);
        }
        
        System.out.println("=== 集成测试完成 ===");
    }
    
    /**
     * 检查数据库连接
     */
    private boolean checkDatabaseConnection() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * 初始化数据库表和序列
     */
    private void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            // 创建或更新tracking_sequences表
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS tracking_sequences (
                    biz_tag VARCHAR(128) NOT NULL PRIMARY KEY,
                    max_id BIGINT NOT NULL DEFAULT 0,
                    step INTEGER NOT NULL DEFAULT 2000,
                    description VARCHAR(256),
                    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            stmt.execute(createTableSql);
            
            // 初始化测试序列
            String insertSql = """
                INSERT INTO tracking_sequences (biz_tag, max_id, step, description) 
                VALUES ('test_tracking', 0, 2000, 'QPS测试序列')
                ON CONFLICT (biz_tag) DO UPDATE SET 
                    max_id = 0,
                    step = 2000,
                    updated_time = CURRENT_TIMESTAMP
                """;
            stmt.execute(insertSql);
            
            System.out.println("✅ 数据库表和序列初始化完成");
        }
    }
    
    /**
     * 运行指定线程数的集成测试
     */
    private void runIntegrationTest(int threadCount) throws InterruptedException {
        DatabaseLeafSegmentGenerator generator = new DatabaseLeafSegmentGenerator();
        
        // 预热阶段
        System.out.println("  🔥 预热中...");
        runTest(generator, threadCount, WARMUP_SECONDS, false);
        
        // 正式测试
        System.out.println("  ⚡ 正式测试中...");
        TestResult result = runTest(generator, threadCount, TEST_DURATION_SECONDS, true);
        
        // 输出结果
        printResult(result, threadCount);
        
        // 清理资源
        generator.shutdown();
    }
    
    /**
     * 执行测试
     */
    private TestResult runTest(DatabaseLeafSegmentGenerator generator, int threadCount, 
                              int duration, boolean collectStats) throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(threadCount);
        
        LongAdder totalOperations = new LongAdder();
        LongAdder successOperations = new LongAdder();
        LongAdder dbOperations = new LongAdder();
        Set<Long> generatedIds = collectStats ? ConcurrentHashMap.newKeySet() : null;
        
        Instant testStart = Instant.now();
        
        // 启动工作线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            long id = generator.generateId();
                            totalOperations.increment();
                            
                            if (id > 0) {
                                successOperations.increment();
                                if (collectStats && generatedIds != null) {
                                    generatedIds.add(id);
                                }
                            }
                        } catch (Exception e) {
                            // 继续测试
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    stopLatch.countDown();
                }
            });
        }
        
        // 开始测试
        startLatch.countDown();
        Thread.sleep(duration * 1000);
        
        // 停止测试
        executor.shutdownNow();
        stopLatch.await(5, TimeUnit.SECONDS);
        
        Instant testEnd = Instant.now();
        Duration actualDuration = Duration.between(testStart, testEnd);
        
        return new TestResult(
            totalOperations.sum(),
            successOperations.sum(),
            actualDuration.toMillis(),
            collectStats ? generatedIds.size() : 0,
            generator.getDbOperationCount()
        );
    }
    
    /**
     * 打印测试结果
     */
    private void printResult(TestResult result, int threadCount) {
        double durationSeconds = result.durationMs / 1000.0;
        double qps = result.successOperations / durationSeconds;
        double successRate = (double) result.successOperations / result.totalOperations * 100;
        double dbOpsPerSec = result.dbOperations / durationSeconds;
        
        System.out.printf("  📊 总操作数: %,d\n", result.totalOperations);
        System.out.printf("  ✅ 成功操作数: %,d\n", result.successOperations);
        System.out.printf("  🎯 成功率: %.2f%%\n", successRate);
        System.out.printf("  ⏱️  执行时间: %.2f秒\n", durationSeconds);
        System.out.printf("  🚀 QPS: %,.0f\n", qps);
        System.out.printf("  🗄️  数据库操作: %,d (%.1f ops/sec)\n", result.dbOperations, dbOpsPerSec);
        
        if (result.uniqueIds > 0) {
            System.out.printf("  🔢 唯一ID数: %,d\n", result.uniqueIds);
            System.out.printf("  ✨ 重复率: %.6f%%\n", 
                (result.successOperations - result.uniqueIds) * 100.0 / result.successOperations);
        }
        
        // 效率分析
        if (result.dbOperations > 0) {
            double idsPerDbOp = (double) result.successOperations / result.dbOperations;
            System.out.printf("  ⚡ 每次数据库操作生成ID数: %.0f\n", idsPerDbOp);
        }
        
        // 性能评级
        String grade = getPerformanceGrade(qps);
        System.out.printf("  🏆 性能评级: %s\n", grade);
    }
    
    /**
     * 根据QPS给出性能评级 (考虑数据库I/O的影响)
     */
    private String getPerformanceGrade(double qps) {
        if (qps >= 50_000) return "🏆 卓越 (≥50K QPS)";
        if (qps >= 25_000) return "🥇 优秀 (≥25K QPS)";
        if (qps >= 10_000) return "🥈 良好 (≥10K QPS)";
        if (qps >= 5_000) return "🥉 一般 (≥5K QPS)";
        if (qps >= 1_000) return "⚠️  需要优化 (≥1K QPS)";
        return "❌ 性能不足 (<1K QPS)";
    }
    
    /**
     * 测试结果
     */
    private static class TestResult {
        final long totalOperations;
        final long successOperations;
        final long durationMs;
        final int uniqueIds;
        final long dbOperations;
        
        TestResult(long totalOperations, long successOperations, long durationMs, 
                  int uniqueIds, long dbOperations) {
            this.totalOperations = totalOperations;
            this.successOperations = successOperations;
            this.durationMs = durationMs;
            this.uniqueIds = uniqueIds;
            this.dbOperations = dbOperations;
        }
    }
    
    /**
     * 带数据库集成的Leaf-Segment生成器
     */
    private static class DatabaseLeafSegmentGenerator {
        private final Object lock = new Object();
        private volatile Segment currentSegment;
        private volatile Segment nextSegment;
        private final AtomicLong dbOperationCounter = new AtomicLong(0);
        private final ExecutorService preloadExecutor = Executors.newSingleThreadExecutor();
        private Connection dbConnection;
        
        public DatabaseLeafSegmentGenerator() {
            try {
                dbConnection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
                // 初始化第一个段
                loadFirstSegment();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize database connection", e);
            }
        }
        
        private void loadFirstSegment() throws SQLException {
            SegmentInfo segmentInfo = getNextSegmentFromDB();
            if (segmentInfo != null) {
                currentSegment = new Segment(segmentInfo.start, segmentInfo.end);
            }
        }
        
        public long generateId() {
            Segment segment = currentSegment;
            if (segment == null) {
                return -1;
            }
            
            long id = segment.nextId();
            
            // 检查是否需要预加载
            if (id != -1 && segment.shouldPreload() && nextSegment == null) {
                preloadNextSegment();
            }
            
            // 如果当前段用尽，切换到下一个段
            if (id == -1 && nextSegment != null) {
                switchToNextSegment();
                id = currentSegment != null ? currentSegment.nextId() : -1;
            }
            
            return id;
        }
        
        private void preloadNextSegment() {
            preloadExecutor.submit(() -> {
                if (nextSegment == null) {
                    synchronized (lock) {
                        if (nextSegment == null) {
                            try {
                                SegmentInfo segmentInfo = getNextSegmentFromDB();
                                if (segmentInfo != null) {
                                    nextSegment = new Segment(segmentInfo.start, segmentInfo.end);
                                }
                            } catch (SQLException e) {
                                // 预加载失败，记录但不抛异常
                                System.err.println("预加载段失败: " + e.getMessage());
                            }
                        }
                    }
                }
            });
        }
        
        private void switchToNextSegment() {
            synchronized (lock) {
                if (nextSegment != null) {
                    currentSegment = nextSegment;
                    nextSegment = null;
                }
            }
        }
        
        /**
         * 从数据库获取下一个段
         */
        private SegmentInfo getNextSegmentFromDB() throws SQLException {
            String updateSql = "UPDATE tracking_sequences SET max_id = max_id + step, updated_time = CURRENT_TIMESTAMP WHERE biz_tag = ? RETURNING max_id, step";
            
            try (PreparedStatement pstmt = dbConnection.prepareStatement(updateSql)) {
                pstmt.setString(1, "test_tracking");
                
                dbOperationCounter.incrementAndGet();
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        long maxId = rs.getLong("max_id");
                        int step = rs.getInt("step");
                        long start = maxId - step + 1;
                        
                        return new SegmentInfo(start, maxId);
                    }
                }
            }
            return null;
        }
        
        public long getDbOperationCount() {
            return dbOperationCounter.get();
        }
        
        public void shutdown() {
            preloadExecutor.shutdown();
            try {
                if (!preloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    preloadExecutor.shutdownNow();
                }
                if (dbConnection != null && !dbConnection.isClosed()) {
                    dbConnection.close();
                }
            } catch (Exception e) {
                // 忽略关闭异常
            }
        }
        
        /**
         * 段信息
         */
        private static class SegmentInfo {
            final long start;
            final long end;
            
            SegmentInfo(long start, long end) {
                this.start = start;
                this.end = end;
            }
        }
        
        /**
         * 段（Segment）
         */
        private static class Segment {
            private final long start;
            private final long max;
            private final AtomicLong current;
            private final long preloadThreshold;
            
            Segment(long start, long max) {
                this.start = start;
                this.max = max;
                this.current = new AtomicLong(start);
                this.preloadThreshold = start + (max - start) * 3 / 4; // 75%时预加载
            }
            
            long nextId() {
                long id = current.getAndIncrement();
                return id <= max ? id : -1;
            }
            
            boolean shouldPreload() {
                return current.get() >= preloadThreshold;
            }
        }
    }
}