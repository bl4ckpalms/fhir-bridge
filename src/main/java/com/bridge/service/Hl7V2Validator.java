package com.bridge.service;

import com.bridge.model.Hl7Message;
import com.bridge.model.ValidationResult;

/**
 * Service interface for validating HL7 v2 messages
 */
public interface Hl7V2Validator {
    
    /**
     * Validates an HL7 v2 message for structure and business rules
     * 
     * @param message the HL7 message to validate
     * @return ValidationResult containing validation status and any errors/warnings
     */
    ValidationResult validate(Hl7Message message);
    
    /**
     * Validates a raw HL7 v2 message string
     * 
     * @param rawMessage the raw HL7 message string to validate
     * @return ValidationResult containing validation status and any errors/warnings
     */
    ValidationResult validate(String rawMessage);
    
    /**
     * Performs structure validation only (no business rules)
     * 
     * @param rawMessage the raw HL7 message string to validate
     * @return ValidationResult containing structure validation results
     */
    ValidationResult validateStructure(String rawMessage);
    
    /**
     * Performs business rule validation on a structurally valid message
     * 
     * @param rawMessage the raw HL7 message string to validate
     * @return ValidationResult containing business rule validation results
     */
    ValidationResult validateBusinessRules(String rawMessage);
}