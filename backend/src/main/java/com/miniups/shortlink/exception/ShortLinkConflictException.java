package com.miniups.shortlink.exception;

public class ShortLinkConflictException extends RuntimeException {

    public ShortLinkConflictException(String shortCode) {
        super("Short link code already exists: " + shortCode);
    }
}
