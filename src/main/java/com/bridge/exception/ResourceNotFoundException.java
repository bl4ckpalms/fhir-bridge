package com.bridge.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends BridgeException {
    
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
    
    public ResourceNotFoundException(String message, Object details) {
        super("RESOURCE_NOT_FOUND", message, details);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super("RESOURCE_NOT_FOUND", message, cause);
    }
    
    public ResourceNotFoundException(String message, Object details, Throwable cause) {
        super("RESOURCE_NOT_FOUND", message, details, cause);
    }
}