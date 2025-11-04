package com.miniups.rag.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RagSourceDto {
    String title;
    String source;
    double similarity;
    double confidence;
    Double semanticScore;
    Double keywordScore;

    // Manual constructor (Lombok @AllArgsConstructor not working with @Value)
    public RagSourceDto(String title, String source, double similarity, double confidence,
                       Double semanticScore, Double keywordScore) {
        this.title = title;
        this.source = source;
        this.similarity = similarity;
        this.confidence = confidence;
        this.semanticScore = semanticScore;
        this.keywordScore = keywordScore;
    }

    // Manual getters
    public String getTitle() { return title; }
    public String getSource() { return source; }
    public double getSimilarity() { return similarity; }
    public double getConfidence() { return confidence; }
    public Double getSemanticScore() { return semanticScore; }
    public Double getKeywordScore() { return keywordScore; }
}
