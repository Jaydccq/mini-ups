package com.miniups.rag.retrieval;

import java.util.Map;
import java.util.UUID;

public record RagSearchResult(
    UUID id,
    String documentId,
    String source,
    int chunkIndex,
    String content,
    double semanticScore,
    double keywordScore,
    double finalScore,
    Map<String, Object> metadata
) {
}
