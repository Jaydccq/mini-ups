/**
 * Exception Metrics Configuration
 * 
 * 异常处理监控配置，提供异常统计和监控功能
 * 
 * Features:
 * - 异常计数和分类统计
 * - 异常处理性能监控
 * - 异常发生趋势分析
 * - 健康检查集成
 * 
 * Metrics:
 * - exception.count - 异常总数统计
 * - exception.type - 按异常类型分类
 * - exception.endpoint - 按API端点分类
 * - exception.processing.time - 异常处理耗时
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.config;

import com.miniups.exception.BaseBusinessException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ExceptionMetricsConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMetricsConfig.class);
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicLong> exceptionCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counterCache = new ConcurrentHashMap<>();
    
    public ExceptionMetricsConfig(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    

    public void recordException(Exception exception, String endpoint) {
        if (meterRegistry == null) {
            recordSimpleException(exception, endpoint);
            return;
        }
        
        String exceptionType = exception.getClass().getSimpleName();
        String errorCode = getErrorCode(exception);
        String severity = getSeverity(exception);
        
        String cacheKey = String.format("%s:%s:%s:%s", exceptionType, endpoint, errorCode, severity);
        
        Counter counter = counterCache.computeIfAbsent(cacheKey, key ->
            Counter.builder("exception.count")
                .tag("type", exceptionType)
                .tag("endpoint", endpoint)
                .tag("error_code", errorCode)
                .tag("severity", severity)
                .description("Exception count by type and endpoint")
                .register(meterRegistry)
        );
        
        counter.increment();
        
        logger.debug("record: type={}, endpoint={}, error_code={}, severity={}",
                    exceptionType, endpoint, errorCode, severity);
    }
    

    public Timer.Sample startTimer() {
        if (meterRegistry == null) {
            return null;
        }
        return Timer.start(meterRegistry);
    }
    

    public void stopTimer(Timer.Sample sample, Exception exception) {
        if (meterRegistry == null || sample == null) {
            return;
        }
        
        sample.stop(Timer.builder("exception.processing.time")
            .tag("type", exception.getClass().getSimpleName())
            .tag("severity", getSeverity(exception))
            .description("Exception processing time")
            .register(meterRegistry));
    }
    

    private void recordSimpleException(Exception exception, String endpoint) {
        String key = String.format("%s:%s", exception.getClass().getSimpleName(), endpoint);
        exceptionCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.info("Total: {} times {}", key, exceptionCounts.get(key).get());
    }

    private String getErrorCode(Exception exception) {
        if (exception instanceof BaseBusinessException) {
            return ((BaseBusinessException) exception).getErrorCode();
        }
        return "UNKNOWN";
    }

    private String getSeverity(Exception exception) {
        if (exception instanceof BaseBusinessException) {
            return ((BaseBusinessException) exception).getSeverity().toString();
        } else if (exception instanceof NullPointerException) {
            return "HIGH";
        } else if (exception instanceof IllegalArgumentException) {
            return "LOW";
        }
        return "MEDIUM";
    }
    

    public static String getCurrentEndpoint() {
        try {
            ServletRequestAttributes requestAttributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                
                String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                if (pattern != null && !pattern.isEmpty()) {
                    return request.getMethod() + " " + pattern;
                }
                
                String uri = request.getRequestURI();
                String normalizedUri = normalizePathParameters(uri);
                return request.getMethod() + " " + normalizedUri;
            }
        } catch (Exception e) {
            // Log the exception to help with debugging, as this may indicate
            // the method is being called outside of a request context
            logger.warn("Could not determine current endpoint. This may happen if called outside of a request context.", e);
        }
        return "UNKNOWN";
    }
    

    public static String normalizePathParameters(String uri) {
        if (uri == null) {
            return "UNKNOWN";
        }
        
        String normalized = uri.replaceAll("/\\d+(?=/|$)", "/{id}");
        
        normalized = normalized.replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(?=/|$)", "/{uuid}");
        
        normalized = normalized.replaceAll("/[^/]{10,}(?=/|$)", "/{param}");
        
        return normalized;
    }
    

    public ConcurrentHashMap<String, AtomicLong> getExceptionCounts() {
        return new ConcurrentHashMap<>(exceptionCounts);
    }
    

    public void clearExceptionCounts() {
        exceptionCounts.clear();
        logger.info("Counter cache has been cleared, current size: {}", exceptionCounts.size());
    }
    

    public void clearCounterCache() {
        counterCache.clear();
        logger.info("Counter cache has clear size: {}", counterCache.size());
    }
    

    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("counterCacheSize", counterCache.size());
        stats.put("exceptionCountsSize", exceptionCounts.size());
        stats.put("cachedCounters", counterCache.keySet());
        return stats;
    }
}