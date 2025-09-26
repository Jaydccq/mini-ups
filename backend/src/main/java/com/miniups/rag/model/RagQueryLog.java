package com.miniups.rag.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rag_query_log")
@Getter
@Setter
public class RagQueryLog {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "username")
    private String username;

    @Column(name = "role")
    private String role;

    @Column(name = "query", columnDefinition = "TEXT")
    private String query;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback")
    private RagFeedbackType feedback;

    @Column(name = "feedback_comment", columnDefinition = "TEXT")
    private String feedbackComment;

    @Column(name = "feedback_at")
    private OffsetDateTime feedbackAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void applyFeedback(RagFeedbackType feedbackType, String comment) {
        this.feedback = feedbackType;
        this.feedbackComment = comment;
        this.feedbackAt = OffsetDateTime.now();
    }
}
