package com.miniups.shortlink.stream;

import com.miniups.shortlink.config.ShortLinkProperties;
import com.miniups.shortlink.model.ShortLinkAccessLogRecord;
import com.miniups.shortlink.repository.ShortLinkAccessLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortLinkStreamConsumerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private ShortLinkAccessLogRepository accessLogRepository;

    @Mock
    private MapRecord<String, Object, Object> mapRecord1;

    @Mock
    private MapRecord<String, Object, Object> mapRecord2;

    private ShortLinkProperties properties;
    private ShortLinkStreamConsumer consumer;

    @BeforeEach
    void setUp() {
        properties = new ShortLinkProperties();
        ShortLinkProperties.Stream stream = new ShortLinkProperties.Stream();
        stream.setKey("test:stream:shortlink:monitor");
        stream.setGroup("test-shortlink-monitor");
        stream.setConsumerName("test-backend-consumer");
        stream.setBatchSize(100);
        stream.setPollDelayMillis(5000);
        properties.setStream(stream);

        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);

        consumer = new ShortLinkStreamConsumer(stringRedisTemplate, properties, accessLogRepository);
    }

    @Test
    void ensureGroup_shouldCreateGroupSuccessfully() {
        when(streamOperations.createGroup(anyString(), anyString())).thenReturn("OK");

        consumer.ensureGroup();

        verify(streamOperations).createGroup("test:stream:shortlink:monitor", "test-shortlink-monitor");
    }

    @Test
    void ensureGroup_shouldHandleBusyGroupException() {
        RuntimeException busyGroupException = new RuntimeException("BUSYGROUP Consumer Group name already exists");
        when(streamOperations.createGroup(anyString(), anyString())).thenThrow(busyGroupException);

        consumer.ensureGroup();

        verify(streamOperations).createGroup("test:stream:shortlink:monitor", "test-shortlink-monitor");
    }

    @Test
    void ensureGroup_shouldInitializeStreamWhenGroupCreationFails() {
        RuntimeException otherException = new RuntimeException("NOGROUP No such stream");
        when(streamOperations.createGroup(anyString(), anyString())).thenThrow(otherException);

        RecordId bootstrapRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any())).thenReturn(bootstrapRecordId);
        when(streamOperations.createGroup(anyString(), anyString())).thenReturn("OK");
        when(streamOperations.delete(anyString(), any(RecordId.class))).thenReturn(1L);

        consumer.ensureGroup();

        verify(streamOperations).add(any());
        verify(streamOperations).delete("test:stream:shortlink:monitor", bootstrapRecordId);
    }

    @Test
    void consume_shouldProcessRecordsSuccessfully() {
        // Arrange
        Map<Object, Object> record1Data = new HashMap<>();
        record1Data.put("shortCode", "abc123");
        record1Data.put("userId", "456");
        record1Data.put("clientIp", "192.168.1.1");
        record1Data.put("userAgent", "Mozilla/5.0");
        record1Data.put("timestamp", LocalDateTime.now().toString());

        Map<Object, Object> record2Data = new HashMap<>();
        record2Data.put("shortCode", "def456");
        record2Data.put("userId", "");
        record2Data.put("clientIp", "192.168.1.2");
        record2Data.put("userAgent", "Chrome");
        record2Data.put("timestamp", LocalDateTime.now().toString());

        RecordId recordId1 = RecordId.of("1234567890-0");
        RecordId recordId2 = RecordId.of("1234567890-1");

        when(mapRecord1.getValue()).thenReturn(record1Data);
        when(mapRecord1.getId()).thenReturn(recordId1);
        when(mapRecord2.getValue()).thenReturn(record2Data);
        when(mapRecord2.getId()).thenReturn(recordId2);

        List<MapRecord<String, Object, Object>> records = Arrays.asList(mapRecord1, mapRecord2);
        when(streamOperations.read(any(Consumer.class), any(), any())).thenReturn(records);
        when(accessLogRepository.insert(any(ShortLinkAccessLogRecord.class))).thenReturn(1);
        when(streamOperations.acknowledge(anyString(), anyString(), any(RecordId.class))).thenReturn(1L);

        // Act
        consumer.consume();

        // Assert
        ArgumentCaptor<ShortLinkAccessLogRecord> logRecordCaptor = ArgumentCaptor.forClass(ShortLinkAccessLogRecord.class);
        verify(accessLogRepository).insert(logRecordCaptor.capture());

        List<ShortLinkAccessLogRecord> capturedRecords = logRecordCaptor.getAllValues();
        assertThat(capturedRecords).hasSize(2);

        ShortLinkAccessLogRecord firstRecord = capturedRecords.get(0);
        assertThat(firstRecord.getShortCode()).isEqualTo("abc123");
        assertThat(firstRecord.getOwnerUserId()).isEqualTo(456L);
        assertThat(firstRecord.getClientIp()).isEqualTo("192.168.1.1");
        assertThat(firstRecord.getUserAgent()).isEqualTo("Mozilla/5.0");

        ShortLinkAccessLogRecord secondRecord = capturedRecords.get(1);
        assertThat(secondRecord.getShortCode()).isEqualTo("def456");
        assertThat(secondRecord.getOwnerUserId()).isNull();
        assertThat(secondRecord.getClientIp()).isEqualTo("192.168.1.2");
        assertThat(secondRecord.getUserAgent()).isEqualTo("Chrome");

        verify(streamOperations).acknowledge("test:stream:shortlink:monitor", "test-shortlink-monitor", recordId1);
        verify(streamOperations).acknowledge("test:stream:shortlink:monitor", "test-shortlink-monitor", recordId2);
    }

    @Test
    void consume_shouldHandleEmptyRecords() {
        when(streamOperations.read(any(Consumer.class), any(), any())).thenReturn(Collections.emptyList());

        consumer.consume();

        verify(accessLogRepository, never()).insert(any());
        verify(streamOperations, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    void consume_shouldHandleNullRecords() {
        when(streamOperations.read(any(Consumer.class), any(), any())).thenReturn(null);

        consumer.consume();

        verify(accessLogRepository, never()).insert(any());
        verify(streamOperations, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    void consume_shouldContinueProcessingAfterFailure() {
        // Arrange
        Map<Object, Object> record1Data = new HashMap<>();
        record1Data.put("shortCode", "abc123");
        record1Data.put("userId", "456");
        record1Data.put("clientIp", "192.168.1.1");
        record1Data.put("userAgent", "Mozilla/5.0");
        record1Data.put("timestamp", LocalDateTime.now().toString());

        Map<Object, Object> record2Data = new HashMap<>();
        record2Data.put("shortCode", "def456");
        record2Data.put("userId", "789");
        record2Data.put("clientIp", "192.168.1.2");
        record2Data.put("userAgent", "Chrome");
        record2Data.put("timestamp", LocalDateTime.now().toString());

        RecordId recordId1 = RecordId.of("1234567890-0");
        RecordId recordId2 = RecordId.of("1234567890-1");

        when(mapRecord1.getValue()).thenReturn(record1Data);
        when(mapRecord1.getId()).thenReturn(recordId1);
        when(mapRecord2.getValue()).thenReturn(record2Data);
        when(mapRecord2.getId()).thenReturn(recordId2);

        List<MapRecord<String, Object, Object>> records = Arrays.asList(mapRecord1, mapRecord2);
        when(streamOperations.read(any(Consumer.class), any(), any())).thenReturn(records);

        // First record fails to insert
        doThrow(new RuntimeException("Database error")).when(accessLogRepository).insert(any(ShortLinkAccessLogRecord.class));

        // Act
        consumer.consume();

        // Assert
        verify(accessLogRepository).insert(any(ShortLinkAccessLogRecord.class));
        // Should not acknowledge failed records
        verify(streamOperations, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    void mapRecord_shouldMapRecordCorrectly() {
        Map<Object, Object> recordData = new HashMap<>();
        recordData.put("shortCode", "abc123");
        recordData.put("userId", "456");
        recordData.put("clientIp", "192.168.1.1");
        recordData.put("userAgent", "Mozilla/5.0");
        recordData.put("timestamp", "2023-12-01T10:30:00");

        when(mapRecord1.getValue()).thenReturn(recordData);
        when(mapRecord1.getId()).thenReturn(RecordId.of("1234567890-0"));
        when(streamOperations.read(any(Consumer.class), any(), any())).thenReturn(Arrays.asList(mapRecord1));
        when(accessLogRepository.insert(any(ShortLinkAccessLogRecord.class))).thenReturn(1);
        when(streamOperations.acknowledge(anyString(), anyString(), any(RecordId.class))).thenReturn(1L);

        consumer.consume();

        ArgumentCaptor<ShortLinkAccessLogRecord> captor = ArgumentCaptor.forClass(ShortLinkAccessLogRecord.class);
        verify(accessLogRepository).insert(captor.capture());

        ShortLinkAccessLogRecord record = captor.getValue();
        assertThat(record.getShortCode()).isEqualTo("abc123");
        assertThat(record.getOwnerUserId()).isEqualTo(456L);
        assertThat(record.getClientIp()).isEqualTo("192.168.1.1");
        assertThat(record.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(record.getAccessedAt()).isEqualTo(LocalDateTime.parse("2023-12-01T10:30:00"));
    }

    @Test
    void mapRecord_shouldHandleEmptyUserId() {
        Map<Object, Object> recordData = new HashMap<>();
        recordData.put("shortCode", "abc123");
        recordData.put("userId", "");
        recordData.put("clientIp", "192.168.1.1");
        recordData.put("userAgent", "Mozilla/5.0");
        recordData.put("timestamp", "2023-12-01T10:30:00");

        when(mapRecord1.getValue()).thenReturn(recordData);
        when(mapRecord1.getId()).thenReturn(RecordId.of("1234567890-0"));
        when(streamOperations.read(any(Consumer.class), any(), any())).thenReturn(Arrays.asList(mapRecord1));
        when(accessLogRepository.insert(any(ShortLinkAccessLogRecord.class))).thenReturn(1);
        when(streamOperations.acknowledge(anyString(), anyString(), any(RecordId.class))).thenReturn(1L);

        consumer.consume();

        ArgumentCaptor<ShortLinkAccessLogRecord> captor = ArgumentCaptor.forClass(ShortLinkAccessLogRecord.class);
        verify(accessLogRepository).insert(captor.capture());

        ShortLinkAccessLogRecord record = captor.getValue();
        assertThat(record.getOwnerUserId()).isNull();
    }

    @Test
    void mapRecord_shouldHandleEmptyTimestamp() {
        Map<Object, Object> recordData = new HashMap<>();
        recordData.put("shortCode", "abc123");
        recordData.put("userId", "456");
        recordData.put("clientIp", "192.168.1.1");
        recordData.put("userAgent", "Mozilla/5.0");
        recordData.put("timestamp", "");

        when(mapRecord1.getValue()).thenReturn(recordData);
        when(mapRecord1.getId()).thenReturn(RecordId.of("1234567890-0"));
        when(streamOperations.read(any(Consumer.class), any(), any())).thenReturn(Arrays.asList(mapRecord1));
        when(accessLogRepository.insert(any(ShortLinkAccessLogRecord.class))).thenReturn(1);
        when(streamOperations.acknowledge(anyString(), anyString(), any(RecordId.class))).thenReturn(1L);

        LocalDateTime beforeCall = LocalDateTime.now();
        consumer.consume();
        LocalDateTime afterCall = LocalDateTime.now();

        ArgumentCaptor<ShortLinkAccessLogRecord> captor = ArgumentCaptor.forClass(ShortLinkAccessLogRecord.class);
        verify(accessLogRepository).insert(captor.capture());

        ShortLinkAccessLogRecord record = captor.getValue();
        assertThat(record.getAccessedAt()).isBetween(beforeCall.minusSeconds(1), afterCall.plusSeconds(1));
    }

    @Test
    void mapRecord_shouldHandleNullValues() {
        Map<Object, Object> recordData = new HashMap<>();
        recordData.put("shortCode", null);
        recordData.put("userId", null);
        recordData.put("clientIp", null);
        recordData.put("userAgent", null);
        recordData.put("timestamp", null);

        when(mapRecord1.getValue()).thenReturn(recordData);
        when(mapRecord1.getId()).thenReturn(RecordId.of("1234567890-0"));
        when(streamOperations.read(any(Consumer.class), any(), any())).thenReturn(Arrays.asList(mapRecord1));
        when(accessLogRepository.insert(any(ShortLinkAccessLogRecord.class))).thenReturn(1);
        when(streamOperations.acknowledge(anyString(), anyString(), any(RecordId.class))).thenReturn(1L);

        LocalDateTime beforeCall = LocalDateTime.now();
        consumer.consume();
        LocalDateTime afterCall = LocalDateTime.now();

        ArgumentCaptor<ShortLinkAccessLogRecord> captor = ArgumentCaptor.forClass(ShortLinkAccessLogRecord.class);
        verify(accessLogRepository).insert(captor.capture());

        ShortLinkAccessLogRecord record = captor.getValue();
        assertThat(record.getShortCode()).isEmpty();
        assertThat(record.getOwnerUserId()).isNull();
        assertThat(record.getClientIp()).isEmpty();
        assertThat(record.getUserAgent()).isEmpty();
        assertThat(record.getAccessedAt()).isBetween(beforeCall.minusSeconds(1), afterCall.plusSeconds(1));
    }
}