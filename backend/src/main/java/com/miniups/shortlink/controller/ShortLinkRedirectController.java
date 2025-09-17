package com.miniups.shortlink.controller;

import com.miniups.shortlink.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Tag(name = "Short Link Redirect", description = "Public redirect endpoint with Redis stream monitoring")
public class ShortLinkRedirectController {

    private final ShortLinkService shortLinkService;

    public ShortLinkRedirectController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @GetMapping({"/s/{shortCode}", "/short-links/redirect/{shortCode}"})
    @Operation(summary = "Redirect short link", description = "Redirect user to the original URL with monitoring and rate limiting")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String destination = shortLinkService.resolveRedirect(shortCode, request);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(destination));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}
