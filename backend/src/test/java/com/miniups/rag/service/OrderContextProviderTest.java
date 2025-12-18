package com.miniups.rag.service;

import com.miniups.model.entity.Shipment;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.rag.model.OrderSummary;
import com.miniups.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderContextProvider Tests")
class OrderContextProviderTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    private OrderContextProvider orderContextProvider;

    @BeforeEach
    void setUp() {
        orderContextProvider = new OrderContextProvider(shipmentRepository);
    }

    @Nested
    @DisplayName("Order Query Detection")
    class OrderQueryDetectionTests {

        @Test
        @DisplayName("Should detect order-related queries in Chinese")
        void shouldDetectChineseOrderQueries() {
            assertThat(orderContextProvider.isOrderRelatedQuery("我的订单在哪")).isTrue();
            assertThat(orderContextProvider.isOrderRelatedQuery("包裹什么时候到")).isTrue();
            assertThat(orderContextProvider.isOrderRelatedQuery("查询物流状态")).isTrue();
            assertThat(orderContextProvider.isOrderRelatedQuery("可以修改地址吗")).isTrue();
        }

        @Test
        @DisplayName("Should detect order-related queries in English")
        void shouldDetectEnglishOrderQueries() {
            assertThat(orderContextProvider.isOrderRelatedQuery("Where is my order?")).isTrue();
            assertThat(orderContextProvider.isOrderRelatedQuery("tracking status")).isTrue();
            assertThat(orderContextProvider.isOrderRelatedQuery("delivery address")).isTrue();
        }

        @Test
        @DisplayName("Should detect tracking numbers in query")
        void shouldDetectTrackingNumbers() {
            assertThat(orderContextProvider.isOrderRelatedQuery("查询UPS000000000001")).isTrue();
            assertThat(orderContextProvider.isOrderRelatedQuery("my package UPS123456789012")).isTrue();
        }

        @Test
        @DisplayName("Should not detect unrelated queries")
        void shouldNotDetectUnrelatedQueries() {
            assertThat(orderContextProvider.isOrderRelatedQuery("天气怎么样")).isFalse();
            assertThat(orderContextProvider.isOrderRelatedQuery("hello world")).isFalse();
        }

        @Test
        @DisplayName("Should handle null and empty queries")
        void shouldHandleNullAndEmptyQueries() {
            assertThat(orderContextProvider.isOrderRelatedQuery(null)).isFalse();
            assertThat(orderContextProvider.isOrderRelatedQuery("")).isFalse();
            assertThat(orderContextProvider.isOrderRelatedQuery("   ")).isFalse();
        }
    }

    @Nested
    @DisplayName("Tracking Number Extraction")
    class TrackingNumberExtractionTests {

        @Test
        @DisplayName("Should extract valid tracking number")
        void shouldExtractTrackingNumber() {
            assertThat(orderContextProvider.extractTrackingNumber("查询UPS000000000001"))
                .isEqualTo("UPS000000000001");
            assertThat(orderContextProvider.extractTrackingNumber("my order ups123456789012 status"))
                .isEqualTo("UPS123456789012");
        }

        @Test
        @DisplayName("Should return null when no tracking number")
        void shouldReturnNullWhenNoTracking() {
            assertThat(orderContextProvider.extractTrackingNumber("我的订单在哪")).isNull();
        }
    }

    @Nested
    @DisplayName("User Order Context")
    class UserOrderContextTests {

        @Test
        @DisplayName("Should return user orders")
        void shouldReturnUserOrders() {
            Shipment shipment = new Shipment();
            shipment.setUpsTrackingId("UPS000000000001");
            shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            shipment.setDeliveryAddress("123 Main St");
            shipment.setDeliveryCity("Durham");
            shipment.setCreatedAt(LocalDateTime.now());
            
            when(shipmentRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(shipment));

            List<OrderSummary> orders = orderContextProvider.getUserOrderContext(1L, 5);

            assertThat(orders).hasSize(1);
            assertThat(orders.get(0).trackingNumber()).isEqualTo("UPS000000000001");
            assertThat(orders.get(0).status()).isEqualTo("IN_TRANSIT");
            assertThat(orders.get(0).canModifyAddress()).isTrue();
        }

        @Test
        @DisplayName("Should return empty list for null userId")
        void shouldReturnEmptyForNullUserId() {
            List<OrderSummary> orders = orderContextProvider.getUserOrderContext(null, 5);
            assertThat(orders).isEmpty();
        }

        @Test
        @DisplayName("Should limit results")
        void shouldLimitResults() {
            Shipment s1 = createShipment("UPS000000000001");
            Shipment s2 = createShipment("UPS000000000002");
            Shipment s3 = createShipment("UPS000000000003");
            
            when(shipmentRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(s1, s2, s3));

            List<OrderSummary> orders = orderContextProvider.getUserOrderContext(1L, 2);
            assertThat(orders).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Order Context Formatting")
    class OrderContextFormattingTests {

        @Test
        @DisplayName("Should format order context for prompt")
        void shouldFormatOrderContext() {
            OrderSummary order = new OrderSummary(
                "UPS000000000001",
                "IN_TRANSIT",
                "运输中",
                "123 Main St",
                "Durham",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                true
            );

            String context = orderContextProvider.formatOrderContext(List.of(order));

            assertThat(context).contains("UPS000000000001");
            assertThat(context).contains("运输中");
            assertThat(context).contains("123 Main St");
        }

        @Test
        @DisplayName("Should return empty string for empty list")
        void shouldReturnEmptyForEmptyList() {
            assertThat(orderContextProvider.formatOrderContext(List.of())).isEmpty();
            assertThat(orderContextProvider.formatOrderContext(null)).isEmpty();
        }
    }

    private Shipment createShipment(String trackingId) {
        Shipment shipment = new Shipment();
        shipment.setUpsTrackingId(trackingId);
        shipment.setStatus(ShipmentStatus.CREATED);
        return shipment;
    }
}
