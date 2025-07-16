package com.bridge.model;

import java.time.LocalDateTime;

/**
 * Domain model representing an HL7 v2 message
 */
public class Hl7Message {
    private String messageId;
    private String messageType;
    private String sendingApplication;
    private String receivingApplication;
    private LocalDateTime timestamp;
    private String rawMessage;
    private MessageStatus status;

    // Constructors, getters, and setters will be added in later tasks
}