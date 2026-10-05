package com.portal.exception;

/**
 * Exception thrown when a requested entity/resource does not exist. (HTTP 404)
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
