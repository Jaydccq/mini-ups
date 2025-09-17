package com.miniups.network.netty;

import com.miniups.network.netty.client.NettyClient;
import com.miniups.network.netty.config.NettyProperties;
import com.miniups.network.netty.handler.MessageHandlerService;
import com.miniups.proto.WorldUpsProto.UCommands;
import com.miniups.proto.WorldUpsProto.UResponses;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for NettyClient components.
 * 
 * These tests validate the Netty pipeline configuration and message
 * encoding/decoding functionality without requiring actual network
 * connections.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Netty Client Tests")
class NettyClientTest {

    @Mock
    private EventLoopGroup mockWorkerGroup;

    @Mock
    private MessageHandlerService mockMessageHandlerService;

    private NettyProperties nettyProperties;

    @BeforeEach
    void setUp() {
        // Initialize test properties
        nettyProperties = new NettyProperties();
        nettyProperties.setWorkerThreads(2);
        nettyProperties.setConnectionTimeoutMs(5000);
        nettyProperties.setKeepAlive(true);
        nettyProperties.setTcpNoDelay(true);
    }

    @Test
    @DisplayName("Should correctly encode and decode protobuf messages through pipeline")
    void testProtobufPipelineEncodingDecoding() {
        // Create an embedded channel to test the pipeline without network I/O
        EmbeddedChannel channel = new EmbeddedChannel(
            // Outbound handlers (encoding)
            new ProtobufVarint32LengthFieldPrepender(),
            new ProtobufEncoder(),
            
            // Inbound handlers (decoding)
            new ProtobufVarint32FrameDecoder(),
            new ProtobufDecoder(UResponses.getDefaultInstance())
        );

        // Create a test command message
        UCommands testCommand = UCommands.newBuilder()
            .setSimspeed(100)
            .setDisconnect(false)
            .build();

        // Test outbound encoding (UCommands -> bytes)
        assertThat(channel.writeOutbound(testCommand)).isTrue();
        
        // Read the encoded bytes
        Object encodedMessage = channel.readOutbound();
        assertThat(encodedMessage).isNotNull();

        // Test inbound decoding by creating a test response
        UResponses testResponse = UResponses.newBuilder()
            .setFinished(true)
            .build();

        // Encode the response manually to simulate incoming data
        byte[] responseBytes = testResponse.toByteArray();
        
        // Create a length-prefixed message (Varint32 + data)
        byte[] lengthPrefix = encodeVarint32(responseBytes.length);
        byte[] completeMessage = new byte[lengthPrefix.length + responseBytes.length];
        System.arraycopy(lengthPrefix, 0, completeMessage, 0, lengthPrefix.length);
        System.arraycopy(responseBytes, 0, completeMessage, lengthPrefix.length, responseBytes.length);

        // Write the complete message to the inbound side
        assertThat(channel.writeInbound(io.netty.buffer.Unpooled.wrappedBuffer(completeMessage))).isTrue();

        // Read the decoded message
        UResponses decodedResponse = channel.readInbound();
        assertThat(decodedResponse).isNotNull();
        assertThat(decodedResponse.getFinished()).isTrue();

        // Clean up
        channel.finish();
    }

    @Test
    @DisplayName("Should handle NettyProperties configuration correctly")
    void testNettyPropertiesConfiguration() {
        // Test default values
        NettyProperties defaultProps = new NettyProperties();
        assertThat(defaultProps.getWorkerThreads()).isEqualTo(2);
        assertThat(defaultProps.getConnectionTimeoutMs()).isEqualTo(10000);
        assertThat(defaultProps.isKeepAlive()).isTrue();
        assertThat(defaultProps.isTcpNoDelay()).isTrue();

        // Test reconnection settings
        assertThat(defaultProps.getReconnection().isEnabled()).isTrue();
        assertThat(defaultProps.getReconnection().getMaxAttempts()).isEqualTo(10);
        assertThat(defaultProps.getReconnection().getInitialDelayMs()).isEqualTo(1000L);
        assertThat(defaultProps.getReconnection().getMaxDelayMs()).isEqualTo(30000L);
        assertThat(defaultProps.getReconnection().getBackoffMultiplier()).isEqualTo(2.0);

        // Test message settings
        assertThat(defaultProps.getMessage().getResponseTimeoutMs()).isEqualTo(30000L);
        assertThat(defaultProps.getMessage().getMaxPendingResponses()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should validate NettyClient state management")
    void testNettyClientStateManagement() {
        // Create a NettyClient with mocked dependencies
        NettyClient nettyClient = new NettyClient(mockWorkerGroup, nettyProperties, mockMessageHandlerService);
        
        // Initialize the client
        nettyClient.initialize();

        // Test initial state
        assertThat(nettyClient.isConnected()).isFalse();
        assertThat(nettyClient.isShutdown()).isFalse();
        assertThat(nettyClient.getPendingResponseCount()).isEqualTo(0);

        // Test getter methods
        assertThat(nettyClient.getCurrentHost()).isNull();
        assertThat(nettyClient.getCurrentPort()).isEqualTo(0);
        assertThat(nettyClient.getWorldId()).isNull();
    }

    /**
     * Helper method to encode a 32-bit integer as a Varint32.
     * This mimics the Protobuf Varint32 encoding used by Netty.
     */
    private byte[] encodeVarint32(int value) {
        if (value < 0x80) {
            return new byte[]{(byte) value};
        } else if (value < 0x4000) {
            return new byte[]{
                (byte) ((value & 0x7F) | 0x80),
                (byte) (value >>> 7)
            };
        } else if (value < 0x200000) {
            return new byte[]{
                (byte) ((value & 0x7F) | 0x80),
                (byte) ((value >>> 7) | 0x80),
                (byte) (value >>> 14)
            };
        } else if (value < 0x10000000) {
            return new byte[]{
                (byte) ((value & 0x7F) | 0x80),
                (byte) ((value >>> 7) | 0x80),
                (byte) ((value >>> 14) | 0x80),
                (byte) (value >>> 21)
            };
        } else {
            return new byte[]{
                (byte) ((value & 0x7F) | 0x80),
                (byte) ((value >>> 7) | 0x80),
                (byte) ((value >>> 14) | 0x80),
                (byte) ((value >>> 21) | 0x80),
                (byte) (value >>> 28)
            };
        }
    }
}