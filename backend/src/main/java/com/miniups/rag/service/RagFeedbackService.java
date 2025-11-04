package com.miniups.rag.service;

import com.miniups.rag.model.RagFeedbackType;
import com.miniups.rag.model.RagQueryLog;
import com.miniups.rag.repository.RagQueryLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagFeedbackService {


    private static final Logger log = LoggerFactory.getLogger(RagFeedbackService.class);
    private final RagQueryLogRepository queryLogRepository;
    private final MeterRegistry meterRegistry;

    // Manual constructor (Lombok @RequiredArgsConstructor not working)
    public RagFeedbackService(RagQueryLogRepository queryLogRepository, MeterRegistry meterRegistry) {
        this.queryLogRepository = queryLogRepository;
        this.meterRegistry = meterRegistry;
    }

    public RagQueryLog submitFeedback(UUID logId, RagFeedbackType feedbackType, String comment, String role) {
        RagQueryLog logEntry = queryLogRepository.selectById(logId);
        if (logEntry == null) {
            return null;
        }

        logEntry.applyFeedback(feedbackType, comment);
        int updated = queryLogRepository.update(logEntry);
        if (updated > 0) {
            recordMetric(feedbackType, role);
            return logEntry;
        }
        return null;
    }

    private void recordMetric(RagFeedbackType feedbackType, String role) {
        try {
            meterRegistry.counter(
                "rag.feedback.total",
                "type",
                feedbackType.name().toLowerCase(),
                "role",
                role.toLowerCase()
            ).increment();
        } catch (Exception ex) {
            log.warn("Failed to record feedback metric", ex);
        }
    }
}
