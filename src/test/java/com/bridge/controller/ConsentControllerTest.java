package com.bridge.controller;

import com.bridge.model.*;
import com.bridge.service.AuditService;
import com.bridge.service.ConsentVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConsentController
 */
@ExtendWith(MockitoExtension.class)
class ConsentControllerTest {

    @InjectMocks
    private ConsentController controller;

    @Mock
    private ConsentVerificationService consentVerificationService;

    @Mock
    private AuditService auditService;

    private ConsentVerificationResult validConsentResult;
    private ConsentVerificationResult invalidConsentResult;
    private List<ConsentRecord> mockActiveConsents;

    @BeforeEach
    void setUp() {
        // Setup valid consent result
        validConsentResult = new ConsentVerificationResult();
        validConsentResult.setValid(true);
        validConsentResult.setPatientId("patient-123");
        validConsentResult.setOrganizationId("org-456");
        validConsentResult.setConsentStatus(ConsentStatus.ACTIVE);
        validConsentResult.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS, DataCategory.LAB_RESULTS));
        validConsentResult.setEffectiveDate(LocalDateTime.now().minusDays(30));
        validConsentResult.setExpirationDate(LocalDateTime.now().plusDays(365));
        validConsentResult.setVerificationTimestamp(LocalDateTime.now());

        // Setup invalid consent result
        invalidConsentResult = new ConsentVerificationResult();
        invalidConsentResult.setValid(false);
        invalidConsentResult.setPatientId("patient-123");
        invalidConsentResult.setOrganizationId("org-456");
        invalidConsentResult.setConsentStatus(ConsentStatus.EXPIRED);
        invalidConsentResult.setReason("Consent has expired");
        invalidConsentResult.setVerificationTimestamp(LocalDateTime.now());

        // Setup mock active consents
        ConsentRecord activeConsent1 = new ConsentRecord("patient-123", "org-456", ConsentStatus.ACTIVE);
        activeConsent1.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS, DataCategory.LAB_RESULTS));
        activeConsent1.setEffectiveDate(LocalDateTime.now().minusDays(30));
        activeConsent1.setExpirationDate(LocalDateTime.now().plusDays(365));

        ConsentRecord activeConsent2 = new ConsentRecord("patient-123", "org-789", ConsentStatus.ACTIVE);
        activeConsent2.setAllowedCategories(Arrays.asList(DataCategory.MEDICATIONS, DataCategory.ALLERGIES));
        activeConsent2.setEffectiveDate(LocalDateTime.now().minusDays(15));

        mockActiveConsents = Arrays.asList(activeConsent1, activeConsent2);
    }

    @Test
    void testGetConsentStatus_Success() {
        // Arrange
        when(consentVerificationService.verifyConsent("patient-123", "DEFAULT_ORG"))
            .thenReturn(validConsentResult);

        // Act
        ResponseEntity<?> response = controller.getConsentStatus("patient-123", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ConsentController.ConsentStatusResponse statusResponse = 
            (ConsentController.ConsentStatusResponse) response.getBody();
        assertEquals("patient-123", statusResponse.getPatientId());
        assertEquals("DEFAULT_ORG", statusResponse.getOrganizationId());
        assertTrue(statusResponse.isConsentValid());
        assertEquals(ConsentStatus.ACTIVE, statusResponse.getConsentStatus());
        assertEquals(2, statusResponse.getAllowedCategories().size());
        assertNotNull(statusResponse.getRequestId());

        // Verify service interactions
        verify(consentVerificationService).verifyConsent("patient-123", "DEFAULT_ORG");
        verify(auditService).logConsentVerification(eq("SYSTEM"), eq("patient-123"), 
            eq("CONSENT_STATUS_CHECK"), eq("SUCCESS"), any(Map.class));
    }

    @Test
    void testGetConsentStatus_WithOrganizationId() {
        // Arrange
        when(consentVerificationService.verifyConsent("patient-123", "org-456"))
            .thenReturn(validConsentResult);

        // Act
        ResponseEntity<?> response = controller.getConsentStatus("patient-123", "org-456");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConsentController.ConsentStatusResponse statusResponse = 
            (ConsentController.ConsentStatusResponse) response.getBody();
        assertEquals("org-456", statusResponse.getOrganizationId());

        verify(consentVerificationService).verifyConsent("patient-123", "org-456");
    }

    @Test
    void testGetConsentStatus_InvalidConsent() {
        // Arrange
        when(consentVerificationService.verifyConsent("patient-123", "DEFAULT_ORG"))
            .thenReturn(invalidConsentResult);

        // Act
        ResponseEntity<?> response = controller.getConsentStatus("patient-123", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConsentController.ConsentStatusResponse statusResponse = 
            (ConsentController.ConsentStatusResponse) response.getBody();
        assertFalse(statusResponse.isConsentValid());
        assertEquals(ConsentStatus.EXPIRED, statusResponse.getConsentStatus());
        assertEquals("Consent has expired", statusResponse.getReason());

        verify(auditService).logConsentVerification(eq("SYSTEM"), eq("patient-123"), 
            eq("CONSENT_STATUS_CHECK"), eq("DENIED"), any(Map.class));
    }

    @Test
    void testGetConsentStatus_ServiceError() {
        // Arrange
        when(consentVerificationService.verifyConsent("patient-123", "DEFAULT_ORG"))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<?> response = controller.getConsentStatus("patient-123", null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();
        assertNotNull(errorResponse.get("error"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertEquals("CONSENT_CHECK_ERROR", error.get("code"));

        verify(auditService).logConsentVerification(eq("SYSTEM"), eq("patient-123"), 
            eq("CONSENT_STATUS_CHECK_ERROR"), eq("ERROR"), any(Map.class));
    }

    @Test
    void testCheckConsentForCategories_Success() {
        // Arrange
        ConsentController.ConsentCheckRequest request = new ConsentController.ConsentCheckRequest();
        request.setOrganizationId("org-456");
        request.setDataCategories(Arrays.asList(DataCategory.DEMOGRAPHICS, DataCategory.LAB_RESULTS));

        when(consentVerificationService.verifyConsentForCategories("patient-123", "org-456", 
            request.getDataCategories())).thenReturn(validConsentResult);

        // Act
        ResponseEntity<?> response = controller.checkConsentForCategories("patient-123", request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConsentController.ConsentCategoryResponse categoryResponse = 
            (ConsentController.ConsentCategoryResponse) response.getBody();
        assertEquals("patient-123", categoryResponse.getPatientId());
        assertEquals("org-456", categoryResponse.getOrganizationId());
        assertTrue(categoryResponse.isConsentValid());
        assertEquals(2, categoryResponse.getRequestedCategories().size());
        assertEquals(2, categoryResponse.getAllowedCategories().size());

        verify(consentVerificationService).verifyConsentForCategories("patient-123", "org-456", 
            request.getDataCategories());
        verify(auditService).logConsentVerification(eq("SYSTEM"), eq("patient-123"), 
            eq("CONSENT_CATEGORY_CHECK"), eq("SUCCESS"), any(Map.class));
    }

    @Test
    void testCheckConsentForCategories_DefaultOrganization() {
        // Arrange
        ConsentController.ConsentCheckRequest request = new ConsentController.ConsentCheckRequest();
        request.setDataCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));

        when(consentVerificationService.verifyConsentForCategories("patient-123", "DEFAULT_ORG", 
            request.getDataCategories())).thenReturn(validConsentResult);

        // Act
        ResponseEntity<?> response = controller.checkConsentForCategories("patient-123", request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConsentController.ConsentCategoryResponse categoryResponse = 
            (ConsentController.ConsentCategoryResponse) response.getBody();
        assertEquals("DEFAULT_ORG", categoryResponse.getOrganizationId());

        verify(consentVerificationService).verifyConsentForCategories("patient-123", "DEFAULT_ORG", 
            request.getDataCategories());
    }

    @Test
    void testCheckConsentForCategories_ServiceError() {
        // Arrange
        ConsentController.ConsentCheckRequest request = new ConsentController.ConsentCheckRequest();
        request.setOrganizationId("org-456");
        request.setDataCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));

        when(consentVerificationService.verifyConsentForCategories("patient-123", "org-456", 
            request.getDataCategories())).thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<?> response = controller.checkConsentForCategories("patient-123", request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertEquals("CONSENT_CATEGORY_CHECK_ERROR", error.get("code"));

        verify(auditService).logConsentVerification(eq("SYSTEM"), eq("patient-123"), 
            eq("CONSENT_CATEGORY_CHECK_ERROR"), eq("ERROR"), any(Map.class));
    }

    @Test
    void testUpdateConsent_Success() {
        // Arrange
        ConsentController.ConsentUpdateRequest request = new ConsentController.ConsentUpdateRequest();
        request.setOrganizationId("org-456");
        request.setStatus(ConsentStatus.ACTIVE);
        request.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS, DataCategory.LAB_RESULTS));
        request.setExpirationDate(LocalDateTime.now().plusDays(365));

        // Act
        ResponseEntity<?> response = controller.updateConsent("patient-123", request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConsentController.ConsentUpdateResponse updateResponse = 
            (ConsentController.ConsentUpdateResponse) response.getBody();
        assertEquals("patient-123", updateResponse.getPatientId());
        assertEquals("org-456", updateResponse.getOrganizationId());
        assertEquals(ConsentStatus.ACTIVE, updateResponse.getStatus());
        assertEquals(2, updateResponse.getAllowedCategories().size());
        assertNotNull(updateResponse.getUpdateTimestamp());
        assertNotNull(updateResponse.getMessage());

        verify(auditService).logConsentVerification(eq("SYSTEM"), eq("patient-123"), 
            eq("CONSENT_UPDATE"), eq("SUCCESS"), any(Map.class));
    }

    @Test
    void testUpdateConsent_ServiceError() {
        // Arrange
        ConsentController.ConsentUpdateRequest request = new ConsentController.ConsentUpdateRequest();
        request.setOrganizationId("org-456");
        request.setStatus(ConsentStatus.ACTIVE);

        doThrow(new RuntimeException("Service error")).when(auditService)
            .logConsentVerification(anyString(), anyString(), anyString(), anyString(), any(Map.class));

        // Act
        ResponseEntity<?> response = controller.updateConsent("patient-123", request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertEquals("CONSENT_UPDATE_ERROR", error.get("code"));
    }

    @Test
    void testGetConsentHistory_Success() {
        // Act
        ResponseEntity<?> response = controller.getConsentHistory("patient-123", "org-456");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConsentController.ConsentHistoryResponse historyResponse = 
            (ConsentController.ConsentHistoryResponse) response.getBody();
        assertEquals("patient-123", historyResponse.getPatientId());
        assertEquals("org-456", historyResponse.getOrganizationId());
        assertNotNull(historyResponse.getMessage());
        assertNotNull(historyResponse.getRetrievalTimestamp());

        verify(auditService).logConsentVerification(eq("SYSTEM"), eq("patient-123"), 
            eq("CONSENT_HISTORY_ACCESS"), eq("SUCCESS"), any(Map.class));
    }

    @Test
    void testGetConsentHistory_ServiceError() {
        // Arrange
        doThrow(new RuntimeException("Service error")).when(auditService)
            .logConsentVerification(anyString(), anyString(), anyString(), anyString(), any(Map.class));

        // Act
        ResponseEntity<?> response = controller.getConsentHistory("patient-123", "org-456");

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertEquals("CONSENT_HISTORY_ERROR", error.get("code"));
    }

    @Test
    void testGetActiveConsents_Success() {
        // Arrange
        when(consentVerificationService.getActiveConsentsForPatient("patient-123"))
            .thenReturn(mockActiveConsents);

        // Act
        ResponseEntity<?> response = controller.getActiveConsents("patient-123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConsentController.ActiveConsentsResponse activeResponse = 
            (ConsentController.ActiveConsentsResponse) response.getBody();
        assertEquals("patient-123", activeResponse.getPatientId());
        assertEquals(2, activeResponse.getConsentCount());
        assertEquals(2, activeResponse.getActiveConsents().size());
        assertNotNull(activeResponse.getRetrievalTimestamp());

        verify(consentVerificationService).getActiveConsentsForPatient("patient-123");
        verify(auditService).logConsentVerification(eq("SYSTEM"), eq("patient-123"), 
            eq("ACTIVE_CONSENTS_ACCESS"), eq("SUCCESS"), any(Map.class));
    }

    @Test
    void testGetActiveConsents_EmptyList() {
        // Arrange
        when(consentVerificationService.getActiveConsentsForPatient("patient-123"))
            .thenReturn(Arrays.asList());

        // Act
        ResponseEntity<?> response = controller.getActiveConsents("patient-123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConsentController.ActiveConsentsResponse activeResponse = 
            (ConsentController.ActiveConsentsResponse) response.getBody();
        assertEquals(0, activeResponse.getConsentCount());
        assertTrue(activeResponse.getActiveConsents().isEmpty());
    }

    @Test
    void testGetActiveConsents_ServiceError() {
        // Arrange
        when(consentVerificationService.getActiveConsentsForPatient("patient-123"))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<?> response = controller.getActiveConsents("patient-123");

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertEquals("ACTIVE_CONSENTS_ERROR", error.get("code"));

        verify(auditService).logConsentVerification(eq("SYSTEM"), eq("patient-123"), 
            eq("ACTIVE_CONSENTS_ACCESS_ERROR"), eq("ERROR"), any(Map.class));
    }

    @Test
    void testConsentCheckRequest_GettersSetters() {
        // Test DTO getters and setters
        ConsentController.ConsentCheckRequest request = new ConsentController.ConsentCheckRequest();
        request.setOrganizationId("org-123");
        request.setDataCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));

        assertEquals("org-123", request.getOrganizationId());
        assertEquals(1, request.getDataCategories().size());
        assertEquals(DataCategory.DEMOGRAPHICS, request.getDataCategories().get(0));
    }

    @Test
    void testConsentUpdateRequest_GettersSetters() {
        // Test DTO getters and setters
        ConsentController.ConsentUpdateRequest request = new ConsentController.ConsentUpdateRequest();
        LocalDateTime expiration = LocalDateTime.now().plusDays(365);
        
        request.setOrganizationId("org-123");
        request.setStatus(ConsentStatus.ACTIVE);
        request.setAllowedCategories(Arrays.asList(DataCategory.LAB_RESULTS));
        request.setExpirationDate(expiration);
        request.setPolicyReference("policy-ref-123");

        assertEquals("org-123", request.getOrganizationId());
        assertEquals(ConsentStatus.ACTIVE, request.getStatus());
        assertEquals(1, request.getAllowedCategories().size());
        assertEquals(expiration, request.getExpirationDate());
        assertEquals("policy-ref-123", request.getPolicyReference());
    }

    @Test
    void testConsentStatusResponse_GettersSetters() {
        // Test DTO getters and setters
        ConsentController.ConsentStatusResponse response = new ConsentController.ConsentStatusResponse();
        LocalDateTime now = LocalDateTime.now();
        
        response.setRequestId("req-123");
        response.setPatientId("patient-123");
        response.setOrganizationId("org-123");
        response.setConsentValid(true);
        response.setConsentStatus(ConsentStatus.ACTIVE);
        response.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));
        response.setDeniedCategories(Arrays.asList(DataCategory.MENTAL_HEALTH));
        response.setEffectiveDate(now);
        response.setExpirationDate(now.plusDays(365));
        response.setReason("Valid consent");
        response.setVerificationTimestamp(now);

        assertEquals("req-123", response.getRequestId());
        assertEquals("patient-123", response.getPatientId());
        assertEquals("org-123", response.getOrganizationId());
        assertTrue(response.isConsentValid());
        assertEquals(ConsentStatus.ACTIVE, response.getConsentStatus());
        assertEquals(1, response.getAllowedCategories().size());
        assertEquals(1, response.getDeniedCategories().size());
        assertEquals(now, response.getEffectiveDate());
        assertEquals("Valid consent", response.getReason());
    }
}