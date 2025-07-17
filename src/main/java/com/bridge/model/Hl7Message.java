package com.bridge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing an HL7 v2 message
 */
public class Hl7Message {
    
    @NotBlank(message = "Message ID is required")
    @Size(max = 100, message = "Message ID must not exceed 100 characters")
    private String messageId;
    
    @NotBlank(message = "Message type is required")
    @Size(max = 10, message = "Message type must not exceed 10 characters")
    private String messageType;
    
    @NotBlank(message = "Sending application is required")
    @Size(max = 100, message = "Sending application must not exceed 100 characters")
    private String sendingApplication;
    
    @NotBlank(message = "Receiving application is required")
    @Size(max = 100, message = "Receiving application must not exceed 100 characters")
    private String receivingApplication;
    
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;
    
    @NotBlank(message = "Raw message content is required")
    @Size(max = 10000, message = "Raw message must not exceed 10000 characters")
    private String rawMessage;
    
    @NotNull(message = "Message status is required")
    private MessageStatus status;

    // Default constructor
    public Hl7Message() {
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.RECEIVED;
    }

    // Constructor with required fields
    public Hl7Message(String messageId, String messageType, String sendingApplication, 
                      String receivingApplication, String rawMessage) {
        this();
        this.messageId = messageId;
        this.messageType = messageType;
        this.sendingApplication = sendingApplication;
        this.receivingApplication = receivingApplication;
        this.rawMessage = rawMessage;
    }

    // Getters and setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSendingApplication() {
        return sendingApplication;
    }

    public void setSendingApplication(String sendingApplication) {
        this.sendingApplication = sendingApplication;
    }

    public String getReceivingApplication() {
        return receivingApplication;
    }

    public void setReceivingApplication(String receivingApplication) {
        this.receivingApplication = receivingApplication;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hl7Message that = (Hl7Message) o;
        return Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "Hl7Message{" +
                "messageId='" + messageId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", sendingApplication='" + sendingApplication + '\'' +
                ", receivingApplication='" + receivingApplication + '\'' +
                ", timestamp=" + timestamp +
                ", status=" + status +
                '}';
    }
}