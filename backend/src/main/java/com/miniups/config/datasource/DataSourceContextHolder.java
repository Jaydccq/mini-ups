package com.miniups.config.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据源上下文持有器
 *
 * 使用ThreadLocal存储当前线程的数据源类型，支持读写分离路由。
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>AOP切面在事务开始前根据@Transactional(readOnly)设置数据源类型</li>
 *   <li>DynamicRoutingDataSource通过此类获取当前应使用的数据源</li>
 *   <li>事务结束后自动清理ThreadLocal防止内存泄漏</li>
 * </ol>
 *
 * @author Mini-UPS Team
 */
public class DataSourceContextHolder {

    private static final Logger log = LoggerFactory.getLogger(DataSourceContextHolder.class);

    private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();

    /**
     * 数据源类型枚举
     */
    public enum DataSourceType {
        /** 主库 - 用于写操作 */
        PRIMARY,
        /** 读副本 - 用于只读操作 */
        REPLICA
    }

    /**
     * 设置当前线程使用主库
     */
    public static void setPrimary() {
        log.debug("Switching to PRIMARY datasource");
        contextHolder.set(DataSourceType.PRIMARY);
    }

    /**
     * 设置当前线程使用读副本
     */
    public static void setReplica() {
        log.debug("Switching to REPLICA datasource");
        contextHolder.set(DataSourceType.REPLICA);
    }

    /**
     * 设置数据源类型
     *
     * @param type 数据源类型
     */
    public static void setDataSourceType(DataSourceType type) {
        if (type == null) {
            log.warn("Attempted to set null datasource type, defaulting to PRIMARY");
            contextHolder.set(DataSourceType.PRIMARY);
        } else {
            log.debug("Setting datasource type to: {}", type);
            contextHolder.set(type);
        }
    }

    /**
     * 获取当前线程的数据源类型
     *
     * @return 当前数据源类型，如果未设置则返回PRIMARY
     */
    public static DataSourceType getDataSourceType() {
        DataSourceType type = contextHolder.get();
        if (type == null) {
            log.trace("No datasource type set, defaulting to PRIMARY");
            return DataSourceType.PRIMARY;
        }
        return type;
    }

    /**
     * 检查当前是否使用主库
     *
     * @return true如果使用主库
     */
    public static boolean isPrimary() {
        return getDataSourceType() == DataSourceType.PRIMARY;
    }

    /**
     * 检查当前是否使用读副本
     *
     * @return true如果使用读副本
     */
    public static boolean isReplica() {
        return getDataSourceType() == DataSourceType.REPLICA;
    }

    /**
     * 清理当前线程的数据源设置
     *
     * <p>必须在事务结束后调用，防止ThreadLocal内存泄漏</p>
     */
    public static void clear() {
        log.debug("Clearing datasource context");
        contextHolder.remove();
    }

    /**
     * 强制使用主库执行操作（用于强一致性读取场景）
     *
     * @param action 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public static <T> T executeOnPrimary(java.util.function.Supplier<T> action) {
        DataSourceType previous = contextHolder.get();
        try {
            setPrimary();
            return action.get();
        } finally {
            if (previous != null) {
                contextHolder.set(previous);
            } else {
                clear();
            }
        }
    }

    /**
     * 强制使用读副本执行操作
     *
     * @param action 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public static <T> T executeOnReplica(java.util.function.Supplier<T> action) {
        DataSourceType previous = contextHolder.get();
        try {
            setReplica();
            return action.get();
        } finally {
            if (previous != null) {
                contextHolder.set(previous);
            } else {
                clear();
            }
        }
    }
}
