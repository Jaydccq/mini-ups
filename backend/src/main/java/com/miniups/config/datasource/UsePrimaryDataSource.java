package com.miniups.config.datasource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 强制使用主库注解
 *
 * 用于需要强一致性读取的场景，即使方法标记为@Transactional(readOnly=true)，
 * 也会强制路由到主库。
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>刚写入后需要立即读取的场景（避免主从复制延迟）</li>
 *   <li>需要强一致性的读取操作</li>
 *   <li>涉及锁的读取操作（SELECT FOR UPDATE）</li>
 * </ul>
 *
 * <p>示例：</p>
 * <pre>
 * {@code
 * @UsePrimaryDataSource
 * @Transactional(readOnly = true)
 * public UserDto getUserJustCreated(Long userId) {
 *     // 虽然是只读操作，但强制从主库读取以保证一致性
 *     return userRepository.findById(userId);
 * }
 * }
 * </pre>
 *
 * @author Mini-UPS Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UsePrimaryDataSource {

    /**
     * 原因说明（用于文档和日志）
     */
    String reason() default "";
}
