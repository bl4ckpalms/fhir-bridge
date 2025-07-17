package com.bridge.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of consent verification containing verification status and details
 */
public class ConsentVerificationResult {
    
    private boolean isValid;
    private String patientId;
    private String organizationId;
    private ConsentStatus consentStatus;
    private List<DataCategory> allowedCategories;
    private List<DataCategory> deniedCategories;
    private LocalDateTime effectiveDate;
    private LocalDateTime expirationDate;
    private String reason;
    private String policyReference;
    private LocalDateTime verificationTimestamp;

    // Default constructor
    public ConsentVerificationResult() {
        this.allowedCategories = new ArrayList<>();
        this.deniedCategories = new ArrayList<>();
        this.verificationTimestamp = LocalDateTime.now();
    }

    // Constructor for valid consent
    public ConsentVerificationResult(String patientId, String organizationId, 
                                   ConsentStatus consentStatus, List<DataCategory> allowedCategories) {
        this();
        this.isValid = true;
        this.patientId = patientId;
        this.organizationId = organizationId;
        this.consentStatus = consentStatus;
        this.allowedCategories = allowedCategories != null ? allowedCategories : new ArrayList<>();
    }

    // Constructor for invalid consent
    public ConsentVerificationResult(String patientId, String organizationId, String reason) {
        this();
        this.isValid = false;
        this.patientId = patientId;
        this.organizationId = organizationId;
        this.reason = reason;
    }

    // Static factory methods
    public static ConsentVerificationResult valid(String patientId, String organizationId, 
                                                 ConsentRecord consentRecord) {
        ConsentVerificationResult result = new ConsentVerificationResult(
            patientId, organizationId, consentRecord.getStatus(), consentRecord.getAllowedCategories());
        result.setEffectiveDate(consentRecord.getEffectiveDate());
        result.setExpirationDate(consentRecord.getExpirationDate());
        result.setPolicyReference(consentRecord.getPolicyReference());
        return result;
    }

    public static ConsentVerificationResult invalid(String patientId, String organizationId, String reason) {
        return new ConsentVerificationResult(patientId, organizationId, reason);
    }

    public static ConsentVerificationResult expired(String patientId, String organizationId, 
                                                   LocalDateTime expirationDate) {
        ConsentVerificationResult result = new ConsentVerificationResult(
            patientId, organizationId, "Consent has expired on " + expirationDate);
        result.setConsentStatus(ConsentStatus.EXPIRED);
        result.setExpirationDate(expirationDate);
        return result;
    }

    public static ConsentVerificationResult notFound(String patientId, String organizationId) {
        return new ConsentVerificationResult(patientId, organizationId, 
            "No consent record found for patient and organization");
    }

    public static ConsentVerificationResult revoked(String patientId, String organizationId) {
        ConsentVerificationResult result = new ConsentVerificationResult(
            patientId, organizationId, "Consent has been revoked");
        result.setConsentStatus(ConsentStatus.REVOKED);
        return result;
    }

    // Getters and setters
    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

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

    public ConsentStatus getConsentStatus() {
        return consentStatus;
    }

    public void setConsentStatus(ConsentStatus consentStatus) {
        this.consentStatus = consentStatus;
    }

    public List<DataCategory> getAllowedCategories() {
        return allowedCategories;
    }

    public void setAllowedCategories(List<DataCategory> allowedCategories) {
        this.allowedCategories = allowedCategories != null ? allowedCategories : new ArrayList<>();
    }

    public List<DataCategory> getDeniedCategories() {
        return deniedCategories;
    }

    public void setDeniedCategories(List<DataCategory> deniedCategories) {
        this.deniedCategories = deniedCategories != null ? deniedCategories : new ArrayList<>();
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPolicyReference() {
        return policyReference;
    }

    public void setPolicyReference(String policyReference) {
        this.policyReference = policyReference;
    }

    public LocalDateTime getVerificationTimestamp() {
        return verificationTimestamp;
    }

    public void setVerificationTimestamp(LocalDateTime verificationTimestamp) {
        this.verificationTimestamp = verificationTimestamp;
    }

    // Utility methods
    public boolean hasCategory(DataCategory category) {
        return allowedCategories != null && allowedCategories.contains(category);
    }

    public boolean hasAllCategories(List<DataCategory> categories) {
        return allowedCategories != null && allowedCategories.containsAll(categories);
    }

    public void addAllowedCategory(DataCategory category) {
        if (allowedCategories == null) {
            allowedCategories = new ArrayList<>();
        }
        if (!allowedCategories.contains(category)) {
            allowedCategories.add(category);
        }
    }

    public void addDeniedCategory(DataCategory category) {
        if (deniedCategories == null) {
            deniedCategories = new ArrayList<>();
        }
        if (!deniedCategories.contains(category)) {
            deniedCategories.add(category);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsentVerificationResult that = (ConsentVerificationResult) o;
        return isValid == that.isValid &&
               Objects.equals(patientId, that.patientId) &&
               Objects.equals(organizationId, that.organizationId) &&
               consentStatus == that.consentStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isValid, patientId, organizationId, consentStatus);
    }

    @Override
    public String toString() {
        return "ConsentVerificationResult{" +
                "isValid=" + isValid +
                ", patientId='" + patientId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", consentStatus=" + consentStatus +
                ", allowedCategories=" + allowedCategories +
                ", deniedCategories=" + deniedCategories +
                ", effectiveDate=" + effectiveDate +
                ", expirationDate=" + expirationDate +
                ", reason='" + reason + '\'' +
                ", policyReference='" + policyReference + '\'' +
                ", verificationTimestamp=" + verificationTimestamp +
                '}';
    }
}