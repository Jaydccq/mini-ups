package com.miniups.shortlink.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class ShortLinkShardingConfig {

    /**
     * Temporary simplified configuration to provide the shortLinkJdbcTemplate bean.
     * This uses the primary DataSource instead of sharded configuration for now.
     */
    @Bean(name = "shortLinkJdbcTemplate")
    public NamedParameterJdbcTemplate shortLinkJdbcTemplate(DataSource primaryDataSource) {
        return new NamedParameterJdbcTemplate(primaryDataSource);
    }
}