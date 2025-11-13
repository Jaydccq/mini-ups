package com.miniups.service.messaging.rabbit;

import com.miniups.config.RabbitMQConfig;
import com.miniups.model.entity.OutboxEvent;
import com.miniups.service.messaging.OutboxMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Optional;

/**
 * RabbitMQ implementation of the {@link OutboxMessagePublisher} contract.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "messaging.rabbit", name = "outbox-enabled", havingValue = "true", matchIfMissing = true)
public class RabbitOutboxMessagePublisher implements OutboxMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitOutboxMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${messaging.rabbit.default-exchange:" + RabbitMQConfig.TOPIC_EXCHANGE_NAME + "}")
    private String defaultExchange;

    @Override
    public String channel() {
        return "rabbitmq";
    }

    @Override
    public boolean publish(OutboxEvent event) {
        String routingKey = event.getRoutingKey();
        if (!StringUtils.hasText(routingKey)) {
            log.warn("Skipping RabbitMQ publish for event {} due to missing routing key", event.getEventId());
            return false;
        }

        try {
            String exchange = resolveExchange(routingKey);

            rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                event.getPayload(),
                message -> {
                    Optional.ofNullable(event.getCorrelationId())
                        .ifPresent(correlationId -> message.getMessageProperties().setCorrelationId(correlationId));
                    message.getMessageProperties().setMessageId(event.getEventId());
                    message.getMessageProperties().setTimestamp(Date.from(event.getCreatedAt()));
                    return message;
                },
                new CorrelationData(event.getEventId())
            );

            if (log.isDebugEnabled()) {
                log.debug("Published event {} to RabbitMQ exchange {} with routing key {}", event.getEventId(), exchange, routingKey);
            }
            return true;
        } catch (Exception ex) {
            log.error("RabbitMQ publish failed for event {}", event.getEventId(), ex);
            return false;
        }
    }

    private String resolveExchange(String routingKey) {
        // For now reuse the default topic exchange; hook for per-routing key overrides in the future.
        return defaultExchange;
    }
}
