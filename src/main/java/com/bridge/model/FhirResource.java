package com.bridge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a FHIR R4 resource
 */
public class FhirResource {
    
    @NotBlank(message = "Resource ID is required")
    @Size(max = 100, message = "Resource ID must not exceed 100 characters")
    private String resourceId;
    
    @NotBlank(message = "Resource type is required")
    @Size(max = 50, message = "Resource type must not exceed 50 characters")
    private String resourceType;
    
    @NotBlank(message = "FHIR version is required")
    @Pattern(regexp = "^4\\.[0-9]+\\.[0-9]+$", message = "FHIR version must be in format 4.x.x")
    private String fhirVersion;
    
    @NotBlank(message = "JSON content is required")
    @Size(max = 50000, message = "JSON content must not exceed 50000 characters")
    private String jsonContent;
    
    @Size(max = 100, message = "Source message ID must not exceed 100 characters")
    private String sourceMessageId;
    
    @NotNull(message = "Created timestamp is required")
    private LocalDateTime createdAt;

    // Default constructor
    public FhirResource() {
        this.createdAt = LocalDateTime.now();
        this.fhirVersion = "4.0.1"; // Default to FHIR R4
    }

    // Constructor with required fields
    public FhirResource(String resourceId, String resourceType, String jsonContent) {
        this();
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.jsonContent = jsonContent;
    }

    // Constructor with source message ID
    public FhirResource(String resourceId, String resourceType, String jsonContent, String sourceMessageId) {
        this(resourceId, resourceType, jsonContent);
        this.sourceMessageId = sourceMessageId;
    }

    // Getters and setters
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public void setFhirVersion(String fhirVersion) {
        this.fhirVersion = fhirVersion;
    }

    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public String getSourceMessageId() {
        return sourceMessageId;
    }

    public void setSourceMessageId(String sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FhirResource that = (FhirResource) o;
        return Objects.equals(resourceId, that.resourceId) && 
               Objects.equals(resourceType, that.resourceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, resourceType);
    }

    @Override
    public String toString() {
        return "FhirResource{" +
                "resourceId='" + resourceId + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", fhirVersion='" + fhirVersion + '\'' +
                ", sourceMessageId='" + sourceMessageId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}