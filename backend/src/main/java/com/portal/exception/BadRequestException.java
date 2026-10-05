package com.portal.exception;

/**
 * Exception thrown when client input or uploaded file fails validation. (HTTP 400)
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
