package com.miniups.repository;

import com.miniups.model.entity.TrackingSequence;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TrackingSequenceRepository {

    @Insert("INSERT INTO tracking_sequences (sequence_name, current_value, step, created_at, updated_at, version) " +
            "VALUES (#{sequenceName}, #{currentValue}, #{step}, NOW(), NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TrackingSequence sequence);

    @Update("UPDATE tracking_sequences SET current_value = #{currentValue}, step = #{step}, " +
            "updated_at = NOW(), version = version + 1 WHERE id = #{id} AND version = #{version}")
    int update(TrackingSequence sequence);

    @Select("SELECT * FROM tracking_sequences WHERE id = #{id}")
    TrackingSequence selectById(Long id);

    @Select("SELECT * FROM tracking_sequences WHERE sequence_name = #{sequenceName}")
    TrackingSequence findBySequenceName(String sequenceName);

    @Delete("DELETE FROM tracking_sequences WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM tracking_sequences")
    long count();

    /**
     * 获取下一个ID段（使用乐观锁）
     * 返回更新后的maxId和step
     */
    @Update("UPDATE tracking_sequences SET current_value = current_value + step, " +
            "updated_at = NOW(), version = version + 1 " +
            "WHERE sequence_name = #{bizTag} AND version = " +
            "(SELECT version FROM tracking_sequences WHERE sequence_name = #{bizTag})")
    @Results({
        @Result(property = "maxId", column = "current_value"),
        @Result(property = "step", column = "step")
    })
    @Select("SELECT current_value + step as maxId, step FROM tracking_sequences WHERE sequence_name = #{bizTag}")
    SegmentInfo getNextSegment(@Param("bizTag") String bizTag);

    /**
     * 初始化序列（如果不存在）
     * 使用ON CONFLICT实现幂等性
     */
    @Insert("INSERT INTO tracking_sequences (sequence_name, current_value, step, description, created_at, updated_at, version) " +
            "VALUES (#{bizTag}, 0, #{step}, #{description}, NOW(), NOW(), 0) " +
            "ON CONFLICT (sequence_name) DO NOTHING")
    int initializeSequence(@Param("bizTag") String bizTag,
                          @Param("step") int step,
                          @Param("description") String description);

    /**
     * Update step value for a sequence
     *
     * @param sequenceName The name of the sequence
     * @param step New step value
     * @return Number of rows updated
     */
    @Update("UPDATE tracking_sequences SET step = #{step}, updated_at = NOW() WHERE sequence_name = #{sequenceName}")
    int updateStep(@Param("sequenceName") String sequenceName, @Param("step") int step);

    /**
     * 段信息类
     */
    class SegmentInfo {
        private Long maxId;
        private Integer step;

        public Long getMaxId() {
            return maxId;
        }

        public void setMaxId(Long maxId) {
            this.maxId = maxId;
        }

        public Integer getStep() {
            return step;
        }

        public void setStep(Integer step) {
            this.step = step;
        }
    }
}
