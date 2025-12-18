package com.miniups.config.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * 读写分离AOP切面
 *
 * 拦截所有@Transactional注解的方法，根据readOnly属性自动路由数据源：
 * <ul>
 *   <li>readOnly = true → 路由到读副本（负载均衡）</li>
 *   <li>readOnly = false（默认） → 路由到主库</li>
 * </ul>
 *
 * <p>优先级：此切面必须在Spring事务切面之前执行（@Order(-1)）</p>
 *
 * @author Mini-UPS Team
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // 在事务切面之前执行
@ConditionalOnProperty(
    prefix = "app.datasource.read-write-split",
    name = "enabled",
    havingValue = "true"
)
public class ReadWriteSplitAspect {

    private static final Logger log = LoggerFactory.getLogger(ReadWriteSplitAspect.class);

    private final com.miniups.config.datasource.DataSourceProperties dataSourceProperties;

    public ReadWriteSplitAspect(com.miniups.config.datasource.DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
        log.info("ReadWriteSplitAspect initialized - automatic read/write routing enabled");
    }

    /**
     * 切入点：所有带@Transactional注解的方法
     */
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethod() {}

    /**
     * 切入点：所有带@Transactional注解的类中的public方法
     */
    @Pointcut("@within(org.springframework.transaction.annotation.Transactional)")
    public void transactionalClass() {}

    /**
     * 环绕通知：在事务方法执行前后进行数据源路由
     */
    @Around("transactionalMethod() || transactionalClass()")
    public Object routeDataSource(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 确定数据源类型
            DataSourceContextHolder.DataSourceType targetType = determineDataSourceType(joinPoint);

            // 设置数据源
            DataSourceContextHolder.setDataSourceType(targetType);

            if (dataSourceProperties.getReadWriteSplit().isLogRouting()) {
                log.info("Routing to {} for method: {}.{}",
                    targetType,
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
            }

            // 执行目标方法
            return joinPoint.proceed();

        } finally {
            // 清理ThreadLocal
            DataSourceContextHolder.clear();
        }
    }

    /**
     * 切入点：带@UsePrimaryDataSource注解的方法
     */
    @Pointcut("@annotation(com.miniups.config.datasource.UsePrimaryDataSource)")
    public void usePrimaryMethod() {}

    /**
     * 切入点：带@UseReplicaDataSource注解的方法
     */
    @Pointcut("@annotation(com.miniups.config.datasource.UseReplicaDataSource)")
    public void useReplicaMethod() {}

    /**
     * 强制使用主库
     */
    @Around("usePrimaryMethod()")
    public Object forcePrimary(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DataSourceContextHolder.setPrimary();

            if (dataSourceProperties.getReadWriteSplit().isLogRouting()) {
                log.info("Forced PRIMARY routing for method: {}.{}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
            }

            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    /**
     * 强制使用读副本
     */
    @Around("useReplicaMethod()")
    public Object forceReplica(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DataSourceContextHolder.setReplica();

            if (dataSourceProperties.getReadWriteSplit().isLogRouting()) {
                log.info("Forced REPLICA routing for method: {}.{}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
            }

            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    /**
     * 确定目标数据源类型
     *
     * 优先级：
     * 1. @UsePrimaryDataSource 注解（强制主库）
     * 2. @UseReplicaDataSource 注解（强制读副本）
     * 3. 方法级别的@Transactional
     * 4. 类级别的@Transactional
     * 5. 默认使用主库
     */
    private DataSourceContextHolder.DataSourceType determineDataSourceType(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // 1. 检查@UsePrimaryDataSource注解
        if (method.isAnnotationPresent(UsePrimaryDataSource.class) ||
            targetClass.isAnnotationPresent(UsePrimaryDataSource.class)) {
            return DataSourceContextHolder.DataSourceType.PRIMARY;
        }

        // 2. 检查@UseReplicaDataSource注解
        if (method.isAnnotationPresent(UseReplicaDataSource.class) ||
            targetClass.isAnnotationPresent(UseReplicaDataSource.class)) {
            return DataSourceContextHolder.DataSourceType.REPLICA;
        }

        // 3. 检查方法级别的@Transactional
        Transactional methodTransactional = method.getAnnotation(Transactional.class);
        if (methodTransactional != null) {
            return methodTransactional.readOnly()
                ? DataSourceContextHolder.DataSourceType.REPLICA
                : DataSourceContextHolder.DataSourceType.PRIMARY;
        }

        // 4. 检查类级别的@Transactional
        Transactional classTransactional = targetClass.getAnnotation(Transactional.class);
        if (classTransactional != null) {
            return classTransactional.readOnly()
                ? DataSourceContextHolder.DataSourceType.REPLICA
                : DataSourceContextHolder.DataSourceType.PRIMARY;
        }

        // 5. 默认使用主库（安全起见）
        return DataSourceContextHolder.DataSourceType.PRIMARY;
    }
}
