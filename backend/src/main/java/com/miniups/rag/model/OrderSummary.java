package com.miniups.rag.model;

import java.time.LocalDateTime;

/**
 * Order summary DTO for RAG context injection.
 * Contains essential order information to provide personalized AI responses.
 */
public record OrderSummary(
    String trackingNumber,
    String status,
    String statusDisplayName,
    String deliveryAddress,
    String deliveryCity,
    LocalDateTime createdAt,
    LocalDateTime estimatedDelivery,
    boolean canModifyAddress
) {
    
    /**
     * Formats the order summary for inclusion in LLM prompts.
     */
    public String toPromptFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("- 追踪号: ").append(trackingNumber != null ? trackingNumber : "N/A").append("\n");
        sb.append("  状态: ").append(statusDisplayName != null ? statusDisplayName : status).append("\n");
        if (deliveryAddress != null) {
            sb.append("  收货地址: ").append(deliveryAddress);
            if (deliveryCity != null) {
                sb.append(", ").append(deliveryCity);
            }
            sb.append("\n");
        }
        if (estimatedDelivery != null) {
            sb.append("  预计送达: ").append(estimatedDelivery).append("\n");
        }
        sb.append("  可修改地址: ").append(canModifyAddress ? "是" : "否").append("\n");
        return sb.toString();
    }
}
