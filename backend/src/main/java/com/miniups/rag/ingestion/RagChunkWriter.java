package com.miniups.rag.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.rag.config.RagProperties;
import com.miniups.rag.ingestion.RagTextChunker.TextChunk;
import com.pgvector.PGvector;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagChunkWriter {


    private static final Logger log = LoggerFactory.getLogger(RagChunkWriter.class);
    private final JdbcTemplate jdbcTemplate;
    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    // Manual constructor (Lombok @RequiredArgsConstructor not working)
    public RagChunkWriter(JdbcTemplate jdbcTemplate, RagProperties properties, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void upsert(RagDocumentResource resource, TextChunk chunk, float[] embedding) {
        String tableName = properties.resolveTableName();
        UUID chunkId = UUID.nameUUIDFromBytes(
            (resource.documentId() + ":" + chunk.index()).getBytes(StandardCharsets.UTF_8)
        );
        String sql = "INSERT INTO " + tableName +
            " (id, document_id, source, chunk_index, content, embedding, metadata, content_tsv, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, to_tsvector('simple', ?), NOW(), NOW()) " +
            "ON CONFLICT (id) DO UPDATE SET " +
            "content = EXCLUDED.content, " +
            "embedding = EXCLUDED.embedding, " +
            "metadata = EXCLUDED.metadata, " +
            "content_tsv = EXCLUDED.content_tsv, " +
            "updated_at = NOW()";
        try {
            String metadataJson = objectMapper.writeValueAsString(chunk.metadata());
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setObject(1, chunkId);
                ps.setString(2, resource.documentId());
                ps.setString(3, resource.source());
                ps.setInt(4, chunk.index());
                ps.setString(5, chunk.content());
                ps.setObject(6, new PGvector(embedding));
                ps.setString(7, metadataJson);
                ps.setString(8, chunk.content());
                return ps;
            });
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize metadata for document {} chunk {}", resource.documentId(), chunk.index(), ex);
        } catch (RuntimeException ex) {
            log.error("Failed to upsert RAG chunk {}:{}", resource.documentId(), chunk.index(), ex);
            throw ex;
        }
    }
}
