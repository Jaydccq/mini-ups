/**
 * Shipment Data Access Mapper Interface
 *
 * Function Description:
 * - MyBatis Mapper for shipment data access
 * - Provides complex query methods to support order tracking, status management, user queries, etc.
 * - Supports pagination to handle large volumes of order data
 *
 * Core Query Methods:
 * - findByShipmentId: Find by Amazon order ID (system integration)
 * - findByUpsTrackingId: Find by UPS tracking number (customer query)
 * - findByUserId: Query all orders for a user
 * - findByStatus: Find orders by status (operations management)
 *
 * Performance Features:
 * - Utilizes database indexes to optimize query performance
 * - Parameterized queries prevent SQL injection
 *
 * Business Application Scenarios:
 * - Order status tracking and management
 * - User order history queries
 * - Operations data statistics and reporting
 * - Amazon system integration interface
 * - Customer service support
 *
 *

 */
package com.miniups.repository;

import com.miniups.model.entity.Shipment;
import com.miniups.model.enums.ShipmentStatus;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShipmentRepository {

    // Basic CRUD operations
    @Insert("INSERT INTO shipments (shipment_id, ups_tracking_id, amazon_order_id, warehouse_id, status, " +
            "origin_x, origin_y, dest_x, dest_y, weight, estimated_delivery, actual_delivery, pickup_time, " +
            "world_id, delivery_address, delivery_city, delivery_zip_code, user_id, truck_id, " +
            "created_at, updated_at, version) " +
            "VALUES (#{shipmentId}, #{upsTrackingId}, #{amazonOrderId}, #{warehouseId}, #{status}, " +
            "#{originX}, #{originY}, #{destX}, #{destY}, #{weight}, #{estimatedDelivery}, #{actualDelivery}, " +
            "#{pickupTime}, #{worldId}, #{deliveryAddress}, #{deliveryCity}, #{deliveryZipCode}, " +
            "#{userId}, #{truckId}, NOW(), NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Shipment shipment);

    @Update("UPDATE shipments SET shipment_id = #{shipmentId}, ups_tracking_id = #{upsTrackingId}, " +
            "amazon_order_id = #{amazonOrderId}, warehouse_id = #{warehouseId}, status = #{status}, " +
            "origin_x = #{originX}, origin_y = #{originY}, dest_x = #{destX}, dest_y = #{destY}, " +
            "weight = #{weight}, estimated_delivery = #{estimatedDelivery}, actual_delivery = #{actualDelivery}, " +
            "pickup_time = #{pickupTime}, world_id = #{worldId}, delivery_address = #{deliveryAddress}, " +
            "delivery_city = #{deliveryCity}, delivery_zip_code = #{deliveryZipCode}, " +
            "user_id = #{userId}, truck_id = #{truckId}, updated_at = NOW(), version = version + 1 " +
            "WHERE id = #{id} AND version = #{version}")
    int update(Shipment shipment);

    @Select("SELECT * FROM shipments WHERE id = #{id}")
    Shipment selectById(Long id);

    @Select("SELECT * FROM shipments")
    List<Shipment> selectAll();

    @Delete("DELETE FROM shipments WHERE id = #{id}")
    int deleteById(Long id);

    // Custom query methods
    @Select("SELECT * FROM shipments WHERE shipment_id = #{shipmentId}")
    Shipment findByShipmentId(String shipmentId);

    @Select("SELECT * FROM shipments WHERE ups_tracking_id = #{upsTrackingId}")
    Shipment findByUpsTrackingId(String upsTrackingId);

    @Select("SELECT * FROM shipments WHERE user_id = #{userId}")
    List<Shipment> findByUserId(Long userId);

    @Select("SELECT * FROM shipments WHERE status = #{status}")
    List<Shipment> findByStatus(@Param("status") ShipmentStatus status);

    @Select("SELECT * FROM shipments WHERE user_id = #{userId} AND status = #{status}")
    List<Shipment> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ShipmentStatus status);

    @Select("SELECT COUNT(*) FROM shipments WHERE status = #{status}")
    long countByStatus(@Param("status") ShipmentStatus status);

    @Select("SELECT COUNT(*) > 0 FROM shipments WHERE ups_tracking_id = #{upsTrackingId}")
    boolean existsByUpsTrackingId(String upsTrackingId);

    @Select("SELECT * FROM shipments WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Shipment> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT * FROM shipments WHERE created_at BETWEEN #{startDate} AND #{endDate}")
    List<Shipment> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Select("SELECT * FROM shipments WHERE weight BETWEEN #{minWeight} AND #{maxWeight}")
    List<Shipment> findByWeightBetween(@Param("minWeight") BigDecimal minWeight,
                                       @Param("maxWeight") BigDecimal maxWeight);

    @Select("SELECT * FROM shipments WHERE truck_id = #{truckId}")
    List<Shipment> findByTruckId(@Param("truckId") Long truckId);

    @Select("SELECT status, COUNT(*) as count FROM shipments GROUP BY status")
    @MapKey("status")
    List<Map<String, Object>> getStatusCounts();

    @Select("SELECT * FROM shipments ORDER BY created_at DESC")
    List<Shipment> findRecentShipments();

    @Select("SELECT COUNT(*) FROM shipments")
    long count();

    @Select("SELECT * FROM shipments")
    List<Shipment> findAll();

    @Select("SELECT * FROM shipments WHERE truck_id = #{truck.id}")
    List<Shipment> findByTruck(@Param("truck") com.miniups.model.entity.Truck truck);
}
