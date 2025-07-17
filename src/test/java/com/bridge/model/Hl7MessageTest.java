package com.bridge.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Hl7MessageTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidHl7Message() {
        Hl7Message message = new Hl7Message("MSG001", "ADT^A01", "EHR_SYSTEM", "FHIR_BRIDGE", 
                "MSH|^~\\&|EHR_SYSTEM|HOSPITAL|FHIR_BRIDGE|BRIDGE|20240115103000||ADT^A01|MSG001|P|2.4");
        
        Set<ConstraintViolation<Hl7Message>> violations = validator.validate(message);
        assertTrue(violations.isEmpty(), "Valid message should have no validation errors");
    }

    @Test
    void testMissingMessageId() {
        Hl7Message message = new Hl7Message();
        message.setMessageType("ADT^A01");
        message.setSendingApplication("EHR_SYSTEM");
        message.setReceivingApplication("FHIR_BRIDGE");
        message.setRawMessage("MSH|^~\\&|EHR_SYSTEM|HOSPITAL|FHIR_BRIDGE|BRIDGE|20240115103000||ADT^A01|MSG001|P|2.4");
        
        Set<ConstraintViolation<Hl7Message>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Message ID is required")));
    }

    @Test
    void testBlankMessageId() {
        Hl7Message message = new Hl7Message();
        message.setMessageId("");
        message.setMessageType("ADT^A01");
        message.setSendingApplication("EHR_SYSTEM");
        message.setReceivingApplication("FHIR_BRIDGE");
        message.setRawMessage("MSH|^~\\&|EHR_SYSTEM|HOSPITAL|FHIR_BRIDGE|BRIDGE|20240115103000||ADT^A01|MSG001|P|2.4");
        
        Set<ConstraintViolation<Hl7Message>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Message ID is required")));
    }

    @Test
    void testMessageIdTooLong() {
        Hl7Message message = new Hl7Message();
        message.setMessageId("A".repeat(101)); // 101 characters
        message.setMessageType("ADT^A01");
        message.setSendingApplication("EHR_SYSTEM");
        message.setReceivingApplication("FHIR_BRIDGE");
        message.setRawMessage("MSH|^~\\&|EHR_SYSTEM|HOSPITAL|FHIR_BRIDGE|BRIDGE|20240115103000||ADT^A01|MSG001|P|2.4");
        
        Set<ConstraintViolation<Hl7Message>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 100 characters")));
    }

    @Test
    void testMissingMessageType() {
        Hl7Message message = new Hl7Message();
        message.setMessageId("MSG001");
        message.setSendingApplication("EHR_SYSTEM");
        message.setReceivingApplication("FHIR_BRIDGE");
        message.setRawMessage("MSH|^~\\&|EHR_SYSTEM|HOSPITAL|FHIR_BRIDGE|BRIDGE|20240115103000||ADT^A01|MSG001|P|2.4");
        
        Set<ConstraintViolation<Hl7Message>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Message type is required")));
    }

    @Test
    void testMissingRawMessage() {
        Hl7Message message = new Hl7Message();
        message.setMessageId("MSG001");
        message.setMessageType("ADT^A01");
        message.setSendingApplication("EHR_SYSTEM");
        message.setReceivingApplication("FHIR_BRIDGE");
        
        Set<ConstraintViolation<Hl7Message>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Raw message content is required")));
    }

    @Test
    void testRawMessageTooLong() {
        Hl7Message message = new Hl7Message();
        message.setMessageId("MSG001");
        message.setMessageType("ADT^A01");
        message.setSendingApplication("EHR_SYSTEM");
        message.setReceivingApplication("FHIR_BRIDGE");
        message.setRawMessage("A".repeat(10001)); // 10001 characters
        
        Set<ConstraintViolation<Hl7Message>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 10000 characters")));
    }

    @Test
    void testDefaultConstructor() {
        Hl7Message message = new Hl7Message();
        
        assertNotNull(message.getTimestamp());
        assertEquals(MessageStatus.RECEIVED, message.getStatus());
        assertTrue(message.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testConstructorWithRequiredFields() {
        String messageId = "MSG001";
        String messageType = "ADT^A01";
        String sendingApp = "EHR_SYSTEM";
        String receivingApp = "FHIR_BRIDGE";
        String rawMessage = "MSH|^~\\&|EHR_SYSTEM|HOSPITAL|FHIR_BRIDGE|BRIDGE|20240115103000||ADT^A01|MSG001|P|2.4";
        
        Hl7Message message = new Hl7Message(messageId, messageType, sendingApp, receivingApp, rawMessage);
        
        assertEquals(messageId, message.getMessageId());
        assertEquals(messageType, message.getMessageType());
        assertEquals(sendingApp, message.getSendingApplication());
        assertEquals(receivingApp, message.getReceivingApplication());
        assertEquals(rawMessage, message.getRawMessage());
        assertEquals(MessageStatus.RECEIVED, message.getStatus());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        Hl7Message message1 = new Hl7Message("MSG001", "ADT^A01", "EHR_SYSTEM", "FHIR_BRIDGE", "raw_message");
        Hl7Message message2 = new Hl7Message("MSG001", "ORM^O01", "OTHER_SYSTEM", "FHIR_BRIDGE", "other_message");
        Hl7Message message3 = new Hl7Message("MSG002", "ADT^A01", "EHR_SYSTEM", "FHIR_BRIDGE", "raw_message");
        
        assertEquals(message1, message2); // Same message ID
        assertNotEquals(message1, message3); // Different message ID
        assertEquals(message1.hashCode(), message2.hashCode());
        assertNotEquals(message1.hashCode(), message3.hashCode());
    }

    @Test
    void testToString() {
        Hl7Message message = new Hl7Message("MSG001", "ADT^A01", "EHR_SYSTEM", "FHIR_BRIDGE", "raw_message");
        String toString = message.toString();
        
        assertTrue(toString.contains("MSG001"));
        assertTrue(toString.contains("ADT^A01"));
        assertTrue(toString.contains("EHR_SYSTEM"));
        assertTrue(toString.contains("FHIR_BRIDGE"));
    }
}