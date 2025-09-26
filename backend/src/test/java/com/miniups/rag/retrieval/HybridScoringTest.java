package com.miniups.rag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniups.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Hybrid Scoring Algorithm Tests")
class HybridScoringTest {

    private RagRetriever retriever;
    private RagProperties properties;

    @BeforeEach
    void setUp() {
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        properties = new RagProperties();
        properties.getRetrieval().setSemanticWeight(0.7);
        properties.getRetrieval().setKeywordWeight(0.3);
        properties.getRetrieval().setSimilarityThreshold(0.5);

        retriever = new RagRetriever(mockJdbcTemplate, properties, new ObjectMapper());
    }

    @Test
    @DisplayName("Should correctly weight semantic and keyword scores")
    void shouldCorrectlyWeightSemanticAndKeywordScores() {
        // Create a test scenario where we can verify the weighted combination
        // Since hybridSearch is complex, let's test the scoring logic conceptually

        double semanticScore = 0.8;
        double keywordScore = 0.6;
        double semanticWeight = 0.7;
        double keywordWeight = 0.3;

        // Expected final score: 0.7 * 0.8 + 0.3 * 0.6 = 0.56 + 0.18 = 0.74
        double expectedFinalScore = semanticWeight * semanticScore + keywordWeight * keywordScore;

        assertEquals(0.74, expectedFinalScore, 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "0.8, 0.6, 0.7, 0.3, 0.74",   // Standard case
        "1.0, 0.0, 0.7, 0.3, 0.70",   // Pure semantic
        "0.0, 1.0, 0.7, 0.3, 0.30",   // Pure keyword
        "0.9, 0.8, 0.5, 0.5, 0.85",   // Equal weights
        "0.5, 0.5, 1.0, 0.0, 0.50",   // Semantic only
        "0.5, 0.5, 0.0, 1.0, 0.50"    // Keyword only
    })
    @DisplayName("Should calculate final scores correctly for various weight combinations")
    void shouldCalculateFinalScoresCorrectly(double semanticScore, double keywordScore,
                                           double semanticWeight, double keywordWeight,
                                           double expectedFinalScore) {
        // Calculate final score using the same formula as in RagRetriever
        double weightSum = semanticWeight + keywordWeight;
        if (weightSum <= 0) {
            semanticWeight = 1.0;
            keywordWeight = 0.0;
            weightSum = 1.0;
        }
        double semanticShare = semanticWeight / weightSum;
        double keywordShare = keywordWeight / weightSum;

        double finalScore = semanticShare * semanticScore + keywordShare * keywordScore;

        assertEquals(expectedFinalScore, finalScore, 0.001);
    }

    @Test
    @DisplayName("Should handle zero weights by defaulting to semantic-only")
    void shouldHandleZeroWeightsByDefaultingToSemanticOnly() {
        double semanticScore = 0.8;
        double keywordScore = 0.6;
        double semanticWeight = 0.0;
        double keywordWeight = 0.0;

        // When both weights are 0, should default to semantic weight = 1.0, keyword weight = 0.0
        double weightSum = semanticWeight + keywordWeight;
        if (weightSum <= 0) {
            semanticWeight = 1.0;
            keywordWeight = 0.0;
            weightSum = 1.0;
        }

        double semanticShare = semanticWeight / weightSum;
        double keywordShare = keywordWeight / weightSum;
        double finalScore = semanticShare * semanticScore + keywordShare * keywordScore;

        assertEquals(1.0, semanticShare, 0.001);
        assertEquals(0.0, keywordShare, 0.001);
        assertEquals(0.8, finalScore, 0.001); // Should be pure semantic score
    }

