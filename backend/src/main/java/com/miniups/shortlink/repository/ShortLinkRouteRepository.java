package com.miniups.shortlink.repository;

import com.miniups.shortlink.model.ShortLinkRouteRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ShortLinkRouteRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<ShortLinkRouteRecord> ROUTE_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ShortLinkRouteRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            ShortLinkRouteRecord record = new ShortLinkRouteRecord();
            record.setId(rs.getLong("id"));
            record.setShortCode(rs.getString("short_code"));
            record.setUserId(rs.getLong("user_id"));
            record.setDataSource(rs.getString("data_source"));
            record.setTableName(rs.getString("table_name"));
            record.setOriginalUrl(rs.getString("original_url"));
            Timestamp created = rs.getTimestamp("created_at");
            record.setCreatedAt(created == null ? null : created.toLocalDateTime());
            return record;
        }
    };

    public ShortLinkRouteRepository(@org.springframework.beans.factory.annotation.Qualifier("shortLinkJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertRoute(ShortLinkRouteRecord record) {
        String sql = "INSERT INTO short_link_route (short_code, user_id, data_source, table_name, original_url, created_at) "
                + "VALUES (:shortCode, :userId, :dataSource, :tableName, :originalUrl, :createdAt)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("shortCode", record.getShortCode())
                .addValue("userId", record.getUserId())
                .addValue("dataSource", record.getDataSource())
                .addValue("tableName", record.getTableName())
                .addValue("originalUrl", record.getOriginalUrl())
                .addValue("createdAt", record.getCreatedAt());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        if (keyHolder.getKey() != null) {
            record.setId(keyHolder.getKey().longValue());
        }
    }

    public List<ShortLinkRouteRecord> listRoutes(int page, int size) {
        String sql = "SELECT id, short_code, user_id, data_source, table_name, original_url, created_at "
                + "FROM short_link_route ORDER BY created_at DESC LIMIT :limit OFFSET :offset";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", size)
                .addValue("offset", page * size);
        return jdbcTemplate.query(sql, params, ROUTE_ROW_MAPPER);
    }

    public long countRoutes() {
        String sql = "SELECT COUNT(1) FROM short_link_route";
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return count == null ? 0 : count;
    }

    public void updateOriginalUrl(String shortCode, String originalUrl) {
        String sql = "UPDATE short_link_route SET original_url = :originalUrl WHERE short_code = :shortCode";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("originalUrl", originalUrl)
                .addValue("shortCode", shortCode));
    }

    public void deleteRoute(String shortCode) {
        String sql = "DELETE FROM short_link_route WHERE short_code = :shortCode";
        jdbcTemplate.update(sql, new MapSqlParameterSource("shortCode", shortCode));
    }

    public Map<String, Object> loadRoute(String shortCode) {
        String sql = "SELECT data_source, table_name FROM short_link_route WHERE short_code = :shortCode";
        List<Map<String, Object>> routes = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("shortCode", shortCode));
        return routes.isEmpty() ? new HashMap<>() : routes.get(0);
    }
}
