package com.miniups.repository;

import com.miniups.model.entity.ShipmentStatusHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShipmentStatusHistoryRepository {

    @Insert("INSERT INTO shipment_status_history (shipment_id, status, timestamp, location_x, location_y, notes, created_at, version) " +
            "VALUES (#{shipmentId}, #{status}, #{timestamp}, #{locationX}, #{locationY}, #{notes}, NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ShipmentStatusHistory history);

    @Select("SELECT * FROM shipment_status_history WHERE id = #{id}")
    ShipmentStatusHistory selectById(Long id);

    @Select("SELECT * FROM shipment_status_history WHERE shipment_id = #{shipmentId} ORDER BY timestamp DESC")
    List<ShipmentStatusHistory> findByShipmentId(Long shipmentId);

    @Delete("DELETE FROM shipment_status_history WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM shipment_status_history")
    long count();
}
