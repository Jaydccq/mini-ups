package com.miniups.shortlink.stream;

import com.miniups.shortlink.config.ShortLinkProperties;
import com.miniups.shortlink.model.ShortLinkAccessLogRecord;
import com.miniups.shortlink.repository.ShortLinkAccessLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "shortlink.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ShortLinkStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkStreamConsumer.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ShortLinkProperties properties;
    private final ShortLinkAccessLogRepository accessLogRepository;

    public ShortLinkStreamConsumer(StringRedisTemplate stringRedisTemplate,
                                   ShortLinkProperties properties,
                                   ShortLinkAccessLogRepository accessLogRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        this.accessLogRepository = accessLogRepository;
    }

    @PostConstruct
    public void ensureGroup() {
        ShortLinkProperties.Stream stream = properties.getStream();
        try {
            stringRedisTemplate.opsForStream().createGroup(stream.getKey(), stream.getGroup());
            log.info("Created Redis stream group {} for key {}", stream.getGroup(), stream.getKey());
        } catch (Exception ex) {
            // group already exists is fine
            if (ex.getMessage() != null && ex.getMessage().contains("BUSYGROUP")) {
                log.debug("Stream group {} already exists", stream.getGroup());
            } else {
                try {
                    var recordId = stringRedisTemplate.opsForStream().add(
                            StreamRecords.newRecord().ofStrings(java.util.Map.of("bootstrap", "1")).withStreamKey(stream.getKey()));
                    stringRedisTemplate.opsForStream().createGroup(stream.getKey(), stream.getGroup());
                    if (recordId != null) {
                        stringRedisTemplate.opsForStream().delete(stream.getKey(), recordId);
                    }
                    log.info("Initialized stream {} and created group {}", stream.getKey(), stream.getGroup());
                } catch (Exception createEx) {
                    log.warn("Failed to initialize stream group {}: {}", stream.getGroup(), createEx.getMessage());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Scheduled(fixedDelayString = "${shortlink.stream.poll-delay-millis:5000}")
    public void consume() {
        ShortLinkProperties.Stream stream = properties.getStream();
        StreamReadOptions options = StreamReadOptions.empty()
                .count(stream.getBatchSize())
                .block(Duration.ofMillis(Math.max(stream.getPollDelayMillis(), 100)));

        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(stream.getGroup(), stream.getConsumerName()),
                options,
                StreamOffset.create(stream.getKey(), ReadOffset.lastConsumed()));

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                ShortLinkAccessLogRecord logRecord = mapRecord(record.getValue());
                accessLogRepository.insert(logRecord);
                stringRedisTemplate.opsForStream().acknowledge(stream.getKey(), stream.getGroup(), record.getId());
            } catch (Exception ex) {
                log.error("Failed to process short link stream event {}: {}", record.getId(), ex.getMessage(), ex);
            }
        }
    }

    private ShortLinkAccessLogRecord mapRecord(Map<Object, Object> value) {
        ShortLinkAccessLogRecord record = new ShortLinkAccessLogRecord();
        record.setShortCode(stringValue(value.get("shortCode")));
        String userId = stringValue(value.get("userId"));
        record.setOwnerUserId(userId.isBlank() ? null : Long.parseLong(userId));
        record.setClientIp(stringValue(value.get("clientIp")));
        record.setUserAgent(stringValue(value.get("userAgent")));
        String timestamp = stringValue(value.get("timestamp"));
        record.setAccessedAt(timestamp.isBlank() ? LocalDateTime.now() : LocalDateTime.parse(timestamp));
        return record;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
