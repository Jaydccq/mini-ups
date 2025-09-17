package com.miniups.shortlink.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ShortLinkUpdateRequest {

    @Pattern(regexp = "^(https?://).+$", message = "URL must start with http:// or https://")
    private String originalUrl;

    private LocalDateTime expirationAt;

    private Boolean active;

    @Size(max = 128)
    private String note;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getExpirationAt() {
        return expirationAt;
    }

    public void setExpirationAt(LocalDateTime expirationAt) {
        this.expirationAt = expirationAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
