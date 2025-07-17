package com.bridge.service;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.DefaultValidation;
import com.bridge.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Implementation of HL7 v2 message validator using HAPI HL7 library
 */
@Service
public class Hl7V2ValidatorImpl implements Hl7V2Validator {
    
    private static final Logger logger = LoggerFactory.getLogger(Hl7V2ValidatorImpl.class);
    
    private final HapiContext hapiContext;
    private final PipeParser parser;
    private final ValidationContext validationContext;
    
    // Common HL7 patterns for validation
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{8}$|^\\d{4}$|^\\d{6}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{2}\\d{2}(\\d{2})?(\\.\\d{1,4})?([+-]\\d{4})?$");
    private static final Pattern DATETIME_PATTERN = Pattern.compile("^\\d{8}(\\d{2}\\d{2}(\\d{2})?(\\.\\d{1,4})?([+-]\\d{4})?)?$");
    
    public Hl7V2ValidatorImpl() {
        this.hapiContext = new DefaultHapiContext();
        this.parser = new PipeParser();
        this.validationContext = new DefaultValidation();
        
        // Configure HAPI context with minimal validation for parsing
        // We'll do our own validation after parsing
        ca.uhn.hl7v2.validation.impl.NoValidation noValidation = new ca.uhn.hl7v2.validation.impl.NoValidation();
        this.hapiContext.setValidationContext(noValidation);
        this.parser.setValidationContext(noValidation);
    }
    
    @Override
    public ValidationResult validate(Hl7Message message) {
        if (message == null) {
            ValidationResult result = new ValidationResult(false);
            result.addError(new ValidationError(null, null, "NULL_MESSAGE", "HL7 message cannot be null"));
            return result;
        }
        
        return validate(message.getRawMessage());
    }
    
    @Override
    public ValidationResult validate(String rawMessage) {
        ValidationResult result = validateStructure(rawMessage);
        
        if (result.isValid()) {
            ValidationResult businessResult = validateBusinessRules(rawMessage);
            // Merge business rule validation results
            result.getWarnings().addAll(businessResult.getWarnings());
            result.getErrors().addAll(businessResult.getErrors());
            result.setValid(result.isValid() && businessResult.isValid());
        }
        
        return result;
    }
    
    @Override
    public ValidationResult validateStructure(String rawMessage) {
        ValidationResult result = new ValidationResult();
        
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            result.addError(new ValidationError(null, null, "EMPTY_MESSAGE", "HL7 message cannot be null or empty"));
            return result;
        }
        
        try {
            // Parse the message using HAPI
            Message message = parser.parse(rawMessage);
            
            // Extract message type and version
            Terser terser = new Terser(message);
            String messageType = terser.get("MSH-9-1");
            String triggerEvent = terser.get("MSH-9-2");
            String version = terser.get("MSH-12");
            
            result.setMessageType(messageType + "^" + triggerEvent);
            result.setMessageVersion(version);
            
            // Validate message structure
            validateMessageStructure(message, result);
            
            logger.debug("Structure validation completed for message type: {}, version: {}", 
                        result.getMessageType(), result.getMessageVersion());
            
        } catch (HL7Exception e) {
            logger.error("HL7 parsing error: {}", e.getMessage(), e);
            result.addError(new ValidationError(null, null, "PARSE_ERROR", 
                "Failed to parse HL7 message: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during structure validation: {}", e.getMessage(), e);
            result.addError(new ValidationError(null, null, "VALIDATION_ERROR", 
                "Unexpected validation error: " + e.getMessage()));
        }
        
        return result;
    }
    
    @Override
    public ValidationResult validateBusinessRules(String rawMessage) {
        ValidationResult result = new ValidationResult();
        
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            result.addError(new ValidationError(null, null, "EMPTY_MESSAGE", "HL7 message cannot be null or empty"));
            return result;
        }
        
