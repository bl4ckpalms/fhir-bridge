package com.bridge.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain model representing an audit event for compliance tracking
 */
public class AuditEvent {
    private String eventId;
    private String userId;
    private String action;
    private String resourceType;
    private String resourceId;
    private LocalDateTime timestamp;
    private String outcome;
    private Map<String, Object> details;

    // Constructors, getters, and setters will be added in later tasks
}