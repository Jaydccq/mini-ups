package com.miniups.repository;

import com.miniups.model.entity.AuditLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AuditLogRepository {

    @Insert("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address, created_at, version) " +
            "VALUES (#{userId}, #{action}, #{entityType}, #{entityId}, #{details}, #{ipAddress}, NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuditLog auditLog);

    @Select("SELECT * FROM audit_logs WHERE id = #{id}")
    AuditLog selectById(Long id);

    @Select("SELECT * FROM audit_logs ORDER BY created_at DESC")
    List<AuditLog> selectAll();

    @Delete("DELETE FROM audit_logs WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM audit_logs WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<AuditLog> findByUserId(Long userId);

    @Select("SELECT * FROM audit_logs WHERE entity_type = #{entityType} AND entity_id = #{entityId} ORDER BY created_at DESC")
    List<AuditLog> findByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Select("SELECT COUNT(*) FROM audit_logs")
    long count();

    // Additional methods for service layer compatibility
    @Select("SELECT * FROM audit_logs ORDER BY created_at DESC")
    List<AuditLog> findAll();

    @Select("SELECT COUNT(*) > 0 FROM audit_logs WHERE event_id = #{eventId}")
    boolean existsByEventId(String eventId);
}
