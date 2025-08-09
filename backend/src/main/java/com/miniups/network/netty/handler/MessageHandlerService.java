package com.miniups.network.netty.handler;

import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.TruckStatus;
import com.miniups.proto.WorldUpsProto.UFinished;
import com.miniups.proto.WorldUpsProto.UDeliveryMade;
import com.miniups.proto.WorldUpsProto.UTruck;
import com.miniups.proto.WorldUpsProto.UErr;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.TruckRepository;
import com.miniups.service.AmazonIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for handling World Simulator message processing and business logic.
 * 
 * This service extracts the business logic from the network layer and provides
 * transactional processing of World Simulator responses. It maintains the
 * separation of concerns by handling all database operations and business
 * rules while keeping the Netty handlers focused on network I/O.
 * 
 * Key responsibilities:
 * - Process truck completion notifications
 * - Handle package delivery confirmations  
 * - Update truck status and positions
 * - Integrate with Amazon service for notifications
 * - Manage transaction boundaries for data consistency
 * 
 * All methods in this service are transactional to ensure data consistency
 * when processing World Simulator events.
 * 
 * @author Mini-UPS System
 * @version 1.0
 */
@Slf4j
@Service
public class MessageHandlerService {

    private final TruckRepository truckRepository;
    private final ShipmentRepository shipmentRepository;
    private final AmazonIntegrationService amazonIntegrationService;

    public MessageHandlerService(TruckRepository truckRepository,
                               ShipmentRepository shipmentRepository,
                               AmazonIntegrationService amazonIntegrationService) {
        this.truckRepository = truckRepository;
        this.shipmentRepository = shipmentRepository;
        this.amazonIntegrationService = amazonIntegrationService;
    }

    /**
     * Handle truck completion notifications from World Simulator.
     * 
     * This method processes UFinished messages which indicate that a truck
     * has completed a pickup or delivery task. It updates the truck's position
     * and status in the database and notifies Amazon when appropriate.
     * 
     * @param completion the UFinished message from World Simulator
     */
    @Transactional
    public void handleTruckCompletion(UFinished completion) {
        log.info("Processing truck completion for truck {} at ({}, {}) with status: '{}'", 
                completion.getTruckid(), completion.getX(), completion.getY(), completion.getStatus());
        
        Optional<Truck> truckOpt = truckRepository.findByTruckId(completion.getTruckid());
        if (truckOpt.isEmpty()) {
            log.warn("Truck not found for completion: truckId={}", completion.getTruckid());
            return;
        }
        
        Truck truck = truckOpt.get();
        
        // Update truck position
        truck.setCurrentX(completion.getX());
        truck.setCurrentY(completion.getY());
        
        // Update truck status based on completion status
        switch (completion.getStatus()) {
            case "idle":
                truck.setStatus(TruckStatus.IDLE);
                log.debug("Truck {} set to IDLE status", completion.getTruckid());
                break;
                
            case "arrive warehouse":
                truck.setStatus(TruckStatus.AT_WAREHOUSE);
                log.info("Truck {} arrived at warehouse, notifying Amazon", completion.getTruckid());
                notifyAmazonTruckArrived(truck, completion);
                break;
                
            default:
                log.debug("Truck {} completion with unhandled status: '{}'", 
                         completion.getTruckid(), completion.getStatus());
                break;
        }
        
        // Save the updated truck
        truckRepository.save(truck);
        log.debug("Truck {} status updated successfully", completion.getTruckid());
    }

    /**
     * Handle package delivery notifications from World Simulator.
     * 
     * This method processes UDeliveryMade messages which confirm that a
     * package has been successfully delivered. It updates the shipment
     * status and notifies Amazon of the delivery completion.
     * 
     * @param delivery the UDeliveryMade message from World Simulator
     */
    @Transactional
    public void handleDeliveryMade(UDeliveryMade delivery) {
        log.info("Processing delivery completion for package {} by truck {}", 
                delivery.getPackageid(), delivery.getTruckid());
        
        // Note: In the current system, packageid is actually the shipment_id
        Optional<Shipment> shipmentOpt = shipmentRepository.findByShipmentId(String.valueOf(delivery.getPackageid()));
        if (shipmentOpt.isEmpty()) {
            log.warn("Shipment not found for delivery: packageId={}", delivery.getPackageid());
            return;
        }
        
        Shipment shipment = shipmentOpt.get();
        
        // Update shipment status to delivered
        shipment.updateStatus(ShipmentStatus.DELIVERED);
        shipment.setActualDelivery(LocalDateTime.now());
        shipmentRepository.save(shipment);
        
        log.info("Shipment {} marked as delivered", shipment.getShipmentId());
        
        // Notify Amazon service of the delivery
        try {
            amazonIntegrationService.notifyShipmentDelivered(shipment.getShipmentId());
            log.debug("Amazon notified of shipment {} delivery", shipment.getShipmentId());
        } catch (Exception e) {
            log.error("Failed to notify Amazon of shipment {} delivery: {}", 
                     shipment.getShipmentId(), e.getMessage(), e);
            // Don't rethrow - the shipment is still delivered even if notification fails
        }
    }

