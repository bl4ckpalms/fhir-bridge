package com.bridge.exception;

/**
 * Base exception class for all FHIR Bridge application exceptions
 */
public abstract class BridgeException extends RuntimeException {
    
    private final String errorCode;
    private final Object details;
    
    public BridgeException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public BridgeException(String errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public BridgeException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public BridgeException(String errorCode, String message, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object getDetails() {
        return details;
    }
}