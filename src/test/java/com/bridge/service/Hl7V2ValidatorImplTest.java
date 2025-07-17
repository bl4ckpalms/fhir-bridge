package com.bridge.service;

import com.bridge.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Hl7V2ValidatorImpl
 */
class Hl7V2ValidatorImplTest {
    
    private Hl7V2ValidatorImpl validator;
    
    // Sample valid HL7 ADT message
    private static final String VALID_ADT_MESSAGE = 
        "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
        "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345||(555)123-4567|||S||987654321||||||||||||\r" +
        "PV1|1|I|ICU^101^1|||DOCTOR123^SMITH^JANE|||SUR||||A|||DOCTOR123^SMITH^JANE|INP|CAT|||||||||||||||||||||||||20240715120000";
    
    // Sample valid HL7 ORM message
    private static final String VALID_ORM_MESSAGE = 
        "MSH|^~\\&|LAB_SYSTEM|LAB_FACILITY|EMR_SYSTEM|HOSPITAL|20240715120000||ORM^O01|67890|P|2.4\r" +
        "PID|1||987654321^^^MRN||PATIENT^TEST^MIDDLE||19750615|F|||456 OAK AVE^^TESTCITY^CA^90210||(555)987-6543|||M||123456789||||||||||||\r" +
        "ORC|NW|ORDER123|ORDER123|GROUP123||E||20240715120000|||DOCTOR456^JONES^ROBERT\r" +
        "OBR|1|ORDER123|ORDER123|CBC^COMPLETE BLOOD COUNT^L|||20240715120000||||||||DOCTOR456^JONES^ROBERT";
    
    @BeforeEach
    void setUp() {
        validator = new Hl7V2ValidatorImpl();
    }
    
