package com.miniups.rag.repository;

import com.miniups.rag.model.RagQueryLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RagQueryLogRepository extends JpaRepository<RagQueryLog, UUID> {
}
