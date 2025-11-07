package com.miniups.rag.controller;

import com.miniups.rag.api.RagQueryRequest;
import com.miniups.rag.api.RagQueryResponse;
import com.miniups.rag.service.RagQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagController {

    private final RagQueryService queryService;

    public RagController(RagQueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('ADMIN','USER','DRIVER','OPERATOR')")
    public ResponseEntity<RagQueryResponse> query(
        @Valid @RequestBody RagQueryRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(queryService.handleQuery(request, authentication));
    }
}
