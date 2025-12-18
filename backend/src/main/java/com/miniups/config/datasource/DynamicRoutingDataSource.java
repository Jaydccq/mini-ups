package com.miniups.config.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态路由数据源
 *
 * 基于Spring的AbstractRoutingDataSource实现读写分离。
 * 根据DataSourceContextHolder中存储的数据源类型，动态选择主库或读副本。
 *
 * <p>路由规则：</p>
 * <ul>
 *   <li>PRIMARY - 路由到主库（写操作、强一致性读）</li>
 *   <li>REPLICA - 路由到读副本负载均衡器（只读操作）</li>
 * </ul>
 *
 * @author Mini-UPS Team
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(DynamicRoutingDataSource.class);

    /**
     * 确定当前应使用的数据源键
     *
     * @return 数据源键（PRIMARY 或 REPLICA）
     */
    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceContextHolder.DataSourceType dataSourceType =
            DataSourceContextHolder.getDataSourceType();

        if (log.isDebugEnabled()) {
            log.debug("Routing to datasource: {}", dataSourceType);
        }

        return dataSourceType;
    }

    /**
     * 获取当前正在使用的数据源类型描述
     *
     * @return 数据源类型描述字符串
     */
    public String getCurrentDataSourceDescription() {
        DataSourceContextHolder.DataSourceType type = DataSourceContextHolder.getDataSourceType();
        return switch (type) {
            case PRIMARY -> "Primary (Master) Database";
            case REPLICA -> "Read Replica (Load Balanced)";
        };
    }
}
