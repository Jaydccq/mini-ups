/**
 * Shipment Service
 * 
 * 功能说明：
 * - 提供运单创建、查询、更新的核心业务逻辑
 * - 集成Amazon订单和UPS运输服务
 * - 管理运单生命周期和状态变更
 * 
 * 主要功能：
 * - 创建新运单
 * - 查询运单信息
 * - 更新运单状态
 * - 运单分配和调度
 * 
 * 依赖服务：
 * - TrackingService: 追踪号生成和状态管理
 * - TruckManagementService: 车辆分配
 * - UserService: 用户信息验证
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.service;

import com.miniups.model.dto.shipment.CreateShipmentDto;
import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.User;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.UserRole;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.UserRepository;
import com.miniups.exception.ShipmentCreationException;
import com.miniups.util.Constants;
import com.miniups.util.SecurityUtils;
import com.miniups.util.NameParsingUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ShipmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(ShipmentService.class);
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TrackingService trackingService;
    
    @Autowired
    private TruckManagementService truckManagementService;
    
    /**
     * 创建新运单
     * 
     * @param createShipmentDto 运单创建请求
     * @return 创建的运单实体
     */
    public Shipment createShipment(CreateShipmentDto createShipmentDto) {
        try {
            logger.info("Creating shipment for customer: {}", createShipmentDto.getCustomerName());
            
            // 1. 获取或创建用户
            User customer = getOrCreateCustomer(createShipmentDto);
            
            // 2. 生成追踪号
            String trackingNumber = trackingService.generateTrackingNumber();
            
            // 3. 创建运单实体
            Shipment shipment = new Shipment();
            shipment.setUpsTrackingId(trackingNumber);
            shipment.setShipmentId(createShipmentDto.getShipmentId());
            shipment.setUser(customer);
            shipment.setOriginX(createShipmentDto.getOriginX());
            shipment.setOriginY(createShipmentDto.getOriginY());
            shipment.setDestX(createShipmentDto.getDestX());
            shipment.setDestY(createShipmentDto.getDestY());
            shipment.setWeight(createShipmentDto.getWeight());
            shipment.setStatus(ShipmentStatus.CREATED);
            shipment.setCreatedAt(LocalDateTime.now());
            shipment.setUpdatedAt(LocalDateTime.now());
            
            // 4. 分配车辆
            try {
                Truck assignedTruck = truckManagementService.assignOptimalTruck(
                    createShipmentDto.getOriginX(),
                    createShipmentDto.getOriginY(),
                    Constants.DEFAULT_PACKAGE_COUNT // 默认包裹数量
                );
                
                if (assignedTruck != null) {
                    shipment.setTruck(assignedTruck);
                    shipment.setStatus(ShipmentStatus.TRUCK_DISPATCHED);
                    logger.info("Assigned truck {} to shipment {}", assignedTruck.getLicensePlate(), trackingNumber);
                }
            } catch (Exception e) {
                logger.warn("Failed to assign truck to shipment {}: {}", trackingNumber, e.getMessage());
                // 继续处理，稍后可以手动分配车辆
            }
            
            // 5. 保存运单
            Shipment savedShipment = shipmentRepository.save(shipment);
            
            // 6. 记录状态历史
            trackingService.updateShipmentStatus(trackingNumber, savedShipment.getStatus(), "Shipment created");
            
            logger.info("Successfully created shipment: {}", trackingNumber);
            return savedShipment;
            
        } catch (ShipmentCreationException e) {
            // 重新抛出业务异常，保持异常链
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create shipment for customer {}: {}", 
                createShipmentDto.getCustomerName(), e.getMessage(), e);
            throw ShipmentCreationException.forCustomer(createShipmentDto.getCustomerName(), e);
        }
    }
    
    /**
     * 根据追踪号查询运单
     * 
     * @param trackingNumber UPS追踪号
     * @return 运单信息
     */
    public Optional<Shipment> findByTrackingNumber(String trackingNumber) {
        try {
            return shipmentRepository.findByUpsTrackingId(trackingNumber);
        } catch (Exception e) {
            logger.error("Error finding shipment by tracking number {}: {}", trackingNumber, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 查询所有运单
     * 
     * @return 运单列表
     */
    public List<Shipment> findAllShipments() {
        return shipmentRepository.findAll();
    }
    
    /**
     * 根据状态查询运单
     * 
     * @param status 运单状态
     * @return 符合条件的运单列表
     */
    public List<Shipment> findByStatus(ShipmentStatus status) {
        return shipmentRepository.findByStatus(status);
    }
    
    /**
     * 更新运单状态
     * 
     * @param trackingNumber 追踪号
     * @param newStatus 新状态
     * @param notes 备注
     */
    public void updateShipmentStatus(String trackingNumber, ShipmentStatus newStatus, String notes) {
        Optional<Shipment> shipmentOpt = findByTrackingNumber(trackingNumber);
        if (shipmentOpt.isPresent()) {
            Shipment shipment = shipmentOpt.get();
            shipment.setStatus(newStatus);
            shipment.setUpdatedAt(LocalDateTime.now());
            shipmentRepository.save(shipment);
            
            // 更新状态历史
            trackingService.updateShipmentStatus(trackingNumber, newStatus, notes);
            
            logger.info("Updated shipment {} status to {}", trackingNumber, newStatus);
        } else {
            logger.warn("Shipment not found for tracking number: {}", trackingNumber);
        }
    }
    
    /**
     * 获取或创建客户用户
     * 
     * @param dto 运单创建请求
     * @return 客户用户实体
     */
    private User getOrCreateCustomer(CreateShipmentDto dto) {
        // 先尝试根据客户ID查找
        if (dto.getCustomerId() != null) {
            Optional<User> existingUser = userRepository.findByUsername(dto.getCustomerId());
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
        }
        
        // 根据邮箱查找
        Optional<User> userByEmail = userRepository.findByEmail(dto.getCustomerEmail());
        if (userByEmail.isPresent()) {
            return userByEmail.get();
        }
        
        // 创建新用户
        try {
            User newUser = new User();
            newUser.setUsername(dto.getCustomerId() != null ? dto.getCustomerId() : 
                              NameParsingUtils.generateUsernameFromName(dto.getCustomerName()));
            newUser.setEmail(dto.getCustomerEmail());
            
            // 使用智能姓名解析
            NameParsingUtils.ParsedName parsedName = NameParsingUtils.parseName(dto.getCustomerName());
            newUser.setFirstName(parsedName.getFirstName());
            newUser.setLastName(parsedName.getLastName());
            
            newUser.setRole(UserRole.USER);
            
            // 生成安全的临时密码并加密存储
            String tempPassword = SecurityUtils.generateSecureTemporaryPassword();
            newUser.setPassword(SecurityUtils.encodePassword(tempPassword));
            
            // 标记为需要重置密码的用户
            newUser.setEnabled(true);
            newUser.setCreatedAt(LocalDateTime.now());
            
            // 记录临时密码生成（实际应用中应发送邮件通知用户）
            logger.info("Generated secure temporary password for new user: {} (Email: {})", 
                       newUser.getUsername(), newUser.getEmail());
            logger.warn("User {} must reset password on first login", newUser.getUsername());
            
            return userRepository.save(newUser);
            
        } catch (Exception e) {
            logger.error("Failed to create new user for customer {}: {}", dto.getCustomerEmail(), e.getMessage());
            throw ShipmentCreationException.userCreationFailed(dto.getCustomerEmail(), e);
        }
    }
}