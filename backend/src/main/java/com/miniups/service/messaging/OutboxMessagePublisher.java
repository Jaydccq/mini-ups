package com.miniups.service.messaging;

import com.miniups.model.entity.OutboxEvent;

/**
 * Contract for publishing transactional outbox events to an external messaging system.
 *
 * Implementations are responsible for translating the generic {@link OutboxEvent}
 * payload into the protocol and delivery semantics of the target broker (e.g. RabbitMQ, Kafka).
 */
public interface OutboxMessagePublisher {

    /**
     * @return human readable channel name used for logging (e.g. "rabbitmq", "kafka").
     */
    String channel();

    /**
     * Publish the given event to the underlying messaging system.
     *
     * @param event the event to publish
     * @return {@code true} if the publish operation completed successfully
     * @throws Exception if the underlying client throws synchronously
     */
    boolean publish(OutboxEvent event) throws Exception;
}
