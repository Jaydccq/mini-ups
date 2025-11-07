package com.miniups.rag.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagDatabaseInitializer {


    private static final Logger log = LoggerFactory.getLogger(RagDatabaseInitializer.class);
    private final JdbcTemplate jdbcTemplate;
    private final RagProperties properties;

    public RagDatabaseInitializer(JdbcTemplate jdbcTemplate, RagProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @PostConstruct
    public void onStartup() {
        initialize();
    }

    public void initialize() {
        String tableName = properties.resolveTableName();
        String sanitized = sanitize(tableName);
        int dimensions = properties.getEmbedding().getDimensions();
        int lists = Math.max(1, properties.getStorage().getIvfLists());

        log.info("Initializing RAG storage table '{}' with dimension {}", tableName, dimensions);

        execute("CREATE EXTENSION IF NOT EXISTS vector", "pgvector extension");
        execute(String.format(
            "CREATE TABLE IF NOT EXISTS %s (" +
                "id UUID PRIMARY KEY," +
                "document_id VARCHAR(255)," +
                "source VARCHAR(255) NOT NULL," +
                "chunk_index INTEGER NOT NULL," +
                "content TEXT NOT NULL," +
                "embedding vector(%d)," +
                "metadata JSONB," +
                "content_tsv tsvector," +
                "created_at TIMESTAMPTZ DEFAULT NOW()," +
                "updated_at TIMESTAMPTZ DEFAULT NOW())",
            tableName,
            dimensions
        ), "RAG storage table");

        execute(String.format(
            "ALTER TABLE %s ADD COLUMN IF NOT EXISTS content_tsv tsvector",
            tableName
        ), "RAG tsvector column");

        execute(String.format(
            "CREATE INDEX IF NOT EXISTS idx_%s_source ON %s (source)",
            sanitized,
            tableName
        ), "RAG source index");

        execute(String.format(
            "CREATE UNIQUE INDEX IF NOT EXISTS uidx_%s_document_chunk ON %s (document_id, chunk_index)",
            sanitized,
            tableName
        ), "RAG document chunk uniqueness");

        execute(String.format(
            "CREATE INDEX IF NOT EXISTS idx_%s_embedding ON %s USING ivfflat (embedding vector_cosine_ops) WITH (lists = %d)",
            sanitized,
            tableName,
            lists
        ), "RAG embedding index");

        execute(String.format(
            "CREATE INDEX IF NOT EXISTS idx_%s_content_tsv ON %s USING GIN (content_tsv)",
            sanitized,
            tableName
        ), "RAG tsvector index");

        execute(String.format(
            "UPDATE %s SET content_tsv = to_tsvector('simple', content) WHERE content_tsv IS NULL",
            tableName
        ), "RAG tsvector backfill");

        execute("CREATE TABLE IF NOT EXISTS rag_ingestion_job (" +
            "id UUID PRIMARY KEY," +
            "started_at TIMESTAMPTZ NOT NULL DEFAULT NOW()," +
            "completed_at TIMESTAMPTZ," +
            "status VARCHAR(32) NOT NULL," +
            "trigger VARCHAR(32) NOT NULL," +
            "documents_processed INTEGER DEFAULT 0," +
            "chunks_processed INTEGER DEFAULT 0," +
            "message TEXT," +
            "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()" +
            ")", "RAG ingestion job table");

        execute("CREATE INDEX IF NOT EXISTS idx_rag_ingestion_job_started_at ON rag_ingestion_job (started_at DESC)",
            "RAG ingestion job started index");
    }

    private String sanitize(String value) {
        String cleaned = value.replaceAll("[^a-zA-Z0-9]+", "_");
        if (!StringUtils.hasText(cleaned)) {
            return "rag_embedding";
        }
        return cleaned.length() <= 48 ? cleaned : cleaned.substring(0, 48);
    }

    private void execute(String sql, String description) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ex) {
            throw new IllegalStateException("Failed to initialize " + description + " using SQL: " + sql, ex);
        }
    }
}