        try {
            Message message = parser.parse(rawMessage);
            Terser terser = new Terser(message);
            
            // Validate MSH segment business rules
            validateMshBusinessRules(terser, result);
            
            // Validate PID segment if present
            validatePidBusinessRules(terser, result);
            
            // Validate common healthcare business rules
            validateHealthcareBusinessRules(terser, result);
            
            logger.debug("Business rule validation completed with {} errors and {} warnings", 
                        result.getErrorCount(), result.getWarningCount());
            
        } catch (HL7Exception e) {
            logger.error("HL7 parsing error during business rule validation: {}", e.getMessage(), e);
            result.addError(new ValidationError(null, null, "PARSE_ERROR", 
                "Failed to parse HL7 message for business rule validation: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during business rule validation: {}", e.getMessage(), e);
            result.addError(new ValidationError(null, null, "VALIDATION_ERROR", 
                "Unexpected business rule validation error: " + e.getMessage()));
        }
        
        return result;
    }
    
    private void validateMessageStructure(Message message, ValidationResult result) throws HL7Exception {
        // Validate required segments
        validateRequiredSegments(message, result);
        
        // Validate segment order
        validateSegmentOrder(message, result);
        
        // Validate field requirements within segments
        validateSegmentFields(message, result);
    }
    
    private void validateRequiredSegments(Message message, ValidationResult result) throws HL7Exception {
        // MSH segment is always required
        if (message.get("MSH") == null) {
            result.addError(new ValidationError(null, "MSH", "MISSING_SEGMENT", 
                "MSH segment is required in all HL7 messages"));
        }
        
        // Additional segment validation based on message type could be added here
        Terser terser = new Terser(message);
        String messageType = terser.get("MSH-9-1");
        
        if ("ADT".equals(messageType)) {
            if (message.get("PID") == null) {
                result.addError(new ValidationError(null, "PID", "MISSING_SEGMENT", 
                    "PID segment is required for ADT messages"));
            }
        }
    }
    
    private void validateSegmentOrder(Message message, ValidationResult result) {
        // Basic segment order validation - MSH should be first
        String[] names = message.getNames();
        if (names.length > 0 && !"MSH".equals(names[0])) {
            result.addWarning(new ValidationWarning(null, names[0], "SEGMENT_ORDER", 
                "MSH segment should be the first segment in the message"));
        }
    }
    
    private void validateSegmentFields(Message message, ValidationResult result) throws HL7Exception {
        Terser terser = new Terser(message);
        
        // Validate MSH required fields
        validateRequiredField(terser, "MSH-1", "MSH", "Field Separator", result);
        validateRequiredField(terser, "MSH-2", "MSH", "Encoding Characters", result);
        validateRequiredField(terser, "MSH-3", "MSH", "Sending Application", result);
        validateRequiredField(terser, "MSH-4", "MSH", "Sending Facility", result);
        validateRequiredField(terser, "MSH-5", "MSH", "Receiving Application", result);
        validateRequiredField(terser, "MSH-6", "MSH", "Receiving Facility", result);
        validateRequiredField(terser, "MSH-7", "MSH", "Date/Time of Message", result);
        validateRequiredField(terser, "MSH-9", "MSH", "Message Type", result);
        validateRequiredField(terser, "MSH-10", "MSH", "Message Control ID", result);
        validateRequiredField(terser, "MSH-11", "MSH", "Processing ID", result);
        validateRequiredField(terser, "MSH-12", "MSH", "Version ID", result);
    }
    
    private void validateRequiredField(Terser terser, String fieldPath, String segment, 
                                     String fieldName, ValidationResult result) throws HL7Exception {
        String value = terser.get(fieldPath);
        if (value == null || value.trim().isEmpty()) {
            result.addError(new ValidationError(fieldName, segment, "MISSING_FIELD", 
                String.format("Required field %s (%s) is missing or empty", fieldName, fieldPath)));
        }
    }
    
