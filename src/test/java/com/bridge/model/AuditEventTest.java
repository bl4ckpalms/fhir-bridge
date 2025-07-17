package com.bridge.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidAuditEvent() {
        AuditEvent event = new AuditEvent("user-123", "TRANSFORM", "Patient", "patient-456", "SUCCESS");
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Valid audit event should have no validation errors");
    }

    @Test
    void testMissingAction() {
        AuditEvent event = new AuditEvent();
        event.setUserId("user-123");
        event.setOutcome("SUCCESS");
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Action is required")));
    }

    @Test
    void testBlankAction() {
        AuditEvent event = new AuditEvent();
        event.setAction("");
        event.setUserId("user-123");
        event.setOutcome("SUCCESS");
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Action is required")));
    }

    @Test
    void testActionTooLong() {
        AuditEvent event = new AuditEvent();
        event.setAction("A".repeat(101)); // 101 characters
        event.setUserId("user-123");
        event.setOutcome("SUCCESS");
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 100 characters")));
    }

    @Test
    void testMissingOutcome() {
        AuditEvent event = new AuditEvent();
        event.setAction("TRANSFORM");
        event.setUserId("user-123");
        event.setOutcome(null); // Explicitly set to null to override default
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Outcome is required")));
    }

    @Test
    void testBlankOutcome() {
        AuditEvent event = new AuditEvent();
        event.setAction("TRANSFORM");
        event.setUserId("user-123");
        event.setOutcome("");
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Outcome is required")));
    }

    @Test
    void testOutcomeTooLong() {
        AuditEvent event = new AuditEvent();
        event.setAction("TRANSFORM");
        event.setUserId("user-123");
        event.setOutcome("A".repeat(21)); // 21 characters
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 20 characters")));
    }

    @Test
    void testUserIdTooLong() {
        AuditEvent event = new AuditEvent();
        event.setAction("TRANSFORM");
        event.setUserId("A".repeat(101)); // 101 characters
        event.setOutcome("SUCCESS");
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 100 characters")));
    }

    @Test
    void testResourceTypeTooLong() {
        AuditEvent event = new AuditEvent();
        event.setAction("TRANSFORM");
        event.setUserId("user-123");
        event.setResourceType("A".repeat(51)); // 51 characters
        event.setOutcome("SUCCESS");
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 50 characters")));
    }

    @Test
    void testResourceIdTooLong() {
        AuditEvent event = new AuditEvent();
        event.setAction("TRANSFORM");
        event.setUserId("user-123");
        event.setResourceId("A".repeat(101)); // 101 characters
        event.setOutcome("SUCCESS");
        
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 100 characters")));
    }

    @Test
    void testDefaultConstructor() {
        AuditEvent event = new AuditEvent();
        
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertNotNull(event.getDetails());
        assertEquals("SUCCESS", event.getOutcome());
        assertTrue(event.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(event.getDetails().isEmpty());
    }

    @Test
    void testConstructorWithActionAndOutcome() {
        String action = "TRANSFORM";
        String outcome = "FAILURE";
        
        AuditEvent event = new AuditEvent(action, outcome);
        
        assertEquals(action, event.getAction());
        assertEquals(outcome, event.getOutcome());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertNotNull(event.getDetails());
    }

    @Test
    void testConstructorWithAllFields() {
        String userId = "user-123";
        String action = "TRANSFORM";
        String resourceType = "Patient";
        String resourceId = "patient-456";
        String outcome = "SUCCESS";
        
        AuditEvent event = new AuditEvent(userId, action, resourceType, resourceId, outcome);
        
        assertEquals(userId, event.getUserId());
        assertEquals(action, event.getAction());
        assertEquals(resourceType, event.getResourceType());
        assertEquals(resourceId, event.getResourceId());
        assertEquals(outcome, event.getOutcome());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testAddDetail() {
        AuditEvent event = new AuditEvent("TRANSFORM", "SUCCESS");
        
        event.addDetail("sourceSystem", "EHR_SYSTEM");
        event.addDetail("transformationTime", 150);
        
        assertEquals("EHR_SYSTEM", event.getDetail("sourceSystem"));
        assertEquals(150, event.getDetail("transformationTime"));
        assertEquals(2, event.getDetails().size());
    }

    @Test
    void testRemoveDetail() {
        AuditEvent event = new AuditEvent("TRANSFORM", "SUCCESS");
        event.addDetail("sourceSystem", "EHR_SYSTEM");
        event.addDetail("transformationTime", 150);
        
        event.removeDetail("sourceSystem");
        
        assertNull(event.getDetail("sourceSystem"));
        assertEquals(150, event.getDetail("transformationTime"));
        assertEquals(1, event.getDetails().size());
    }

    @Test
    void testGetDetailWhenNotExists() {
        AuditEvent event = new AuditEvent("TRANSFORM", "SUCCESS");
        
        assertNull(event.getDetail("nonExistentKey"));
    }

    @Test
    void testSetDetailsWithNull() {
        AuditEvent event = new AuditEvent("TRANSFORM", "SUCCESS");
        event.setDetails(null);
        
        assertNotNull(event.getDetails());
        assertTrue(event.getDetails().isEmpty());
    }

    @Test
    void testIsSuccessful() {
        AuditEvent successEvent = new AuditEvent("TRANSFORM", "SUCCESS");
        AuditEvent failureEvent = new AuditEvent("TRANSFORM", "FAILURE");
        AuditEvent errorEvent = new AuditEvent("TRANSFORM", "ERROR");
        AuditEvent customEvent = new AuditEvent("TRANSFORM", "CUSTOM");
        
        assertTrue(successEvent.isSuccessful());
        assertFalse(failureEvent.isSuccessful());
        assertFalse(errorEvent.isSuccessful());
        assertFalse(customEvent.isSuccessful());
    }

    @Test
    void testIsFailure() {
        AuditEvent successEvent = new AuditEvent("TRANSFORM", "SUCCESS");
        AuditEvent failureEvent = new AuditEvent("TRANSFORM", "FAILURE");
        AuditEvent errorEvent = new AuditEvent("TRANSFORM", "ERROR");
        AuditEvent customEvent = new AuditEvent("TRANSFORM", "CUSTOM");
        
        assertFalse(successEvent.isFailure());
        assertTrue(failureEvent.isFailure());
        assertTrue(errorEvent.isFailure());
        assertFalse(customEvent.isFailure());
    }

    @Test
    void testIsSuccessfulCaseInsensitive() {
        AuditEvent event1 = new AuditEvent("TRANSFORM", "success");
        AuditEvent event2 = new AuditEvent("TRANSFORM", "Success");
        AuditEvent event3 = new AuditEvent("TRANSFORM", "SUCCESS");
        
        assertTrue(event1.isSuccessful());
        assertTrue(event2.isSuccessful());
        assertTrue(event3.isSuccessful());
    }

    @Test
    void testIsFailureCaseInsensitive() {
        AuditEvent event1 = new AuditEvent("TRANSFORM", "failure");
        AuditEvent event2 = new AuditEvent("TRANSFORM", "Failure");
        AuditEvent event3 = new AuditEvent("TRANSFORM", "error");
        AuditEvent event4 = new AuditEvent("TRANSFORM", "Error");
        
        assertTrue(event1.isFailure());
        assertTrue(event2.isFailure());
        assertTrue(event3.isFailure());
        assertTrue(event4.isFailure());
    }

    @Test
    void testEqualsAndHashCode() {
        AuditEvent event1 = new AuditEvent("TRANSFORM", "SUCCESS");
        AuditEvent event2 = new AuditEvent("VALIDATE", "FAILURE");
        
        // Set same event ID for testing
        String eventId = "test-event-123";
        event1.setEventId(eventId);
        event2.setEventId(eventId);
        
        AuditEvent event3 = new AuditEvent("TRANSFORM", "SUCCESS");
        event3.setEventId("different-event-456");
        
        assertEquals(event1, event2); // Same event ID
        assertNotEquals(event1, event3); // Different event ID
        assertEquals(event1.hashCode(), event2.hashCode());
        assertNotEquals(event1.hashCode(), event3.hashCode());
    }

    @Test
    void testToString() {
        AuditEvent event = new AuditEvent("user-123", "TRANSFORM", "Patient", "patient-456", "SUCCESS");
        event.addDetail("sourceSystem", "EHR_SYSTEM");
        String toString = event.toString();
        
        assertTrue(toString.contains("user-123"));
        assertTrue(toString.contains("TRANSFORM"));
        assertTrue(toString.contains("Patient"));
        assertTrue(toString.contains("patient-456"));
        assertTrue(toString.contains("SUCCESS"));
        assertTrue(toString.contains("detailsCount=1"));
    }
}