package com.miniups.repository;

import com.miniups.model.entity.OutboxEvent;
import org.apache.ibatis.annotations.*;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

@Mapper
public interface OutboxEventRepository {

    @Insert("INSERT INTO outbox_events (event_id, event_type, aggregate_type, aggregate_id, payload, " +
            "routing_key, status, correlation_id, retry_count, max_retries, next_retry_at, error_message, " +
            "source_service, created_at, updated_at, published_at) " +
            "VALUES (#{eventId}, #{eventType}, #{aggregateType}, #{aggregateId}, #{payload}, " +
            "#{routingKey}, #{status}, #{correlationId}, #{retryCount}, #{maxRetries}, #{nextRetryAt}, " +
            "#{errorMessage}, #{sourceService}, NOW(), NOW(), #{publishedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OutboxEvent event);

    @Update("UPDATE outbox_events SET event_id = #{eventId}, event_type = #{eventType}, " +
            "aggregate_type = #{aggregateType}, aggregate_id = #{aggregateId}, payload = #{payload}, " +
            "routing_key = #{routingKey}, status = #{status}, correlation_id = #{correlationId}, " +
            "retry_count = #{retryCount}, max_retries = #{maxRetries}, next_retry_at = #{nextRetryAt}, " +
            "error_message = #{errorMessage}, source_service = #{sourceService}, updated_at = NOW(), " +
            "published_at = #{publishedAt} WHERE id = #{id}")
    int update(OutboxEvent event);

    @Select("SELECT * FROM outbox_events WHERE id = #{id}")
    OutboxEvent selectById(Long id);

    @Select("SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT #{limit}")
    List<OutboxEvent> findPendingEvents(@Param("limit") int limit);

    @Select("SELECT * FROM outbox_events WHERE status = #{status}")
    List<OutboxEvent> findByStatus(@Param("status") String status);

    @Delete("DELETE FROM outbox_events WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM outbox_events")
    long count();

    /**
     * Find events ready for processing with pagination
     *
     * @param now Current timestamp to check against next_retry_at
     * @param pageRequest Pagination parameters (page number and size)
     * @return List of events ready for processing
     */
    @Select("<script>" +
            "SELECT * FROM outbox_events " +
            "WHERE status = 'PENDING' " +
            "AND (next_retry_at IS NULL OR next_retry_at &lt;= #{now}) " +
            "ORDER BY created_at ASC " +
            "LIMIT #{pageRequest.pageSize} OFFSET #{pageRequest.offset}" +
            "</script>")
    List<OutboxEvent> findEventsReadyForProcessing(@Param("now") Instant now,
                                                    @Param("pageRequest") PageRequest pageRequest);

    /**
     * Atomically claim events for processing by setting status to PROCESSING
     * This prevents multiple instances from processing the same events
     *
     * @param eventIds List of event IDs to claim
     * @param instanceId The instance ID claiming these events
     * @return Number of events successfully claimed
     */
    @Update("<script>" +
            "UPDATE outbox_events " +
            "SET status = 'PROCESSING', " +
            "    source_service = #{instanceId}, " +
            "    updated_at = NOW() " +
            "WHERE id IN " +
            "<foreach item='id' collection='eventIds' open='(' separator=',' close=')'>" +
            "  #{id}" +
            "</foreach> " +
            "AND status = 'PENDING'" +
            "</script>")
    int claimEventsForProcessing(@Param("eventIds") List<Long> eventIds,
                                  @Param("instanceId") String instanceId);


    /**
     * Count events by status
     *
     * @param status The event status to count
     * @return Number of events with the given status
     */
    @Select("SELECT COUNT(*) FROM outbox_events WHERE status = #{status}")
    long countByStatus(@Param("status") OutboxEvent.OutboxStatus status);

    /**
     * Find the oldest pending event
     *
     * @return The oldest pending event, or null if none exists
     */
    @Select("SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 1")
    OutboxEvent findOldestPendingEvent();

    /**
     * Reset stuck processing events back to pending status
     * Events are considered stuck if they've been in PROCESSING status longer than the timeout
     *
     * @param timeoutThreshold Events updated before this time will be reset
     * @return Number of events reset
     */
    @Update("UPDATE outbox_events " +
            "SET status = 'PENDING', " +
            "    source_service = NULL, " +
            "    updated_at = NOW() " +
            "WHERE status = 'PROCESSING' " +
            "AND updated_at < #{timeoutThreshold}")
    int resetStuckProcessingEvents(@Param("timeoutThreshold") Instant timeoutThreshold);

    /**
     * Delete published events older than the specified time
     * Used for cleanup to prevent unbounded table growth
     *
     * @param olderThan Delete events published before this time
     * @return Number of events deleted
     */
    @Delete("DELETE FROM outbox_events " +
            "WHERE status = 'PUBLISHED' " +
            "AND published_at < #{olderThan}")
    int deletePublishedEventsOlderThan(@Param("olderThan") Instant olderThan);
}
