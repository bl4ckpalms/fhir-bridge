package com.bridge.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConsentRecordTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidConsentRecord() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS, DataCategory.CLINICAL_NOTES));
        
        Set<ConstraintViolation<ConsentRecord>> violations = validator.validate(consent);
        assertTrue(violations.isEmpty(), "Valid consent record should have no validation errors");
    }

    @Test
    void testMissingPatientId() {
        ConsentRecord consent = new ConsentRecord();
        consent.setOrganizationId("org-456");
        consent.setStatus(ConsentStatus.ACTIVE);
        
        Set<ConstraintViolation<ConsentRecord>> violations = validator.validate(consent);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Patient ID is required")));
    }

    @Test
    void testBlankPatientId() {
        ConsentRecord consent = new ConsentRecord();
        consent.setPatientId("");
        consent.setOrganizationId("org-456");
        consent.setStatus(ConsentStatus.ACTIVE);
        
        Set<ConstraintViolation<ConsentRecord>> violations = validator.validate(consent);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Patient ID is required")));
    }

    @Test
    void testPatientIdTooLong() {
        ConsentRecord consent = new ConsentRecord();
        consent.setPatientId("A".repeat(101)); // 101 characters
        consent.setOrganizationId("org-456");
        consent.setStatus(ConsentStatus.ACTIVE);
        
        Set<ConstraintViolation<ConsentRecord>> violations = validator.validate(consent);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 100 characters")));
    }

    @Test
    void testMissingOrganizationId() {
        ConsentRecord consent = new ConsentRecord();
        consent.setPatientId("patient-123");
        consent.setStatus(ConsentStatus.ACTIVE);
        
        Set<ConstraintViolation<ConsentRecord>> violations = validator.validate(consent);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Organization ID is required")));
    }

    @Test
    void testMissingConsentStatus() {
        ConsentRecord consent = new ConsentRecord();
        consent.setPatientId("patient-123");
        consent.setOrganizationId("org-456");
        consent.setStatus(null); // Explicitly set to null to override default
        
        Set<ConstraintViolation<ConsentRecord>> violations = validator.validate(consent);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Consent status is required")));
    }

    @Test
    void testMissingEffectiveDate() {
        ConsentRecord consent = new ConsentRecord();
        consent.setPatientId("patient-123");
        consent.setOrganizationId("org-456");
        consent.setStatus(ConsentStatus.ACTIVE);
        consent.setEffectiveDate(null);
        
        Set<ConstraintViolation<ConsentRecord>> violations = validator.validate(consent);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Effective date is required")));
    }

    @Test
    void testPolicyReferenceTooLong() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setPolicyReference("A".repeat(501)); // 501 characters
        
        Set<ConstraintViolation<ConsentRecord>> violations = validator.validate(consent);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 500 characters")));
    }

    @Test
    void testDefaultConstructor() {
        ConsentRecord consent = new ConsentRecord();
        
        assertNotNull(consent.getAllowedCategories());
        assertTrue(consent.getAllowedCategories().isEmpty());
        assertEquals(ConsentStatus.PENDING, consent.getStatus());
        assertNotNull(consent.getEffectiveDate());
        assertTrue(consent.getEffectiveDate().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testConstructorWithRequiredFields() {
        String patientId = "patient-123";
        String organizationId = "org-456";
        ConsentStatus status = ConsentStatus.ACTIVE;
        
        ConsentRecord consent = new ConsentRecord(patientId, organizationId, status);
        
        assertEquals(patientId, consent.getPatientId());
        assertEquals(organizationId, consent.getOrganizationId());
        assertEquals(status, consent.getStatus());
        assertNotNull(consent.getAllowedCategories());
        assertNotNull(consent.getEffectiveDate());
    }

    @Test
    void testConstructorWithDates() {
        String patientId = "patient-123";
        String organizationId = "org-456";
        ConsentStatus status = ConsentStatus.ACTIVE;
        LocalDateTime effectiveDate = LocalDateTime.now().minusDays(1);
        LocalDateTime expirationDate = LocalDateTime.now().plusDays(30);
        
        ConsentRecord consent = new ConsentRecord(patientId, organizationId, status, effectiveDate, expirationDate);
        
        assertEquals(patientId, consent.getPatientId());
        assertEquals(organizationId, consent.getOrganizationId());
        assertEquals(status, consent.getStatus());
        assertEquals(effectiveDate, consent.getEffectiveDate());
        assertEquals(expirationDate, consent.getExpirationDate());
    }

    @Test
    void testIsActiveWhenActive() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setEffectiveDate(LocalDateTime.now().minusDays(1));
        consent.setExpirationDate(LocalDateTime.now().plusDays(30));
        
        assertTrue(consent.isActive());
    }

    @Test
    void testIsActiveWhenInactive() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.INACTIVE);
        consent.setEffectiveDate(LocalDateTime.now().minusDays(1));
        consent.setExpirationDate(LocalDateTime.now().plusDays(30));
        
        assertFalse(consent.isActive());
    }

    @Test
    void testIsActiveWhenNotYetEffective() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setEffectiveDate(LocalDateTime.now().plusDays(1));
        consent.setExpirationDate(LocalDateTime.now().plusDays(30));
        
        assertFalse(consent.isActive());
    }

    @Test
    void testIsActiveWhenExpired() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setEffectiveDate(LocalDateTime.now().minusDays(30));
        consent.setExpirationDate(LocalDateTime.now().minusDays(1));
        
        assertFalse(consent.isActive());
    }

    @Test
    void testIsExpiredWhenExpired() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setExpirationDate(LocalDateTime.now().minusDays(1));
        
        assertTrue(consent.isExpired());
    }

    @Test
    void testIsExpiredWhenNotExpired() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setExpirationDate(LocalDateTime.now().plusDays(30));
        
        assertFalse(consent.isExpired());
    }

    @Test
    void testIsExpiredWhenNoExpirationDate() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setExpirationDate(null);
        
        assertFalse(consent.isExpired());
    }

    @Test
    void testAddAllowedCategory() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        
        consent.addAllowedCategory(DataCategory.DEMOGRAPHICS);
        consent.addAllowedCategory(DataCategory.CLINICAL_NOTES);
        consent.addAllowedCategory(DataCategory.DEMOGRAPHICS); // Duplicate
        
        assertEquals(2, consent.getAllowedCategories().size());
        assertTrue(consent.getAllowedCategories().contains(DataCategory.DEMOGRAPHICS));
        assertTrue(consent.getAllowedCategories().contains(DataCategory.CLINICAL_NOTES));
    }

    @Test
    void testRemoveAllowedCategory() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.addAllowedCategory(DataCategory.DEMOGRAPHICS);
        consent.addAllowedCategory(DataCategory.CLINICAL_NOTES);
        
        consent.removeAllowedCategory(DataCategory.DEMOGRAPHICS);
        
        assertEquals(1, consent.getAllowedCategories().size());
        assertFalse(consent.getAllowedCategories().contains(DataCategory.DEMOGRAPHICS));
        assertTrue(consent.getAllowedCategories().contains(DataCategory.CLINICAL_NOTES));
    }

    @Test
    void testSetAllowedCategoriesWithNull() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setAllowedCategories(null);
        
        assertNotNull(consent.getAllowedCategories());
        assertTrue(consent.getAllowedCategories().isEmpty());
    }

    @Test
    void testEqualsAndHashCode() {
        ConsentRecord consent1 = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        ConsentRecord consent2 = new ConsentRecord("patient-123", "org-456", ConsentStatus.INACTIVE);
        ConsentRecord consent3 = new ConsentRecord("patient-789", "org-456", ConsentStatus.ACTIVE);
        ConsentRecord consent4 = new ConsentRecord("patient-123", "org-789", ConsentStatus.ACTIVE);
        
        assertEquals(consent1, consent2); // Same patient and organization
        assertNotEquals(consent1, consent3); // Different patient
        assertNotEquals(consent1, consent4); // Different organization
        assertEquals(consent1.hashCode(), consent2.hashCode());
        assertNotEquals(consent1.hashCode(), consent3.hashCode());
    }

    @Test
    void testToString() {
        ConsentRecord consent = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        consent.setPolicyReference("POLICY-001");
        String toString = consent.toString();
        
        assertTrue(toString.contains("patient-123"));
        assertTrue(toString.contains("org-456"));
        assertTrue(toString.contains("ACTIVE"));
        assertTrue(toString.contains("POLICY-001"));
    }
}