    @Test
    @DisplayName("Should validate null message and return error")
    void testValidateNullMessage() {
        ValidationResult result = validator.validate((String) null);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrorCount());
        assertEquals("EMPTY_MESSAGE", result.getErrors().get(0).getCode());
    }
    
    @Test
    @DisplayName("Should validate empty message and return error")
    void testValidateEmptyMessage() {
        ValidationResult result = validator.validate("");
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrorCount());
        assertEquals("EMPTY_MESSAGE", result.getErrors().get(0).getCode());
    }
    
    @Test
    @DisplayName("Should validate null Hl7Message object and return error")
    void testValidateNullHl7MessageObject() {
        ValidationResult result = validator.validate((Hl7Message) null);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrorCount());
        assertEquals("NULL_MESSAGE", result.getErrors().get(0).getCode());
    }
    
    @Test
    @DisplayName("Should validate valid ADT message successfully")
    void testValidateValidAdtMessage() {
        ValidationResult result = validator.validate(VALID_ADT_MESSAGE);
        
        assertTrue(result.isValid(), "Valid ADT message should pass validation");
        assertEquals("ADT^A01", result.getMessageType());
        assertEquals("2.4", result.getMessageVersion());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    @DisplayName("Should validate valid ORM message successfully")
    void testValidateValidOrmMessage() {
        ValidationResult result = validator.validate(VALID_ORM_MESSAGE);
        
        assertTrue(result.isValid(), "Valid ORM message should pass validation");
        assertEquals("ORM^O01", result.getMessageType());
        assertEquals("2.4", result.getMessageVersion());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    @DisplayName("Should validate Hl7Message object successfully")
    void testValidateHl7MessageObject() {
        Hl7Message message = new Hl7Message();
        message.setRawMessage(VALID_ADT_MESSAGE);
        
        ValidationResult result = validator.validate(message);
        
        assertTrue(result.isValid());
        assertEquals("ADT^A01", result.getMessageType());
    }
    
    @Test
    @DisplayName("Should detect malformed HL7 message")
    void testValidateMalformedMessage() {
        String malformedMessage = "INVALID|MESSAGE|FORMAT";
        
        ValidationResult result = validator.validate(malformedMessage);
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertEquals("PARSE_ERROR", result.getErrors().get(0).getCode());
    }
    
    @Test
    @DisplayName("Should validate structure only")
    void testValidateStructureOnly() {
        ValidationResult result = validator.validateStructure(VALID_ADT_MESSAGE);
        
        assertTrue(result.isValid());
        assertEquals("ADT^A01", result.getMessageType());
        assertEquals("2.4", result.getMessageVersion());
    }
    
    @Test
    @DisplayName("Should detect missing MSH segment")
    void testValidateMissingMshSegment() {
        String messageWithoutMsh = "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M";
        
        ValidationResult result = validator.validateStructure(messageWithoutMsh);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> "PARSE_ERROR".equals(error.getCode())));
    }
    
    @Test
    @DisplayName("Should validate business rules successfully")
    void testValidateBusinessRules() {
        ValidationResult result = validator.validateBusinessRules(VALID_ADT_MESSAGE);
        
        assertTrue(result.isValid());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    @DisplayName("Should detect invalid datetime format in MSH-7")
    void testValidateInvalidDatetime() {
        String messageWithInvalidDate = 
            "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|INVALID_DATE||ADT^A01|12345|P|2.4\r" +
            "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M";
        
        ValidationResult result = validator.validateBusinessRules(messageWithInvalidDate);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> "INVALID_DATETIME".equals(error.getCode())));
    }
    
    @Test
    @DisplayName("Should detect invalid processing ID")
    void testValidateInvalidProcessingId() {
        String messageWithInvalidProcessingId = 
            "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|X|2.4\r" +
            "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M";
        
        ValidationResult result = validator.validateBusinessRules(messageWithInvalidProcessingId);
        
        assertTrue(result.isValid()); // Should be valid but with warnings
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> "INVALID_PROCESSING_ID".equals(warning.getCode())));
    }
    
    @Test
    @DisplayName("Should detect invalid gender code")
    void testValidateInvalidGender() {
        String messageWithInvalidGender = 
            "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
            "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|X";
        
        ValidationResult result = validator.validateBusinessRules(messageWithInvalidGender);
        
        assertTrue(result.isValid()); // Should be valid but with warnings
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> "INVALID_GENDER".equals(warning.getCode())));
    }
    
    @Test
    @DisplayName("Should detect invalid date of birth format")
    void testValidateInvalidDateOfBirth() {
        String messageWithInvalidDob = 
            "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
            "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||INVALID_DATE|M";
        
        ValidationResult result = validator.validateBusinessRules(messageWithInvalidDob);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> "INVALID_DATE".equals(error.getCode())));
    }
    
    @Test
    @DisplayName("Should warn when sending and receiving applications are the same")
    void testValidateSameApplications() {
        String messageWithSameApps = 
            "MSH|^~\\&|SAME_APP|SENDING_FACILITY|SAME_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
            "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M";
        
        ValidationResult result = validator.validateBusinessRules(messageWithSameApps);
        
        assertTrue(result.isValid()); // Should be valid but with warnings
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> "SAME_APPLICATIONS".equals(warning.getCode())));
    }
    
    @Test
    @DisplayName("Should warn about long message control ID")
    void testValidateLongControlId() {
        String longControlId = "A".repeat(25); // 25 characters, longer than recommended 20
        String messageWithLongControlId = 
            "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|" + longControlId + "|P|2.4\r" +
            "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M";
        
        ValidationResult result = validator.validateBusinessRules(messageWithLongControlId);
        
        assertTrue(result.isValid()); // Should be valid but with warnings
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> "LONG_CONTROL_ID".equals(warning.getCode())));
    }
    
    @Test
    @DisplayName("Should detect missing required fields in MSH segment")
    void testValidateMissingRequiredFields() {
        String messageWithMissingFields = 
            "MSH|^~\\&|||RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
            "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M";
        
        ValidationResult result = validator.validateStructure(messageWithMissingFields);
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        // Should have errors for missing sending application and sending facility
        long missingFieldErrors = result.getErrors().stream()
            .filter(error -> "MISSING_FIELD".equals(error.getCode()))
            .count();
        assertTrue(missingFieldErrors > 0);
    }
    
    @Test
    @DisplayName("Should detect unsupported HL7 version as parse error")
    void testValidateUnsupportedVersion() {
        String messageWithUnsupportedVersion = 
            "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|P|3.0\r" +
            "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M";
        
        ValidationResult result = validator.validateBusinessRules(messageWithUnsupportedVersion);
        
        // HAPI library throws an exception for unsupported versions, so this becomes a parse error
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> "PARSE_ERROR".equals(error.getCode())));
    }
    
    @Test
    @DisplayName("Should detect empty patient ID when PID segment is present")
    void testValidateEmptyPatientId() {
        String messageWithEmptyPatientId = 
            "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
            "PID|1||||DOE^JOHN^MIDDLE||19800101|M";
        
        ValidationResult result = validator.validateBusinessRules(messageWithEmptyPatientId);
        
        // The PID-3 field is completely empty (no patient ID provided)
        // This should trigger our empty patient ID validation
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> "EMPTY_PATIENT_ID".equals(error.getCode())));
    }
}