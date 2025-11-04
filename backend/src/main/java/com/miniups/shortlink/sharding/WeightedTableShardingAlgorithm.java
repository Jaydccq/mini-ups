package com.miniups.shortlink.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;

/**
 * Weighted table sharding ensuring smooth expansion without data migration.
 */
public class WeightedTableShardingAlgorithm implements StandardShardingAlgorithm<String> {

    private static final String TABLE_WEIGHTS_KEY = "tableWeights";
    private NavigableMap<Integer, String> weightMap;

    @Override
    public void init(Properties props) {
        String weights = props.getProperty(TABLE_WEIGHTS_KEY);
        if (weights == null || weights.isBlank()) {
            throw new IllegalArgumentException("tableWeights property is required for WeightedTableShardingAlgorithm");
        }
        Map<String, Integer> parsed = parseWeights(weights.trim());
        this.weightMap = ShortLinkShardUtils.buildWeightMap(parsed);
    }

    private Map<String, Integer> parseWeights(String weights) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String[] pairs = weights.split(",");
        for (String pair : pairs) {
            String[] kv = pair.trim().split(":");
            if (kv.length != 2) {
                continue;
            }
            String table = kv[0].trim();
            String rawWeight = kv[1].trim();
            if (table.isEmpty() || rawWeight.isEmpty()) {
                continue;
            }
            try {
                int weight = Integer.parseInt(rawWeight);
                if (weight <= 0) {
                    continue;
                }
                result.put(table, weight);
            } catch (NumberFormatException ignored) {
                // Skip invalid weight entries
            }
        }
        return result;
    }

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<String> shardingValue) {
        String code = shardingValue.getValue();
        String targetTable = ShortLinkShardUtils.resolveTable(code, weightMap);
        if (!availableTargetNames.contains(targetTable)) {
            throw new IllegalStateException("Resolved table " + targetTable + " is not in available target names " + availableTargetNames);
        }
        return targetTable;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<String> shardingValue) {
        // Range queries scatter across all nodes for simplicity
        return availableTargetNames;
    }

    public Properties getProps() {
        Properties properties = new Properties();
        properties.setProperty(TABLE_WEIGHTS_KEY, weightMap == null ? "" : "configured");
        return properties;
    }

    @Override
    public String getType() {
        return "WEIGHTED_SHORT_LINK";
    }

    public NavigableMap<Integer, String> getWeightMap() {
        return weightMap == null ? ShortLinkShardUtils.buildWeightMap(Collections.emptyMap()) : weightMap;
    }
}
