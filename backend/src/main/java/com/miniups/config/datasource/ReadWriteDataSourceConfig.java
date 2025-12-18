package com.miniups.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读写分离数据源配置
 *
 * 当启用读写分离时，此配置类会：
 * <ul>
 *   <li>创建主库数据源（用于写操作）</li>
 *   <li>创建多个读副本数据源</li>
 *   <li>创建负载均衡器包装读副本</li>
 *   <li>创建动态路由数据源</li>
 *   <li>使用LazyConnectionDataSourceProxy延迟获取连接</li>
 * </ul>
 *
 * @author Mini-UPS Team
 */
@Configuration
@ConditionalOnProperty(
    prefix = "app.datasource.read-write-split",
    name = "enabled",
    havingValue = "true"
)
public class ReadWriteDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ReadWriteDataSourceConfig.class);

    private final com.miniups.config.datasource.DataSourceProperties dataSourceProperties;

    public ReadWriteDataSourceConfig(com.miniups.config.datasource.DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    /**
     * 创建主库数据源
     */
    @Bean(name = "primaryDataSource")
    public DataSource primaryDataSource() {
        com.miniups.config.datasource.DataSourceProperties.DataSourceConfig primaryConfig =
            dataSourceProperties.getPrimary();

        HikariDataSource dataSource = createHikariDataSource(primaryConfig, "primary");
        log.info("Primary datasource created: {}", primaryConfig.getJdbcUrl());
        return dataSource;
    }

    /**
     * 创建读副本负载均衡数据源
     */
    @Bean(name = "replicaDataSource")
    public LoadBalancedReplicaDataSource replicaDataSource() {
        List<com.miniups.config.datasource.DataSourceProperties.DataSourceConfig> replicaConfigs =
            dataSourceProperties.getReplicas();

        if (replicaConfigs.isEmpty()) {
            throw new IllegalStateException(
                "Read-write split is enabled but no replicas configured. " +
                "Please configure at least one replica in app.datasource.replicas");
        }

        List<DataSource> replicas = new ArrayList<>();
        List<String> replicaNames = new ArrayList<>();

        for (int i = 0; i < replicaConfigs.size(); i++) {
            com.miniups.config.datasource.DataSourceProperties.DataSourceConfig config = replicaConfigs.get(i);
            String name = config.getName() != null ? config.getName() : "replica-" + (i + 1);
            replicaNames.add(name);

            HikariDataSource replicaDs = createHikariDataSource(config, name);
            // 读副本设置为只读模式
            replicaDs.setReadOnly(true);
            replicas.add(replicaDs);

            log.info("Replica datasource created: {} -> {}", name, config.getJdbcUrl());
        }

        com.miniups.config.datasource.DataSourceProperties.ReadWriteSplit rwConfig =
            dataSourceProperties.getReadWriteSplit();

        LoadBalancedReplicaDataSource loadBalancer = new LoadBalancedReplicaDataSource(
            replicas,
            replicaNames,
            rwConfig.getHealthCheckIntervalSeconds(),
            rwConfig.getValidationTimeoutSeconds()
        );

        log.info("LoadBalancedReplicaDataSource created with {} replicas", replicas.size());
        return loadBalancer;
    }

    /**
     * 创建动态路由数据源
     */
    @Bean(name = "routingDataSource")
    public DynamicRoutingDataSource routingDataSource(
            DataSource primaryDataSource,
            LoadBalancedReplicaDataSource replicaDataSource) {

        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceContextHolder.DataSourceType.PRIMARY, primaryDataSource);
        targetDataSources.put(DataSourceContextHolder.DataSourceType.REPLICA, replicaDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        routingDataSource.afterPropertiesSet();

        log.info("DynamicRoutingDataSource configured with PRIMARY and REPLICA targets");
        return routingDataSource;
    }

    /**
     * 创建延迟连接数据源代理
     *
     * LazyConnectionDataSourceProxy确保只有在真正需要连接时才获取，
     * 这使得在事务开始时可以正确选择数据源。
     */
    @Bean
    @Primary
    public DataSource dataSource(DynamicRoutingDataSource routingDataSource) {
        LazyConnectionDataSourceProxy proxy = new LazyConnectionDataSourceProxy(routingDataSource);
        log.info("LazyConnectionDataSourceProxy created as primary DataSource");
        return proxy;
    }

    /**
     * 创建HikariCP数据源
     */
    private HikariDataSource createHikariDataSource(
            com.miniups.config.datasource.DataSourceProperties.DataSourceConfig config,
            String defaultPoolName) {

        HikariDataSource ds = new HikariDataSource();

        ds.setJdbcUrl(config.getJdbcUrl());
        ds.setUsername(config.getUsername());
        ds.setPassword(config.getPassword());
        ds.setDriverClassName(config.getDriverClassName());

        com.miniups.config.datasource.DataSourceProperties.HikariConfig hikari = config.getHikari();

        ds.setPoolName(hikari.getPoolName() != null ? hikari.getPoolName() : "HikariPool-" + defaultPoolName);
        ds.setMaximumPoolSize(hikari.getMaximumPoolSize());
        ds.setMinimumIdle(hikari.getMinimumIdle());
        ds.setIdleTimeout(hikari.getIdleTimeout());
        ds.setConnectionTimeout(hikari.getConnectionTimeout());
        ds.setMaxLifetime(hikari.getMaxLifetime());
        ds.setConnectionTestQuery(hikari.getConnectionTestQuery());

        return ds;
    }
}
