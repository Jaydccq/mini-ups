package com.miniups.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 追踪号序列数据访问接口
 * 
 * 支持Leaf-Segment模式的高并发ID序列管理
 * 
 * 核心功能：
 * - 原子化批量获取ID段
 * - 支持动态步长调整
 * - 高性能数据库操作
 * 
 * @author Mini-UPS Team
 */
@Repository
public interface TrackingSequenceRepository extends JpaRepository<com.miniups.model.entity.TrackingSequence, String> {
    
    /**
     * 原子化获取下一个ID段
     * 
     * 这个方法执行以下操作：
     * 1. 使用FOR UPDATE锁定记录避免并发问题
     * 2. 更新max_id为当前值+步长
     * 3. 返回更新后的信息
     * 
     * 相当于执行：
     * BEGIN
     * SELECT max_id, step FROM tracking_sequences WHERE biz_tag = :bizTag FOR UPDATE
     * UPDATE tracking_sequences SET max_id = max_id + step WHERE biz_tag = :bizTag
     * SELECT max_id, step FROM tracking_sequences WHERE biz_tag = :bizTag
     * COMMIT
     * 
     * @param bizTag 业务标识
     * @return 返回包含新max_id和step的对象，如果记录不存在返回null
     */
    @Transactional
    @Query(value = """
        WITH updated AS (
            UPDATE tracking_sequences 
            SET max_id = max_id + step,
                updated_at = CURRENT_TIMESTAMP 
            WHERE biz_tag = :bizTag 
            RETURNING max_id, step, biz_tag
        )
        SELECT u.max_id as maxId, u.step as step, u.biz_tag as bizTag
        FROM updated u
        """, nativeQuery = true)
    SegmentInfo getNextSegment(@Param("bizTag") String bizTag);
    
    /**
     * 获取序列当前信息（只读）
     * 
     * @param bizTag 业务标识
     * @return 当前序列信息
     */
    @Query(value = """
        SELECT max_id as maxId, step as step, biz_tag as bizTag, description
        FROM tracking_sequences 
        WHERE biz_tag = :bizTag
        """, nativeQuery = true)
    SegmentInfo getCurrentSegmentInfo(@Param("bizTag") String bizTag);
    
    /**
     * 动态调整步长
     * 
     * @param bizTag 业务标识
     * @param newStep 新的步长
     * @return 影响的行数
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE tracking_sequences 
        SET step = :newStep, updated_at = CURRENT_TIMESTAMP 
        WHERE biz_tag = :bizTag
        """, nativeQuery = true)
    int updateStep(@Param("bizTag") String bizTag, @Param("newStep") int newStep);
    
    /**
     * 检查业务标识是否存在
     * 
     * @param bizTag 业务标识
     * @return true表示存在
     */
    boolean existsByBizTag(String bizTag);
    
    /**
     * 初始化新的业务序列
     * 
     * @param bizTag 业务标识
     * @param step 初始步长
     * @param description 描述
     * @return 影响的行数
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO tracking_sequences (biz_tag, max_id, step, description) 
        VALUES (:bizTag, 0, :step, :description)
        ON CONFLICT (biz_tag) DO NOTHING
        """, nativeQuery = true)
    int initializeSequence(@Param("bizTag") String bizTag, 
                          @Param("step") int step, 
                          @Param("description") String description);
    
    /**
     * 段信息投影接口
     * 
     * 用于接收数据库查询结果
     */
    interface SegmentInfo {
        Long getMaxId();
        Integer getStep();
        String getBizTag();
        String getDescription();
    }
}