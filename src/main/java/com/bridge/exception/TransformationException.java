package com.bridge.exception;

/**
 * Exception thrown when HL7 to FHIR transformation fails
 */
public class TransformationException extends BridgeException {
    
    public TransformationException(String message) {
        super("TRANSFORMATION_ERROR", message);
    }
    
    public TransformationException(String message, Object details) {
        super("TRANSFORMATION_ERROR", message, details);
    }
    
    public TransformationException(String message, Throwable cause) {
        super("TRANSFORMATION_ERROR", message, cause);
    }
    
    public TransformationException(String message, Object details, Throwable cause) {
        super("TRANSFORMATION_ERROR", message, details, cause);
    }
}