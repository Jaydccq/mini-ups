package com.miniups.shortlink.exception;

public class ShortLinkServiceException extends RuntimeException {

    public ShortLinkServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShortLinkServiceException(String message) {
        super(message);
    }
}
