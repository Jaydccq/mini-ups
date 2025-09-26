package com.miniups.rag.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagQueryResponse {
    UUID logId;
    String answer;
    double confidence;
    List<RagSourceDto> sources;
    List<String> warnings;
}
