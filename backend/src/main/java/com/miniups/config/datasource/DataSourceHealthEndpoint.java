package com.miniups.config.datasource;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源健康监控端点
 *
 * 提供读写分离状态和统计信息的Actuator端点。
 *
 * <p>访问方式：</p>
 * <ul>
 *   <li>GET /actuator/datasource - 获取数据源状态</li>
 *   <li>POST /actuator/datasource - 刷新健康检查</li>
 * </ul>
 *
 * @author Mini-UPS Team
 */
@Component
@Endpoint(id = "datasource")
@ConditionalOnProperty(
    prefix = "app.datasource.read-write-split",
    name = "enabled",
    havingValue = "true"
)
public class DataSourceHealthEndpoint {

    private final LoadBalancedReplicaDataSource replicaDataSource;
    private final DataSourceProperties dataSourceProperties;

    public DataSourceHealthEndpoint(
            LoadBalancedReplicaDataSource replicaDataSource,
            DataSourceProperties dataSourceProperties) {
        this.replicaDataSource = replicaDataSource;
        this.dataSourceProperties = dataSourceProperties;
    }

    /**
     * 获取数据源状态信息
     */
    @ReadOperation
    public Map<String, Object> getDataSourceStatus() {
        Map<String, Object> status = new HashMap<>();

        // 读写分离配置信息
        DataSourceProperties.ReadWriteSplit config = dataSourceProperties.getReadWriteSplit();
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("enabled", config.isEnabled());
        configInfo.put("healthCheckIntervalSeconds", config.getHealthCheckIntervalSeconds());
        configInfo.put("validationTimeoutSeconds", config.getValidationTimeoutSeconds());
        configInfo.put("fallbackToPrimary", config.isFallbackToPrimary());
        configInfo.put("logRouting", config.isLogRouting());
        status.put("configuration", configInfo);

        // 主库信息
        DataSourceProperties.DataSourceConfig primaryConfig = dataSourceProperties.getPrimary();
        Map<String, Object> primaryInfo = new HashMap<>();
        primaryInfo.put("jdbcUrl", maskPassword(primaryConfig.getJdbcUrl()));
        primaryInfo.put("poolName", primaryConfig.getHikari().getPoolName());
        primaryInfo.put("maximumPoolSize", primaryConfig.getHikari().getMaximumPoolSize());
        status.put("primary", primaryInfo);

        // 读副本状态
        List<LoadBalancedReplicaDataSource.ReplicaStatus> replicaStatuses =
            replicaDataSource.getReplicaStatuses();
        status.put("replicas", replicaStatuses);

        // 连接统计
        LoadBalancedReplicaDataSource.ConnectionStats stats = replicaDataSource.getConnectionStats();
        Map<String, Object> statsInfo = new HashMap<>();
        statsInfo.put("totalRequests", stats.totalRequests());
        statsInfo.put("successfulConnections", stats.successfulConnections());
        statsInfo.put("failedConnections", stats.failedConnections());
        statsInfo.put("successRate", String.format("%.2f%%", stats.getSuccessRate()));
        statsInfo.put("healthyReplicas", stats.healthyReplicas());
        statsInfo.put("totalReplicas", stats.totalReplicas());
        status.put("statistics", statsInfo);

        // 健康摘要
        int healthyCount = replicaDataSource.getHealthyReplicaCount();
        int totalCount = replicaDataSource.getTotalReplicaCount();
        String healthStatus = healthyCount == totalCount ? "HEALTHY" :
            (healthyCount > 0 ? "DEGRADED" : "UNHEALTHY");
        status.put("overallHealth", healthStatus);
        status.put("healthSummary", String.format("%d/%d replicas healthy", healthyCount, totalCount));

        return status;
    }

    /**
     * 强制刷新健康检查
     */
    @WriteOperation
    public Map<String, Object> refreshHealth() {
        replicaDataSource.refreshHealthStatus();

        Map<String, Object> result = new HashMap<>();
        result.put("action", "Health check refreshed");
        result.put("healthyReplicas", replicaDataSource.getHealthyReplicaCount());
        result.put("totalReplicas", replicaDataSource.getTotalReplicaCount());
        result.put("replicas", replicaDataSource.getReplicaStatuses());
        return result;
    }

    /**
     * 遮蔽URL中的密码
     */
    private String maskPassword(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        // 移除URL中的密码参数
        return jdbcUrl.replaceAll("password=[^&]*", "password=***");
    }
}
