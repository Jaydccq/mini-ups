package com.miniups.rag.controller;

import com.miniups.rag.api.RagFeedbackRequest;
import com.miniups.rag.model.RagFeedbackType;
import com.miniups.rag.model.RagQueryLog;
import com.miniups.rag.service.RagFeedbackService;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/feedback")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagFeedbackController {

    private final RagFeedbackService feedbackService;

    public RagFeedbackController(RagFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','DRIVER','OPERATOR')")
    public ResponseEntity<Void> submitFeedback(@Valid @RequestBody RagFeedbackRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication != null && authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()
            ? authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
            : "UNKNOWN";

        RagFeedbackType feedbackType;
        try {
            feedbackType = RagFeedbackType.valueOf(request.getFeedback().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }

        RagQueryLog updated = feedbackService.submitFeedback(request.getLogId(), feedbackType, request.getComment(), role);
        if (updated != null) {
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.notFound().build();
    }
}
