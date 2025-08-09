package com.miniups.network.netty.handler;

import com.miniups.network.netty.client.NettyClient;
import com.miniups.proto.WorldUpsProto.UResponses;
import com.miniups.proto.WorldUpsProto.UFinished;
import com.miniups.proto.WorldUpsProto.UDeliveryMade;
import com.miniups.proto.WorldUpsProto.UTruck;
import com.miniups.proto.WorldUpsProto.UErr;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Netty channel handler for processing incoming messages from World Simulator.
 * 
 * This handler is responsible for:
 * - Processing UResponses protobuf messages
 * - Correlating responses with pending requests using sequence numbers
 * - Delegating business logic to MessageHandlerService
 * - Handling connection events and errors
 * - Managing idle state for keep-alive functionality
 * 
 * The handler maintains separation between network I/O and business logic
 * by delegating all transaction-related processing to the MessageHandlerService,
 * which operates within Spring's transaction management context.
 * 
 * @author Mini-UPS System
 * @version 1.0
 */
@Slf4j
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private final MessageHandlerService messageHandlerService;
    private final Map<Long, CompletableFuture<Object>> pendingResponses;
    private final NettyClient nettyClient;

    public ClientHandler(MessageHandlerService messageHandlerService,
                        Map<Long, CompletableFuture<Object>> pendingResponses,
                        NettyClient nettyClient) {
        this.messageHandlerService = messageHandlerService;
        this.pendingResponses = pendingResponses;
        this.nettyClient = nettyClient;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel active: connected to World Simulator at {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("Channel inactive: disconnected from World Simulator at {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof UResponses)) {
            log.warn("Received unexpected message type: {}", msg.getClass().getSimpleName());
            return;
        }

        UResponses responses = (UResponses) msg;
        log.debug("Received UResponses with {} completions, {} deliveries, {} truck statuses, {} errors", 
                 responses.getCompletionsCount(),
                 responses.getDeliveredCount(),
                 responses.getTruckstatusCount(),
                 responses.getErrorCount());

        // Process different types of responses
        processCompletions(responses);
        processDeliveries(responses);
        processTruckStatuses(responses);
        processErrors(responses);
        processAcknowledgments(responses);
    }

    /**
     * Process truck completion messages (UFinished).
     * These indicate that a truck has completed a pickup or delivery task.
     */
    private void processCompletions(UResponses responses) {
        for (UFinished completion : responses.getCompletionsList()) {
            log.debug("Processing completion for truck {} at ({}, {}) with status '{}', seqnum: {}", 
                     completion.getTruckid(), 
                     completion.getX(), 
                     completion.getY(), 
                     completion.getStatus(),
                     completion.getSeqnum());

            try {
                // Delegate business logic to service layer (within transaction context)
                messageHandlerService.handleTruckCompletion(completion);
                
                // Complete any pending future waiting for this response
                completePendingResponse(completion.getSeqnum(), completion);
                
            } catch (Exception e) {
                log.error("Error processing truck completion for truck {}, seqnum {}: {}", 
                         completion.getTruckid(), completion.getSeqnum(), e.getMessage(), e);
                
                // Complete the future with exception
                completeWithException(completion.getSeqnum(), e);
            }
        }
    }

    /**
     * Process delivery completion messages (UDeliveryMade).
     * These indicate that a package has been successfully delivered.
     */
    private void processDeliveries(UResponses responses) {
        for (UDeliveryMade delivery : responses.getDeliveredList()) {
            log.debug("Processing delivery made for truck {}, package {}, seqnum: {}", 
                     delivery.getTruckid(), 
                     delivery.getPackageid(), 
                     delivery.getSeqnum());

            try {
                // Delegate business logic to service layer
                messageHandlerService.handleDeliveryMade(delivery);
                
                // Complete any pending future
                completePendingResponse(delivery.getSeqnum(), delivery);
                
            } catch (Exception e) {
                log.error("Error processing delivery made for truck {}, package {}, seqnum {}: {}", 
                         delivery.getTruckid(), delivery.getPackageid(), delivery.getSeqnum(), e.getMessage(), e);
                
                completeWithException(delivery.getSeqnum(), e);
            }
        }
    }

    /**
     * Process truck status updates (UTruck).
     * These provide current position and status information for trucks.
     */
    private void processTruckStatuses(UResponses responses) {
        for (UTruck truckStatus : responses.getTruckstatusList()) {
            log.debug("Processing truck status for truck {} at ({}, {}) with status '{}', seqnum: {}", 
                     truckStatus.getTruckid(), 
                     truckStatus.getX(), 
                     truckStatus.getY(), 
                     truckStatus.getStatus(),
                     truckStatus.getSeqnum());

            try {
                // Delegate business logic to service layer
                messageHandlerService.handleTruckStatus(truckStatus);
                
                // Complete any pending future
                completePendingResponse(truckStatus.getSeqnum(), truckStatus);
                
            } catch (Exception e) {
                log.error("Error processing truck status for truck {}, seqnum {}: {}", 
                         truckStatus.getTruckid(), truckStatus.getSeqnum(), e.getMessage(), e);
                
                completeWithException(truckStatus.getSeqnum(), e);
            }
        }
    }

    /**
     * Process error messages (UErr).
     * These indicate errors in processing commands sent to the World Simulator.
     */
    private void processErrors(UResponses responses) {
        for (UErr error : responses.getErrorList()) {
            log.error("Received error from World Simulator: '{}' for original seqnum {}, error seqnum: {}", 
                     error.getErr(), 
                     error.getOriginseqnum(), 
                     error.getSeqnum());

            try {
                // Delegate error handling to service layer
                messageHandlerService.handleError(error);
                
                // Complete the original request future with an exception
                RuntimeException exception = new RuntimeException(
                    "World Simulator error: " + error.getErr() + " (seqnum: " + error.getOriginseqnum() + ")");
                completeWithException(error.getOriginseqnum(), exception);
                
                // Also complete the error message future if someone is waiting for it
                completePendingResponse(error.getSeqnum(), error);
                
            } catch (Exception e) {
                log.error("Error processing error message for seqnum {}: {}", 
                         error.getSeqnum(), e.getMessage(), e);
            }
        }
    }

    /**
     * Process acknowledgment messages.
     * These confirm that certain commands have been received and processed.
     */
    private void processAcknowledgments(UResponses responses) {
        for (Long ackSeqnum : responses.getAcksList()) {
            log.debug("Received acknowledgment for seqnum: {}", ackSeqnum);
            
            // Complete pending response with acknowledgment
            completePendingResponse(ackSeqnum, "ACK");
        }
    }

    /**
     * Complete a pending response future if one exists for the given sequence number.
     */
    private void completePendingResponse(long seqnum, Object response) {
        CompletableFuture<Object> future = pendingResponses.remove(seqnum);
        if (future != null && !future.isDone()) {
            future.complete(response);
            log.debug("Completed pending response for seqnum: {}", seqnum);
        }
    }

    /**
     * Complete a pending response future with an exception.
     */
    private void completeWithException(long seqnum, Exception exception) {
        CompletableFuture<Object> future = pendingResponses.remove(seqnum);
        if (future != null && !future.isDone()) {
            future.completeExceptionally(exception);
            log.debug("Completed pending response with exception for seqnum: {}", seqnum);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            
            if (idleEvent.state() == IdleState.READER_IDLE) {
                log.warn("Read idle timeout detected - no data received from World Simulator for 60 seconds");
                // Could implement keep-alive logic here if needed
            }
        }
        
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in channel handler for connection to {}: {}", 
                 ctx.channel().remoteAddress(), cause.getMessage(), cause);
        
        // Close the channel on serious errors - this will trigger reconnection
        ctx.close();
    }
}