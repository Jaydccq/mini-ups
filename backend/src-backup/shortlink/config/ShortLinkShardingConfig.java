package com.miniups.shortlink.config;

import com.miniups.shortlink.sharding.WeightedTableShardingAlgorithm;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.sharding.api.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class ShortLinkShardingConfig {

    private final DataSource primaryDataSource;
    private final ShortLinkProperties shortLinkProperties;

    public ShortLinkShardingConfig(DataSource primaryDataSource, ShortLinkProperties shortLinkProperties) {
        this.primaryDataSource = primaryDataSource;
        this.shortLinkProperties = shortLinkProperties;
    }

    @Bean(name = "shortLinkShardingDataSource")
    public DataSource shortLinkShardingDataSource() throws SQLException {
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put("ds0", primaryDataSource);

        ShardingTableRuleConfiguration tableRule = new ShardingTableRuleConfiguration("short_links", "ds0.short_links_$->{0..3}");
        tableRule.setTableShardingStrategy(new StandardShardingStrategyConfiguration("shard_key", "shortlink-weighted"));
        tableRule.setKeyGenerateStrategy(new KeyGenerateStrategyConfiguration("id", "snowflake"));

        ShardingRuleConfiguration ruleConfiguration = new ShardingRuleConfiguration();
        ruleConfiguration.getTables().add(tableRule);
        ruleConfiguration.setDefaultDataSourceName("ds0");

        Properties weightedProps = new Properties();
        weightedProps.setProperty("algorithmClassName", WeightedTableShardingAlgorithm.class.getName());
        weightedProps.setProperty("tableWeights", shortLinkProperties.getSharding().getTableWeights());
        ruleConfiguration.getShardingAlgorithms().put("shortlink-weighted",
                new ShardingSphereAlgorithmConfiguration("CLASS_BASED", weightedProps));

        Properties snowflakeProps = new Properties();
        snowflakeProps.setProperty("worker-id", "17");
        ruleConfiguration.getKeyGenerators().put("snowflake",
                new ShardingSphereAlgorithmConfiguration("SNOWFLAKE", snowflakeProps));

        Properties globalProps = new Properties();
        globalProps.setProperty("sql-show", "false");

        return ShardingSphereDataSourceFactory.createDataSource(dataSourceMap, Collections.singleton(ruleConfiguration), globalProps);
    }

    @Bean(name = "shortLinkJdbcTemplate")
    public NamedParameterJdbcTemplate shortLinkJdbcTemplate(@Qualifier("shortLinkShardingDataSource") DataSource shardingDataSource) {
        return new NamedParameterJdbcTemplate(shardingDataSource);
    }
}
