package com.portal.exception;

/**
 * Exception thrown when authentication is missing or invalid. (HTTP 401)
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
