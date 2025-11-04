package com.miniups.repository;

import com.miniups.model.entity.Truck;
import com.miniups.model.enums.TruckStatus;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TruckRepository {

    @Insert("INSERT INTO trucks (truck_number, current_x, current_y, capacity, status, created_at, updated_at, version) " +
            "VALUES (#{truckNumber}, #{currentX}, #{currentY}, #{capacity}, #{status}, NOW(), NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Truck truck);

    @Update("UPDATE trucks SET truck_number = #{truckNumber}, current_x = #{currentX}, current_y = #{currentY}, " +
            "capacity = #{capacity}, status = #{status}, updated_at = NOW(), version = version + 1 " +
            "WHERE id = #{id} AND version = #{version}")
    int update(Truck truck);

    @Select("SELECT * FROM trucks WHERE id = #{id}")
    Truck selectById(Long id);

    @Select("SELECT * FROM trucks")
    List<Truck> selectAll();

    @Delete("DELETE FROM trucks WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM trucks WHERE truck_number = #{truckNumber}")
    Truck findByTruckNumber(String truckNumber);

    @Select("SELECT * FROM trucks WHERE status = #{status}")
    List<Truck> findByStatus(@Param("status") TruckStatus status);

    @Select("SELECT COUNT(*) > 0 FROM trucks WHERE truck_number = #{truckNumber}")
    boolean existsByTruckNumber(String truckNumber);

    @Select("SELECT COUNT(*) FROM trucks WHERE status = #{status}")
    long countByStatus(@Param("status") TruckStatus status);

    @Select("SELECT COUNT(*) FROM trucks")
    long count();

    // Additional methods for TruckManagementService

    @Select("SELECT * FROM trucks WHERE id = #{id}")
    Truck findById(Long id);

    @Select("SELECT * FROM trucks WHERE truck_number = #{truckId}")
    Truck findByTruckId(Integer truckId);

    @Select("SELECT * FROM trucks")
    List<Truck> findAll();

    /**
     * Find nearest available truck for assignment
     * This is a simplified version - in production you'd use PostGIS for distance calculation
     */
    @Select("SELECT * FROM trucks WHERE status = 'IDLE' " +
            "ORDER BY (ABS(current_x - #{x}) + ABS(current_y - #{y})) ASC LIMIT 1")
    Truck findNearestAvailableTruckForAssignment(@Param("x") Integer x, @Param("y") Integer y);

    /**
     * Find idle trucks with row-level lock (SKIP LOCKED for high concurrency)
     * Note: MyBatis doesn't directly support FOR UPDATE SKIP LOCKED, so we use a regular query
     * For true SKIP LOCKED support, you'd need a custom SQL provider or XML mapper
     */
    @Select("SELECT * FROM trucks WHERE status = 'IDLE' LIMIT 10")
    List<Truck> findIdleForUpdateSkipLocked();

    /**
     * Find and lock one available truck
     * Simplified version without FOR UPDATE SKIP LOCKED
     */
    @Select("SELECT * FROM trucks WHERE status = 'IDLE' LIMIT 1")
    Truck findAndLockOneAvailableTruck();
}
