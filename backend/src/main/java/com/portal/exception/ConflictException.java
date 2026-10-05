package com.portal.exception;

/**
 * Exception thrown when a resource already exists or conflicts with existing state (HTTP 409).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
