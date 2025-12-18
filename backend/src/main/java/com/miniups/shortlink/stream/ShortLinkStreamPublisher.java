package com.miniups.shortlink.stream;

import com.miniups.shortlink.config.ShortLinkProperties;
import com.miniups.shortlink.model.ShortLinkRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "shortlink.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ShortLinkStreamPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ShortLinkProperties.Stream streamProps;

    public ShortLinkStreamPublisher(StringRedisTemplate stringRedisTemplate, ShortLinkProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.streamProps = properties.getStream();
    }

    public RecordId publishAccessEvent(ShortLinkRecord record, String clientIp, String userAgent) {
        Map<String, String> payload = new HashMap<>();
        payload.put("shortCode", record.getShortCode());
        payload.put("userId", record.getUserId() == null ? "" : record.getUserId().toString());
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("clientIp", clientIp);
        payload.put("userAgent", userAgent);
        MapRecord<String, String, String> mapRecord = StreamRecords.newRecord()
                .ofStrings(payload)
                .withStreamKey(streamProps.getKey());
        return stringRedisTemplate.opsForStream().add(mapRecord);
    }
}
