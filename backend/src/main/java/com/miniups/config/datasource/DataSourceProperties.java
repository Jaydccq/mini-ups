package com.miniups.config.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据源配置属性
 *
 * 支持主库和多个读副本的配置。
 *
 * <p>配置示例：</p>
 * <pre>
 * app:
 *   datasource:
 *     read-write-split:
 *       enabled: true
 *       health-check-interval-seconds: 30
 *       validation-timeout-seconds: 5
 *     primary:
 *       jdbc-url: jdbc:postgresql://master:5432/ups_db
 *       username: postgres
 *       password: secret
 *       hikari:
 *         maximum-pool-size: 20
 *     replicas:
 *       - name: replica-1
 *         jdbc-url: jdbc:postgresql://replica1:5432/ups_db
 *         username: postgres
 *         password: secret
 *         hikari:
 *           maximum-pool-size: 30
 * </pre>
 *
 * @author Mini-UPS Team
 */
@Component
@ConfigurationProperties(prefix = "app.datasource")
public class DataSourceProperties {

    /** 读写分离配置 */
    private ReadWriteSplit readWriteSplit = new ReadWriteSplit();

    /** 主库配置 */
    private DataSourceConfig primary = new DataSourceConfig();

    /** 读副本配置列表 */
    private List<DataSourceConfig> replicas = new ArrayList<>();

    // ========== Getters and Setters ==========

    public ReadWriteSplit getReadWriteSplit() {
        return readWriteSplit;
    }

    public void setReadWriteSplit(ReadWriteSplit readWriteSplit) {
        this.readWriteSplit = readWriteSplit;
    }

    public DataSourceConfig getPrimary() {
        return primary;
    }

    public void setPrimary(DataSourceConfig primary) {
        this.primary = primary;
    }

    public List<DataSourceConfig> getReplicas() {
        return replicas;
    }

    public void setReplicas(List<DataSourceConfig> replicas) {
        this.replicas = replicas;
    }

    /**
     * 检查是否启用读写分离
     */
    public boolean isReadWriteSplitEnabled() {
        return readWriteSplit.isEnabled() && !replicas.isEmpty();
    }

    // ========== 内部配置类 ==========

    /**
     * 读写分离配置
     */
    public static class ReadWriteSplit {

        /** 是否启用读写分离 */
        private boolean enabled = false;

        /** 健康检查间隔（秒） */
        private int healthCheckIntervalSeconds = 30;

        /** 连接验证超时（秒） */
        private int validationTimeoutSeconds = 5;

        /** 所有副本不可用时是否回退到主库 */
        private boolean fallbackToPrimary = true;

        /** 是否记录数据源路由日志 */
        private boolean logRouting = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getHealthCheckIntervalSeconds() {
            return healthCheckIntervalSeconds;
        }

        public void setHealthCheckIntervalSeconds(int healthCheckIntervalSeconds) {
            this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
        }

        public int getValidationTimeoutSeconds() {
            return validationTimeoutSeconds;
        }

        public void setValidationTimeoutSeconds(int validationTimeoutSeconds) {
            this.validationTimeoutSeconds = validationTimeoutSeconds;
        }

        public boolean isFallbackToPrimary() {
            return fallbackToPrimary;
        }

        public void setFallbackToPrimary(boolean fallbackToPrimary) {
            this.fallbackToPrimary = fallbackToPrimary;
        }

        public boolean isLogRouting() {
            return logRouting;
        }

        public void setLogRouting(boolean logRouting) {
            this.logRouting = logRouting;
        }
    }

    /**
     * 单个数据源配置
     */
    public static class DataSourceConfig {

        /** 副本名称（用于日志和监控） */
        private String name;

        /** JDBC URL */
        private String jdbcUrl;

        /** 用户名 */
        private String username;

        /** 密码 */
        private String password;

        /** 驱动类名 */
        private String driverClassName = "org.postgresql.Driver";

        /** HikariCP配置 */
        private HikariConfig hikari = new HikariConfig();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public HikariConfig getHikari() {
            return hikari;
        }

        public void setHikari(HikariConfig hikari) {
            this.hikari = hikari;
        }
    }

    /**
     * HikariCP连接池配置
     */
    public static class HikariConfig {

        /** 连接池名称 */
        private String poolName;

        /** 最大连接数 */
        private int maximumPoolSize = 20;

        /** 最小空闲连接 */
        private int minimumIdle = 5;

        /** 空闲超时（毫秒） */
        private long idleTimeout = 300000;

        /** 连接超时（毫秒） */
        private long connectionTimeout = 20000;

        /** 连接最大生命周期（毫秒） */
        private long maxLifetime = 1800000;

        /** 连接是否只读 */
        private boolean readOnly = false;

        /** 连接验证查询 */
        private String connectionTestQuery = "SELECT 1";

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public long getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getMaxLifetime() {
            return maxLifetime;
        }

        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public String getConnectionTestQuery() {
            return connectionTestQuery;
        }

        public void setConnectionTestQuery(String connectionTestQuery) {
            this.connectionTestQuery = connectionTestQuery;
        }
    }
}
