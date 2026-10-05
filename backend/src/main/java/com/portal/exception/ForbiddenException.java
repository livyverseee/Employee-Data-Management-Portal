package com.portal.exception;

/**
 * Exception thrown when authenticated user lacks permissions for an operation. (HTTP 403)
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
