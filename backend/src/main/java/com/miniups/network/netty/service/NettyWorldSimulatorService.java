package com.miniups.network.netty.service;

import com.miniups.model.entity.Truck;
import com.miniups.network.netty.client.NettyClient;
import com.miniups.proto.WorldUpsProto.*;
import com.miniups.repository.TruckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Netty-based World Simulator Service.
 * 
 * This service provides the same API as the original WorldSimulatorService
 * but uses the new Netty-based network implementation for better performance
 * and reliability.
 * 
 * Key improvements over the original implementation:
 * - Non-blocking I/O with Netty
 * - Automatic reconnection with exponential backoff
 * - Separated business logic through MessageHandlerService
 * - Better resource management and scalability
 * 
 * @author Mini-UPS System
 * @version 1.0
 */
@Service
@ConditionalOnProperty(
    name = "world.simulator.client.type",
    havingValue = "netty"
)
public class NettyWorldSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(NettyWorldSimulatorService.class);

    @Value("${world.simulator.host:localhost}")
    private String worldHost;

    @Value("${world.simulator.port:12345}")
    private int worldPort;

    @Value("${world.simulator.enabled:true}")
    private boolean worldSimulatorEnabled;

    private final NettyClient nettyClient;
    private final TruckRepository truckRepository;

    // Sequence number generator for message correlation
    private final AtomicLong sequenceGenerator = new AtomicLong(1);
    
    // World connection state
    private volatile Long worldId = null;
    private volatile boolean connected = false;

    public NettyWorldSimulatorService(NettyClient nettyClient, TruckRepository truckRepository) {
        this.nettyClient = nettyClient;
        this.truckRepository = truckRepository;
    }

    @PostConstruct
    public void initialize() {
        if (!worldSimulatorEnabled) {
            log.info("World Simulator integration is disabled");
            return;
        }

        log.info("Initializing Netty World Simulator Service...");
        connectToWorldSimulator();
    }

    @PreDestroy
    public void shutdown() {
        if (connected && nettyClient != null) {
            log.info("Shutting down Netty World Simulator Service...");
            
            // Send disconnect command
            try {
                UCommands disconnectCommand = UCommands.newBuilder()
                    .setDisconnect(true)
                    .build();
                nettyClient.sendCommand(disconnectCommand);
                
                // Wait a bit for the disconnect to be processed
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("Error sending disconnect command: {}", e.getMessage());
            }
            
            // Disconnect the client
            nettyClient.disconnect();
            connected = false;
        }
    }

    /**
     * Connect to the World Simulator and initialize trucks.
     */
    private void connectToWorldSimulator() {
        connectToWorldSimulator(null);
    }
    
    /**
     * Connect to the World Simulator with optional world ID.
     */
    public void connectToWorldSimulator(Long targetWorldId) {
        try {
            log.info("Connecting to World Simulator at {}:{} with world_id: {}", worldHost, worldPort, targetWorldId);
            
            // Get all trucks from database
            List<Truck> trucks = truckRepository.findAll();
            log.info("Found {} trucks to initialize", trucks.size());
            
            // Build initial connection message
            UConnect.Builder connectBuilder = UConnect.newBuilder()
                .setIsAmazon(false); // We are UPS
            
            // Set target world ID if provided
            if (targetWorldId != null) {
                connectBuilder.setWorldid(targetWorldId);
            }
            
            // Add truck initialization data
            for (Truck truck : trucks) {
                UInitTruck initTruck = UInitTruck.newBuilder()
                    .setId(truck.getTruckId())
                    .setX(truck.getCurrentX())
                    .setY(truck.getCurrentY())
                    .build();
                connectBuilder.addTrucks(initTruck);
            }
            
            UConnect connectMessage = connectBuilder.build();
            
            // Connect to World Simulator
            nettyClient.connect(worldHost, worldPort, targetWorldId)
                .thenCompose(result -> {
                    log.info("TCP connection established, sending UConnect message");
                    
                    // Send UConnect message and wait for UConnected response
                    return nettyClient.sendConnectAndWait(connectMessage, 30000);
                })
                .thenAccept(connectedResponse -> {
                    this.worldId = connectedResponse.getWorldid();
                    
                    if ("connected!".equals(connectedResponse.getResult())) {
                        connected = true;
                        log.info("Successfully connected to World Simulator with world_id: {}", this.worldId);
                        
                        // Set simulation speed
                        setSimulationSpeed(1000);
                    } else {
                        log.error("Failed to connect to World Simulator: {}", connectedResponse.getResult());
                        connected = false;
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Failed to connect to World Simulator: {}", throwable.getMessage(), throwable);
                    connected = false;
                    return null;
                });
                
        } catch (Exception e) {
            log.error("Error during World Simulator connection: {}", e.getMessage(), e);
            connected = false;
        }
    }

    /**
     * Send a truck to pickup packages at a warehouse.
     * 
     * @param truckId the ID of the truck
     * @param warehouseId the warehouse ID
     * @return CompletableFuture that completes when the command is acknowledged
     */
    public CompletableFuture<Boolean> sendTruckToPickup(Integer truckId, Integer warehouseId) {
        if (!connected) {
            return CompletableFuture.failedFuture(new RuntimeException("Not connected to World Simulator"));
        }

        long seqNum = sequenceGenerator.getAndIncrement();
        
        log.info("Sending truck {} to pickup at warehouse {} (seqnum: {})", truckId, warehouseId, seqNum);
        
        UGoPickup pickupCommand = UGoPickup.newBuilder()
            .setTruckid(truckId)
            .setWhid(warehouseId)
            .setSeqnum(seqNum)
            .build();
            
        UCommands command = UCommands.newBuilder()
            .addPickups(pickupCommand)
            .build();
            
        return nettyClient.sendCommandAndWait(command, seqNum, 30000)
            .thenApply(response -> {
                log.debug("Pickup command acknowledged for truck {} (seqnum: {})", truckId, seqNum);
                return true;
            })
            .exceptionally(throwable -> {
                log.error("Failed to send pickup command for truck {}: {}", truckId, throwable.getMessage());
                return false;
            });
    }

    /**
     * Send a truck to deliver packages.
     * 
     * @param truckId the ID of the truck
     * @param deliveries map of package ID to delivery coordinates [x, y]
     * @return CompletableFuture that completes when the command is acknowledged
     */
    public CompletableFuture<Boolean> sendTruckToDeliver(Integer truckId, Map<Long, int[]> deliveries) {
        if (!connected) {
            return CompletableFuture.failedFuture(new RuntimeException("Not connected to World Simulator"));
        }

        long seqNum = sequenceGenerator.getAndIncrement();
        
        log.info("Sending truck {} to deliver {} packages (seqnum: {})", truckId, deliveries.size(), seqNum);
        
        UGoDeliver.Builder deliverBuilder = UGoDeliver.newBuilder()
            .setTruckid(truckId)
            .setSeqnum(seqNum);
            
        // Add delivery locations
        for (Map.Entry<Long, int[]> entry : deliveries.entrySet()) {
            Long packageId = entry.getKey();
            int[] coordinates = entry.getValue();
            
            UDeliveryLocation location = UDeliveryLocation.newBuilder()
                .setPackageid(packageId)
                .setX(coordinates[0])
                .setY(coordinates[1])
                .build();
                
            deliverBuilder.addPackages(location);
        }
        
        UGoDeliver deliverCommand = deliverBuilder.build();
        UCommands command = UCommands.newBuilder()
            .addDeliveries(deliverCommand)
            .build();
            
        return nettyClient.sendCommandAndWait(command, seqNum, 30000)
            .thenApply(response -> {
                log.debug("Delivery command acknowledged for truck {} (seqnum: {})", truckId, seqNum);
                return true;
            })
            .exceptionally(throwable -> {
                log.error("Failed to send delivery command for truck {}: {}", truckId, throwable.getMessage());
                return false;
            });
    }

    /**
     * Query the status of a specific truck.
     * 
     * @param truckId the ID of the truck to query
     * @return CompletableFuture with the truck status
     */
    public CompletableFuture<UTruck> queryTruckStatus(Integer truckId) {
        if (!connected) {
            return CompletableFuture.failedFuture(new RuntimeException("Not connected to World Simulator"));
        }

        long seqNum = sequenceGenerator.getAndIncrement();
        
        log.debug("Querying status for truck {} (seqnum: {})", truckId, seqNum);
        
        UQuery query = UQuery.newBuilder()
            .setTruckid(truckId)
            .setSeqnum(seqNum)
            .build();
            
        UCommands command = UCommands.newBuilder()
            .addQueries(query)
            .build();
            
        return nettyClient.sendCommandAndWait(command, seqNum, 10000)
            .thenApply(response -> {
                if (response instanceof UTruck) {
                    UTruck truckStatus = (UTruck) response;
                    log.debug("Received status for truck {}: {} at ({}, {})", 
                             truckId, truckStatus.getStatus(), truckStatus.getX(), truckStatus.getY());
                    return truckStatus;
                } else {
                    throw new RuntimeException("Unexpected response type: " + response.getClass());
                }
            })
            .exceptionally(throwable -> {
                log.error("Failed to query truck {} status: {}", truckId, throwable.getMessage());
                throw new RuntimeException("Failed to query truck status", throwable);
            });
    }

    /**
     * Set the simulation speed.
     * 
     * @param speed the simulation speed multiplier
     */
    public void setSimulationSpeed(int speed) {
        if (!connected) {
            log.warn("Cannot set simulation speed - not connected to World Simulator");
            return;
        }

        log.info("Setting World Simulator speed to {}", speed);
        
        UCommands speedCommand = UCommands.newBuilder()
            .setSimspeed(speed)
            .build();
            
        nettyClient.sendCommand(speedCommand)
            .addListener(future -> {
                if (future.isSuccess()) {
                    log.debug("Simulation speed set to {}", speed);
                } else {
                    log.error("Failed to set simulation speed: {}", future.cause().getMessage());
                }
            });
    }

    /**
     * Check if the service is connected to the World Simulator.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected && nettyClient.isConnected();
    }

    /**
     * Get the current world ID.
     * 
     * @return the world ID, or null if not connected
     */
    public Long getWorldId() {
        return worldId;
    }
    
    /**
     * Manually connect to a specific world ID (for testing and Amazon integration).
     * This method can be called to join an existing world created by Amazon service.
     * 
     * @param targetWorldId the world ID to connect to
     */
    public void connectToSpecificWorld(Long targetWorldId) {
        if (connected) {
            log.warn("Already connected to world {}. Disconnecting first...", worldId);
            nettyClient.disconnect();
            connected = false;
            worldId = null;
        }
        
        log.info("Attempting to connect to specific world_id: {}", targetWorldId);
        connectToWorldSimulator(targetWorldId);
    }
}