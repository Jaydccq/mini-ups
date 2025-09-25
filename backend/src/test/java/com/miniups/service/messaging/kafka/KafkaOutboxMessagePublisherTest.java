package com.miniups.service.messaging.kafka;

import com.miniups.config.KafkaMessagingProperties;
import com.miniups.model.entity.OutboxEvent;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaOutboxMessagePublisherTest {

    private KafkaMessagingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new KafkaMessagingProperties();
        properties.setSendTimeoutMs(1500);
    }

    @Test
    void publishUsesRoutingKeyWithPrefix() {
        properties.setTopicPrefix("dev.");

        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        KafkaOutboxMessagePublisher publisher = new KafkaOutboxMessagePublisher(
                kafkaTemplate(mockProducer), properties);

        OutboxEvent event = sampleEvent("shipment.create.request");

        boolean published = publisher.publish(event);

        assertTrue(published);
        assertEquals("dev.shipment.create.request", mockProducer.history().get(0).topic());
    }

    @Test
    void publishFallsBackToDefaultTopicWhenRoutingKeyMissing() {
        properties.setDefaultTopic("outbox.fallback");

        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        KafkaOutboxMessagePublisher publisher = new KafkaOutboxMessagePublisher(
                kafkaTemplate(mockProducer), properties);

        OutboxEvent event = sampleEvent(null);

        boolean published = publisher.publish(event);

        assertTrue(published);
        assertEquals("outbox.fallback", mockProducer.history().get(0).topic());
    }

    @Test
    void publishReturnsFalseWhenTopicCannotBeResolved() {
        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        KafkaOutboxMessagePublisher publisher = new KafkaOutboxMessagePublisher(
                kafkaTemplate(mockProducer), properties);

        OutboxEvent event = sampleEvent(null);

        boolean published = publisher.publish(event);

        assertFalse(published);
        assertTrue(mockProducer.history().isEmpty());
    }

    @Test
    void publishReturnsFalseOnProducerFailure() {
        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        mockProducer.errorNext(new RuntimeException("synthetic failure"));

        KafkaOutboxMessagePublisher publisher = new KafkaOutboxMessagePublisher(
                kafkaTemplate(mockProducer), properties);

        OutboxEvent event = sampleEvent("shipment.create.request");

        boolean published = publisher.publish(event);

        assertFalse(published);
    }

    private KafkaTemplate<String, String> kafkaTemplate(MockProducer<String, String> mockProducer) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(configs);
        producerFactory.setProducerSupplier(() -> mockProducer);

        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic("unused");
        return template;
    }

    private OutboxEvent sampleEvent(String routingKey) {
        return OutboxEvent.builder()
                .eventId("event-123")
                .aggregateId("agg-456")
                .aggregateType("Shipment")
                .eventType("ShipmentCreated")
                .payload("{\"hello\":\"world\"}")
                .routingKey(routingKey)
                .status(OutboxEvent.OutboxStatus.PENDING)
                .correlationId("corr-789")
                .createdAt(Instant.now())
                .build();
    }

}
