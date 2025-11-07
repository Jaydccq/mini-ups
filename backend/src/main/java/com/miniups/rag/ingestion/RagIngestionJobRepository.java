package com.miniups.rag.ingestion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RagIngestionJobRepository {

    private final JdbcTemplate jdbcTemplate;

    public RagIngestionJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID startJob(String trigger) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO rag_ingestion_job (id, status, trigger) VALUES (?, ?, ?)",
            id,
            "RUNNING",
            trigger
        );
        return id;
    }

    public void markCompleted(UUID id, int documents, int chunks) {
        jdbcTemplate.update(
            "UPDATE rag_ingestion_job SET status = ?, completed_at = NOW(), documents_processed = ?, chunks_processed = ? WHERE id = ?",
            "COMPLETED",
            documents,
            chunks,
            id
        );
    }

    public void markFailed(UUID id, String message) {
        jdbcTemplate.update(
            "UPDATE rag_ingestion_job SET status = ?, completed_at = NOW(), message = ? WHERE id = ?",
            "FAILED",
            message,
            id
        );
    }

    public RagIngestionJobSummary findLatest() {
        var results = jdbcTemplate.query(
            "SELECT * FROM rag_ingestion_job ORDER BY started_at DESC LIMIT 1",
            new JobRowMapper()
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public RagIngestionJobSummary findById(UUID id) {
        var results = jdbcTemplate.query(
            "SELECT * FROM rag_ingestion_job WHERE id = ?",
            new JobRowMapper(),
            id
        );
        return results.isEmpty() ? null : results.get(0);
    }

    private static class JobRowMapper implements RowMapper<RagIngestionJobSummary> {
        @Override
        public RagIngestionJobSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RagIngestionJobSummary(
                (UUID) rs.getObject("id"),
                toOffsetDateTime(rs, "started_at"),
                toOffsetDateTime(rs, "completed_at"),
                rs.getString("status"),
                rs.getString("trigger"),
                rs.getInt("documents_processed"),
                rs.getInt("chunks_processed"),
                rs.getString("message")
            );
        }

        private OffsetDateTime toOffsetDateTime(ResultSet rs, String column) throws SQLException {
            java.sql.Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
        }
    }
}
