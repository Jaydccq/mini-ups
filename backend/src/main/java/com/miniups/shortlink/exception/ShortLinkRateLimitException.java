package com.miniups.shortlink.exception;

public class ShortLinkRateLimitException extends RuntimeException {

    public ShortLinkRateLimitException(String message) {
        super(message);
    }
}
