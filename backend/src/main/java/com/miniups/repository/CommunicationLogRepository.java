package com.miniups.repository;

import com.miniups.model.entity.CommunicationLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommunicationLogRepository {

    @Insert("INSERT INTO communication_logs (direction, message_type, endpoint, payload, response, " +
            "status_code, processing_time_ms, error_message, success, shipment_id, truck_id, warehouse_id, " +
            "created_at, updated_at) " +
            "VALUES (#{direction}, #{messageType}, #{endpoint}, #{payload}, #{response}, " +
            "#{statusCode}, #{processingTimeMs}, #{errorMessage}, #{success}, #{shipmentId}, #{truckId}, #{warehouseId}, " +
            "NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommunicationLog log);

    @Update("UPDATE communication_logs SET " +
            "response = #{response}, " +
            "status_code = #{statusCode}, " +
            "processing_time_ms = #{processingTimeMs}, " +
            "success = #{success}, " +
            "error_message = #{errorMessage}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(CommunicationLog log);

    @Select("SELECT * FROM communication_logs WHERE id = #{id}")
    CommunicationLog selectById(Long id);

    @Select("SELECT * FROM communication_logs ORDER BY created_at DESC LIMIT 50")
    List<CommunicationLog> findTop50ByOrderByCreatedAtDesc();

    @Select("SELECT * FROM communication_logs WHERE shipment_id = #{shipmentId} ORDER BY created_at ASC")
    List<CommunicationLog> findByShipmentIdOrderByCreatedAtAsc(String shipmentId);

    @Select("SELECT * FROM communication_logs WHERE success = false ORDER BY created_at DESC LIMIT 20")
    List<CommunicationLog> findTop20BySuccessFalseOrderByCreatedAtDesc();

    @Select("<script>" +
            "SELECT * FROM communication_logs WHERE created_at &gt;= #{since} " +
            "<if test='direction != null'>AND direction = #{direction}</if> " +
            "<if test='messageType != null'>AND message_type = #{messageType}</if> " +
            "<if test='success != null'>AND success = #{success}</if> " +
            "ORDER BY created_at DESC" +
            "</script>")
    List<CommunicationLog> findFilteredLogs(@Param("direction") String direction,
                                            @Param("messageType") String messageType,
                                            @Param("success") Boolean success,
                                            @Param("since") LocalDateTime since);

    @Select("SELECT message_type, COUNT(*) as count FROM communication_logs " +
            "WHERE created_at >= #{since} " +
            "GROUP BY message_type")
    List<Object[]> countMessageTypesSince(@Param("since") LocalDateTime since);

    @Select("SELECT success, COUNT(*) as count FROM communication_logs " +
            "WHERE created_at >= #{since} " +
            "GROUP BY success")
    List<Object[]> countSuccessRatesSince(@Param("since") LocalDateTime since);

    @Select("SELECT AVG(processing_time_ms) FROM communication_logs " +
            "WHERE created_at >= #{since} AND processing_time_ms IS NOT NULL")
    Double averageProcessingTimeSince(@Param("since") LocalDateTime since);

    @Delete("DELETE FROM communication_logs WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM communication_logs")
    long count();
}
