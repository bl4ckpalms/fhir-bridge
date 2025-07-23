package com.bridge.exception;

import com.bridge.model.ValidationError;
import java.util.List;

/**
 * Exception thrown when validation fails for HL7 messages or FHIR resources
 */
public class ValidationException extends BridgeException {
    
    public ValidationException(String message, List<ValidationError> validationErrors) {
        super("VALIDATION_ERROR", message, validationErrors);
    }
    
    public ValidationException(String message, List<ValidationError> validationErrors, Throwable cause) {
        super("VALIDATION_ERROR", message, validationErrors, cause);
    }
    
    @SuppressWarnings("unchecked")
    public List<ValidationError> getValidationErrors() {
        return (List<ValidationError>) getDetails();
    }
}