/**
 * World Simulator Connection Service
 * 
 * Functionality:
 * - Manage TCP connection and communication with World Simulator
 * - Implement Google Protocol Buffer message serialization/deserialization
 * - Handle core business of truck scheduling, pickup, delivery
 * 
 * Connection Management:
 * - TCP Socket connection to World Simulator (port 12345)
 * - Automatic reconnection mechanism and error handling
 * - Message queue and async processing
 * 
 * Protocol Support:
 * - UConnect: Connect to world and initialize trucks
 * - UGoPickup: Send truck to warehouse for pickup
 * - UGoDeliver: Send truck for delivery
 * - UQuery: Query truck status
 * - UResponses: Handle world response messages
 * 
 * Business Integration:
 * - Tightly integrated with UPS business logic
 * - Real-time truck location and status updates
 * - Support multi-package delivery optimization
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.miniups.model.entity.Shipment;
import com.miniups.model.entity.Truck;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.model.enums.TruckStatus;
import com.miniups.proto.WorldUpsProto;
import com.miniups.repository.ShipmentRepository;
import com.miniups.repository.TruckRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WorldSimulatorService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldSimulatorService.class);
    
    @Value("${world.simulator.host:host.docker.internal}")
    private String worldHost;
    
    @Value("${world.simulator.port:12345}")
    private int worldPort;
    
    @Value("${world.simulator.connection-timeout:10000}")
    private int connectionTimeout;
    
    @Value("${world.simulator.default-sim-speed:1000}")
    private int defaultSimSpeed;
    
    @Autowired
    private TruckRepository truckRepository;
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // Connection management
    private Socket socket;
    private volatile boolean connected = false;
    private volatile boolean running = false;
    private volatile boolean reconnectionInProgress = false;
    private Long worldId;
    
    // Reconnection configuration
    @Value("${world.simulator.reconnection.enabled:true}")
    private boolean reconnectionEnabled;
    
    @Value("${world.simulator.reconnection.initial-delay:1000}")
    private long reconnectionInitialDelay;
    
    @Value("${world.simulator.reconnection.max-delay:30000}")
    private long reconnectionMaxDelay;
    
    @Value("${world.simulator.reconnection.multiplier:2.0}")
    private double reconnectionMultiplier;
    
    @Value("${world.simulator.reconnection.max-attempts:10}")
    private int reconnectionMaxAttempts;
    
    // Threading and message handling
    private ExecutorService executorService;
    private BlockingQueue<WorldUpsProto.UCommands> messageQueue;
    private Future<?> senderTask;
    private Future<?> receiverTask;
    
    // Sequence number management
    private final AtomicLong sequenceNumber = new AtomicLong(0);
    
    // Response handling
    private final Map<Long, CompletableFuture<Object>> pendingResponses = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> responseTimeouts = new ConcurrentHashMap<>();
    
    // Truck initialization data
    private List<Truck> availableTrucks = new ArrayList<>();
    
    @PostConstruct
    public void initialize() {
        this.executorService = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "WorldSimulator-" + Thread.currentThread().getId());
            t.setDaemon(true);
            return t;
        });
        this.messageQueue = new LinkedBlockingQueue<>();
        
        logger.info("WorldSimulatorService initialized");
    }
    
    @PreDestroy
    public void shutdown() {
        disconnect();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("WorldSimulatorService shut down");
    }
    
    /**
     * Connect to World Simulator
     * 
     * @param worldId World ID, null means create new world
     * @return Whether connection was successful
     */
    public synchronized boolean connect(Long worldId) {
        if (connected) {
            logger.warn("Already connected to World Simulator");
            return true;
        }
        
        try {
            logger.info("Connecting to World Simulator at {}:{}", worldHost, worldPort);
            
            // Establish TCP connection
            socket = new Socket(worldHost, worldPort);
            socket.setSoTimeout(connectionTimeout);
            
            // Initialize truck data
            initializeTrucks();
            
            // Create connection message
            WorldUpsProto.UConnect.Builder connectBuilder = WorldUpsProto.UConnect.newBuilder();
            connectBuilder.setIsAmazon(false); // UPS system
            
            if (worldId != null) {
                connectBuilder.setWorldid(worldId);
            }
            
            // Add truck initialization information
            for (Truck truck : availableTrucks) {
                WorldUpsProto.UInitTruck.Builder truckBuilder = WorldUpsProto.UInitTruck.newBuilder();
                truckBuilder.setId(truck.getTruckId());
                truckBuilder.setX(truck.getCurrentX() != null ? truck.getCurrentX() : 0);
                truckBuilder.setY(truck.getCurrentY() != null ? truck.getCurrentY() : 0);
                connectBuilder.addTrucks(truckBuilder.build());
            }
            
            WorldUpsProto.UConnect connectMessage = connectBuilder.build();
            
            // Send connection message
            sendProtobufMessage(connectMessage);
            
            // Receive connection response
            byte[] responseData = receiveMessage();
            WorldUpsProto.UConnected response = WorldUpsProto.UConnected.parseFrom(responseData);
            
            if ("connected!".equals(response.getResult())) {
                this.worldId = response.getWorldid();
                this.connected = true;
                this.running = true;
                
                logger.info("Successfully connected to World Simulator with world ID: {}", this.worldId);
                
                // Start message processing threads
                startMessageProcessing();
                
                // Set default simulation speed
                setSimulationSpeed(defaultSimSpeed);
                
                return true;
            } else {
                logger.error("Failed to connect to World Simulator: {}", response.getResult());
                closeSocket();
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error connecting to World Simulator", e);
            closeSocket();
            return false;
        }
    }
    
    /**
     * Disconnect from World Simulator
     */
    public synchronized void disconnect() {
        if (!connected) {
            return;
        }
        
        try {
            logger.info("Disconnecting from World Simulator");
            
            running = false;
            
            // Send disconnect command
            if (socket != null && !socket.isClosed()) {
                WorldUpsProto.UCommands.Builder commandsBuilder = WorldUpsProto.UCommands.newBuilder();
                commandsBuilder.setDisconnect(true);
                sendCommandsAsync(commandsBuilder.build());
                
                // Wait a moment for message to send
                Thread.sleep(500);
            }
            
            // Stop message processing threads
            stopMessageProcessing();
            
            // Clean up state
            connected = false;
            worldId = null;
            pendingResponses.clear();
            responseTimeouts.clear();
            messageQueue.clear();
            
            closeSocket();
            
            logger.info("Disconnected from World Simulator");
            
        } catch (Exception e) {
            logger.error("Error disconnecting from World Simulator", e);
        }
    }
    
    /**
     * Send truck to warehouse for pickup
     * 
     * @param truckId Truck ID
     * @param warehouseId Warehouse ID
     * @return Whether operation was successful
     */
    public CompletableFuture<Boolean> sendTruckToPickup(Integer truckId, Integer warehouseId) {
        if (truckId == null) {
            throw new IllegalArgumentException("Truck ID cannot be null");
        }
        if (warehouseId == null || warehouseId < 0) {
            throw new IllegalArgumentException("Warehouse ID must be a positive number");
        }
        
        if (!connected) {
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            long seqNum = getNextSequenceNumber();
            
            WorldUpsProto.UGoPickup.Builder pickupBuilder = WorldUpsProto.UGoPickup.newBuilder();
            pickupBuilder.setTruckid(truckId);
            pickupBuilder.setWhid(warehouseId);
            pickupBuilder.setSeqnum(seqNum);
            
            WorldUpsProto.UCommands.Builder commandsBuilder = WorldUpsProto.UCommands.newBuilder();
            commandsBuilder.addPickups(pickupBuilder.build());
            
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingResponses.put(seqNum, future);
            responseTimeouts.put(seqNum, LocalDateTime.now().plusSeconds(30));
            
            sendCommandsAsync(commandsBuilder.build());
            
            logger.info("Sent truck {} to pickup at warehouse {}", truckId, warehouseId);
            
            return future.thenApply(result -> {
                if (result instanceof String && ((String) result).contains("Error")) {
                    logger.error("Pickup failed: {}", result);
                    return false;
                }
                return true;
            });
            
        } catch (Exception e) {
            logger.error("Error sending truck to pickup", e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Send truck for delivery
     * 
     * @param truckId Truck ID
     * @param deliveries Delivery list (packageId -> coordinates)
     * @return Whether operation was successful
     */
    public CompletableFuture<Boolean> sendTruckToDeliver(Integer truckId, Map<Long, int[]> deliveries) {
        if (truckId == null) {
            throw new IllegalArgumentException("Truck ID cannot be null");
        }
        if (deliveries == null || deliveries.isEmpty()) {
            throw new IllegalArgumentException("Deliveries cannot be null or empty");
        }
        
        if (!connected) {
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            long seqNum = getNextSequenceNumber();
            
            WorldUpsProto.UGoDeliver.Builder deliverBuilder = WorldUpsProto.UGoDeliver.newBuilder();
            deliverBuilder.setTruckid(truckId);
            deliverBuilder.setSeqnum(seqNum);
            
            // Add delivery locations
            for (Map.Entry<Long, int[]> delivery : deliveries.entrySet()) {
                WorldUpsProto.UDeliveryLocation.Builder locationBuilder = WorldUpsProto.UDeliveryLocation.newBuilder();
                locationBuilder.setPackageid(delivery.getKey());
                locationBuilder.setX(delivery.getValue()[0]);
                locationBuilder.setY(delivery.getValue()[1]);
                deliverBuilder.addPackages(locationBuilder.build());
            }
            
            WorldUpsProto.UCommands.Builder commandsBuilder = WorldUpsProto.UCommands.newBuilder();
            commandsBuilder.addDeliveries(deliverBuilder.build());
            
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingResponses.put(seqNum, future);
            responseTimeouts.put(seqNum, LocalDateTime.now().plusSeconds(30));
            
            sendCommandsAsync(commandsBuilder.build());
            
            logger.info("Sent truck {} to deliver {} packages", truckId, deliveries.size());
            
            return future.thenApply(result -> {
                if (result instanceof String && ((String) result).contains("Error")) {
                    logger.error("Delivery failed: {}", result);
                    return false;
                }
                return true;
            });
            
        } catch (Exception e) {
            logger.error("Error sending truck to deliver", e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Query truck status
     * 
     * @param truckId Truck ID
     * @return Truck status information
     */
    public CompletableFuture<WorldUpsProto.UTruck> queryTruckStatus(Integer truckId) {
        if (!connected) {
            return CompletableFuture.completedFuture(null);
        }
        
        try {
            long seqNum = getNextSequenceNumber();
            
            WorldUpsProto.UQuery.Builder queryBuilder = WorldUpsProto.UQuery.newBuilder();
            queryBuilder.setTruckid(truckId);
            queryBuilder.setSeqnum(seqNum);
            
            WorldUpsProto.UCommands.Builder commandsBuilder = WorldUpsProto.UCommands.newBuilder();
            commandsBuilder.addQueries(queryBuilder.build());
            
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingResponses.put(seqNum, future);
            responseTimeouts.put(seqNum, LocalDateTime.now().plusSeconds(10));
            
            sendCommandsAsync(commandsBuilder.build());
            
            return future.thenApply(result -> {
                if (result instanceof WorldUpsProto.UTruck) {
                    return (WorldUpsProto.UTruck) result;
                }
                return null;
            });
            
        } catch (Exception e) {
            logger.error("Error querying truck status", e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Set simulation speed
     * 
     * @param speed Speed value
     */
    public void setSimulationSpeed(int speed) {
        if (!connected) {
            return;
        }
        
        try {
            WorldUpsProto.UCommands.Builder commandsBuilder = WorldUpsProto.UCommands.newBuilder();
            commandsBuilder.setSimspeed(speed);
            
            sendCommandsAsync(commandsBuilder.build());
            
            logger.info("Set simulation speed to {}", speed);
            
        } catch (Exception e) {
            logger.error("Error setting simulation speed", e);
        }
    }
    
    // Private helper methods
    
    private void initializeTrucks() {
        availableTrucks = truckRepository.findAll();
        if (availableTrucks.isEmpty()) {
            // Create default trucks
            for (int i = 1; i <= 10; i++) {
                Truck truck = new Truck();
                truck.setTruckId(i);
                truck.setStatus(TruckStatus.IDLE);
                truck.setCurrentX(0);
                truck.setCurrentY(0);
                truck.setCapacity(1000);
                availableTrucks.add(truckRepository.save(truck));
            }
            logger.info("Created {} default trucks", availableTrucks.size());
        }
        logger.info("Initialized {} trucks for world connection", availableTrucks.size());
    }
    
    private void startMessageProcessing() {
        // 启动消息发送线程
        senderTask = executorService.submit(this::messageSenderLoop);
        
        // 启动消息接收线程
        receiverTask = executorService.submit(this::messageReceiverLoop);
        
        // 启动超时清理线程
        executorService.submit(this::timeoutCleanupLoop);
    }
    
    private void stopMessageProcessing() {
        if (senderTask != null) {
            senderTask.cancel(true);
        }
        if (receiverTask != null) {
            receiverTask.cancel(true);
        }
    }
    
    private void messageSenderLoop() {
        logger.info("Message sender loop started");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                WorldUpsProto.UCommands command = messageQueue.poll(1, TimeUnit.SECONDS);
                if (command != null) {
                    sendProtobufMessage(command);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in message sender loop", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        logger.info("Message sender loop stopped");
    }
    
    private void messageReceiverLoop() {
        logger.info("Message receiver loop started");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] messageData = receiveMessage();
                if (messageData != null) {
                    processIncomingMessage(messageData);
                }
            } catch (Exception e) {
                if (running) {
                    logger.error("Error in message receiver loop", e);
                    
                    // Check if the error is due to connection issues
                    if (isConnectionError(e)) {
                        logger.warn("Connection error detected, triggering reconnection");
                        handleConnectionLoss();
                        break; // Exit the loop, reconnection will restart it
                    }
                    
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        
        logger.info("Message receiver loop stopped");
    }
    
    private void timeoutCleanupLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                LocalDateTime now = LocalDateTime.now();
                List<Long> timedOutSeqNums = new ArrayList<>();
                
                for (Map.Entry<Long, LocalDateTime> entry : responseTimeouts.entrySet()) {
                    if (now.isAfter(entry.getValue())) {
                        timedOutSeqNums.add(entry.getKey());
                    }
                }
                
                for (Long seqNum : timedOutSeqNums) {
                    CompletableFuture<Object> future = pendingResponses.remove(seqNum);
                    responseTimeouts.remove(seqNum);
                    if (future != null) {
                        future.completeExceptionally(new TimeoutException("Response timeout for seqnum: " + seqNum));
                    }
                }
                
                Thread.sleep(5000); // Check every 5 seconds
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in timeout cleanup loop", e);
            }
        }
    }
    
    private void processIncomingMessage(byte[] messageData) {
        try {
            WorldUpsProto.UResponses responses = WorldUpsProto.UResponses.parseFrom(messageData);
            
            // 发送ACK
            List<Long> acksToSend = new ArrayList<>();
            
            // 处理完成通知
            for (WorldUpsProto.UFinished completion : responses.getCompletionsList()) {
                acksToSend.add(completion.getSeqnum());
                handleTruckCompletion(completion);
            }
            
            // 处理配送完成通知
            for (WorldUpsProto.UDeliveryMade delivery : responses.getDeliveredList()) {
                acksToSend.add(delivery.getSeqnum());
                handleDeliveryMade(delivery);
            }
            
            // 处理卡车状态响应
            for (WorldUpsProto.UTruck truckStatus : responses.getTruckstatusList()) {
                acksToSend.add(truckStatus.getSeqnum());
                handleTruckStatus(truckStatus);
            }
            
            // 处理错误响应
            for (WorldUpsProto.UErr error : responses.getErrorList()) {
                acksToSend.add(error.getSeqnum());
                handleError(error);
            }
            
            // 处理ACK
            for (Long ack : responses.getAcksList()) {
                handleAcknowledgement(ack);
            }
            
            // 发送ACK响应
            if (!acksToSend.isEmpty()) {
                sendAcknowledgements(acksToSend);
            }
            
        } catch (InvalidProtocolBufferException e) {
            logger.error("Error parsing incoming message", e);
        }
    }
    
    @Transactional
    protected void handleTruckCompletion(WorldUpsProto.UFinished completion) {
        logger.info("Truck {} completed task at ({}, {}) with status: {}", 
                   completion.getTruckid(), completion.getX(), completion.getY(), completion.getStatus());
        
        // 更新卡车状态
        Optional<Truck> truckOpt = truckRepository.findByTruckId(completion.getTruckid());
        if (truckOpt.isPresent()) {
            Truck truck = truckOpt.get();
            truck.setCurrentX(completion.getX());
            truck.setCurrentY(completion.getY());
            
            // 根据状态更新卡车状态
            if ("idle".equals(completion.getStatus())) {
                truck.setStatus(TruckStatus.IDLE);
            } else if ("arrive warehouse".equals(completion.getStatus())) {
                truck.setStatus(TruckStatus.AT_WAREHOUSE);
                // 通知Amazon卡车已到达
                notifyAmazonTruckArrived(truck, completion);
            }
            
            truckRepository.save(truck);
        }
        
        // 完成对应的Future
        CompletableFuture<Object> future = pendingResponses.remove(completion.getSeqnum());
        if (future != null) {
            future.complete(completion);
        }
    }
    
    @Transactional
    protected void handleDeliveryMade(WorldUpsProto.UDeliveryMade delivery) {
        logger.info("Package {} delivered by truck {}", delivery.getPackageid(), delivery.getTruckid());
        
        // 查找对应的运输订单
        // 注意：packageid在这里实际上是shipment_id
        Optional<Shipment> shipmentOpt = shipmentRepository.findByShipmentId(String.valueOf(delivery.getPackageid()));
        if (shipmentOpt.isPresent()) {
            Shipment shipment = shipmentOpt.get();
            shipment.updateStatus(ShipmentStatus.DELIVERED);
            shipment.setActualDelivery(LocalDateTime.now());
            shipmentRepository.save(shipment);
            
            // 通知Amazon包裹已送达
            getAmazonIntegrationService().notifyShipmentDelivered(shipment.getShipmentId());
            
            logger.info("Updated shipment {} status to delivered", shipment.getShipmentId());
        }
        
        // 完成对应的Future
        CompletableFuture<Object> future = pendingResponses.remove(delivery.getSeqnum());
        if (future != null) {
            future.complete(delivery);
        }
    }
    
    @Transactional
    protected void handleTruckStatus(WorldUpsProto.UTruck truckStatus) {
        logger.debug("Truck {} status: {} at ({}, {})", 
                    truckStatus.getTruckid(), truckStatus.getStatus(), 
                    truckStatus.getX(), truckStatus.getY());
        
        // 更新数据库中的卡车状态
        Optional<Truck> truckOpt = truckRepository.findByTruckId(truckStatus.getTruckid());
        if (truckOpt.isPresent()) {
            Truck truck = truckOpt.get();
            truck.setCurrentX(truckStatus.getX());
            truck.setCurrentY(truckStatus.getY());
            
            // 转换状态
            switch (truckStatus.getStatus()) {
                case "idle":
                    truck.setStatus(TruckStatus.IDLE);
                    break;
                case "traveling":
                    truck.setStatus(TruckStatus.EN_ROUTE);
                    break;
                case "arrive warehouse":
                    truck.setStatus(TruckStatus.AT_WAREHOUSE);
                    break;
                case "loading":
                    truck.setStatus(TruckStatus.LOADING);
                    break;
                case "delivering":
                    truck.setStatus(TruckStatus.DELIVERING);
                    break;
            }
            
            truckRepository.save(truck);
        }
        
        // 完成对应的Future
        CompletableFuture<Object> future = pendingResponses.remove(truckStatus.getSeqnum());
        if (future != null) {
            future.complete(truckStatus);
        }
    }
    
    private void handleError(WorldUpsProto.UErr error) {
        logger.error("World Simulator error for seqnum {}: {}", error.getOriginseqnum(), error.getErr());
        
        // 完成对应的Future
        CompletableFuture<Object> future = pendingResponses.remove(error.getOriginseqnum());
        if (future != null) {
            future.complete("Error: " + error.getErr());
        }
    }
    
    private void handleAcknowledgement(Long ack) {
        logger.debug("Received ACK for seqnum {}", ack);
        responseTimeouts.remove(ack);
    }
    
    private void notifyAmazonTruckArrived(Truck truck, WorldUpsProto.UFinished completion) {
        // 查找这个卡车正在处理的运输订单
        Optional<Shipment> shipmentOpt = shipmentRepository.findByTruck(truck);
        if (shipmentOpt.isPresent()) {
            Shipment shipment = shipmentOpt.get();
            // 假设仓库ID可以从坐标推断，或者需要额外的映射
            // 这里简化处理，使用坐标作为仓库ID
            String warehouseId = completion.getX() + "_" + completion.getY();
            
            getAmazonIntegrationService().notifyTruckArrived(
                truck.getId().toString(), 
                warehouseId, 
                shipment.getShipmentId()
            );
        }
    }
    
    private void sendCommandsAsync(WorldUpsProto.UCommands commands) {
        messageQueue.offer(commands);
    }
    
    private void sendAcknowledgements(List<Long> acks) {
        try {
            WorldUpsProto.UCommands.Builder commandsBuilder = WorldUpsProto.UCommands.newBuilder();
            commandsBuilder.addAllAcks(acks);
            sendCommandsAsync(commandsBuilder.build());
        } catch (Exception e) {
            logger.error("Error sending acknowledgements", e);
        }
    }
    
    private void sendProtobufMessage(com.google.protobuf.Message message) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket is not connected");
        }
        
        byte[] messageBytes = message.toByteArray();
        
        // 使用Varint32编码消息长度
        byte[] lengthBytes = encodeVarint32(messageBytes.length);
        
        // 发送长度前缀和消息
        socket.getOutputStream().write(lengthBytes);
        socket.getOutputStream().write(messageBytes);
        socket.getOutputStream().flush();
        
        logger.debug("Sent protobuf message of {} bytes", messageBytes.length);
    }
    
    private byte[] receiveMessage() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket is not connected");
        }
        
        // 读取Varint32编码的消息长度
        int messageLength = readVarint32();
        
        // 读取指定长度的消息数据
        byte[] messageData = new byte[messageLength];
        int totalRead = 0;
        
        while (totalRead < messageLength) {
            int bytesRead = socket.getInputStream().read(messageData, totalRead, messageLength - totalRead);
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of stream");
            }
            totalRead += bytesRead;
        }
        
        logger.debug("Received protobuf message of {} bytes", messageLength);
        return messageData;
    }
    
    private byte[] encodeVarint32(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        while ((value & 0x80) != 0) {
            buffer.put((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        buffer.put((byte) value);
        
        byte[] result = new byte[buffer.position()];
        buffer.flip();
        buffer.get(result);
        return result;
    }
    
    private int readVarint32() throws IOException {
        int result = 0;
        int shift = 0;
        
        while (shift < 32) {
            int b = socket.getInputStream().read();
            if (b == -1) {
                throw new IOException("Unexpected end of stream while reading varint32");
            }
            
            result |= (b & 0x7F) << shift;
            
            if ((b & 0x80) == 0) {
                return result;
            }
            
            shift += 7;
        }
        
        throw new IOException("Varint32 too long");
    }
    
    private void closeSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.debug("Error closing socket", e);
            }
            socket = null;
        }
    }
    
    private long getNextSequenceNumber() {
        return sequenceNumber.incrementAndGet();
    }
    
    // Public getters
    
    public boolean isConnected() {
        return connected;
    }
    
    public Long getWorldId() {
        return worldId;
    }
    
    public List<Truck> getAvailableTrucks() {
        return new ArrayList<>(availableTrucks);
    }
    
    /**
     * Get AmazonIntegrationService lazily to avoid circular dependency
     */
    private AmazonIntegrationService getAmazonIntegrationService() {
        return applicationContext.getBean(AmazonIntegrationService.class);
    }
    
    /**
     * Check if connection is healthy
     */
    public boolean isConnectionHealthy() {
        return connected && socket != null && !socket.isClosed();
    }
    
    /**
     * Handle connection loss and trigger reconnection
     */
    private void handleConnectionLoss() {
        if (reconnectionInProgress) {
            return;
        }
        
        synchronized (this) {
            if (reconnectionInProgress) {
                return;
            }
            reconnectionInProgress = true;
        }
        
        logger.warn("Connection lost, cleaning up and attempting reconnection");
        
        // Clean up current connection
        cleanupConnection();
        
        if (reconnectionEnabled) {
            // Start reconnection in a separate thread
            executorService.submit(this::attemptReconnection);
        } else {
            logger.warn("Reconnection is disabled, stopping service");
            running = false;
        }
    }
    
    /**
     * Attempt to reconnect with exponential backoff
     */
    private void attemptReconnection() {
        long delay = reconnectionInitialDelay;
        int attempts = 0;
        
        while (running && attempts < reconnectionMaxAttempts) {
            attempts++;
            
            try {
                logger.info("Reconnection attempt {} of {}", attempts, reconnectionMaxAttempts);
                
                // Wait before attempting reconnection
                if (attempts > 1) {
                    Thread.sleep(delay);
                }
                
                // Attempt to reconnect
                if (connect(worldId)) {
                    logger.info("Successfully reconnected to World Simulator");
                    reconnectionInProgress = false;
                    return;
                }
                
                // Exponential backoff for next attempt
                delay = Math.min((long) (delay * reconnectionMultiplier), reconnectionMaxDelay);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error during reconnection attempt {}", attempts, e);
            }
        }
        
        if (attempts >= reconnectionMaxAttempts) {
            logger.error("Failed to reconnect after {} attempts, giving up", reconnectionMaxAttempts);
        }
        
        reconnectionInProgress = false;
        running = false;
    }
    
    /**
     * Clean up connection resources
     */
    private void cleanupConnection() {
        connected = false;
        
        // Stop message processing threads
        stopMessageProcessing();
        
        // Close socket
        closeSocket();
        
        // Clear pending responses with timeout exceptions
        for (Map.Entry<Long, CompletableFuture<Object>> entry : pendingResponses.entrySet()) {
            entry.getValue().completeExceptionally(
                new IOException("Connection lost during operation")
            );
        }
        pendingResponses.clear();
        responseTimeouts.clear();
        
        // Clear message queue
        messageQueue.clear();
    }
    
    /**
     * Check if an exception indicates a connection error
     */
    private boolean isConnectionError(Exception e) {
        return e instanceof IOException ||
               e.getCause() instanceof IOException ||
               e.getMessage().contains("Connection reset") ||
               e.getMessage().contains("Socket closed") ||
               e.getMessage().contains("Broken pipe") ||
               e.getMessage().contains("Connection refused") ||
               e.getMessage().contains("Unexpected end of stream");
    }
    
    /**
     * Test connection health by sending a ping if possible
     */
    public boolean testConnection() {
        if (!isConnectionHealthy()) {
            return false;
        }
        
        try {
            // Use a simple query to test connection
            CompletableFuture<WorldUpsProto.UTruck> future = queryTruckStatus(1);
            // Wait up to 5 seconds for response
            future.get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            logger.debug("Connection test failed", e);
            return false;
        }
    }
}