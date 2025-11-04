package com.miniups.rag.repository;

import com.miniups.rag.model.RagQueryLog;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Mapper
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public interface RagQueryLogRepository {

    @Insert("INSERT INTO rag_query_log (id, user_id, username, role, query, answer, confidence, " +
            "sources, feedback, feedback_comment, feedback_at, created_at, updated_at) " +
            "VALUES (#{id}, #{userId}, #{username}, #{role}, #{query}, #{answer}, #{confidence}, " +
            "#{sources}, #{feedback}, #{feedbackComment}, #{feedbackAt}, #{createdAt}, #{updatedAt})")
    int insert(RagQueryLog log);

    @Update("UPDATE rag_query_log SET user_id = #{userId}, username = #{username}, role = #{role}, " +
            "query = #{query}, answer = #{answer}, confidence = #{confidence}, sources = #{sources}, " +
            "feedback = #{feedback}, feedback_comment = #{feedbackComment}, feedback_at = #{feedbackAt}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int update(RagQueryLog log);

    @Select("SELECT * FROM rag_query_log WHERE id = #{id}")
    RagQueryLog selectById(UUID id);

    @Select("SELECT * FROM rag_query_log")
    List<RagQueryLog> selectAll();

    @Delete("DELETE FROM rag_query_log WHERE id = #{id}")
    int deleteById(UUID id);

    @Select("SELECT COUNT(*) FROM rag_query_log")
    long count();
}
