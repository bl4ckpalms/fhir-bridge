package com.bridge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Domain model representing patient consent information
 */
public class ConsentRecord {
    
    @NotBlank(message = "Patient ID is required")
    @Size(max = 100, message = "Patient ID must not exceed 100 characters")
    private String patientId;
    
    @NotBlank(message = "Organization ID is required")
    @Size(max = 100, message = "Organization ID must not exceed 100 characters")
    private String organizationId;
    
    @NotNull(message = "Consent status is required")
    private ConsentStatus status;
    
    @Valid
    @NotNull(message = "Allowed categories list is required")
    private List<DataCategory> allowedCategories;
    
    @NotNull(message = "Effective date is required")
    private LocalDateTime effectiveDate;
    
    private LocalDateTime expirationDate;
    
    @Size(max = 500, message = "Policy reference must not exceed 500 characters")
    private String policyReference;

    // Default constructor
    public ConsentRecord() {
        this.allowedCategories = new ArrayList<>();
        this.status = ConsentStatus.PENDING;
        this.effectiveDate = LocalDateTime.now();
    }

    // Constructor with required fields
    public ConsentRecord(String patientId, String organizationId, ConsentStatus status) {
        this();
        this.patientId = patientId;
        this.organizationId = organizationId;
        this.status = status;
    }

    // Constructor with expiration date
    public ConsentRecord(String patientId, String organizationId, ConsentStatus status, 
                        LocalDateTime effectiveDate, LocalDateTime expirationDate) {
        this(patientId, organizationId, status);
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
    }

    // Getters and setters
    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public void setStatus(ConsentStatus status) {
        this.status = status;
    }

    public List<DataCategory> getAllowedCategories() {
        return allowedCategories;
    }

    public void setAllowedCategories(List<DataCategory> allowedCategories) {
        this.allowedCategories = allowedCategories != null ? allowedCategories : new ArrayList<>();
    }

    public LocalDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDateTime effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getPolicyReference() {
        return policyReference;
    }

    public void setPolicyReference(String policyReference) {
        this.policyReference = policyReference;
    }

    // Utility methods
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == ConsentStatus.ACTIVE && 
               effectiveDate != null && effectiveDate.isBefore(now) &&
               (expirationDate == null || expirationDate.isAfter(now));
    }

    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDateTime.now());
    }

    public void addAllowedCategory(DataCategory category) {
        if (allowedCategories == null) {
            allowedCategories = new ArrayList<>();
        }
        if (!allowedCategories.contains(category)) {
            allowedCategories.add(category);
        }
    }

    public void removeAllowedCategory(DataCategory category) {
        if (allowedCategories != null) {
            allowedCategories.remove(category);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsentRecord that = (ConsentRecord) o;
        return Objects.equals(patientId, that.patientId) && 
               Objects.equals(organizationId, that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId, organizationId);
    }

    @Override
    public String toString() {
        return "ConsentRecord{" +
                "patientId='" + patientId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", status=" + status +
                ", allowedCategories=" + allowedCategories +
                ", effectiveDate=" + effectiveDate +
                ", expirationDate=" + expirationDate +
                ", policyReference='" + policyReference + '\'' +
                '}';
    }
}