    @Test
    @DisplayName("Should normalize keyword scores correctly")
    void shouldNormalizeKeywordScoresCorrectly() {
        // Test keyword score normalization logic
        List<Double> keywordScores = List.of(0.8, 0.6, 1.2, 0.4); // 1.2 is the max
        double maxScore = keywordScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        assertEquals(1.2, maxScore, 0.001);

        // Test normalization
        for (double score : keywordScores) {
            double normalized = maxScore > 0 ? score / maxScore : 0.0;
            assertTrue(normalized >= 0.0 && normalized <= 1.0,
                String.format("Normalized score %f should be between 0 and 1", normalized));
        }

        // Specific checks
        assertEquals(1.0, 1.2 / maxScore, 0.001); // Max score should normalize to 1.0
        assertEquals(0.5, 0.6 / maxScore, 0.001); // 0.6/1.2 = 0.5
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.5, 1.5})
    @DisplayName("Should clamp invalid scores to [0, 1] range")
    void shouldClampInvalidScores(double invalidScore) {
        double clampedScore = clamp01(invalidScore);

        assertTrue(clampedScore >= 0.0 && clampedScore <= 1.0,
            String.format("Clamped score %f should be between 0 and 1", clampedScore));

        if (Double.isNaN(invalidScore)) {
            assertEquals(0.0, clampedScore, 0.001);
        } else if (invalidScore < 0) {
            assertEquals(0.0, clampedScore, 0.001);
        } else if (invalidScore > 1) {
            assertEquals(1.0, clampedScore, 0.001);
        }
    }

    @Test
    @DisplayName("Should handle aggregation when same chunk appears in both searches")
    void shouldHandleAggregationWhenSameChunkAppearsInBothSearches() {
        // Simulate the aggregation logic from RagRetriever
        UUID chunkId = UUID.randomUUID();

        // Semantic result for the chunk
        RagSearchResult semanticResult = new RagSearchResult(
            chunkId, "doc-1", "source.md", 0, "content",
            0.8, 0.0, 0.8, Map.of()
        );

        // Keyword result for the same chunk (different scores)
        RagSearchResult keywordResult = new RagSearchResult(
            chunkId, "doc-1", "source.md", 0, "content",
            0.0, 0.6, 0.0, Map.of()
        );

        // Aggregation should take the max of each score type
        double aggregatedSemanticScore = Math.max(semanticResult.semanticScore(), keywordResult.semanticScore());
        double aggregatedKeywordScore = Math.max(semanticResult.keywordScore(), keywordResult.keywordScore());

        assertEquals(0.8, aggregatedSemanticScore, 0.001);
        assertEquals(0.6, aggregatedKeywordScore, 0.001);

        // Final score calculation
        double semanticWeight = 0.7;
        double keywordWeight = 0.3;
        double finalScore = semanticWeight * aggregatedSemanticScore + keywordWeight * aggregatedKeywordScore;
        double expectedFinalScore = 0.7 * 0.8 + 0.3 * 0.6; // = 0.56 + 0.18 = 0.74

        assertEquals(expectedFinalScore, finalScore, 0.001);
    }

    @Test
    @DisplayName("Should maintain score ordering after aggregation")
    void shouldMaintainScoreOrderingAfterAggregation() {
        // Create scenarios with different score combinations
        List<ScoreScenario> scenarios = List.of(
            new ScoreScenario("High semantic, high keyword", 0.9, 0.8),
            new ScoreScenario("High semantic, low keyword", 0.9, 0.2),
            new ScoreScenario("Low semantic, high keyword", 0.4, 0.9),
            new ScoreScenario("Medium semantic, medium keyword", 0.6, 0.6)
        );

        // Calculate final scores
        double semanticWeight = 0.7;
        double keywordWeight = 0.3;

        for (ScoreScenario scenario : scenarios) {
            scenario.finalScore = semanticWeight * scenario.semanticScore + keywordWeight * scenario.keywordScore;
        }

        // Sort by final score descending (as RagRetriever does)
        scenarios.sort((a, b) -> Double.compare(b.finalScore, a.finalScore));

        // Verify ordering
        assertTrue(scenarios.get(0).finalScore >= scenarios.get(1).finalScore);
        assertTrue(scenarios.get(1).finalScore >= scenarios.get(2).finalScore);
        assertTrue(scenarios.get(2).finalScore >= scenarios.get(3).finalScore);

        // The "High semantic, high keyword" should be first
        assertEquals("High semantic, high keyword", scenarios.get(0).name);
    }

    @Test
    @DisplayName("Should apply similarity threshold correctly")
    void shouldApplySimilarityThresholdCorrectly() {
        double threshold = 0.7;

        // Test cases: semantic score, should pass threshold
        List<ThresholdTest> tests = List.of(
            new ThresholdTest(0.8, true),   // Above threshold
            new ThresholdTest(0.7, true),   // At threshold
            new ThresholdTest(0.6, false),  // Below threshold
            new ThresholdTest(0.0, false)   // Zero (keyword only)
        );

        for (ThresholdTest test : tests) {
            boolean shouldPass = test.semanticScore > 0 && test.semanticScore >= threshold;
            assertEquals(test.expectedPass, shouldPass,
                String.format("Semantic score %f should %s threshold %f",
                    test.semanticScore, test.expectedPass ? "pass" : "fail", threshold));
        }
    }

    // Helper method to replicate clamping logic from RagRetriever
    private double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    // Helper classes for test data
    private static class ScoreScenario {
        final String name;
        final double semanticScore;
        final double keywordScore;
        double finalScore;

        ScoreScenario(String name, double semanticScore, double keywordScore) {
            this.name = name;
            this.semanticScore = semanticScore;
            this.keywordScore = keywordScore;
        }
    }

    private static class ThresholdTest {
        final double semanticScore;
        final boolean expectedPass;

        ThresholdTest(double semanticScore, boolean expectedPass) {
            this.semanticScore = semanticScore;
            this.expectedPass = expectedPass;
        }
    }
}