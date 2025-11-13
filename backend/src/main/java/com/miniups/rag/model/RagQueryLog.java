package com.miniups.rag.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RagQueryLog {

    private UUID id;
    private String userId;
    private String username;
    private String role;
    private String query;
    private String answer;
    private Double confidence;
    private String sources;
    private RagFeedbackType feedback;
    private String feedbackComment;
    private OffsetDateTime feedbackAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void applyFeedback(RagFeedbackType feedbackType, String comment) {
        this.feedback = feedbackType;
        this.feedbackComment = comment;
        this.feedbackAt = OffsetDateTime.now();
    }

    // Manual getters (Lombok @Getter not working properly)
    public UUID getId() { return id; }

    // Manual setters (Lombok @Setter not working properly)
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setRole(String role) { this.role = role; }
    public void setQuery(String query) { this.query = query; }
    public void setAnswer(String answer) { this.answer = answer; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public void setSources(String sources) { this.sources = sources; }
}
