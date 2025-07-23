package com.bridge.exception;

/**
 * Exception thrown when system-level errors occur
 */
public class SystemException extends BridgeException {
    
    public SystemException(String message) {
        super("SYSTEM_ERROR", message);
    }
    
    public SystemException(String message, Object details) {
        super("SYSTEM_ERROR", message, details);
    }
    
    public SystemException(String message, Throwable cause) {
        super("SYSTEM_ERROR", message, cause);
    }
    
    public SystemException(String message, Object details, Throwable cause) {
        super("SYSTEM_ERROR", message, details, cause);
    }
}