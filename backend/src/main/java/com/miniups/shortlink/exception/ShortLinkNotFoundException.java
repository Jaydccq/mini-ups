package com.miniups.shortlink.exception;

public class ShortLinkNotFoundException extends RuntimeException {

    public ShortLinkNotFoundException(String shortCode) {
        super("Short link not found: " + shortCode);
    }
}
