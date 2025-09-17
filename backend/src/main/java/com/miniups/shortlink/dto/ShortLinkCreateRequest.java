package com.miniups.shortlink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ShortLinkCreateRequest {

    @NotBlank(message = "Original URL is required")
    @Pattern(regexp = "^(https?://).+$", message = "URL must start with http:// or https://")
    private String originalUrl;

    @Size(max = 16, message = "Custom code must be at most 16 characters")
    private String customCode;

    private LocalDateTime expirationAt;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
    }

    public LocalDateTime getExpirationAt() {
        return expirationAt;
    }

    public void setExpirationAt(LocalDateTime expirationAt) {
        this.expirationAt = expirationAt;
    }
}
