package com.bridge.exception;

/**
 * Exception thrown when authentication fails
 */
public class AuthenticationException extends BridgeException {
    
    public AuthenticationException(String message) {
        super("AUTHENTICATION_ERROR", message);
    }
    
    public AuthenticationException(String message, Object details) {
        super("AUTHENTICATION_ERROR", message, details);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super("AUTHENTICATION_ERROR", message, cause);
    }
    
    public AuthenticationException(String message, Object details, Throwable cause) {
        super("AUTHENTICATION_ERROR", message, details, cause);
    }
}