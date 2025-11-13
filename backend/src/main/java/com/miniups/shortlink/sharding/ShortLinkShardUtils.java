package com.miniups.shortlink.sharding;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class ShortLinkShardUtils {

    private ShortLinkShardUtils() {
    }

    public static Map<String, Integer> parseWeights(String weights) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (weights == null || weights.isBlank()) {
            return result;
        }
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

    public static NavigableMap<Integer, String> buildWeightMap(Map<String, Integer> weights) {
        NavigableMap<Integer, String> cumulative = new TreeMap<>();
        int cursor = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            int weight = Math.max(entry.getValue(), 1);
            cursor += weight;
            cumulative.put(cursor, entry.getKey());
        }
        return cumulative;
    }

    public static String resolveTable(String code, NavigableMap<Integer, String> weightMap) {
        if (weightMap.isEmpty()) {
            throw new IllegalStateException("No sharding table weights configured");
        }
        long hash = Integer.toUnsignedLong(Hashing.murmur3_32_fixed().hashString(code, StandardCharsets.UTF_8).asInt());
        int totalWeight = weightMap.lastKey();
        int slot = (int) (hash % totalWeight) + 1; // shift to 1-based for ceilingKey
        Map.Entry<Integer, String> target = weightMap.ceilingEntry(slot);
        if (target == null) {
            return weightMap.lastEntry().getValue();
        }
        return target.getValue();
    }
}
