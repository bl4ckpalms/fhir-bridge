package com.bridge.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationWarning
 */
class ValidationWarningTest {
    
    @Test
    @DisplayName("Should initialize with default constructor")
    void testDefaultConstructor() {
        ValidationWarning warning = new ValidationWarning();
        
        assertNull(warning.getField());
        assertNull(warning.getSegment());
        assertNull(warning.getCode());
        assertNull(warning.getMessage());
        assertNull(warning.getLocation());
        assertNull(warning.getActualValue());
        assertNull(warning.getRecommendedValue());
    }
    
    @Test
    @DisplayName("Should initialize with field and message")
    void testConstructorWithFieldAndMessage() {
        ValidationWarning warning = new ValidationWarning("testField", "Test warning");
        
        assertEquals("testField", warning.getField());
        assertEquals("Test warning", warning.getMessage());
    }
    
    @Test
    @DisplayName("Should initialize with field, segment and message")
    void testConstructorWithFieldSegmentAndMessage() {
        ValidationWarning warning = new ValidationWarning("testField", "MSH", "Test warning");
        
        assertEquals("testField", warning.getField());
        assertEquals("MSH", warning.getSegment());
        assertEquals("Test warning", warning.getMessage());
    }
    
    @Test
    @DisplayName("Should initialize with all parameters")
    void testConstructorWithAllParameters() {
        ValidationWarning warning = new ValidationWarning("testField", "MSH", "WARN001", "Test warning");
        
        assertEquals("testField", warning.getField());
        assertEquals("MSH", warning.getSegment());
        assertEquals("WARN001", warning.getCode());
        assertEquals("Test warning", warning.getMessage());
    }
    
    @Test
    @DisplayName("Should set and get all properties")
    void testSettersAndGetters() {
        ValidationWarning warning = new ValidationWarning();
        
        warning.setField("testField");
        warning.setSegment("PID");
        warning.setCode("WARN002");
        warning.setMessage("Test warning message");
        warning.setLocation("PID-8");
        warning.setActualValue("X");
        warning.setRecommendedValue("M or F");
        
        assertEquals("testField", warning.getField());
        assertEquals("PID", warning.getSegment());
        assertEquals("WARN002", warning.getCode());
        assertEquals("Test warning message", warning.getMessage());
        assertEquals("PID-8", warning.getLocation());
        assertEquals("X", warning.getActualValue());
        assertEquals("M or F", warning.getRecommendedValue());
    }
    
    @Test
    @DisplayName("Should test equality correctly")
    void testEquals() {
        ValidationWarning warning1 = new ValidationWarning("field1", "MSH", "WARN001", "Message 1");
        ValidationWarning warning2 = new ValidationWarning("field1", "MSH", "WARN001", "Message 1");
        ValidationWarning warning3 = new ValidationWarning("field2", "MSH", "WARN001", "Message 1");
        
        assertEquals(warning1, warning2);
        assertNotEquals(warning1, warning3);
        assertNotEquals(warning1, null);
        assertNotEquals(warning1, "not a warning");
    }
    
    @Test
    @DisplayName("Should generate consistent hash codes")
    void testHashCode() {
        ValidationWarning warning1 = new ValidationWarning("field1", "MSH", "WARN001", "Message 1");
        ValidationWarning warning2 = new ValidationWarning("field1", "MSH", "WARN001", "Message 1");
        
        assertEquals(warning1.hashCode(), warning2.hashCode());
    }
    
    @Test
    @DisplayName("Should generate meaningful toString with minimal fields")
    void testToStringMinimal() {
        ValidationWarning warning = new ValidationWarning("testField", "Test warning");
        
        String toString = warning.toString();
        
        assertTrue(toString.contains("field='testField'"));
        assertTrue(toString.contains("message='Test warning'"));
    }
    
    @Test
    @DisplayName("Should generate meaningful toString with all fields")
    void testToStringComplete() {
        ValidationWarning warning = new ValidationWarning("testField", "MSH", "WARN001", "Test warning");
        warning.setActualValue("actual");
        warning.setRecommendedValue("recommended");
        
        String toString = warning.toString();
        
        assertTrue(toString.contains("segment='MSH'"));
        assertTrue(toString.contains("field='testField'"));
        assertTrue(toString.contains("code='WARN001'"));
        assertTrue(toString.contains("message='Test warning'"));
        assertTrue(toString.contains("actualValue='actual'"));
        assertTrue(toString.contains("recommendedValue='recommended'"));
    }
}