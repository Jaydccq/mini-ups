package com.miniups.shortlink.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeightedTableShardingAlgorithmTest {

    private WeightedTableShardingAlgorithm algorithm;
    private Properties properties;

    @BeforeEach
    void setUp() {
        algorithm = new WeightedTableShardingAlgorithm();
        properties = new Properties();
    }

    @Test
    void init_shouldParseWeightsCorrectly() {
        properties.setProperty("tableWeights", "short_links_0:4,short_links_1:3,short_links_2:2,short_links_3:1");

        algorithm.init(properties);

        NavigableMap<Integer, String> weightMap = algorithm.getWeightMap();
        assertThat(weightMap).hasSize(4);
        assertThat(weightMap.get(4)).isEqualTo("short_links_0");
        assertThat(weightMap.get(7)).isEqualTo("short_links_1");
        assertThat(weightMap.get(9)).isEqualTo("short_links_2");
        assertThat(weightMap.get(10)).isEqualTo("short_links_3");
    }

    @Test
    void init_shouldThrowExceptionWhenTableWeightsMissing() {
        assertThatThrownBy(() -> algorithm.init(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tableWeights property is required for WeightedTableShardingAlgorithm");
    }

    @Test
    void init_shouldThrowExceptionWhenTableWeightsEmpty() {
        properties.setProperty("tableWeights", "");

        assertThatThrownBy(() -> algorithm.init(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tableWeights property is required for WeightedTableShardingAlgorithm");
    }

    @Test
    void init_shouldThrowExceptionWhenTableWeightsBlank() {
        properties.setProperty("tableWeights", "   ");

        assertThatThrownBy(() -> algorithm.init(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tableWeights property is required for WeightedTableShardingAlgorithm");
    }

    @Test
    void doSharding_precise_shouldReturnCorrectTable() {
        properties.setProperty("tableWeights", "short_links_0:4,short_links_1:3,short_links_2:2,short_links_3:1");
        algorithm.init(properties);

        List<String> availableTargets = Arrays.asList("short_links_0", "short_links_1", "short_links_2", "short_links_3");

        // Test with a known short code that should map to a specific table
        PreciseShardingValue<String> shardingValue = createPreciseShardingValue("abc123");

        String result = algorithm.doSharding(availableTargets, shardingValue);

        assertThat(availableTargets).contains(result);
        assertThat(result).startsWith("short_links_");
    }

    @Test
    void doSharding_precise_shouldBeConsistent() {
        properties.setProperty("tableWeights", "short_links_0:4,short_links_1:3,short_links_2:2,short_links_3:1");
        algorithm.init(properties);

        List<String> availableTargets = Arrays.asList("short_links_0", "short_links_1", "short_links_2", "short_links_3");
        String testCode = "consistent_test";
        PreciseShardingValue<String> shardingValue = createPreciseShardingValue(testCode);

        // Call multiple times and ensure consistent results
        String result1 = algorithm.doSharding(availableTargets, shardingValue);
        String result2 = algorithm.doSharding(availableTargets, shardingValue);
        String result3 = algorithm.doSharding(availableTargets, shardingValue);

        assertThat(result1).isEqualTo(result2).isEqualTo(result3);
    }

    @Test
    void doSharding_precise_shouldThrowExceptionWhenTargetNotAvailable() {
        properties.setProperty("tableWeights", "short_links_0:4,short_links_1:3");
        algorithm.init(properties);

        // Available targets don't include the resolved table
        List<String> availableTargets = Arrays.asList("other_table_0", "other_table_1");
        PreciseShardingValue<String> shardingValue = createPreciseShardingValue("abc123");

        assertThatThrownBy(() -> algorithm.doSharding(availableTargets, shardingValue))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not in available target names");
    }

    @Test
    void doSharding_range_shouldReturnAllAvailableTargets() {
        properties.setProperty("tableWeights", "short_links_0:4,short_links_1:3");
        algorithm.init(properties);

        List<String> availableTargets = Arrays.asList("short_links_0", "short_links_1");
        RangeShardingValue<String> rangeValue = createRangeShardingValue("a", "z");

        Collection<String> result = algorithm.doSharding(availableTargets, rangeValue);

        assertThat(result).containsExactlyInAnyOrderElementsOf(availableTargets);
    }

    @Test
    void doSharding_shouldDistributeAccordingToWeights() {
        properties.setProperty("tableWeights", "short_links_0:8,short_links_1:2");
        algorithm.init(properties);

        List<String> availableTargets = Arrays.asList("short_links_0", "short_links_1");

        int table0Count = 0;
        int table1Count = 0;
        int totalTests = 1000;

        // Test distribution over many short codes
        for (int i = 0; i < totalTests; i++) {
            String testCode = "test_" + i;
            PreciseShardingValue<String> shardingValue = createPreciseShardingValue(testCode);
            String result = algorithm.doSharding(availableTargets, shardingValue);

            if ("short_links_0".equals(result)) {
                table0Count++;
            } else if ("short_links_1".equals(result)) {
                table1Count++;
            }
        }

        // With 8:2 ratio, we expect roughly 80% to 20% distribution
        // Allow some variance due to hash distribution
        double table0Ratio = (double) table0Count / totalTests;
        double table1Ratio = (double) table1Count / totalTests;

        assertThat(table0Ratio).isBetween(0.75, 0.85); // Should be around 0.8
        assertThat(table1Ratio).isBetween(0.15, 0.25); // Should be around 0.2
    }

    @Test
    void getType_shouldReturnCorrectType() {
        String type = algorithm.getType();

        assertThat(type).isEqualTo("WEIGHTED_SHORT_LINK");
    }

    @Test
    void getProps_shouldReturnPropertiesWithConfiguredFlag() {
        properties.setProperty("tableWeights", "short_links_0:4,short_links_1:3");
        algorithm.init(properties);

        Properties result = algorithm.getProps();

        assertThat(result.getProperty("tableWeights")).isEqualTo("configured");
    }

    @Test
    void getProps_shouldReturnEmptyWhenNotInitialized() {
        Properties result = algorithm.getProps();

        assertThat(result.getProperty("tableWeights")).isEmpty();
    }

    @Test
    void parseWeights_shouldSkipInvalidEntries() {
        properties.setProperty("tableWeights", "short_links_0:4,invalid_entry,short_links_1:3,:2,table_no_weight:");
        algorithm.init(properties);

        NavigableMap<Integer, String> weightMap = algorithm.getWeightMap();

        // Should only include valid entries
        assertThat(weightMap).hasSize(2);
        assertThat(weightMap.get(4)).isEqualTo("short_links_0");
        assertThat(weightMap.get(7)).isEqualTo("short_links_1");
    }

    @Test
    void getWeightMap_shouldReturnEmptyMapWhenNotInitialized() {
        NavigableMap<Integer, String> weightMap = algorithm.getWeightMap();

        assertThat(weightMap).isEmpty();
    }

    private PreciseShardingValue<String> createPreciseShardingValue(String value) {
        PreciseShardingValue<String> shardingValue = mock(PreciseShardingValue.class);
        when(shardingValue.getValue()).thenReturn(value);
        return shardingValue;
    }

    private RangeShardingValue<String> createRangeShardingValue(String lower, String upper) {
        RangeShardingValue<String> rangeValue = mock(RangeShardingValue.class);
        // Range sharding doesn't use the actual values in our implementation
        return rangeValue;
    }
}