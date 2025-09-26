package com.miniups.rag.service;

import com.miniups.rag.model.RagFeedbackType;
import com.miniups.rag.model.RagQueryLog;
import com.miniups.rag.repository.RagQueryLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagFeedbackService {

    private final RagQueryLogRepository queryLogRepository;
    private final MeterRegistry meterRegistry;

    public Optional<RagQueryLog> submitFeedback(UUID logId, RagFeedbackType feedbackType, String comment, String role) {
        return queryLogRepository.findById(logId).map(logEntry -> {
            logEntry.applyFeedback(feedbackType, comment);
            RagQueryLog saved = queryLogRepository.save(logEntry);
            recordMetric(feedbackType, role);
            return saved;
        });
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
