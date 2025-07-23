package com.bridge.exception;

/**
 * Exception thrown when authorization fails
 */
public class AuthorizationException extends BridgeException {
    
    public AuthorizationException(String message) {
        super("AUTHORIZATION_ERROR", message);
    }
    
    public AuthorizationException(String message, Object details) {
        super("AUTHORIZATION_ERROR", message, details);
    }
    
    public AuthorizationException(String message, Throwable cause) {
        super("AUTHORIZATION_ERROR", message, cause);
    }
    
    public AuthorizationException(String message, Object details, Throwable cause) {
        super("AUTHORIZATION_ERROR", message, details, cause);
    }
}