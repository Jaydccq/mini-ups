package com.miniups.shortlink.repository;

import com.miniups.shortlink.model.ShortLinkRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ShortLinkRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<ShortLinkRecord> ROW_MAPPER = new RowMapper<>() {
        @Override
        public ShortLinkRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            ShortLinkRecord record = new ShortLinkRecord();
            record.setId(rs.getLong("id"));
            record.setShortCode(rs.getString("short_code"));
            record.setShardKey(rs.getString("shard_key"));
            record.setOriginalUrl(rs.getString("original_url"));
            long userId = rs.getLong("user_id");
            record.setUserId(rs.wasNull() ? null : userId);
            Timestamp expiration = rs.getTimestamp("expiration_at");
            record.setExpirationAt(expiration == null ? null : expiration.toLocalDateTime());
            record.setActive(rs.getBoolean("active"));
            record.setAccessCount(rs.getLong("access_count"));
            Timestamp created = rs.getTimestamp("created_at");
            record.setCreatedAt(created == null ? null : created.toLocalDateTime());
            Timestamp updated = rs.getTimestamp("updated_at");
            record.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
            Timestamp lastAccess = rs.getTimestamp("last_access_at");
            record.setLastAccessAt(lastAccess == null ? null : lastAccess.toLocalDateTime());
            return record;
        }
    };

    public ShortLinkRepository(@org.springframework.beans.factory.annotation.Qualifier("shortLinkJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ShortLinkRecord> findByShortCode(String shortCode) {
        String sql = "SELECT id, short_code, shard_key, original_url, user_id, expiration_at, active, access_count, created_at, updated_at, last_access_at "
                + "FROM short_links WHERE short_code = :code LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource("code", shortCode);
        return jdbcTemplate.query(sql, params, ROW_MAPPER).stream().findFirst();
    }

    public ShortLinkRecord insert(ShortLinkRecord record) {
        String sql = "INSERT INTO short_links (short_code, shard_key, original_url, user_id, expiration_at, active, access_count, created_at, updated_at) "
                + "VALUES (:shortCode, :shardKey, :originalUrl, :userId, :expirationAt, :active, :accessCount, :createdAt, :updatedAt)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("shortCode", record.getShortCode())
                .addValue("shardKey", record.getShardKey())
                .addValue("originalUrl", record.getOriginalUrl())
                .addValue("userId", record.getUserId())
                .addValue("expirationAt", record.getExpirationAt())
                .addValue("active", record.isActive())
                .addValue("accessCount", record.getAccessCount())
                .addValue("createdAt", record.getCreatedAt())
                .addValue("updatedAt", record.getUpdatedAt());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key != null) {
            record.setId(key.longValue());
        }
        return record;
    }

    public int updateOriginalUrl(String shortCode, String newUrl, LocalDateTime updatedAt, LocalDateTime expirationAt, boolean active) {
        String sql = "UPDATE short_links SET original_url = :originalUrl, updated_at = :updatedAt, expiration_at = :expirationAt, active = :active "
                + "WHERE short_code = :shortCode";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("originalUrl", newUrl)
                .addValue("updatedAt", updatedAt)
                .addValue("expirationAt", expirationAt)
                .addValue("active", active)
                .addValue("shortCode", shortCode);
        return jdbcTemplate.update(sql, params);
    }

    public int incrementAccessCount(String shortCode, LocalDateTime accessedAt) {
        String sql = "UPDATE short_links SET access_count = access_count + 1, last_access_at = :lastAccess, updated_at = :updatedAt "
                + "WHERE short_code = :shortCode";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lastAccess", accessedAt)
                .addValue("updatedAt", accessedAt)
                .addValue("shortCode", shortCode);
        return jdbcTemplate.update(sql, params);
    }

    public int deactivateIfExpired(String shortCode, LocalDateTime now) {
        String sql = "UPDATE short_links SET active = false, updated_at = :updatedAt "
                + "WHERE short_code = :shortCode AND expiration_at IS NOT NULL AND expiration_at < :now";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("updatedAt", now)
                .addValue("now", now)
                .addValue("shortCode", shortCode);
        return jdbcTemplate.update(sql, params);
    }
}
