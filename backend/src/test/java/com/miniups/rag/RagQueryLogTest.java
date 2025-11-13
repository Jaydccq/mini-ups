package com.miniups.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.miniups.rag.model.RagFeedbackType;
import com.miniups.rag.model.RagQueryLog;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class RagQueryLogTest {

    @Test
    void applyFeedbackUpdatesFields() {
        RagQueryLog log = new RagQueryLog();
        log.onCreate();
        OffsetDateTime before = log.getCreatedAt();

        log.applyFeedback(RagFeedbackType.POSITIVE, "thanks");

        assertEquals(RagFeedbackType.POSITIVE, log.getFeedback());
        assertEquals("thanks", log.getFeedbackComment());
        assertNotNull(log.getFeedbackAt());
        assertTrue(!log.getFeedbackAt().isBefore(before));
    }
}
