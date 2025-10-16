package com.miniups.service;

import com.miniups.model.entity.OutboxEvent;
import com.miniups.repository.OutboxEventRepository;
import com.miniups.service.messaging.OutboxMessagePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxPollerServiceTest {

    private OutboxEventRepository outboxEventRepository;
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        redisTemplate = mock(RedisTemplate.class);
    }

    @Test
    void publishEventReturnsTrueWhenAllPublishersSucceed() throws Exception {
        OutboxMessagePublisher publisherOne = mockPublisher("rabbitmq", true, false);
        OutboxMessagePublisher publisherTwo = mockPublisher("kafka", true, false);

        TestableOutboxPollerService service = new TestableOutboxPollerService(
                outboxEventRepository, redisTemplate, List.of(publisherOne, publisherTwo));

        boolean result = service.invokePublish(buildEvent());

        assertTrue(result, "Event should be treated as published when all publishers succeed");
    }

    @Test
    void publishEventReturnsFalseWhenPublisherReportsFailure() throws Exception {
        OutboxMessagePublisher successPublisher = mockPublisher("rabbitmq", true, false);
        OutboxMessagePublisher failingPublisher = mockPublisher("kafka", false, false);

        TestableOutboxPollerService service = new TestableOutboxPollerService(
                outboxEventRepository, redisTemplate, List.of(successPublisher, failingPublisher));

        boolean result = service.invokePublish(buildEvent());

        assertFalse(result, "Event should not be marked published if any publisher fails");
    }

    @Test
    void publishEventReturnsFalseWhenPublisherThrowsException() throws Exception {
        OutboxMessagePublisher throwingPublisher = mockPublisher("kafka", true, true);

        TestableOutboxPollerService service = new TestableOutboxPollerService(
                outboxEventRepository, redisTemplate, List.of(throwingPublisher));

        boolean result = service.invokePublish(buildEvent());

        assertFalse(result, "Exceptions from publishers should surface as failures");
    }

    private OutboxMessagePublisher mockPublisher(String channel, boolean shouldSucceed, boolean shouldThrow) throws Exception {
        OutboxMessagePublisher publisher = mock(OutboxMessagePublisher.class);
        when(publisher.channel()).thenReturn(channel);
        if (shouldThrow) {
            Mockito.doThrow(new RuntimeException("synthetic failure"))
                    .when(publisher).publish(any(OutboxEvent.class));
        } else {
            when(publisher.publish(any(OutboxEvent.class))).thenReturn(shouldSucceed);
        }
        return publisher;
    }

    private OutboxEvent buildEvent() {
        return OutboxEvent.builder()
                .eventId("event-123")
                .aggregateId("agg-1")
                .aggregateType("Shipment")
                .eventType("ShipmentCreated")
                .payload("{\"foo\":\"bar\"}")
                .routingKey("shipment.create.request")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .maxRetries(5)
                .build();
    }

    private static class TestableOutboxPollerService extends OutboxPollerService {
        TestableOutboxPollerService(OutboxEventRepository outboxEventRepository,
                                    RedisTemplate<String, String> redisTemplate,
                                    List<OutboxMessagePublisher> publishers) {
            super(outboxEventRepository, redisTemplate, publishers);
        }

        boolean invokePublish(OutboxEvent event) {
            return super.publishEvent(event);
        }
    }
}
