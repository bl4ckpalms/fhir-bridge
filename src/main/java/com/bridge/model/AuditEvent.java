package com.bridge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing an audit event for compliance tracking
 */
public class AuditEvent {
    
    @NotBlank(message = "Event ID is required")
    @Size(max = 100, message = "Event ID must not exceed 100 characters")
    private String eventId;
    
    @Size(max = 100, message = "User ID must not exceed 100 characters")
    private String userId;
    
    @NotBlank(message = "Action is required")
    @Size(max = 100, message = "Action must not exceed 100 characters")
    private String action;
    
    @Size(max = 50, message = "Resource type must not exceed 50 characters")
    private String resourceType;
    
    @Size(max = 100, message = "Resource ID must not exceed 100 characters")
    private String resourceId;
    
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;
    
    @NotBlank(message = "Outcome is required")
    @Size(max = 20, message = "Outcome must not exceed 20 characters")
    private String outcome;
    
    private Map<String, Object> details;

    // Default constructor
    public AuditEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.details = new HashMap<>();
        this.outcome = "SUCCESS";
    }

    // Constructor with required fields
    public AuditEvent(String action, String outcome) {
        this();
        this.action = action;
        this.outcome = outcome;
    }

    // Constructor with user and resource information
    public AuditEvent(String userId, String action, String resourceType, String resourceId, String outcome) {
        this(action, outcome);
        this.userId = userId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    // Getters and setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? details : new HashMap<>();
    }

    // Utility methods
    public void addDetail(String key, Object value) {
        if (details == null) {
            details = new HashMap<>();
        }
        details.put(key, value);
    }

    public void removeDetail(String key) {
        if (details != null) {
            details.remove(key);
        }
    }

    public Object getDetail(String key) {
        return details != null ? details.get(key) : null;
    }

    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(outcome);
    }

    public boolean isFailure() {
        return "FAILURE".equalsIgnoreCase(outcome) || "ERROR".equalsIgnoreCase(outcome);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditEvent that = (AuditEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "AuditEvent{" +
                "eventId='" + eventId + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", timestamp=" + timestamp +
                ", outcome='" + outcome + '\'' +
                ", detailsCount=" + (details != null ? details.size() : 0) +
                '}';
    }
}