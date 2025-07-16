package com.bridge.model;

import java.time.LocalDateTime;

/**
 * Domain model representing a FHIR R4 resource
 */
public class FhirResource {
    private String resourceId;
    private String resourceType;
    private String fhirVersion;
    private String jsonContent;
    private String sourceMessageId;
    private LocalDateTime createdAt;

    // Constructors, getters, and setters will be added in later tasks
}