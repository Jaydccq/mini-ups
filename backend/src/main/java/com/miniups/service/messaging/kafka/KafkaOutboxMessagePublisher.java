package com.miniups.service.messaging.kafka;

import com.miniups.config.KafkaMessagingProperties;
import com.miniups.model.entity.OutboxEvent;
import com.miniups.service.messaging.OutboxMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka implementation of the {@link OutboxMessagePublisher} contract.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "messaging.kafka", name = "outbox-enabled", havingValue = "true")
public class KafkaOutboxMessagePublisher implements OutboxMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxMessagePublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaMessagingProperties properties;

    public KafkaOutboxMessagePublisher(KafkaTemplate<String, String> kafkaTemplate,
                                      KafkaMessagingProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public String channel() {
        return "kafka";
    }

    @Override
    public boolean publish(OutboxEvent event) {
        String topic = resolveTopic(event.getRoutingKey());
        if (!StringUtils.hasText(topic)) {
            log.warn("Skipping Kafka publish for event {} due to missing topic", event.getEventId());
            return false;
        }

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getAggregateId(), event.getPayload());
            record.headers().add(new RecordHeader("event-id", event.getEventId().getBytes(StandardCharsets.UTF_8)));
            Optional.ofNullable(event.getCorrelationId())
                .filter(StringUtils::hasText)
                .ifPresent(correlationId -> record.headers().add(new RecordHeader("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8))));
            record.headers().add(new RecordHeader("aggregate-type", event.getAggregateType().getBytes(StandardCharsets.UTF_8)));

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
            SendResult<String, String> result = future.get(properties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);

            if (log.isDebugEnabled()) {
                log.debug("Published event {} to Kafka topic {} with offset {}", event.getEventId(), topic,
                        result.getRecordMetadata().offset());
            }
            return true;

        } catch (Exception ex) {
            log.error("Kafka publish failed for event {}", event.getEventId(), ex);
            return false;
        }
    }

    private String resolveTopic(String routingKey) {
        if (StringUtils.hasText(routingKey)) {
            String prefix = properties.getTopicPrefix();
            return StringUtils.hasText(prefix)
                    ? prefix + routingKey
                    : routingKey;
        }
        return properties.getDefaultTopic();
    }
}
