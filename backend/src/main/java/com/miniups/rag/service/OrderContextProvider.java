package com.miniups.rag.service;

import com.miniups.model.entity.Shipment;
import com.miniups.model.enums.ShipmentStatus;
import com.miniups.rag.model.OrderSummary;
import com.miniups.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for providing order context to the RAG query pipeline.
 * Fetches user-specific order data and determines if queries are order-related.
 */
@Service
public class OrderContextProvider {

    private static final Logger log = LoggerFactory.getLogger(OrderContextProvider.class);
    
    // Pattern to match UPS tracking numbers (UPS + 12 digits)
    private static final Pattern TRACKING_PATTERN = Pattern.compile("(?i)UPS\\d{12}");
    
    // Keywords indicating order-related queries
    private static final List<String> ORDER_KEYWORDS = List.of(
        "订单", "包裹", "快递", "物流", "追踪", "tracking", "shipment", "delivery",
        "配送", "送达", "状态", "地址", "修改", "延迟", "延误", "什么时候到",
        "order", "package", "status", "where", "address", "change", "delay",
        "我的", "my", "到了吗", "在哪", "多久", "预计"
    );

    private final ShipmentRepository shipmentRepository;

    public OrderContextProvider(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    /**
     * Get order context for a user's recent shipments.
     *
     * @param userId the user ID
     * @param limit maximum number of orders to retrieve
     * @return list of order summaries
     */
    public List<OrderSummary> getUserOrderContext(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        
        try {
            List<Shipment> shipments = shipmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            return shipments.stream()
                .limit(limit)
                .map(this::toOrderSummary)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch orders for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get order details by tracking number.
     *
     * @param trackingNumber the UPS tracking number
     * @return order summary or null if not found
     */
    public OrderSummary getOrderByTracking(String trackingNumber) {
        if (!StringUtils.hasText(trackingNumber)) {
            return null;
        }
        
        try {
            Shipment shipment = shipmentRepository.findByUpsTrackingId(trackingNumber.trim());
            return shipment != null ? toOrderSummary(shipment) : null;
        } catch (Exception e) {
            log.warn("Failed to fetch order by tracking {}: {}", trackingNumber, e.getMessage());
            return null;
        }
    }

    /**
     * Determine if a query is related to orders/shipments.
     *
     * @param query the user query
     * @return true if the query appears to be order-related
     */
    public boolean isOrderRelatedQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return false;
        }
        
        String lowerQuery = query.toLowerCase();
        
        // Check for tracking number pattern
        if (TRACKING_PATTERN.matcher(query).find()) {
            return true;
        }
        
        // Check for order-related keywords
        return ORDER_KEYWORDS.stream().anyMatch(lowerQuery::contains);
    }

    /**
     * Extract tracking number from query if present.
     *
     * @param query the user query
     * @return tracking number or null
     */
    public String extractTrackingNumber(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        
        var matcher = TRACKING_PATTERN.matcher(query);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null;
    }

    /**
     * Format order summaries for LLM prompt injection.
     *
     * @param orders list of order summaries
     * @return formatted string for prompt
     */
    public String formatOrderContext(List<OrderSummary> orders) {
        if (orders == null || orders.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("您最近的订单信息:\n");
        for (OrderSummary order : orders) {
            sb.append(order.toPromptFormat());
        }
        return sb.toString();
    }

    private OrderSummary toOrderSummary(Shipment shipment) {
        ShipmentStatus status = shipment.getStatus();
        return new OrderSummary(
            shipment.getUpsTrackingId(),
            status != null ? status.name() : "UNKNOWN",
            status != null ? status.getDisplayName() : "未知",
            shipment.getDeliveryAddress(),
            shipment.getDeliveryCity(),
            shipment.getCreatedAt(),
            shipment.getEstimatedDelivery(),
            shipment.canChangeAddress()
        );
    }
}
