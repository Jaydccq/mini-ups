package com.miniups.rag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG Retriever Integration Tests with PostgreSQL Testcontainers.
 *
 * IMPORTANT: This test requires Docker to run Testcontainers.
 * In CI/CD environments without Docker, set CI=true to skip these tests.
 *
 * To run locally with Docker:
 *   mvn test -Dtest=RagRetrieverIntegrationTest
 *
 * To skip in CI/CD without Docker:
 *   export CI=true
 *   mvn test
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("RAG Retriever Integration Tests")
@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "Requires Docker/Testcontainers which is not available in CI/CD")
class RagRetrieverIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("sql/init-test-db.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("rag.enabled", () -> "true");
    }

    private JdbcTemplate jdbcTemplate;
    private RagRetriever retriever;
    private RagProperties properties;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(postgres.getDriverClassName());
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);

        properties = new RagProperties();
        properties.getRetrieval().setSemanticWeight(0.7);
        properties.getRetrieval().setKeywordWeight(0.3);
        properties.getRetrieval().setSimilarityThreshold(0.5);

        retriever = new RagRetriever(jdbcTemplate, properties, new ObjectMapper());

        // Set up test database schema and data
        setupTestData();
    }

    private void setupTestData() {
        // Create extension and table
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS rag_document_chunk (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                document_id VARCHAR(255) NOT NULL,
                source VARCHAR(500) NOT NULL,
                chunk_index INTEGER NOT NULL,
                content TEXT NOT NULL,
                content_tsv TSVECTOR,
                metadata JSONB,
                embedding VECTOR(1536),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // Create indexes
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunk_embedding ON rag_document_chunk USING hnsw (embedding vector_cosine_ops)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunk_content_tsv ON rag_document_chunk USING gin(content_tsv)");

        // Insert test data
        insertTestChunk("doc-1", "knowledge/delivery.md", 0,
            "延迟配送处理流程：1. 联系司机确认位置 2. 通知客户状态 3. 更新系统记录",
            Map.of("title", "配送手册", "section", "延迟处理"),
            generateEmbedding(0.1f, 0.2f, 0.3f));

        insertTestChunk("doc-1", "knowledge/delivery.md", 1,
            "货物损坏处理：检查包装完整性，拍照记录，联系保险公司处理理赔事宜",
            Map.of("title", "配送手册", "section", "损坏处理"),
            generateEmbedding(0.4f, 0.5f, 0.6f));

        insertTestChunk("doc-2", "knowledge/tracking.md", 0,
            "跟踪号查询系统使用方法：输入完整跟踪号，系统显示实时位置和状态信息",
            Map.of("title", "跟踪系统", "section", "查询方法"),
            generateEmbedding(0.7f, 0.8f, 0.9f));

        insertTestChunk("doc-3", "knowledge/routes.md", 0,
            "路线优化算法：考虑交通状况、配送时间窗口、车辆容量等因素进行最优路径规划",
            Map.of("title", "路线规划", "section", "优化算法"),
            generateEmbedding(0.2f, 0.4f, 0.8f));
    }

    private void insertTestChunk(String documentId, String source, int chunkIndex,
                                String content, Map<String, Object> metadata, float[] embedding) {
        String sql = """
            INSERT INTO rag_document_chunk (document_id, source, chunk_index, content, content_tsv, metadata, embedding)
            VALUES (?, ?, ?, ?, to_tsvector('simple', ?), ?::jsonb, ?::vector)
            """;

        try {
            String metadataJson = new ObjectMapper().writeValueAsString(metadata);
            String embeddingStr = toVectorLiteral(embedding);

            jdbcTemplate.update(sql, documentId, source, chunkIndex, content, content, metadataJson, embeddingStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert test data", e);
        }
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder(embedding.length * 8);
        builder.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(Float.toString(embedding[i]));
        }
        builder.append(']');
        return builder.toString();
    }

    private float[] generateEmbedding(float... values) {
        float[] embedding = new float[1536];
        System.arraycopy(values, 0, embedding, 0, Math.min(values.length, embedding.length));
        // Fill rest with small random values
        for (int i = values.length; i < embedding.length; i++) {
            embedding[i] = (float) (Math.random() * 0.1 - 0.05);
        }
        return embedding;
    }

    @Test
    @DisplayName("Should perform hybrid search with real database")
    void shouldPerformHybridSearchWithRealDatabase() {
        // Given
        float[] queryVector = generateEmbedding(0.15f, 0.25f, 0.35f); // Similar to first chunk
        String query = "延迟配送处理";

        // When
        List<RagSearchResult> results = retriever.hybridSearch(query, queryVector, 5, 0.3);

        // Then
        assertFalse(results.isEmpty(), "Should return results for hybrid search");

        RagSearchResult topResult = results.get(0);
        assertNotNull(topResult.id());
        assertNotNull(topResult.documentId());
        assertNotNull(topResult.source());
        assertNotNull(topResult.content());
        assertTrue(topResult.semanticScore() >= 0.0 && topResult.semanticScore() <= 1.0);
        assertTrue(topResult.keywordScore() >= 0.0);
        assertTrue(topResult.finalScore() >= 0.0 && topResult.finalScore() <= 1.0);

        // The query should match the first chunk best due to similar embedding and keyword match
        assertTrue(topResult.content().contains("延迟配送"));
        assertFalse(topResult.metadata().isEmpty());
        assertEquals("配送手册", topResult.metadata().get("title"));
    }

    @Test
    @DisplayName("Should respect similarity threshold in real database")
    void shouldRespectSimilarityThresholdInRealDatabase() {
        // Given - Vector very different from any test data
        float[] veryDifferentVector = generateEmbedding(0.9f, 0.95f, 0.99f);
        String query = "完全不相关的查询内容";

        // When - Set high similarity threshold
        List<RagSearchResult> results = retriever.hybridSearch(query, veryDifferentVector, 5, 0.9);

        // Then - Should return fewer or no results due to high threshold
        assertTrue(results.size() <= 2, "High threshold should filter out most results");

        if (!results.isEmpty()) {
            for (RagSearchResult result : results) {
                // Results that pass should have decent semantic scores or good keyword matches
                assertTrue(result.semanticScore() > 0.8 || result.keywordScore() > 0.0);
            }
        }
    }

    @Test
    @DisplayName("Should handle pure semantic search when no keyword matches")
    void shouldHandlePureSemanticSearch() {
        // Given - Query with no Chinese keywords that would match our test data
        float[] queryVector = generateEmbedding(0.1f, 0.2f, 0.3f);
        String query = "xyz123nonexistentkeywords789";

        // When
        List<RagSearchResult> results = retriever.hybridSearch(query, queryVector, 5, 0.3);

        // Then - Should still return results based on semantic similarity
        assertFalse(results.isEmpty(), "Should return results based on semantic similarity alone");

        for (RagSearchResult result : results) {
            assertTrue(result.semanticScore() > 0.0, "Should have semantic scores");
            assertEquals(0.0, result.keywordScore(), 0.001, "Should have no keyword scores for non-matching query");
            // Final score should be weighted toward semantic (0.7 weight)
            assertEquals(result.semanticScore() * 0.7, result.finalScore(), 0.001);
        }
    }

    @Test
    @DisplayName("Should handle pure keyword search when semantic is below threshold")
    void shouldHandlePureKeywordSearch() {
        // Given - Configure to prefer keyword search
        properties.getRetrieval().setSemanticWeight(0.1);
        properties.getRetrieval().setKeywordWeight(0.9);
        properties.getRetrieval().setSimilarityThreshold(0.9); // Very high threshold

        retriever = new RagRetriever(jdbcTemplate, properties, new ObjectMapper());

        float[] veryDifferentVector = generateEmbedding(0.9f, 0.95f, 0.99f);
        String query = "配送 跟踪"; // Should match multiple chunks

        // When
        List<RagSearchResult> results = retriever.hybridSearch(query, veryDifferentVector, 5, 0.9);

        // Then
        assertFalse(results.isEmpty(), "Should return results based on keyword matches");

        for (RagSearchResult result : results) {
            assertTrue(result.keywordScore() > 0.0, "Should have keyword scores");
            // Due to high semantic threshold, semantic scores might be filtered out
        }
    }

    @Test
    @DisplayName("Should correctly aggregate scores for same chunk in both searches")
    void shouldCorrectlyAggregateScoresForSameChunk() {
        // Given - Query that should match first chunk in both semantic and keyword searches
        float[] similarVector = generateEmbedding(0.11f, 0.21f, 0.31f); // Very similar to first chunk
        String query = "延迟配送处理流程"; // Exact match for content

        // When
        List<RagSearchResult> results = retriever.hybridSearch(query, similarVector, 5, 0.3);

        // Then
        assertFalse(results.isEmpty());

        // Find the result that should have both semantic and keyword matches
        RagSearchResult bestMatch = results.stream()
            .filter(r -> r.content().contains("延迟配送处理流程"))
            .findFirst()
            .orElse(results.get(0));

        assertTrue(bestMatch.semanticScore() > 0.0, "Should have semantic score");
        assertTrue(bestMatch.keywordScore() > 0.0, "Should have keyword score");

        // Final score should be weighted combination
        double expectedFinalScore = 0.7 * bestMatch.semanticScore() + 0.3 * bestMatch.keywordScore();
        assertEquals(expectedFinalScore, bestMatch.finalScore(), 0.001);
        assertTrue(bestMatch.finalScore() <= 1.0, "Final score should not exceed 1.0");
    }

    @Test
    @DisplayName("Should return results sorted by final score descending")
    void shouldReturnResultsSortedByFinalScoreDescending() {
        // Given
        float[] queryVector = generateEmbedding(0.1f, 0.2f, 0.3f);
        String query = "配送";

        // When
        List<RagSearchResult> results = retriever.hybridSearch(query, queryVector, 10, 0.1);

        // Then
        assertFalse(results.isEmpty());
        assertTrue(results.size() > 1, "Should return multiple results for sorting test");

        // Verify results are sorted by finalScore descending
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).finalScore() >= results.get(i + 1).finalScore(),
                String.format("Results should be sorted by finalScore descending: %f >= %f at positions %d, %d",
                    results.get(i).finalScore(), results.get(i + 1).finalScore(), i, i + 1));
        }
    }

    @Test
    @DisplayName("Should limit results to requested topK")
    void shouldLimitResultsToRequestedTopK() {
        // Given
        float[] queryVector = generateEmbedding(0.5f, 0.5f, 0.5f);
        String query = "系统"; // Should match multiple chunks
        int topK = 2;

        // When
        List<RagSearchResult> results = retriever.hybridSearch(query, queryVector, topK, 0.1);

        // Then
        assertTrue(results.size() <= topK,
            String.format("Should return at most %d results, but got %d", topK, results.size()));
    }

    @Test
    @DisplayName("Should handle metadata parsing correctly")
    void shouldHandleMetadataParsingCorrectly() {
        // Given
        float[] queryVector = generateEmbedding(0.1f, 0.2f, 0.3f);
        String query = "配送手册";

        // When
        List<RagSearchResult> results = retriever.hybridSearch(query, queryVector, 5, 0.3);

        // Then
        assertFalse(results.isEmpty());

        boolean foundExpectedMetadata = false;
        for (RagSearchResult result : results) {
            Map<String, Object> metadata = result.metadata();
            if (metadata.containsKey("title") && "配送手册".equals(metadata.get("title"))) {
                foundExpectedMetadata = true;
                assertTrue(metadata.containsKey("section"));
                assertNotNull(metadata.get("section"));
                break;
            }
        }

        assertTrue(foundExpectedMetadata, "Should find result with expected metadata");
    }
}
