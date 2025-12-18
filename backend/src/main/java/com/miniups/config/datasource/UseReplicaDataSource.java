package com.miniups.config.datasource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 强制使用读副本注解
 *
 * 用于明确指定使用读副本的场景，即使方法没有标记@Transactional(readOnly=true)。
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>报表生成等大量读取操作</li>
 *   <li>历史数据查询</li>
 *   <li>可以容忍短暂不一致的读取</li>
 * </ul>
 *
 * <p>示例：</p>
 * <pre>
 * {@code
 * @UseReplicaDataSource
 * public List<OrderDto> generateDailyReport() {
 *     // 强制使用读副本，减轻主库压力
 *     return orderRepository.findAllByDate(yesterday);
 * }
 * }
 * </pre>
 *
 * @author Mini-UPS Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseReplicaDataSource {

    /**
     * 原因说明（用于文档和日志）
     */
    String reason() default "";
}
