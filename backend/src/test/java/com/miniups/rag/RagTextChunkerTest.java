package com.miniups.rag;

import com.miniups.rag.config.RagProperties;
import com.miniups.rag.ingestion.RagDocumentResource;
import com.miniups.rag.ingestion.RagTextChunker;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RagTextChunkerTest {

    @Test
    void chunkSplitsLongContentWithOverlap() {
        RagProperties properties = new RagProperties();
        properties.getIngestion().setChunkSize(120);
        properties.getIngestion().setChunkOverlap(30);

        RagTextChunker chunker = new RagTextChunker(properties);
        String content = "调度员需要按照以下步骤处理延迟配送：\n\n" +
            "1. 联系司机确认当前位置和预计到达时间。\n" +
            "2. 通知客户最新状态并提供补偿方案。\n" +
            "3. 更新系统内的配送记录，标记为延迟处理。\n\n" +
            "如果延迟超过两小时，需要升级给管理员进一步处理。";

        RagDocumentResource resource = new RagDocumentResource(
            "doc-1",
            "knowledge/delivery.md",
            "延迟配送处理",
            content,
            Map.of(),
            Path.of("knowledge/delivery.md")
        );

        List<RagTextChunker.TextChunk> chunks = chunker.chunk(resource);

        assertFalse(chunks.isEmpty(), "Expected at least one chunk");
        assertTrue(chunks.size() >= 2, "Expected multiple chunks for long content");

        for (RagTextChunker.TextChunk chunk : chunks) {
            assertNotNull(chunk.content());
            assertFalse(chunk.content().isBlank());
            assertTrue(chunk.metadata().containsKey("title"));
            Object idx = chunk.metadata().get("chunkIndex");
            assertNotNull(idx);
            assertEquals(chunk.index(), ((Number) idx).intValue());
        }
    }
}
