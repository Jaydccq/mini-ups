package com.miniups.rag.ingestion;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RagIngestionJobSummary(
    UUID id,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    String status,
    String trigger,
    int documentsProcessed,
    int chunksProcessed,
    String message
) {
}
