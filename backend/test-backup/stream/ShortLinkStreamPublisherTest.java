package com.miniups.shortlink.stream;

import com.miniups.shortlink.config.ShortLinkProperties;
import com.miniups.shortlink.model.ShortLinkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortLinkStreamPublisherTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    private ShortLinkProperties properties;
    private ShortLinkStreamPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new ShortLinkProperties();
        ShortLinkProperties.Stream stream = new ShortLinkProperties.Stream();
        stream.setKey("test:stream:shortlink:monitor");
        stream.setGroup("test-shortlink-monitor");
        stream.setConsumerName("test-backend-consumer");
        properties.setStream(stream);

        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);

        publisher = new ShortLinkStreamPublisher(stringRedisTemplate, properties);
    }

    @Test
    void publishAccessEvent_shouldPublishEventWithCorrectPayload() {
        // Arrange
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode("abc123");
        record.setUserId(456L);
        record.setOriginalUrl("https://example.com");

        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0 (Chrome)";

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any(MapRecord.class))).thenReturn(expectedRecordId);

        // Act
        RecordId result = publisher.publishAccessEvent(record, clientIp, userAgent);

        // Assert
        assertThat(result).isEqualTo(expectedRecordId);

        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());

        MapRecord<String, String, String> capturedRecord = captor.getValue();
        assertThat(capturedRecord.getStream()).isEqualTo("test:stream:shortlink:monitor");

        Map<String, String> payload = capturedRecord.getValue();
        assertThat(payload.get("shortCode")).isEqualTo("abc123");
        assertThat(payload.get("userId")).isEqualTo("456");
        assertThat(payload.get("clientIp")).isEqualTo("192.168.1.1");
        assertThat(payload.get("userAgent")).isEqualTo("Mozilla/5.0 (Chrome)");
        assertThat(payload.get("timestamp")).isNotNull();
        assertThat(payload.get("timestamp")).contains(LocalDateTime.now().toLocalDate().toString());
    }

    @Test
    void publishAccessEvent_shouldHandleNullUserId() {
        // Arrange
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode("abc123");
        record.setUserId(null); // Null user ID
        record.setOriginalUrl("https://example.com");

        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0 (Chrome)";

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any(MapRecord.class))).thenReturn(expectedRecordId);

        // Act
        RecordId result = publisher.publishAccessEvent(record, clientIp, userAgent);

        // Assert
        assertThat(result).isEqualTo(expectedRecordId);

        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());

        MapRecord<String, String, String> capturedRecord = captor.getValue();
        Map<String, String> payload = capturedRecord.getValue();
        assertThat(payload.get("userId")).isEmpty();
    }

    @Test
    void publishAccessEvent_shouldHandleNullClientIp() {
        // Arrange
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode("abc123");
        record.setUserId(456L);

        String clientIp = null;
        String userAgent = "Mozilla/5.0 (Chrome)";

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any(MapRecord.class))).thenReturn(expectedRecordId);

        // Act
        RecordId result = publisher.publishAccessEvent(record, clientIp, userAgent);

        // Assert
        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());

        MapRecord<String, String, String> capturedRecord = captor.getValue();
        Map<String, String> payload = capturedRecord.getValue();
        assertThat(payload.get("clientIp")).isNull();
    }

    @Test
    void publishAccessEvent_shouldHandleNullUserAgent() {
        // Arrange
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode("abc123");
        record.setUserId(456L);

        String clientIp = "192.168.1.1";
        String userAgent = null;

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any(MapRecord.class))).thenReturn(expectedRecordId);

        // Act
        RecordId result = publisher.publishAccessEvent(record, clientIp, userAgent);

        // Assert
        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());

        MapRecord<String, String, String> capturedRecord = captor.getValue();
        Map<String, String> payload = capturedRecord.getValue();
        assertThat(payload.get("userAgent")).isNull();
    }

    @Test
    void publishAccessEvent_shouldUseCorrectStreamKey() {
        // Arrange
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode("abc123");
        record.setUserId(456L);

        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0 (Chrome)";

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any(MapRecord.class))).thenReturn(expectedRecordId);

        // Act
        publisher.publishAccessEvent(record, clientIp, userAgent);

        // Assert
        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());

        MapRecord<String, String, String> capturedRecord = captor.getValue();
        assertThat(capturedRecord.getStream()).isEqualTo("test:stream:shortlink:monitor");
    }

    @Test
    void publishAccessEvent_shouldIncludeTimestamp() {
        // Arrange
        ShortLinkRecord record = new ShortLinkRecord();
        record.setShortCode("abc123");
        record.setUserId(456L);

        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0 (Chrome)";

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any(MapRecord.class))).thenReturn(expectedRecordId);

        LocalDateTime beforeCall = LocalDateTime.now();

        // Act
        publisher.publishAccessEvent(record, clientIp, userAgent);

        LocalDateTime afterCall = LocalDateTime.now();

        // Assert
        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());

        MapRecord<String, String, String> capturedRecord = captor.getValue();
        Map<String, String> payload = capturedRecord.getValue();

        String timestampStr = payload.get("timestamp");
        assertThat(timestampStr).isNotNull();

        LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
        assertThat(timestamp).isBetween(beforeCall.minusSeconds(1), afterCall.plusSeconds(1));
    }
}