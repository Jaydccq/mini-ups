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

    // Manual constructor (Lombok @AllArgsConstructor not working with @Value)
    public RagQueryResponse(UUID logId, String answer, double confidence,
                           List<RagSourceDto> sources, List<String> warnings) {
        this.logId = logId;
        this.answer = answer;
        this.confidence = confidence;
        this.sources = sources;
        this.warnings = warnings;
    }

    // Manual getters (for @Value)
    public UUID getLogId() { return logId; }
    public String getAnswer() { return answer; }
    public double getConfidence() { return confidence; }
    public List<RagSourceDto> getSources() { return sources; }
    public List<String> getWarnings() { return warnings; }
}
