package com.bridge.entity;

import com.bridge.model.ConsentStatus;
import com.bridge.model.DataCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity for patient consent records
 */
@Entity
@Table(name = "consent_records", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "organization_id"}),
       indexes = {
           @Index(name = "idx_consent_patient", columnList = "patient_id"),
           @Index(name = "idx_consent_org", columnList = "organization_id"),
           @Index(name = "idx_consent_status", columnList = "status"),
           @Index(name = "idx_consent_effective", columnList = "effective_date"),
           @Index(name = "idx_consent_expiration", columnList = "expiration_date")
       })
public class ConsentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Patient ID is required")
    @Size(max = 100, message = "Patient ID must not exceed 100 characters")
    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    @NotBlank(message = "Organization ID is required")
    @Size(max = 100, message = "Organization ID must not exceed 100 characters")
    @Column(name = "organization_id", nullable = false, length = 100)
    private String organizationId;

    @NotNull(message = "Consent status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConsentStatus status;

    @ElementCollection(targetClass = DataCategory.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "consent_allowed_categories", 
                     joinColumns = @JoinColumn(name = "consent_id"),
                     indexes = @Index(name = "idx_consent_categories", columnList = "consent_id"))
    @Column(name = "data_category", length = 50)
    private List<DataCategory> allowedCategories = new ArrayList<>();

    @NotNull(message = "Effective date is required")
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Size(max = 500, message = "Policy reference must not exceed 500 characters")
    @Column(name = "policy_reference", length = 500)
    private String policyReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Default constructor
    public ConsentEntity() {
        this.status = ConsentStatus.PENDING;
        this.effectiveDate = LocalDateTime.now();
    }

    // Constructor with required fields
    public ConsentEntity(String patientId, String organizationId, ConsentStatus status) {
        this();
        this.patientId = patientId;
        this.organizationId = organizationId;
        this.status = status;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
        // Create a new list if it's a Hibernate collection to avoid UnsupportedOperationException
        if (!(allowedCategories instanceof ArrayList)) {
            allowedCategories = new ArrayList<>(allowedCategories);
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
        ConsentEntity that = (ConsentEntity) o;
        return Objects.equals(id, that.id) && 
               Objects.equals(patientId, that.patientId) && 
               Objects.equals(organizationId, that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, patientId, organizationId);
    }

    @Override
    public String toString() {
        return "ConsentEntity{" +
                "id=" + id +
                ", patientId='" + patientId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", status=" + status +
                ", allowedCategories=" + allowedCategories +
                ", effectiveDate=" + effectiveDate +
                ", expirationDate=" + expirationDate +
                ", policyReference='" + policyReference + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}