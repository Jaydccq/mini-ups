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
}
