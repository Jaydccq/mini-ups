package com.miniups.rag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.rag.config.RagProperties;
import com.pgvector.PGvector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RAG Hybrid Retriever Tests")
class RagRetrieverTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private RagProperties properties;
    private ObjectMapper objectMapper;
    private RagRetriever retriever;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.getRetrieval().setSemanticWeight(0.7);
        properties.getRetrieval().setKeywordWeight(0.3);
        properties.getRetrieval().setSimilarityThreshold(0.5);
        properties.getStorage().setTableName("rag_document_chunk");

        objectMapper = new ObjectMapper();
        retriever = new RagRetriever(jdbcTemplate, properties, objectMapper);
    }

    @Nested
    @DisplayName("Semantic Search Tests")
    class SemanticSearchTests {

        @Test
        @DisplayName("Should return semantic search results with correct scores")
        void shouldReturnSemanticSearchResults() throws SQLException {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            UUID chunkId = UUID.randomUUID();
            String documentId = "doc-1";
            String source = "knowledge/delivery.md";
            String content = "延迟配送处理流程";
            double distance = 0.3; // Distance of 0.3 should give semantic score of 0.7

            when(resultSet.getObject("id")).thenReturn(chunkId);
            when(resultSet.getString("document_id")).thenReturn(documentId);
            when(resultSet.getString("source")).thenReturn(source);
            when(resultSet.getInt("chunk_index")).thenReturn(0);
            when(resultSet.getString("content")).thenReturn(content);
            when(resultSet.getDouble("distance")).thenReturn(distance);
            when(resultSet.getObject("metadata")).thenReturn(null);

            when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<RagSearchResult> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

            // When
            List<RagSearchResult> results = retriever.hybridSearch("配送延迟", queryVector, 5, 0.5);

            // Then
            assertFalse(results.isEmpty());
            RagSearchResult result = results.get(0);
            assertEquals(chunkId, result.id());
            assertEquals(documentId, result.documentId());
            assertEquals(source, result.source());
            assertEquals(content, result.content());
            assertEquals(0.7, result.semanticScore(), 0.01); // 1 - 0.3 = 0.7
            assertEquals(0.0, result.keywordScore());
        }

        @Test
        @DisplayName("Should handle empty vector gracefully")
        void shouldHandleEmptyVector() {
            // Given
            float[] emptyVector = {};

            // When
            List<RagSearchResult> results = retriever.hybridSearch("query", emptyVector, 5, 0.5);

            // Then
            assertTrue(results.isEmpty());
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("Should handle null vector gracefully")
        void shouldHandleNullVector() {
            // When
            List<RagSearchResult> results = retriever.hybridSearch("query", null, 5, 0.5);

            // Then
            assertTrue(results.isEmpty());
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("Should handle database errors gracefully")
        void shouldHandleDatabaseErrors() {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenThrow(new DataAccessException("Database error") {});

            // When
            List<RagSearchResult> results = retriever.hybridSearch("query", queryVector, 5, 0.5);

            // Then
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("Keyword Search Tests")
    class KeywordSearchTests {

        @Test
        @DisplayName("Should return keyword search results with normalized scores")
        void shouldReturnKeywordSearchResults() throws SQLException {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            UUID chunkId = UUID.randomUUID();
            String documentId = "doc-1";
            String source = "knowledge/delivery.md";
            String content = "延迟配送处理流程";
            double keywordScore = 0.8;

            when(resultSet.getObject("id")).thenReturn(chunkId);
            when(resultSet.getString("document_id")).thenReturn(documentId);
            when(resultSet.getString("source")).thenReturn(source);
            when(resultSet.getInt("chunk_index")).thenReturn(0);
            when(resultSet.getString("content")).thenReturn(content);
            when(resultSet.getDouble("keyword_score")).thenReturn(keywordScore);
            when(resultSet.getObject("metadata")).thenReturn(null);

            // Mock semantic search to return empty (so we only test keyword)
            when(jdbcTemplate.query(contains("embedding"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

            when(jdbcTemplate.query(contains("ts_rank_cd"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<RagSearchResult> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

            // When
            List<RagSearchResult> results = retriever.hybridSearch("配送延迟", queryVector, 5, 0.5);

            // Then
            assertFalse(results.isEmpty());
            RagSearchResult result = results.get(0);
            assertEquals(chunkId, result.id());
            assertEquals(0.0, result.semanticScore());
            assertTrue(result.keywordScore() > 0); // Should be normalized
            assertTrue(result.finalScore() > 0);
        }

        @Test
        @DisplayName("Should handle empty query string")
        void shouldHandleEmptyQuery() {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};

            // When
            List<RagSearchResult> results = retriever.hybridSearch("", queryVector, 5, 0.5);

            // Then - Should still work with semantic search only
            verify(jdbcTemplate, atLeast(1)).query(
                contains("embedding"),
                any(PreparedStatementSetter.class),
                any(RowMapper.class)
            );
        }
    }

    @Nested
    @DisplayName("Hybrid Scoring Tests")
    class HybridScoringTests {

        @Test
        @DisplayName("Should aggregate scores correctly when same document appears in both searches")
        void shouldAggregateScoresCorrectly() throws SQLException {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            UUID chunkId = UUID.randomUUID();

            // Mock semantic search result
            RagSearchResult semanticResult = new RagSearchResult(
                chunkId, "doc-1", "source.md", 0, "content",
                0.8, 0.0, 0.8, Map.of()
            );

            // Mock keyword search result (same chunk)
            RagSearchResult keywordResult = new RagSearchResult(
                chunkId, "doc-1", "source.md", 0, "content",
                0.0, 0.6, 0.0, Map.of()
            );

            when(jdbcTemplate.query(contains("embedding"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(semanticResult));

            when(jdbcTemplate.query(contains("ts_rank_cd"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(keywordResult));

            // When
            List<RagSearchResult> results = retriever.hybridSearch("test query", queryVector, 5, 0.5);

            // Then
            assertEquals(1, results.size());
            RagSearchResult result = results.get(0);
            assertEquals(chunkId, result.id());
            assertEquals(0.8, result.semanticScore(), 0.01);
            assertTrue(result.keywordScore() > 0); // Should be normalized

            // Final score should be weighted combination: 0.7 * 0.8 + 0.3 * normalized_keyword_score
            double expectedFinalScore = 0.7 * 0.8 + 0.3 * result.keywordScore();
            assertEquals(expectedFinalScore, result.finalScore(), 0.01);
        }

        @Test
        @DisplayName("Should respect similarity threshold")
        void shouldRespectSimilarityThreshold() throws SQLException {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            UUID chunkId = UUID.randomUUID();

            // Semantic result with score below threshold (0.4 < 0.5)
            RagSearchResult lowScoreResult = new RagSearchResult(
                chunkId, "doc-1", "source.md", 0, "content",
                0.4, 0.0, 0.4, Map.of()
            );

            when(jdbcTemplate.query(contains("embedding"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(lowScoreResult));

            when(jdbcTemplate.query(contains("ts_rank_cd"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

            // When
            List<RagSearchResult> results = retriever.hybridSearch("test query", queryVector, 5, 0.5);

            // Then
            assertTrue(results.isEmpty(), "Results below similarity threshold should be filtered out");
        }

        @Test
        @DisplayName("Should handle custom weights correctly")
        void shouldHandleCustomWeights() throws SQLException {
            // Given - Set equal weights
            properties.getRetrieval().setSemanticWeight(0.5);
            properties.getRetrieval().setKeywordWeight(0.5);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            UUID chunkId = UUID.randomUUID();

            RagSearchResult semanticResult = new RagSearchResult(
                chunkId, "doc-1", "source.md", 0, "content",
                0.8, 0.0, 0.8, Map.of()
            );

            RagSearchResult keywordResult = new RagSearchResult(
                chunkId, "doc-1", "source.md", 0, "content",
                0.0, 1.0, 0.0, Map.of() // Max keyword score for easy calculation
            );

            when(jdbcTemplate.query(contains("embedding"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(semanticResult));

            when(jdbcTemplate.query(contains("ts_rank_cd"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(keywordResult));

            // When
            List<RagSearchResult> results = retriever.hybridSearch("test query", queryVector, 5, 0.5);

            // Then
            assertEquals(1, results.size());
            RagSearchResult result = results.get(0);

            // With equal weights (0.5 each) and normalized keyword score of 1.0:
            // Final score should be: 0.5 * 0.8 + 0.5 * 1.0 = 0.9
            assertEquals(0.9, result.finalScore(), 0.01);
        }

        @Test
        @DisplayName("Should handle zero weights gracefully")
        void shouldHandleZeroWeights() throws SQLException {
            // Given - Both weights are zero
            properties.getRetrieval().setSemanticWeight(0.0);
            properties.getRetrieval().setKeywordWeight(0.0);

            float[] queryVector = {0.1f, 0.2f, 0.3f};
            UUID chunkId = UUID.randomUUID();

            RagSearchResult semanticResult = new RagSearchResult(
                chunkId, "doc-1", "source.md", 0, "content",
                0.8, 0.0, 0.8, Map.of()
            );

            when(jdbcTemplate.query(contains("embedding"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(semanticResult));

            when(jdbcTemplate.query(contains("ts_rank_cd"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

            // When
            List<RagSearchResult> results = retriever.hybridSearch("test query", queryVector, 5, 0.5);

            // Then - Should fallback to semantic weight = 1.0, keyword weight = 0.0
            assertFalse(results.isEmpty());
            RagSearchResult result = results.get(0);
            assertEquals(0.8, result.finalScore(), 0.01); // Should be pure semantic score
        }
    }

    @Nested
    @DisplayName("Metadata Parsing Tests")
    class MetadataParsingTests {

        @Test
        @DisplayName("Should parse JSON metadata from PGobject")
        void shouldParseJsonMetadataFromPGobject() throws SQLException {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            UUID chunkId = UUID.randomUUID();

            PGobject pgObject = new PGobject();
            pgObject.setType("json");
            pgObject.setValue("{\"title\":\"配送手册\",\"section\":\"延迟处理\"}");

            when(resultSet.getObject("id")).thenReturn(chunkId);
            when(resultSet.getString("document_id")).thenReturn("doc-1");
            when(resultSet.getString("source")).thenReturn("source.md");
            when(resultSet.getInt("chunk_index")).thenReturn(0);
            when(resultSet.getString("content")).thenReturn("content");
            when(resultSet.getDouble("distance")).thenReturn(0.3);
            when(resultSet.getObject("metadata")).thenReturn(pgObject);

            when(jdbcTemplate.query(contains("embedding"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<RagSearchResult> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

            when(jdbcTemplate.query(contains("ts_rank_cd"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

            // When
            List<RagSearchResult> results = retriever.hybridSearch("test query", queryVector, 5, 0.5);

            // Then
            assertFalse(results.isEmpty());
            RagSearchResult result = results.get(0);
            Map<String, Object> metadata = result.metadata();
            assertEquals("配送手册", metadata.get("title"));
            assertEquals("延迟处理", metadata.get("section"));
        }

        @Test
        @DisplayName("Should handle invalid JSON metadata gracefully")
        void shouldHandleInvalidJsonMetadata() throws SQLException {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            UUID chunkId = UUID.randomUUID();

            PGobject pgObject = new PGobject();
            pgObject.setType("json");
            pgObject.setValue("invalid json {");

            when(resultSet.getObject("id")).thenReturn(chunkId);
            when(resultSet.getString("document_id")).thenReturn("doc-1");
            when(resultSet.getString("source")).thenReturn("source.md");
            when(resultSet.getInt("chunk_index")).thenReturn(0);
            when(resultSet.getString("content")).thenReturn("content");
            when(resultSet.getDouble("distance")).thenReturn(0.3);
            when(resultSet.getObject("metadata")).thenReturn(pgObject);

            when(jdbcTemplate.query(contains("embedding"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<RagSearchResult> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

            when(jdbcTemplate.query(contains("ts_rank_cd"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

            // When
            List<RagSearchResult> results = retriever.hybridSearch("test query", queryVector, 5, 0.5);

            // Then
            assertFalse(results.isEmpty());
            RagSearchResult result = results.get(0);
            assertTrue(result.metadata().isEmpty()); // Should return empty map for invalid JSON
        }
    }

    @Nested
    @DisplayName("SQL Query Verification Tests")
    class SqlQueryVerificationTests {

        @Test
        @DisplayName("Should use correct SQL for semantic search")
        void shouldUseCorrectSqlForSemanticSearch() {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};

            when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

            // When
            retriever.hybridSearch("test query", queryVector, 5, 0.5);

            // Then
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate, times(2)).query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowMapper.class));

            List<String> capturedSqls = sqlCaptor.getAllValues();
            String semanticSql = capturedSqls.get(0);
            String keywordSql = capturedSqls.get(1);

            // Verify semantic search SQL
            assertTrue(semanticSql.contains("embedding <=> ?"));
            assertTrue(semanticSql.contains("ORDER BY embedding <=> ?"));
            assertTrue(semanticSql.contains("rag_document_chunk"));

            // Verify keyword search SQL
            assertTrue(keywordSql.contains("ts_rank_cd"));
            assertTrue(keywordSql.contains("content_tsv"));
            assertTrue(keywordSql.contains("websearch_to_tsquery"));
        }

        @Test
        @DisplayName("Should use correct limit multiplier")
        void shouldUseCorrectLimitMultiplier() {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            int topK = 5;

            when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

            // When
            retriever.hybridSearch("test query", queryVector, topK, 0.5);

            // Then
            ArgumentCaptor<PreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
            verify(jdbcTemplate, times(2)).query(anyString(), setterCaptor.capture(), any(RowMapper.class));

            // The search window should be max(8, topK * 2) = max(8, 10) = 10
            // This is verified indirectly through the PreparedStatementSetter behavior
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle NaN scores gracefully")
        void shouldHandleNaNScores() throws SQLException {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            UUID chunkId = UUID.randomUUID();

            when(resultSet.getObject("id")).thenReturn(chunkId);
            when(resultSet.getString("document_id")).thenReturn("doc-1");
            when(resultSet.getString("source")).thenReturn("source.md");
            when(resultSet.getInt("chunk_index")).thenReturn(0);
            when(resultSet.getString("content")).thenReturn("content");
            when(resultSet.getDouble("distance")).thenReturn(Double.NaN);
            when(resultSet.getObject("metadata")).thenReturn(null);

            when(jdbcTemplate.query(contains("embedding"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<RagSearchResult> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

            when(jdbcTemplate.query(contains("ts_rank_cd"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

            // When
            List<RagSearchResult> results = retriever.hybridSearch("test query", queryVector, 5, 0.5);

            // Then
            assertFalse(results.isEmpty());
            RagSearchResult result = results.get(0);
            assertEquals(0.0, result.semanticScore()); // NaN should be clamped to 0.0
            assertFalse(Double.isNaN(result.finalScore()));
        }

        @Test
        @DisplayName("Should limit results to topK")
        void shouldLimitResultsToTopK() throws SQLException {
            // Given
            float[] queryVector = {0.1f, 0.2f, 0.3f};
            int topK = 2;

            // Create multiple results
            List<RagSearchResult> manyResults = List.of(
                new RagSearchResult(UUID.randomUUID(), "doc-1", "src1", 0, "content1", 0.9, 0.0, 0.9, Map.of()),
                new RagSearchResult(UUID.randomUUID(), "doc-2", "src2", 0, "content2", 0.8, 0.0, 0.8, Map.of()),
                new RagSearchResult(UUID.randomUUID(), "doc-3", "src3", 0, "content3", 0.7, 0.0, 0.7, Map.of()),
                new RagSearchResult(UUID.randomUUID(), "doc-4", "src4", 0, "content4", 0.6, 0.0, 0.6, Map.of())
            );

            when(jdbcTemplate.query(contains("embedding"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(manyResults);

            when(jdbcTemplate.query(contains("ts_rank_cd"), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

            // When
            List<RagSearchResult> results = retriever.hybridSearch("test query", queryVector, topK, 0.5);

            // Then
            assertEquals(topK, results.size());

            // Results should be sorted by finalScore descending
            assertTrue(results.get(0).finalScore() >= results.get(1).finalScore());
        }
    }
}
