package com.miniups.repository;

import com.miniups.model.entity.LeafAlloc;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface LeafAllocRepository {

    @Insert("INSERT INTO leaf_alloc (biz_tag, max_id, step, description, update_time) " +
            "VALUES (#{bizTag}, #{maxId}, #{step}, #{description}, NOW())")
    int insert(LeafAlloc leafAlloc);

    @Update("UPDATE leaf_alloc SET max_id = max_id + #{step}, update_time = NOW() " +
            "WHERE biz_tag = #{bizTag}")
    int updateMaxId(@Param("bizTag") String bizTag, @Param("step") int step);

    @Update("UPDATE leaf_alloc SET max_id = #{maxId}, step = #{step}, " +
            "description = #{description}, update_time = NOW() WHERE biz_tag = #{bizTag}")
    int update(LeafAlloc leafAlloc);

    @Select("SELECT * FROM leaf_alloc WHERE biz_tag = #{bizTag}")
    LeafAlloc findByBizTag(String bizTag);

    @Select("SELECT * FROM leaf_alloc")
    List<LeafAlloc> selectAll();

    @Delete("DELETE FROM leaf_alloc WHERE biz_tag = #{bizTag}")
    int deleteByBizTag(String bizTag);

    @Select("SELECT COUNT(*) FROM leaf_alloc")
    long count();

    // Atomic segment allocation with optimistic locking
    @Update("UPDATE leaf_alloc SET max_id = max_id + step, version = version + 1, " +
            "last_alloc_time = NOW(), update_time = NOW() " +
            "WHERE biz_tag = #{bizTag} AND version = #{version}")
    int allocateNextSegment(@Param("bizTag") String bizTag, @Param("version") long version);

    // Find all active allocations
    @Select("SELECT * FROM leaf_alloc WHERE active = true")
    List<LeafAlloc> findByActiveTrue();

    // Update allocation rate statistics
    @Update("UPDATE leaf_alloc SET avg_rate = #{avgRate}, update_time = NOW() " +
            "WHERE biz_tag = #{bizTag}")
    void updateAllocationRate(@Param("bizTag") String bizTag, @Param("avgRate") double avgRate);

    // Initialize a new allocation (returns rows affected)
    @Insert("INSERT INTO leaf_alloc (biz_tag, max_id, step, description, version, active, update_time, last_alloc_time) " +
            "VALUES (#{bizTag}, 0, #{step}, #{description}, 0, true, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE biz_tag = biz_tag")
    int initializeAllocation(@Param("bizTag") String bizTag, @Param("step") int step,
                           @Param("description") String description);

    // Get allocation status as a map
    @Select("SELECT biz_tag as bizTag, max_id as maxId, step, version, active, " +
            "avg_rate as avgRate, description, update_time as updateTime, " +
            "last_alloc_time as lastAllocTime " +
            "FROM leaf_alloc WHERE biz_tag = #{bizTag}")
    @MapKey("bizTag")
    Map<String, Object> getAllocationStatus(String bizTag);
}
