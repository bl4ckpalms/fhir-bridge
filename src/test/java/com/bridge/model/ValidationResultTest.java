package com.bridge.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationResult
 */
class ValidationResultTest {
    
    private ValidationResult validationResult;
    
    @BeforeEach
    void setUp() {
        validationResult = new ValidationResult();
    }
    
    @Test
    @DisplayName("Should initialize with valid state and empty collections")
    void testDefaultConstructor() {
        assertTrue(validationResult.isValid());
        assertNotNull(validationResult.getErrors());
        assertNotNull(validationResult.getWarnings());
        assertTrue(validationResult.getErrors().isEmpty());
        assertTrue(validationResult.getWarnings().isEmpty());
        assertEquals(0, validationResult.getErrorCount());
        assertEquals(0, validationResult.getWarningCount());
        assertFalse(validationResult.hasErrors());
        assertFalse(validationResult.hasWarnings());
    }
    
    @Test
    @DisplayName("Should initialize with specified validity state")
    void testConstructorWithValidityState() {
        ValidationResult invalidResult = new ValidationResult(false);
        
        assertFalse(invalidResult.isValid());
        assertNotNull(invalidResult.getErrors());
        assertNotNull(invalidResult.getWarnings());
    }
    
    @Test
    @DisplayName("Should add error and set invalid state")
    void testAddError() {
        ValidationError error = new ValidationError("field1", "segment1", "Error message");
        
        validationResult.addError(error);
        
        assertFalse(validationResult.isValid());
        assertEquals(1, validationResult.getErrorCount());
        assertTrue(validationResult.hasErrors());
        assertEquals(error, validationResult.getErrors().get(0));
    }
    
    @Test
    @DisplayName("Should add warning without affecting validity")
    void testAddWarning() {
        ValidationWarning warning = new ValidationWarning("field1", "segment1", "Warning message");
        
        validationResult.addWarning(warning);
        
        assertTrue(validationResult.isValid());
        assertEquals(1, validationResult.getWarningCount());
        assertTrue(validationResult.hasWarnings());
        assertEquals(warning, validationResult.getWarnings().get(0));
    }
    
    @Test
    @DisplayName("Should set message type and version")
    void testSetMessageTypeAndVersion() {
        validationResult.setMessageType("ADT^A01");
        validationResult.setMessageVersion("2.4");
        
        assertEquals("ADT^A01", validationResult.getMessageType());
        assertEquals("2.4", validationResult.getMessageVersion());
    }
    
    @Test
    @DisplayName("Should handle multiple errors and warnings")
    void testMultipleErrorsAndWarnings() {
        ValidationError error1 = new ValidationError("field1", "Error 1");
        ValidationError error2 = new ValidationError("field2", "Error 2");
        ValidationWarning warning1 = new ValidationWarning("field3", "Warning 1");
        ValidationWarning warning2 = new ValidationWarning("field4", "Warning 2");
        
        validationResult.addError(error1);
        validationResult.addError(error2);
        validationResult.addWarning(warning1);
        validationResult.addWarning(warning2);
        
        assertFalse(validationResult.isValid());
        assertEquals(2, validationResult.getErrorCount());
        assertEquals(2, validationResult.getWarningCount());
        assertTrue(validationResult.hasErrors());
        assertTrue(validationResult.hasWarnings());
    }
    
    @Test
    @DisplayName("Should test equality correctly")
    void testEquals() {
        ValidationResult result1 = new ValidationResult();
        ValidationResult result2 = new ValidationResult();
        
        result1.setMessageType("ADT^A01");
        result1.setMessageVersion("2.4");
        result2.setMessageType("ADT^A01");
        result2.setMessageVersion("2.4");
        
        assertEquals(result1, result2);
        
        result2.setMessageType("ORM^O01");
        assertNotEquals(result1, result2);
    }
    
    @Test
    @DisplayName("Should generate consistent hash codes")
    void testHashCode() {
        ValidationResult result1 = new ValidationResult();
        ValidationResult result2 = new ValidationResult();
        
        result1.setMessageType("ADT^A01");
        result2.setMessageType("ADT^A01");
        
        assertEquals(result1.hashCode(), result2.hashCode());
    }
    
    @Test
    @DisplayName("Should generate meaningful toString")
    void testToString() {
        validationResult.setMessageType("ADT^A01");
        validationResult.setMessageVersion("2.4");
        validationResult.addError(new ValidationError("field1", "Error"));
        validationResult.addWarning(new ValidationWarning("field2", "Warning"));
        
        String toString = validationResult.toString();
        
        assertTrue(toString.contains("ADT^A01"));
        assertTrue(toString.contains("2.4"));
        assertTrue(toString.contains("errorCount=1"));
        assertTrue(toString.contains("warningCount=1"));
        assertTrue(toString.contains("valid=false"));
    }
}