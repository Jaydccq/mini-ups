package com.miniups.shortlink.repository;

import com.miniups.shortlink.model.ShortLinkAccessLogRecord;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShortLinkAccessLogRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ShortLinkAccessLogRepository(@org.springframework.beans.factory.annotation.Qualifier("shortLinkJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(ShortLinkAccessLogRecord record) {
        String sql = "INSERT INTO short_link_access_log (short_code, owner_user_id, accessed_at, client_ip, user_agent) "
                + "VALUES (:shortCode, :ownerUserId, :accessedAt, :clientIp, :userAgent)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("shortCode", record.getShortCode())
                .addValue("ownerUserId", record.getOwnerUserId())
                .addValue("accessedAt", record.getAccessedAt())
                .addValue("clientIp", record.getClientIp())
                .addValue("userAgent", record.getUserAgent());
        jdbcTemplate.update(sql, params);
    }
}
