package com.miniups.rag.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class RagFeedbackRequest {

    @NotNull
    private UUID logId;

    @NotNull
    private String feedback; // POSITIVE or NEGATIVE

    private String comment;

    // Manual getters
    public UUID getLogId() { return logId; }
    public String getFeedback() { return feedback; }
    public String getComment() { return comment; }
}
