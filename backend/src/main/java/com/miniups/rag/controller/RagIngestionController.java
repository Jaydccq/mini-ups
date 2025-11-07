package com.miniups.rag.controller;

import com.miniups.rag.ingestion.RagIngestionJobSummary;
import com.miniups.rag.ingestion.RagIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/ingest")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagIngestionController {

    private final RagIngestionService ingestionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RagIngestionJobSummary> triggerIngestion() {
        RagIngestionJobSummary job = ingestionService.triggerManualIngestion();
        if (job != null) {
            return ResponseEntity.ok(job);
        }
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<RagIngestionJobSummary> latest() {
        RagIngestionJobSummary job = ingestionService.latestJob();
        if (job != null) {
            return ResponseEntity.ok(job);
        }
        return ResponseEntity.noContent().build();
    }
}
