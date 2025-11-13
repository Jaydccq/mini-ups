package com.miniups.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@TestConfiguration
public class TestShortLinkJdbcConfig {

    @Bean
    @Qualifier("shortLinkJdbcTemplate")
    public NamedParameterJdbcTemplate shortLinkJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
