package com.bridge.service;

import com.bridge.entity.ConsentEntity;
import com.bridge.model.*;
import com.bridge.repository.ConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentVerificationServiceImplTest {

    @Mock
    private ConsentRepository consentRepository;

    @InjectMocks
    private ConsentVerificationServiceImpl consentVerificationService;

    private ConsentEntity activeConsentEntity;
    private ConsentEntity expiredConsentEntity;
    private ConsentEntity revokedConsentEntity;
    private String patientId;
    private String organizationId;

    @BeforeEach
    void setUp() {
        patientId = "patient-123";
        organizationId = "org-456";

        // Active consent entity
        activeConsentEntity = new ConsentEntity();
        activeConsentEntity.setId(1L);
        activeConsentEntity.setPatientId(patientId);
        activeConsentEntity.setOrganizationId(organizationId);
        activeConsentEntity.setStatus(ConsentStatus.ACTIVE);
        activeConsentEntity.setEffectiveDate(LocalDateTime.now().minusDays(1));
        activeConsentEntity.setExpirationDate(LocalDateTime.now().plusDays(30));
        activeConsentEntity.setAllowedCategories(Arrays.asList(
            DataCategory.DEMOGRAPHICS, DataCategory.MEDICAL_HISTORY, DataCategory.LAB_RESULTS));
        activeConsentEntity.setPolicyReference("POLICY-001");

        // Expired consent entity
        expiredConsentEntity = new ConsentEntity();
        expiredConsentEntity.setId(2L);
        expiredConsentEntity.setPatientId(patientId);
        expiredConsentEntity.setOrganizationId(organizationId);
        expiredConsentEntity.setStatus(ConsentStatus.ACTIVE);
        expiredConsentEntity.setEffectiveDate(LocalDateTime.now().minusDays(10));
        expiredConsentEntity.setExpirationDate(LocalDateTime.now().minusDays(1));
        expiredConsentEntity.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));

        // Revoked consent entity
        revokedConsentEntity = new ConsentEntity();
        revokedConsentEntity.setId(3L);
        revokedConsentEntity.setPatientId(patientId);
        revokedConsentEntity.setOrganizationId(organizationId);
        revokedConsentEntity.setStatus(ConsentStatus.REVOKED);
        revokedConsentEntity.setEffectiveDate(LocalDateTime.now().minusDays(5));
        revokedConsentEntity.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));
    }

    @Test
    void verifyConsent_ValidActiveConsent_ReturnsValidResult() {
        // Arrange
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(activeConsentEntity));

        // Act
        ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);

        // Assert
        assertTrue(result.isValid());
        assertEquals(patientId, result.getPatientId());
        assertEquals(organizationId, result.getOrganizationId());
        assertEquals(ConsentStatus.ACTIVE, result.getConsentStatus());
        assertEquals(3, result.getAllowedCategories().size());
        assertTrue(result.getAllowedCategories().contains(DataCategory.DEMOGRAPHICS));
        assertTrue(result.getAllowedCategories().contains(DataCategory.MEDICAL_HISTORY));
        assertTrue(result.getAllowedCategories().contains(DataCategory.LAB_RESULTS));
        assertEquals("POLICY-001", result.getPolicyReference());
    }

    @Test
    void verifyConsent_ExpiredConsent_ReturnsExpiredResult() {
        // Arrange
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(expiredConsentEntity));
        when(consentRepository.save(any(ConsentEntity.class))).thenReturn(expiredConsentEntity);

        // Act
        ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);

        // Assert
        assertFalse(result.isValid());
        assertEquals(patientId, result.getPatientId());
        assertEquals(organizationId, result.getOrganizationId());
        assertTrue(result.getReason().contains("expired"));
        verify(consentRepository).save(any(ConsentEntity.class));
    }

    @Test
    void verifyConsent_RevokedConsent_ReturnsRevokedResult() {
        // Arrange
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(revokedConsentEntity));

        // Act
        ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);

        // Assert
        assertFalse(result.isValid());
        assertEquals(patientId, result.getPatientId());
        assertEquals(organizationId, result.getOrganizationId());
        assertEquals(ConsentStatus.REVOKED, result.getConsentStatus());
        assertTrue(result.getReason().contains("revoked"));
    }

    @Test
    void verifyConsent_NoConsentFound_ReturnsNotFoundResult() {
        // Arrange
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.empty());

        // Act
        ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);

        // Assert
        assertFalse(result.isValid());
        assertEquals(patientId, result.getPatientId());
        assertEquals(organizationId, result.getOrganizationId());
        assertTrue(result.getReason().contains("No consent record found"));
    }

    @Test
    void verifyConsent_InvalidParameters_ReturnsInvalidResult() {
        // Act & Assert
        ConsentVerificationResult result1 = consentVerificationService.verifyConsent(null, organizationId);
        assertFalse(result1.isValid());
        assertTrue(result1.getReason().contains("required"));

        ConsentVerificationResult result2 = consentVerificationService.verifyConsent(patientId, "");
        assertFalse(result2.isValid());
        assertTrue(result2.getReason().contains("required"));

        ConsentVerificationResult result3 = consentVerificationService.verifyConsent("", null);
        assertFalse(result3.isValid());
        assertTrue(result3.getReason().contains("required"));
    }

    @Test
    void verifyConsentForCategories_AllCategoriesAllowed_ReturnsValidResult() {
        // Arrange
        activeConsentEntity.setAllowedCategories(Arrays.asList(DataCategory.ALL));
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(activeConsentEntity));

        List<DataCategory> requestedCategories = Arrays.asList(
            DataCategory.DEMOGRAPHICS, DataCategory.MEDICAL_HISTORY, DataCategory.MEDICATIONS);

        // Act
        ConsentVerificationResult result = consentVerificationService
            .verifyConsentForCategories(patientId, organizationId, requestedCategories);

        // Assert
        assertTrue(result.isValid());
        assertEquals(3, result.getAllowedCategories().size());
        assertTrue(result.getAllowedCategories().containsAll(requestedCategories));
        assertTrue(result.getDeniedCategories().isEmpty());
    }

    @Test
    void verifyConsentForCategories_SomeCategoriesDenied_ReturnsPartialResult() {
        // Arrange
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(activeConsentEntity));

        List<DataCategory> requestedCategories = Arrays.asList(
            DataCategory.DEMOGRAPHICS, DataCategory.MEDICAL_HISTORY, DataCategory.MEDICATIONS, DataCategory.GENETIC_DATA);

        // Act
        ConsentVerificationResult result = consentVerificationService
            .verifyConsentForCategories(patientId, organizationId, requestedCategories);

        // Assert
        assertTrue(result.isValid());
        assertEquals(2, result.getAllowedCategories().size());
        assertTrue(result.getAllowedCategories().contains(DataCategory.DEMOGRAPHICS));
        assertTrue(result.getAllowedCategories().contains(DataCategory.MEDICAL_HISTORY));
        assertEquals(2, result.getDeniedCategories().size());
        assertTrue(result.getDeniedCategories().contains(DataCategory.MEDICATIONS));
        assertTrue(result.getDeniedCategories().contains(DataCategory.GENETIC_DATA));
        assertTrue(result.getReason().contains("Access denied for categories"));
    }

    @Test
    void verifyConsentForCategories_EmptyCategories_ReturnsInvalidResult() {
        // Act
        ConsentVerificationResult result = consentVerificationService
            .verifyConsentForCategories(patientId, organizationId, Arrays.asList());

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Data categories are required"));
    }

    @Test
    void getActiveConsent_ValidConsent_ReturnsConsentRecord() {
        // Arrange
        when(consentRepository.findActiveConsentByPatientIdAndOrganizationId(
            eq(patientId), eq(organizationId), any(LocalDateTime.class)))
            .thenReturn(Optional.of(activeConsentEntity));

        // Act
        Optional<ConsentRecord> result = consentVerificationService.getActiveConsent(patientId, organizationId);

        // Assert
        assertTrue(result.isPresent());
        ConsentRecord consentRecord = result.get();
        assertEquals(patientId, consentRecord.getPatientId());
        assertEquals(organizationId, consentRecord.getOrganizationId());
        assertEquals(ConsentStatus.ACTIVE, consentRecord.getStatus());
        assertEquals(3, consentRecord.getAllowedCategories().size());
    }

    @Test
    void getActiveConsent_NoActiveConsent_ReturnsEmpty() {
        // Arrange
        when(consentRepository.findActiveConsentByPatientIdAndOrganizationId(
            eq(patientId), eq(organizationId), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // Act
        Optional<ConsentRecord> result = consentVerificationService.getActiveConsent(patientId, organizationId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void checkAndUpdateExpiredConsent_ExpiredConsent_UpdatesAndReturnsTrue() {
        // Arrange
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(expiredConsentEntity));
        when(consentRepository.save(any(ConsentEntity.class))).thenReturn(expiredConsentEntity);

        // Act
        boolean result = consentVerificationService.checkAndUpdateExpiredConsent(patientId, organizationId);

        // Assert
        assertTrue(result);
        verify(consentRepository).save(argThat(entity -> 
            entity.getStatus() == ConsentStatus.EXPIRED));
    }

    @Test
    void checkAndUpdateExpiredConsent_NotExpired_ReturnsFalse() {
        // Arrange
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(activeConsentEntity));

        // Act
        boolean result = consentVerificationService.checkAndUpdateExpiredConsent(patientId, organizationId);

        // Assert
        assertFalse(result);
        verify(consentRepository, never()).save(any(ConsentEntity.class));
    }

    @Test
    void processExpiredConsents_HasExpiredConsents_UpdatesAndReturnsCount() {
        // Arrange
        List<ConsentEntity> expiredConsents = Arrays.asList(expiredConsentEntity, revokedConsentEntity);
        when(consentRepository.findExpiredConsents(any(LocalDateTime.class)))
            .thenReturn(expiredConsents);
        when(consentRepository.saveAll(anyList())).thenReturn(expiredConsents);

        // Act
        int result = consentVerificationService.processExpiredConsents();

        // Assert
        assertEquals(2, result);
        verify(consentRepository).saveAll(argThat(entities -> {
            for (Object entity : entities) {
                if (((ConsentEntity) entity).getStatus() != ConsentStatus.EXPIRED) {
                    return false;
                }
            }
            return true;
        }));
    }

    @Test
    void processExpiredConsents_NoExpiredConsents_ReturnsZero() {
        // Arrange
        when(consentRepository.findExpiredConsents(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList());

        // Act
        int result = consentVerificationService.processExpiredConsents();

        // Assert
        assertEquals(0, result);
        verify(consentRepository, never()).saveAll(anyList());
    }

    @Test
    void getActiveConsentsForPatient_HasActiveConsents_ReturnsConsentList() {
        // Arrange
        List<ConsentEntity> activeConsents = Arrays.asList(activeConsentEntity);
        when(consentRepository.findActiveConsentsByPatientId(eq(patientId), any(LocalDateTime.class)))
            .thenReturn(activeConsents);

        // Act
        List<ConsentRecord> result = consentVerificationService.getActiveConsentsForPatient(patientId);

        // Assert
        assertEquals(1, result.size());
        ConsentRecord consentRecord = result.get(0);
        assertEquals(patientId, consentRecord.getPatientId());
        assertEquals(organizationId, consentRecord.getOrganizationId());
        assertEquals(ConsentStatus.ACTIVE, consentRecord.getStatus());
    }

    @Test
    void validateConsentPolicy_ValidConsent_ReturnsTrue() {
        // Arrange
        ConsentRecord consentRecord = new ConsentRecord();
        consentRecord.setPatientId(patientId);
        consentRecord.setOrganizationId(organizationId);
        consentRecord.setStatus(ConsentStatus.ACTIVE);
        consentRecord.setEffectiveDate(LocalDateTime.now().minusDays(1));
        consentRecord.setExpirationDate(LocalDateTime.now().plusDays(30));
        consentRecord.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));

        // Act
        boolean result = consentVerificationService.validateConsentPolicy(consentRecord);

        // Assert
        assertTrue(result);
    }

    @Test
    void validateConsentPolicy_InvalidConsent_ReturnsFalse() {
        // Test null consent
        assertFalse(consentVerificationService.validateConsentPolicy(null));

        // Test missing patient ID
        ConsentRecord invalidConsent1 = new ConsentRecord();
        invalidConsent1.setOrganizationId(organizationId);
        invalidConsent1.setStatus(ConsentStatus.ACTIVE);
        invalidConsent1.setEffectiveDate(LocalDateTime.now());
        assertFalse(consentVerificationService.validateConsentPolicy(invalidConsent1));

        // Test missing organization ID
        ConsentRecord invalidConsent2 = new ConsentRecord();
        invalidConsent2.setPatientId(patientId);
        invalidConsent2.setStatus(ConsentStatus.ACTIVE);
        invalidConsent2.setEffectiveDate(LocalDateTime.now());
        assertFalse(consentVerificationService.validateConsentPolicy(invalidConsent2));

        // Test null status
        ConsentRecord invalidConsent3 = new ConsentRecord();
        invalidConsent3.setPatientId(patientId);
        invalidConsent3.setOrganizationId(organizationId);
        invalidConsent3.setEffectiveDate(LocalDateTime.now());
        invalidConsent3.setStatus(null); // Explicitly set to null
        assertFalse(consentVerificationService.validateConsentPolicy(invalidConsent3));

        // Test null effective date
        ConsentRecord invalidConsent4 = new ConsentRecord();
        invalidConsent4.setPatientId(patientId);
        invalidConsent4.setOrganizationId(organizationId);
        invalidConsent4.setStatus(ConsentStatus.ACTIVE);
        invalidConsent4.setEffectiveDate(null); // Explicitly set to null
        assertFalse(consentVerificationService.validateConsentPolicy(invalidConsent4));

        // Test expiration date before effective date
        ConsentRecord invalidConsent5 = new ConsentRecord();
        invalidConsent5.setPatientId(patientId);
        invalidConsent5.setOrganizationId(organizationId);
        invalidConsent5.setStatus(ConsentStatus.ACTIVE);
        invalidConsent5.setEffectiveDate(LocalDateTime.now());
        invalidConsent5.setExpirationDate(LocalDateTime.now().minusDays(1));
        invalidConsent5.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));
        assertFalse(consentVerificationService.validateConsentPolicy(invalidConsent5));

        // Test active consent without allowed categories
        ConsentRecord invalidConsent6 = new ConsentRecord();
        invalidConsent6.setPatientId(patientId);
        invalidConsent6.setOrganizationId(organizationId);
        invalidConsent6.setStatus(ConsentStatus.ACTIVE);
        invalidConsent6.setEffectiveDate(LocalDateTime.now());
        assertFalse(consentVerificationService.validateConsentPolicy(invalidConsent6));
    }

    @Test
    void validateConsentPolicy_EffectiveDateTooFarInFuture_ReturnsFalse() {
        // Arrange
        ConsentRecord consentRecord = new ConsentRecord();
        consentRecord.setPatientId(patientId);
        consentRecord.setOrganizationId(organizationId);
        consentRecord.setStatus(ConsentStatus.ACTIVE);
        consentRecord.setEffectiveDate(LocalDateTime.now().plusYears(2)); // Too far in future
        consentRecord.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));

        // Act
        boolean result = consentVerificationService.validateConsentPolicy(consentRecord);

        // Assert
        assertFalse(result);
    }

    @Test
    void verifyConsent_PendingStatus_ReturnsInvalidResult() {
        // Arrange
        ConsentEntity pendingConsent = new ConsentEntity();
        pendingConsent.setPatientId(patientId);
        pendingConsent.setOrganizationId(organizationId);
        pendingConsent.setStatus(ConsentStatus.PENDING);
        pendingConsent.setEffectiveDate(LocalDateTime.now().minusDays(1));

        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(pendingConsent));

        // Act
        ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("pending"));
    }

    @Test
    void verifyConsent_SuspendedStatus_ReturnsInvalidResult() {
        // Arrange
        ConsentEntity suspendedConsent = new ConsentEntity();
        suspendedConsent.setPatientId(patientId);
        suspendedConsent.setOrganizationId(organizationId);
        suspendedConsent.setStatus(ConsentStatus.SUSPENDED);
        suspendedConsent.setEffectiveDate(LocalDateTime.now().minusDays(1));

        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(suspendedConsent));

        // Act
        ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("suspended"));
    }

    @Test
    void verifyConsent_DeniedStatus_ReturnsInvalidResult() {
        // Arrange
        ConsentEntity deniedConsent = new ConsentEntity();
        deniedConsent.setPatientId(patientId);
        deniedConsent.setOrganizationId(organizationId);
        deniedConsent.setStatus(ConsentStatus.DENIED);
        deniedConsent.setEffectiveDate(LocalDateTime.now().minusDays(1));

        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(deniedConsent));

        // Act
        ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("denied"));
    }
}