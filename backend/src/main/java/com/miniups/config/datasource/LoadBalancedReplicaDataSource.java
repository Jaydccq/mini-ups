package com.miniups.config.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 读副本负载均衡数据源
 *
 * 在多个PostgreSQL读副本之间进行负载均衡，支持：
 * <ul>
 *   <li>轮询（Round-Robin）负载均衡策略</li>
 *   <li>健康检查和自动故障转移</li>
 *   <li>不健康副本自动跳过</li>
 *   <li>副本恢复后自动重新加入</li>
 *   <li>连接统计和监控</li>
 * </ul>
 *
 * @author Mini-UPS Team
 */
public class LoadBalancedReplicaDataSource extends AbstractDataSource {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancedReplicaDataSource.class);

    /** 读副本列表 */
    private final List<DataSource> replicas;

    /** 健康状态列表 */
    private final CopyOnWriteArrayList<Boolean> healthStatus;

    /** 副本名称列表（用于日志） */
    private final List<String> replicaNames;

    /** 轮询计数器 */
    private final AtomicInteger counter = new AtomicInteger(0);

    /** 统计：获取连接次数 */
    private final AtomicLong connectionRequestCount = new AtomicLong(0);

    /** 统计：成功连接次数 */
    private final AtomicLong successfulConnectionCount = new AtomicLong(0);

    /** 统计：失败连接次数 */
    private final AtomicLong failedConnectionCount = new AtomicLong(0);

    /** 健康检查调度器 */
    private final ScheduledExecutorService healthCheckScheduler;

    /** 健康检查间隔（秒） */
    private final int healthCheckIntervalSeconds;

    /** 连接验证超时（秒） */
    private final int validationTimeoutSeconds;

    /** 是否启用健康检查 */
    private volatile boolean healthCheckEnabled = true;

    /**
     * 构造函数
     *
     * @param replicas 读副本数据源列表
     */
    public LoadBalancedReplicaDataSource(List<DataSource> replicas) {
        this(replicas, null, 30, 5);
    }

    /**
     * 完整构造函数
     *
     * @param replicas 读副本数据源列表
     * @param replicaNames 副本名称列表（可选，用于日志）
     * @param healthCheckIntervalSeconds 健康检查间隔（秒）
     * @param validationTimeoutSeconds 连接验证超时（秒）
     */
    public LoadBalancedReplicaDataSource(
            List<DataSource> replicas,
            List<String> replicaNames,
            int healthCheckIntervalSeconds,
            int validationTimeoutSeconds) {

        if (replicas == null || replicas.isEmpty()) {
            throw new IllegalArgumentException("At least one replica datasource is required");
        }

        this.replicas = new ArrayList<>(replicas);
        this.healthStatus = new CopyOnWriteArrayList<>();
        this.replicaNames = replicaNames != null ? new ArrayList<>(replicaNames) : generateDefaultNames(replicas.size());
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
        this.validationTimeoutSeconds = validationTimeoutSeconds;

        // 初始化所有副本为健康状态
        for (int i = 0; i < replicas.size(); i++) {
            healthStatus.add(true);
        }

        // 启动健康检查
        this.healthCheckScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "replica-health-check");
            t.setDaemon(true);
            return t;
        });
        startHealthCheck();

        log.info("LoadBalancedReplicaDataSource initialized with {} replicas: {}",
            replicas.size(), this.replicaNames);
    }

    private List<String> generateDefaultNames(int size) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            names.add("replica-" + (i + 1));
        }
        return names;
    }

    @Override
    public Connection getConnection() throws SQLException {
        connectionRequestCount.incrementAndGet();
        return getConnectionFromHealthyReplica();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        // 对于读副本，通常使用预配置的凭证
        return getConnection();
    }

    /**
     * 从健康的副本中获取连接（轮询策略）
     */
    private Connection getConnectionFromHealthyReplica() throws SQLException {
        int attempts = 0;
        int maxAttempts = replicas.size() * 2; // 最多尝试两轮

        while (attempts < maxAttempts) {
            int index = selectNextReplicaIndex();

            if (healthStatus.get(index)) {
                try {
                    Connection conn = replicas.get(index).getConnection();
                    successfulConnectionCount.incrementAndGet();
                    if (log.isDebugEnabled()) {
                        log.debug("Connection acquired from replica: {}", replicaNames.get(index));
                    }
                    return conn;
                } catch (SQLException e) {
                    failedConnectionCount.incrementAndGet();
                    log.warn("Failed to get connection from replica {}: {}",
                        replicaNames.get(index), e.getMessage());
                    markAsUnhealthy(index);
                }
            } else {
                log.trace("Skipping unhealthy replica: {}", replicaNames.get(index));
            }
            attempts++;
        }

        // 所有副本都不可用，抛出异常
        failedConnectionCount.incrementAndGet();
        throw new SQLException("No healthy read replicas available. " +
            "All " + replicas.size() + " replicas are either unhealthy or unreachable.");
    }

    /**
     * 轮询选择下一个副本索引
     */
    private int selectNextReplicaIndex() {
        return Math.abs(counter.getAndIncrement() % replicas.size());
    }

    /**
     * 标记副本为不健康
     */
    private void markAsUnhealthy(int index) {
        if (healthStatus.get(index)) {
            healthStatus.set(index, false);
            log.warn("Replica {} marked as UNHEALTHY", replicaNames.get(index));
        }
    }

    /**
     * 标记副本为健康
     */
    private void markAsHealthy(int index) {
        if (!healthStatus.get(index)) {
            healthStatus.set(index, true);
            log.info("Replica {} marked as HEALTHY", replicaNames.get(index));
        }
    }

    /**
     * 启动健康检查定时任务
     */
    private void startHealthCheck() {
        healthCheckScheduler.scheduleWithFixedDelay(
            this::performHealthCheck,
            healthCheckIntervalSeconds, // 初始延迟
            healthCheckIntervalSeconds, // 检查间隔
            TimeUnit.SECONDS
        );
        log.info("Health check started with {} second interval", healthCheckIntervalSeconds);
    }

    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        if (!healthCheckEnabled) {
            return;
        }

        for (int i = 0; i < replicas.size(); i++) {
            checkReplicaHealth(i);
        }
    }

    /**
     * 检查单个副本的健康状态
     */
    private void checkReplicaHealth(int index) {
        try (Connection conn = replicas.get(index).getConnection()) {
            if (conn.isValid(validationTimeoutSeconds)) {
                markAsHealthy(index);
            } else {
                markAsUnhealthy(index);
            }
        } catch (SQLException e) {
            log.debug("Health check failed for replica {}: {}",
                replicaNames.get(index), e.getMessage());
            markAsUnhealthy(index);
        }
    }

    /**
     * 获取健康的副本数量
     */
    public int getHealthyReplicaCount() {
        return (int) healthStatus.stream().filter(Boolean::booleanValue).count();
    }

    /**
     * 获取副本总数
     */
    public int getTotalReplicaCount() {
        return replicas.size();
    }

    /**
     * 获取连接统计信息
     */
    public ConnectionStats getConnectionStats() {
        return new ConnectionStats(
            connectionRequestCount.get(),
            successfulConnectionCount.get(),
            failedConnectionCount.get(),
            getHealthyReplicaCount(),
            getTotalReplicaCount()
        );
    }

    /**
     * 获取各副本的健康状态
     */
    public List<ReplicaStatus> getReplicaStatuses() {
        List<ReplicaStatus> statuses = new ArrayList<>();
        for (int i = 0; i < replicas.size(); i++) {
            statuses.add(new ReplicaStatus(
                replicaNames.get(i),
                healthStatus.get(i)
            ));
        }
        return statuses;
    }

    /**
     * 启用/禁用健康检查
     */
    public void setHealthCheckEnabled(boolean enabled) {
        this.healthCheckEnabled = enabled;
        log.info("Health check {}", enabled ? "enabled" : "disabled");
    }

    /**
     * 强制刷新所有副本的健康状态
     */
    public void refreshHealthStatus() {
        log.info("Forcing health check refresh for all replicas");
        performHealthCheck();
    }

    /**
     * 关闭健康检查调度器
     */
    public void shutdown() {
        healthCheckScheduler.shutdown();
        try {
            if (!healthCheckScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("LoadBalancedReplicaDataSource shutdown completed");
    }

    // ========== DataSource接口方法 ==========

    @Override
    public PrintWriter getLogWriter() {
        try {
            return replicas.get(0).getLogWriter();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get log writer from replica", e);
        }
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        for (DataSource replica : replicas) {
            replica.setLogWriter(out);
        }
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        for (DataSource replica : replicas) {
            replica.setLoginTimeout(seconds);
        }
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return replicas.get(0).getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
    }

    // ========== 统计信息内部类 ==========

    /**
     * 连接统计信息
     */
    public record ConnectionStats(
        long totalRequests,
        long successfulConnections,
        long failedConnections,
        int healthyReplicas,
        int totalReplicas
    ) {
        public double getSuccessRate() {
            return totalRequests == 0 ? 100.0 :
                (successfulConnections * 100.0 / totalRequests);
        }
    }

    /**
     * 副本状态信息
     */
    public record ReplicaStatus(
        String name,
        boolean healthy
    ) {}
}