    /**
     * Handle truck status updates from World Simulator.
     * 
     * This method processes UTruck messages which provide real-time updates
     * about truck positions and status. It keeps the database synchronized
     * with the World Simulator's view of truck states.
     * 
     * @param truckStatus the UTruck message from World Simulator
     */
    @Transactional
    public void handleTruckStatus(UTruck truckStatus) {
        log.debug("Processing truck status update for truck {} at ({}, {}) with status: '{}'", 
                 truckStatus.getTruckid(), truckStatus.getX(), truckStatus.getY(), truckStatus.getStatus());
        
        Optional<Truck> truckOpt = truckRepository.findByTruckId(truckStatus.getTruckid());
        if (truckOpt.isEmpty()) {
            log.warn("Truck not found for status update: truckId={}", truckStatus.getTruckid());
            return;
        }
        
        Truck truck = truckOpt.get();
        
        // Update truck position
        truck.setCurrentX(truckStatus.getX());
        truck.setCurrentY(truckStatus.getY());
        
        // Update truck status based on World Simulator status
        TruckStatus newStatus = mapWorldSimulatorStatus(truckStatus.getStatus());
        if (newStatus != null) {
            truck.setStatus(newStatus);
            log.debug("Truck {} status updated to: {}", truckStatus.getTruckid(), newStatus);
        } else {
            log.warn("Unknown truck status from World Simulator: '{}' for truck {}", 
                    truckStatus.getStatus(), truckStatus.getTruckid());
        }
        
        // Save the updated truck
        truckRepository.save(truck);
    }

    /**
     * Handle error messages from World Simulator.
     * 
     * This method processes UErr messages which indicate errors in command
     * processing by the World Simulator. It logs the errors for debugging
     * and monitoring purposes.
     * 
     * @param error the UErr message from World Simulator
     */
    public void handleError(UErr error) {
        log.error("World Simulator reported error: '{}' for original sequence number {} (error seqnum: {})", 
                 error.getErr(), error.getOriginseqnum(), error.getSeqnum());
        
        // Could implement additional error handling logic here, such as:
        // - Updating metrics/monitoring systems
        // - Triggering alerts for critical errors
        // - Implementing retry logic for certain types of errors
        
        // For now, just log the error for visibility
    }

    /**
     * Map World Simulator status strings to our TruckStatus enum.
     */
    private TruckStatus mapWorldSimulatorStatus(String worldSimStatus) {
        return switch (worldSimStatus) {
            case "idle" -> TruckStatus.IDLE;
            case "traveling" -> TruckStatus.EN_ROUTE;
            case "arrive warehouse" -> TruckStatus.AT_WAREHOUSE;
            case "loading" -> TruckStatus.LOADING;
            case "delivering" -> TruckStatus.DELIVERING;
            default -> {
                log.warn("Unmapped World Simulator status: '{}'", worldSimStatus);
                yield null;
            }
        };
    }

    /**
     * Notify Amazon service when a truck arrives at a warehouse.
     * 
     * This method handles the integration with Amazon by notifying them
     * when one of our trucks arrives at their warehouse for pickup.
     */
    private void notifyAmazonTruckArrived(Truck truck, UFinished completion) {
        try {
            // Find the shipment this truck is handling
            Optional<Shipment> shipmentOpt = shipmentRepository.findByTruck(truck);
            if (shipmentOpt.isEmpty()) {
                log.warn("No active shipment found for truck {} arrival notification", truck.getTruckId());
                return;
            }
            
            Shipment shipment = shipmentOpt.get();
            
            // Generate warehouse ID from coordinates
            // This is a simplified approach - in a real system, you might have
            // a proper warehouse mapping service
            String warehouseId = completion.getX() + "_" + completion.getY();
            
            // Notify Amazon service
            amazonIntegrationService.notifyTruckArrived(
                truck.getId().toString(), 
                warehouseId, 
                shipment.getShipmentId()
            );
            
            log.info("Successfully notified Amazon of truck {} arrival at warehouse {} for shipment {}", 
                    truck.getTruckId(), warehouseId, shipment.getShipmentId());
                    
        } catch (Exception e) {
            log.error("Failed to notify Amazon of truck {} arrival: {}", 
                     truck.getTruckId(), e.getMessage(), e);
            // Don't rethrow - the truck arrival is still processed even if notification fails
        }
    }
}