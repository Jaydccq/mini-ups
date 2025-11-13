package com.miniups.repository;

import com.miniups.model.entity.Driver;
import com.miniups.model.enums.DriverStatus;
import org.apache.ibatis.annotations.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Mapper
public interface DriverRepository {

    @Insert("INSERT INTO drivers (driver_number, name, phone, status, truck_id, created_at, updated_at, version) " +
            "VALUES (#{driverNumber}, #{name}, #{phone}, #{status}, #{truckId}, NOW(), NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Driver driver);

    @Update("UPDATE drivers SET driver_number = #{driverNumber}, name = #{name}, phone = #{phone}, " +
            "status = #{status}, truck_id = #{truckId}, updated_at = NOW(), version = version + 1 " +
            "WHERE id = #{id} AND version = #{version}")
    int update(Driver driver);

    @Select("SELECT * FROM drivers WHERE id = #{id}")
    Driver selectById(Long id);

    @Select("SELECT * FROM drivers")
    List<Driver> selectAll();

    @Delete("DELETE FROM drivers WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM drivers WHERE driver_number = #{driverNumber}")
    Driver findByDriverNumber(String driverNumber);

    @Select("SELECT * FROM drivers WHERE status = #{status}")
    List<Driver> findByStatus(@Param("status") DriverStatus status);

    @Select("SELECT * FROM drivers WHERE truck_id = #{truckId}")
    Driver findByTruckId(Long truckId);

    @Select("SELECT COUNT(*) > 0 FROM drivers WHERE driver_number = #{driverNumber}")
    boolean existsByDriverNumber(String driverNumber);

    @Select("SELECT COUNT(*) FROM drivers")
    long count();

    // Additional methods for service layer compatibility
    @Select("SELECT * FROM drivers")
    List<Driver> findAll();

    @Select("SELECT * FROM drivers WHERE id = #{id}")
    Driver findById(Long id);

    // Pageable methods - unique names to avoid MyBatis conflicts
    @Select("SELECT * FROM drivers WHERE status = #{status}")
    Page<Driver> findByStatusWithPage(@Param("status") DriverStatus status, Pageable pageable);

    @Select("SELECT * FROM drivers")
    Page<Driver> findAllWithPage(Pageable pageable);

    // Additional query methods
    @Select("SELECT * FROM drivers WHERE status = #{status}")
    List<Driver> findAvailableDrivers(@Param("status") DriverStatus status);

    @Select("SELECT * FROM drivers WHERE LOWER(name) LIKE LOWER(CONCAT('%', #{name}, '%'))")
    List<Driver> findByNameContainingIgnoreCase(@Param("name") String name);

    @Select("SELECT COUNT(*) FROM drivers WHERE status = #{status}")
    long countByStatus(@Param("status") DriverStatus status);

    @Select("SELECT COUNT(*) > 0 FROM drivers WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);

    @Select("SELECT * FROM drivers WHERE email = #{email}")
    Driver findByEmail(@Param("email") String email);

    @Select("SELECT COUNT(*) > 0 FROM drivers WHERE license_number = #{licenseNumber}")
    boolean existsByLicenseNumber(@Param("licenseNumber") String licenseNumber);

    @Select("SELECT * FROM drivers WHERE license_number = #{licenseNumber}")
    Driver findByLicenseNumber(@Param("licenseNumber") String licenseNumber);

    // Statistics - returns map with status as key and count as value
    // This method would need custom implementation or SQL
    @Select("SELECT status, COUNT(*) as count FROM drivers GROUP BY status")
    @MapKey("status")
    java.util.Map<String, Object> getDriverStatistics();
}
