package com.miniups.rag.ingestion;

import com.miniups.rag.config.RagProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RagTextChunker {

    private final RagProperties properties;

    public RagTextChunker(RagProperties properties) {
        this.properties = properties;
    }

    public List<TextChunk> chunk(RagDocumentResource resource) {
        List<TextChunk> chunks = new ArrayList<>();
        String content = normalize(resource.content());
        if (content.isBlank()) {
            return chunks;
        }

        int maxSize = Math.max(200, properties.getIngestion().getChunkSize());
        int overlap = Math.min(properties.getIngestion().getChunkOverlap(), maxSize / 2);
        int index = 0;
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxSize, content.length());
            if (end < content.length()) {
                int split = findSplitPosition(content, start, end);
                if (split > start) {
                    end = split;
                }
            }
            String chunkText = content.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                Map<String, Object> metadata = new HashMap<>(resource.metadata());
                metadata.put("title", resource.title());
                metadata.put("chunkIndex", index);
                chunks.add(new TextChunk(index, chunkText, metadata));
                index++;
            }
            if (end >= content.length()) {
                break;
            }
            start = Math.max(0, end - overlap);
        }
        return chunks;
    }

    private String normalize(String text) {
        return text.replaceAll("\r\n", "\n");
    }

    private int findSplitPosition(String content, int start, int desiredEnd) {
        int searchStart = Math.max(start, desiredEnd - 200);
        int newline = content.lastIndexOf('\n', desiredEnd - 1);
        if (newline >= searchStart) {
            return newline;
        }
        int period = content.lastIndexOf('.', desiredEnd - 1);
        if (period >= searchStart) {
            return period + 1;
        }
        int space = content.lastIndexOf(' ', desiredEnd - 1);
        if (space >= searchStart) {
            return space;
        }
        return desiredEnd;
    }

    public record TextChunk(int index, String content, Map<String, Object> metadata) {
    }
}
