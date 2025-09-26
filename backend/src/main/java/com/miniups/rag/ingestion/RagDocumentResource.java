package com.miniups.rag.ingestion;

import java.nio.file.Path;
import java.util.Map;

public record RagDocumentResource(
    String documentId,
    String source,
    String title,
    String content,
    Map<String, Object> metadata,
    Path absolutePath
) {
}
