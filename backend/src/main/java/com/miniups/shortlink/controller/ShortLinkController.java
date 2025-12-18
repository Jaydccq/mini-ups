package com.miniups.shortlink.controller;

import com.miniups.model.dto.common.ApiResponse;
import com.miniups.security.CustomUserDetailsService;
import com.miniups.shortlink.dto.ShortLinkCreateRequest;
import com.miniups.shortlink.dto.ShortLinkPageResponse;
import com.miniups.shortlink.dto.ShortLinkResponse;
import com.miniups.shortlink.dto.ShortLinkUpdateRequest;
import com.miniups.shortlink.service.ShortLinkService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/short-links", "/short-links"})
@Validated
@Tag(name = "Short Links", description = "CRUD operations for short link management with RBAC controls")
@ConditionalOnProperty(name = "shortlink.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    public ShortLinkController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create short link", description = "Generate a new short link using MurmurHash with collision protection")
    public ResponseEntity<ApiResponse<ShortLinkResponse>> createShortLink(@Valid @RequestBody ShortLinkCreateRequest request) {
        Long userId = currentUserId();
        ShortLinkResponse response = shortLinkService.createShortLink(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Short link created", response));
    }

    @PutMapping("/{shortCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update short link", description = "Update destination URL, expiration or state with Redisson write lock")
    public ResponseEntity<ApiResponse<ShortLinkResponse>> updateShortLink(@PathVariable String shortCode,
                                                                          @Valid @RequestBody ShortLinkUpdateRequest request) {
        Long userId = currentUserId();
        ShortLinkResponse response = shortLinkService.updateShortLink(userId, shortCode, request);
        return ResponseEntity.ok(ApiResponse.success("Short link updated", response));
    }

    @GetMapping("/{shortCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get short link", description = "Retrieve metadata for an existing short link")
    public ResponseEntity<ApiResponse<ShortLinkResponse>> getShortLink(@PathVariable String shortCode) {
        Long userId = currentUserId();
        ShortLinkResponse response = shortLinkService.getShortLinkDetails(userId, shortCode);
        return ResponseEntity.ok(ApiResponse.success("Short link fetched", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin list short links", description = "List short links routed via Sharding-JDBC routing table")
    public ResponseEntity<ApiResponse<ShortLinkPageResponse>> listShortLinks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ShortLinkPageResponse response = shortLinkService.listShortLinks(page, size);
        return ResponseEntity.ok(ApiResponse.success("Short link list", response));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal principal)) {
            return null;
        }
        return principal.getUser().getId();
    }
}
