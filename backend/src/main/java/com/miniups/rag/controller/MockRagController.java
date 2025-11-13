package com.miniups.rag.controller;

import com.miniups.rag.api.RagQueryRequest;
import com.miniups.rag.api.RagQueryResponse;
import com.miniups.rag.api.RagSourceDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/api/rag")
@ConditionalOnProperty(name = "rag.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockRagController {


    private static final Logger log = LoggerFactory.getLogger(MockRagController.class);
    private static final Map<String, String> MOCK_ANSWERS = Map.of(
        "sync", "To sync world simulator status:\n1. Check the current world ID in backend configuration\n2. Use the /api/world/sync endpoint to refresh status\n3. Monitor the trucks table for updated positions\n4. Verify warehouse inventory is synchronized\n\nThe system automatically syncs every 30 seconds, but manual sync can be triggered via the admin dashboard.",

        "delay", "For delayed deliveries:\n1. Check the shipment tracking page for real-time updates\n2. Use the driver management system to reassign available drivers\n3. Monitor world simulator for traffic conditions\n4. Send automatic notifications to customers via the messaging system\n\nDelayed shipments are automatically flagged in the dashboard when they exceed expected delivery time.",

        "driver", "Driver check-in process:\n1. Drivers use the mobile app to check in at delivery locations\n2. GPS coordinates are validated against the destination\n3. Photos of delivered packages are uploaded as proof\n4. System updates the shipment status to 'DELIVERED'\n5. Customer receives automatic notification\n\nThe check-in data is stored in the database and can be viewed in the admin panel.",

        "default", "I can help you with Mini-UPS operations including:\n• World simulator synchronization\n• Delivery management\n• Driver processes\n• Shipment tracking\n• System configuration\n\nPlease ask a more specific question about any of these topics."
    );

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('ADMIN','USER','DRIVER','OPERATOR')")
    public ResponseEntity<RagQueryResponse> query(
        @RequestBody RagQueryRequest request,
        Authentication authentication
    ) {
        log.info("Mock RAG query: {}", request.getQuery());

        String query = request.getQuery().toLowerCase();
        String answer = MOCK_ANSWERS.entrySet().stream()
            .filter(entry -> query.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(MOCK_ANSWERS.get("default"));

        List<RagSourceDto> sources = List.of(
            new RagSourceDto(
                "Mini-UPS Operations Manual",
                "docs/operations-guide.md",
                0.85,
                0.92,
                0.80,
                0.90
            ),
            new RagSourceDto(
                "World Simulator Integration Guide",
                "docs/world-simulator.md",
                0.78,
                0.88,
                0.75,
                0.82
            )
        );

        RagQueryResponse response = new RagQueryResponse(
            UUID.randomUUID(),
            answer,
            0.90,
            sources,
            List.of()
        );

        return ResponseEntity.ok(response);
    }
}