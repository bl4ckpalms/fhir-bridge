package com.bridge.exception;

/**
 * Exception thrown when consent-related operations fail
 */
public class ConsentException extends BridgeException {
    
    public ConsentException(String message) {
        super("CONSENT_ERROR", message);
    }
    
    public ConsentException(String message, Object details) {
        super("CONSENT_ERROR", message, details);
    }
    
    public ConsentException(String message, Throwable cause) {
        super("CONSENT_ERROR", message, cause);
    }
    
    public ConsentException(String message, Object details, Throwable cause) {
        super("CONSENT_ERROR", message, details, cause);
    }
}