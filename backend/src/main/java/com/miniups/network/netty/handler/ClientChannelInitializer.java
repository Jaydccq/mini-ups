package com.miniups.network.netty.handler;

import com.miniups.network.netty.client.NettyClient;
import com.miniups.proto.WorldUpsProto.UResponses;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Netty channel initializer for World Simulator TCP client.
 * 
 * This class configures the Netty ChannelPipeline with the necessary
 * handlers for Protocol Buffer communication with the World Simulator.
 * The pipeline includes:
 * 
 * Inbound (receiving data):
 * 1. ProtobufVarint32FrameDecoder - Handles Varint32 length prefixes
 * 2. ProtobufDecoder - Deserializes protobuf messages to UResponses
 * 3. IdleStateHandler - Detects idle connections for keep-alive
 * 4. ReconnectionHandler - Handles automatic reconnection
 * 5. ClientHandler - Business logic for processing responses
 * 
 * Outbound (sending data):
 * 1. ProtobufVarint32LengthFieldPrepender - Adds Varint32 length prefixes
 * 2. ProtobufEncoder - Serializes UCommands to protobuf format
 * 
 * @author Mini-UPS System
 * @version 1.0
 */
@Slf4j
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final MessageHandlerService messageHandlerService;
    private final Map<Long, CompletableFuture<Object>> pendingResponses;
    private final NettyClient nettyClient;

    public ClientChannelInitializer(MessageHandlerService messageHandlerService,
                                  Map<Long, CompletableFuture<Object>> pendingResponses,
                                  NettyClient nettyClient) {
        this.messageHandlerService = messageHandlerService;
        this.pendingResponses = pendingResponses;
        this.nettyClient = nettyClient;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        log.debug("Initializing channel pipeline for connection: {}", ch.remoteAddress());

        // Inbound handlers (for receiving data from World Simulator)
        
        // Frame decoder - handles Varint32 length-prefixed frames
        pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
        
        // Protobuf decoder - converts binary data to UResponses objects
        pipeline.addLast("protobufDecoder", 
            new ProtobufDecoder(UResponses.getDefaultInstance()));

        // Idle state handler for connection keep-alive detection
        // Triggers idle state events if no data is received within specified time
        pipeline.addLast("idleStateHandler", 
            new IdleStateHandler(60, 0, 0)); // 60 seconds read timeout

        // Outbound handlers (for sending data to World Simulator)
        
        // Frame encoder - adds Varint32 length prefixes to outgoing messages
        pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
        
        // Protobuf encoder - converts UCommands objects to binary data
        pipeline.addLast("protobufEncoder", new ProtobufEncoder());

        // Business logic handlers
        
        // Reconnection handler - handles connection loss and automatic reconnection
        pipeline.addLast("reconnectionHandler", 
            new ReconnectionHandler(nettyClient));
            
        // Main client handler - processes business logic for incoming responses
        pipeline.addLast("clientHandler", 
            new ClientHandler(messageHandlerService, pendingResponses, nettyClient));

        log.debug("Channel pipeline initialized successfully with {} handlers", 
                 pipeline.names().size());
    }
}