    private void validateMshBusinessRules(Terser terser, ValidationResult result) throws HL7Exception {
        // Validate timestamp format
        String timestamp = terser.get("MSH-7");
        if (timestamp != null && !timestamp.isEmpty()) {
            if (!DATETIME_PATTERN.matcher(timestamp).matches()) {
                result.addError(new ValidationError("Date/Time of Message", "MSH", "INVALID_DATETIME", 
                    "Invalid datetime format in MSH-7: " + timestamp));
            }
        }
        
        // Validate version
        String version = terser.get("MSH-12");
        if (version != null && !version.isEmpty()) {
            if (!version.matches("^2\\.[0-9]+(\\.[0-9]+)?$")) {
                result.addWarning(new ValidationWarning("Version ID", "MSH", "UNSUPPORTED_VERSION", 
                    "Unsupported HL7 version: " + version + ". Supported versions are 2.x"));
            }
        }
        
        // Validate processing ID
        String processingId = terser.get("MSH-11");
        if (processingId != null && !processingId.isEmpty()) {
            if (!processingId.matches("^[PDT]$")) {
                result.addWarning(new ValidationWarning("Processing ID", "MSH", "INVALID_PROCESSING_ID", 
                    "Processing ID should be P (Production), D (Debug), or T (Training). Found: " + processingId));
            }
        }
    }
    
    private void validatePidBusinessRules(Terser terser, ValidationResult result) throws HL7Exception {
        // Check if PID segment exists - use try-catch to handle missing segments gracefully
        try {
            String patientId = terser.get("PID-3");
            // Check if PID segment exists but patient ID is missing or empty
            if (patientId == null || patientId.trim().isEmpty() || patientId.matches("^\\^*$")) {
                result.addError(new ValidationError("Patient ID", "PID", "EMPTY_PATIENT_ID", 
                    "Patient ID cannot be empty when PID segment is present"));
            } else {
                
                // Validate date of birth format if present
                try {
                    String dob = terser.get("PID-7");
                    if (dob != null && !dob.isEmpty()) {
                        if (!DATE_PATTERN.matcher(dob).matches()) {
                            result.addError(new ValidationError("Date of Birth", "PID", "INVALID_DATE", 
                                "Invalid date format in PID-7: " + dob));
                        }
                    }
                } catch (HL7Exception e) {
                    // PID-7 field access failed, likely due to parsing issues
                    logger.debug("Could not access PID-7 field: {}", e.getMessage());
                }
                
                // Validate gender code if present
                try {
                    String gender = terser.get("PID-8");
                    if (gender != null && !gender.isEmpty()) {
                        if (!gender.matches("^[MFOU]$")) {
                            result.addWarning(new ValidationWarning("Administrative Sex", "PID", "INVALID_GENDER", 
                                "Gender should be M, F, O, or U. Found: " + gender));
                        }
                    }
                } catch (HL7Exception e) {
                    // PID-8 field access failed, likely due to parsing issues
                    logger.debug("Could not access PID-8 field: {}", e.getMessage());
                }
            }
        } catch (HL7Exception e) {
            // PID segment doesn't exist or can't be accessed - this is fine for some message types
            logger.debug("PID segment not accessible: {}", e.getMessage());
        }
    }
    
    private void validateHealthcareBusinessRules(Terser terser, ValidationResult result) throws HL7Exception {
        // Validate that sending and receiving applications are different
        String sendingApp = terser.get("MSH-3");
        String receivingApp = terser.get("MSH-5");
        
        if (sendingApp != null && receivingApp != null && sendingApp.equals(receivingApp)) {
            result.addWarning(new ValidationWarning("Application", "MSH", "SAME_APPLICATIONS", 
                "Sending and receiving applications are the same: " + sendingApp));
        }
        
        // Validate message control ID uniqueness (basic format check)
        String controlId = terser.get("MSH-10");
        if (controlId != null && controlId.length() > 20) {
            result.addWarning(new ValidationWarning("Message Control ID", "MSH", "LONG_CONTROL_ID", 
                "Message Control ID is longer than recommended 20 characters: " + controlId.length()));
        }
    }
}