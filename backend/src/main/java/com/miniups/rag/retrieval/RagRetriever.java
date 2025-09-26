package com.miniups.rag.retrieval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.rag.config.RagProperties;
import com.pgvector.PGvector;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagRetriever {

    private static final int MIN_SEARCH_WINDOW = 8;

    private final JdbcTemplate jdbcTemplate;
    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    public List<RagSearchResult> hybridSearch(String query, float[] queryVector, int topK, double similarityThreshold) {
        if (queryVector == null || queryVector.length == 0) {
            return List.of();
        }

        int searchWindow = Math.max(MIN_SEARCH_WINDOW, topK * 2);
        List<RagSearchResult> semanticResults = searchByVector(queryVector, searchWindow);
        List<RagSearchResult> keywordResults = searchByKeyword(query, searchWindow);

        double semanticWeight = Math.max(0.0, properties.getRetrieval().getSemanticWeight());
        double keywordWeight = Math.max(0.0, properties.getRetrieval().getKeywordWeight());
        double weightSum = semanticWeight + keywordWeight;
        if (weightSum <= 0) {
            semanticWeight = 1.0;
            keywordWeight = 0.0;
            weightSum = 1.0;
        }
        double semanticShare = semanticWeight / weightSum;
        double keywordShare = keywordWeight / weightSum;

        Map<UUID, AggregatedResult> aggregated = new LinkedHashMap<>();
        for (RagSearchResult semantic : semanticResults) {
            AggregatedResult agg = aggregated.computeIfAbsent(semantic.id(), id -> AggregatedResult.from(semantic));
            agg.semanticScore = Math.max(agg.semanticScore, clamp01(semantic.semanticScore()));
        }

        double maxKeywordScore = keywordResults.stream()
            .mapToDouble(RagSearchResult::keywordScore)
            .max()
            .orElse(0.0);
        for (RagSearchResult keyword : keywordResults) {
            double normalized = maxKeywordScore > 0 ? keyword.keywordScore() / maxKeywordScore : 0.0;
            AggregatedResult agg = aggregated.computeIfAbsent(keyword.id(), id -> AggregatedResult.from(keyword));
            agg.keywordScore = Math.max(agg.keywordScore, clamp01(normalized));
        }

        List<RagSearchResult> finalResults = new ArrayList<>();
        for (AggregatedResult agg : aggregated.values()) {
            if (agg.semanticScore > 0 && agg.semanticScore < similarityThreshold) {
                continue;
            }
            if (agg.semanticScore == 0.0 && agg.keywordScore == 0.0) {
                continue;
            }
            double finalScore = clamp01((semanticShare * agg.semanticScore) + (keywordShare * agg.keywordScore));
            finalResults.add(agg.toResult(finalScore));
        }

        finalResults.sort((left, right) -> Double.compare(right.finalScore(), left.finalScore()));
        if (finalResults.size() > topK) {
            return new ArrayList<>(finalResults.subList(0, topK));
        }
        return finalResults;
    }

    private List<RagSearchResult> searchByVector(float[] queryVector, int limit) {
        String tableName = properties.resolveTableName();
        String sql = "SELECT id, document_id, source, chunk_index, content, metadata, (embedding <=> ?) AS distance " +
            "FROM " + tableName + " " +
            "ORDER BY embedding <=> ? " +
            "LIMIT ?";
        try {
            return jdbcTemplate.query(sql, ps -> {
                PGvector vector = new PGvector(queryVector);
                ps.setObject(1, vector);
                ps.setObject(2, vector);
                ps.setInt(3, limit);
            }, new SemanticRowMapper());
        } catch (DataAccessException ex) {
            log.error("Vector search failed", ex);
            return List.of();
        }
    }

    private List<RagSearchResult> searchByKeyword(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        String tableName = properties.resolveTableName();
        String trimmed = query.trim();
        String sql = "SELECT id, document_id, source, chunk_index, content, metadata, " +
            "ts_rank_cd(content_tsv, websearch_to_tsquery('simple', ?)) AS keyword_score " +
            "FROM " + tableName + " " +
            "WHERE content_tsv @@ websearch_to_tsquery('simple', ?) " +
            "ORDER BY keyword_score DESC " +
            "LIMIT ?";
        try {
            return jdbcTemplate.query(sql, ps -> {
                ps.setString(1, trimmed);
                ps.setString(2, trimmed);
                ps.setInt(3, limit);
            }, new KeywordRowMapper());
        } catch (DataAccessException ex) {
            log.error("Keyword search failed", ex);
            return List.of();
        }
    }

    private Map<String, Object> parseMetadata(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        try {
            if (raw instanceof PGobject pgObject) {
                return objectMapper.readValue(pgObject.getValue(), Map.class);
            }
            if (raw instanceof String str) {
                return objectMapper.readValue(str, Map.class);
            }
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse RAG metadata", ex);
        }
        return Map.of();
    }

    private double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private final class SemanticRowMapper implements RowMapper<RagSearchResult> {
        @Override
        public RagSearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = (UUID) rs.getObject("id");
            String documentId = rs.getString("document_id");
            String source = rs.getString("source");
            int chunkIndex = rs.getInt("chunk_index");
            String content = rs.getString("content");
            double distance = rs.getDouble("distance");
            double semanticScore = clamp01(1 - distance);
            Map<String, Object> metadata = parseMetadata(rs.getObject("metadata"));
            return new RagSearchResult(
                id,
                documentId,
                source,
                chunkIndex,
                content,
                semanticScore,
                0.0,
                semanticScore,
                metadata
            );
        }
    }

    private final class KeywordRowMapper implements RowMapper<RagSearchResult> {
        @Override
        public RagSearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = (UUID) rs.getObject("id");
            String documentId = rs.getString("document_id");
            String source = rs.getString("source");
            int chunkIndex = rs.getInt("chunk_index");
            String content = rs.getString("content");
            double keywordScore = Math.max(0.0, rs.getDouble("keyword_score"));
            Map<String, Object> metadata = parseMetadata(rs.getObject("metadata"));
            return new RagSearchResult(
                id,
                documentId,
                source,
                chunkIndex,
                content,
                0.0,
                keywordScore,
                0.0,
                metadata
            );
        }
    }

    private static final class AggregatedResult {
        private final UUID id;
        private final String documentId;
        private final String source;
        private final int chunkIndex;
        private final String content;
        private final Map<String, Object> metadata;
        private double semanticScore;
        private double keywordScore;

        private AggregatedResult(UUID id, String documentId, String source, int chunkIndex, String content, Map<String, Object> metadata) {
            this.id = id;
            this.documentId = documentId;
            this.source = source;
            this.chunkIndex = chunkIndex;
            this.content = content;
            this.metadata = metadata;
        }

        static AggregatedResult from(RagSearchResult result) {
            return new AggregatedResult(
                result.id(),
                result.documentId(),
                result.source(),
                result.chunkIndex(),
                result.content(),
                result.metadata()
            );
        }

        RagSearchResult toResult(double finalScore) {
            return new RagSearchResult(
                id,
                documentId,
                source,
                chunkIndex,
                content,
                semanticScore,
                keywordScore,
                finalScore,
                metadata
            );
        }
    }
}
