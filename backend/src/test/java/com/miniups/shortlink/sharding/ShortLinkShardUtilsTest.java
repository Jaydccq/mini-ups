package com.miniups.shortlink.sharding;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortLinkShardUtilsTest {

    @Test
    void parseWeights_shouldParseValidWeightString() {
        String weights = "short_links_0:4,short_links_1:3,short_links_2:2,short_links_3:1";

        Map<String, Integer> result = ShortLinkShardUtils.parseWeights(weights);

        assertThat(result).hasSize(4);
        assertThat(result.get("short_links_0")).isEqualTo(4);
        assertThat(result.get("short_links_1")).isEqualTo(3);
        assertThat(result.get("short_links_2")).isEqualTo(2);
        assertThat(result.get("short_links_3")).isEqualTo(1);
    }

    @Test
    void parseWeights_shouldReturnEmptyMapForNullInput() {
        Map<String, Integer> result = ShortLinkShardUtils.parseWeights(null);

        assertThat(result).isEmpty();
    }

    @Test
    void parseWeights_shouldReturnEmptyMapForBlankInput() {
        Map<String, Integer> result = ShortLinkShardUtils.parseWeights("   ");

        assertThat(result).isEmpty();
    }

    @Test
    void parseWeights_shouldSkipInvalidEntries() {
        String weights = "short_links_0:4,invalid_entry,short_links_1:3,:2,table_no_weight:";

        Map<String, Integer> result = ShortLinkShardUtils.parseWeights(weights);

        assertThat(result).hasSize(2);
        assertThat(result.get("short_links_0")).isEqualTo(4);
        assertThat(result.get("short_links_1")).isEqualTo(3);
    }

    @Test
    void parseWeights_shouldHandleWhitespace() {
        String weights = " short_links_0 : 4 , short_links_1 : 3 ";

        Map<String, Integer> result = ShortLinkShardUtils.parseWeights(weights);

        assertThat(result).hasSize(2);
        assertThat(result.get("short_links_0")).isEqualTo(4);
        assertThat(result.get("short_links_1")).isEqualTo(3);
    }

    @Test
    void buildWeightMap_shouldCreateCumulativeWeightMap() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("short_links_0", 4);
        weights.put("short_links_1", 3);
        weights.put("short_links_2", 2);
        weights.put("short_links_3", 1);

        NavigableMap<Integer, String> result = ShortLinkShardUtils.buildWeightMap(weights);

        assertThat(result).hasSize(4);
        assertThat(result.get(4)).isEqualTo("short_links_0");  // 0 + 4 = 4
        assertThat(result.get(7)).isEqualTo("short_links_1");  // 4 + 3 = 7
        assertThat(result.get(9)).isEqualTo("short_links_2");  // 7 + 2 = 9
        assertThat(result.get(10)).isEqualTo("short_links_3"); // 9 + 1 = 10
    }

    @Test
    void buildWeightMap_shouldHandleZeroWeights() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("short_links_0", 0);
        weights.put("short_links_1", 2);

        NavigableMap<Integer, String> result = ShortLinkShardUtils.buildWeightMap(weights);

        // Zero weight should be treated as 1
        assertThat(result).hasSize(2);
        assertThat(result.get(1)).isEqualTo("short_links_0");  // 0 becomes 1
        assertThat(result.get(3)).isEqualTo("short_links_1");  // 1 + 2 = 3
    }

    @Test
    void buildWeightMap_shouldHandleNegativeWeights() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("short_links_0", -5);
        weights.put("short_links_1", 3);

        NavigableMap<Integer, String> result = ShortLinkShardUtils.buildWeightMap(weights);

        // Negative weight should be treated as 1
        assertThat(result).hasSize(2);
        assertThat(result.get(1)).isEqualTo("short_links_0");  // -5 becomes 1
        assertThat(result.get(4)).isEqualTo("short_links_1");  // 1 + 3 = 4
    }

    @Test
    void resolveTable_shouldReturnCorrectTableBasedOnHash() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("short_links_0", 4);
        weights.put("short_links_1", 3);
        weights.put("short_links_2", 2);
        weights.put("short_links_3", 1);

        NavigableMap<Integer, String> weightMap = ShortLinkShardUtils.buildWeightMap(weights);

        // Test with known short codes and verify consistent behavior
        String result1 = ShortLinkShardUtils.resolveTable("abc123", weightMap);
        String result2 = ShortLinkShardUtils.resolveTable("def456", weightMap);
        String result3 = ShortLinkShardUtils.resolveTable("ghi789", weightMap);

        // Results should be one of the available tables
        assertThat(result1).isIn("short_links_0", "short_links_1", "short_links_2", "short_links_3");
        assertThat(result2).isIn("short_links_0", "short_links_1", "short_links_2", "short_links_3");
        assertThat(result3).isIn("short_links_0", "short_links_1", "short_links_2", "short_links_3");
    }

    @Test
    void resolveTable_shouldBeConsistentForSameInput() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("short_links_0", 4);
        weights.put("short_links_1", 3);

        NavigableMap<Integer, String> weightMap = ShortLinkShardUtils.buildWeightMap(weights);
        String testCode = "consistent_test";

        String result1 = ShortLinkShardUtils.resolveTable(testCode, weightMap);
        String result2 = ShortLinkShardUtils.resolveTable(testCode, weightMap);
        String result3 = ShortLinkShardUtils.resolveTable(testCode, weightMap);

        assertThat(result1).isEqualTo(result2).isEqualTo(result3);
    }

    @Test
    void resolveTable_shouldThrowExceptionForEmptyWeightMap() {
        NavigableMap<Integer, String> emptyWeightMap = ShortLinkShardUtils.buildWeightMap(new LinkedHashMap<>());

        assertThatThrownBy(() -> ShortLinkShardUtils.resolveTable("abc123", emptyWeightMap))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No sharding table weights configured");
    }

    @Test
    void resolveTable_shouldDistributeAccordingToWeights() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("short_links_0", 8);  // 80% weight
        weights.put("short_links_1", 2);  // 20% weight

        NavigableMap<Integer, String> weightMap = ShortLinkShardUtils.buildWeightMap(weights);

        int table0Count = 0;
        int table1Count = 0;
        int totalTests = 1000;

        // Test distribution over many short codes
        for (int i = 0; i < totalTests; i++) {
            String testCode = "test_" + i;
            String result = ShortLinkShardUtils.resolveTable(testCode, weightMap);

            if ("short_links_0".equals(result)) {
                table0Count++;
            } else if ("short_links_1".equals(result)) {
                table1Count++;
            }
        }

        // With 8:2 ratio, we expect roughly 80% to 20% distribution
        double table0Ratio = (double) table0Count / totalTests;
        double table1Ratio = (double) table1Count / totalTests;

        assertThat(table0Ratio).isBetween(0.75, 0.85); // Should be around 0.8
        assertThat(table1Ratio).isBetween(0.15, 0.25); // Should be around 0.2
    }

    @Test
    void resolveTable_shouldHandleEdgeCaseWhenHashModuloMatchesTotalWeight() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("short_links_0", 5);
        weights.put("short_links_1", 5);

        NavigableMap<Integer, String> weightMap = ShortLinkShardUtils.buildWeightMap(weights);

        // Test many different codes to ensure edge cases are handled
        for (int i = 0; i < 100; i++) {
            String testCode = "edge_test_" + i;
            String result = ShortLinkShardUtils.resolveTable(testCode, weightMap);

            assertThat(result).isIn("short_links_0", "short_links_1");
        }
    }

    @Test
    void resolveTable_shouldHandleSingleTable() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("single_table", 10);

        NavigableMap<Integer, String> weightMap = ShortLinkShardUtils.buildWeightMap(weights);

        String result = ShortLinkShardUtils.resolveTable("any_code", weightMap);

        assertThat(result).isEqualTo("single_table");
    }

    @Test
    void resolveTable_shouldUseMurmurHashForConsistentDistribution() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("short_links_0", 1);
        weights.put("short_links_1", 1);

        NavigableMap<Integer, String> weightMap = ShortLinkShardUtils.buildWeightMap(weights);

        // Test that similar codes can map to different tables (good hash distribution)
        String result1 = ShortLinkShardUtils.resolveTable("a", weightMap);
        String result2 = ShortLinkShardUtils.resolveTable("b", weightMap);
        String result3 = ShortLinkShardUtils.resolveTable("aa", weightMap);
        String result4 = ShortLinkShardUtils.resolveTable("ab", weightMap);

        // At least some should be different to show distribution
        boolean hasDistribution = !result1.equals(result2) || !result1.equals(result3) || !result1.equals(result4);
        assertThat(hasDistribution).isTrue();
    }
}