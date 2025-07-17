package com.bridge.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationError
 */
class ValidationErrorTest {
    
    @Test
    @DisplayName("Should initialize with default constructor")
    void testDefaultConstructor() {
        ValidationError error = new ValidationError();
        
        assertNull(error.getField());
        assertNull(error.getSegment());
        assertNull(error.getCode());
        assertNull(error.getMessage());
        assertEquals(ValidationSeverity.ERROR, error.getSeverity());
        assertNull(error.getLocation());
        assertNull(error.getActualValue());
        assertNull(error.getExpectedValue());
    }
    
    @Test
    @DisplayName("Should initialize with field and message")
    void testConstructorWithFieldAndMessage() {
        ValidationError error = new ValidationError("testField", "Test message");
        
        assertEquals("testField", error.getField());
        assertEquals("Test message", error.getMessage());
        assertEquals(ValidationSeverity.ERROR, error.getSeverity());
    }
    
    @Test
    @DisplayName("Should initialize with field, segment and message")
    void testConstructorWithFieldSegmentAndMessage() {
        ValidationError error = new ValidationError("testField", "MSH", "Test message");
        
        assertEquals("testField", error.getField());
        assertEquals("MSH", error.getSegment());
        assertEquals("Test message", error.getMessage());
        assertEquals(ValidationSeverity.ERROR, error.getSeverity());
    }
    
    @Test
    @DisplayName("Should initialize with all parameters")
    void testConstructorWithAllParameters() {
        ValidationError error = new ValidationError("testField", "MSH", "ERR001", "Test message");
        
        assertEquals("testField", error.getField());
        assertEquals("MSH", error.getSegment());
        assertEquals("ERR001", error.getCode());
        assertEquals("Test message", error.getMessage());
        assertEquals(ValidationSeverity.ERROR, error.getSeverity());
    }
    
    @Test
    @DisplayName("Should set and get all properties")
    void testSettersAndGetters() {
        ValidationError error = new ValidationError();
        
        error.setField("testField");
        error.setSegment("PID");
        error.setCode("ERR002");
        error.setMessage("Test error message");
        error.setSeverity(ValidationSeverity.FATAL);
        error.setLocation("PID-3");
        error.setActualValue("invalid");
        error.setExpectedValue("valid");
        
        assertEquals("testField", error.getField());
        assertEquals("PID", error.getSegment());
        assertEquals("ERR002", error.getCode());
        assertEquals("Test error message", error.getMessage());
        assertEquals(ValidationSeverity.FATAL, error.getSeverity());
        assertEquals("PID-3", error.getLocation());
        assertEquals("invalid", error.getActualValue());
        assertEquals("valid", error.getExpectedValue());
    }
    
    @Test
    @DisplayName("Should test equality correctly")
    void testEquals() {
        ValidationError error1 = new ValidationError("field1", "MSH", "ERR001", "Message 1");
        ValidationError error2 = new ValidationError("field1", "MSH", "ERR001", "Message 1");
        ValidationError error3 = new ValidationError("field2", "MSH", "ERR001", "Message 1");
        
        assertEquals(error1, error2);
        assertNotEquals(error1, error3);
        assertNotEquals(error1, null);
        assertNotEquals(error1, "not an error");
    }
    
    @Test
    @DisplayName("Should generate consistent hash codes")
    void testHashCode() {
        ValidationError error1 = new ValidationError("field1", "MSH", "ERR001", "Message 1");
        ValidationError error2 = new ValidationError("field1", "MSH", "ERR001", "Message 1");
        
        assertEquals(error1.hashCode(), error2.hashCode());
    }
    
    @Test
    @DisplayName("Should generate meaningful toString with minimal fields")
    void testToStringMinimal() {
        ValidationError error = new ValidationError("testField", "Test message");
        
        String toString = error.toString();
        
        assertTrue(toString.contains("field='testField'"));
        assertTrue(toString.contains("message='Test message'"));
        assertTrue(toString.contains("severity=ERROR"));
    }
    
    @Test
    @DisplayName("Should generate meaningful toString with all fields")
    void testToStringComplete() {
        ValidationError error = new ValidationError("testField", "MSH", "ERR001", "Test message");
        error.setActualValue("actual");
        error.setExpectedValue("expected");
        
        String toString = error.toString();
        
        assertTrue(toString.contains("segment='MSH'"));
        assertTrue(toString.contains("field='testField'"));
        assertTrue(toString.contains("code='ERR001'"));
        assertTrue(toString.contains("message='Test message'"));
        assertTrue(toString.contains("actualValue='actual'"));
        assertTrue(toString.contains("expectedValue='expected'"));
        assertTrue(toString.contains("severity=ERROR"));
    }
}