package com.miniups.rag.repository;

import com.miniups.rag.model.RagQueryLog;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public interface RagQueryLogRepository extends JpaRepository<RagQueryLog, UUID> {
}
