package com.bridge.controller;

import com.bridge.model.*;
import com.bridge.service.AuditService;
import com.bridge.service.ConsentVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for consent management operations
 * Handles consent status checking and updates
 */
@RestController
@RequestMapping("/api/v1/consent")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Consent Management", description = "Patient consent verification and management operations")
public class ConsentController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsentController.class);
    
    @Autowired
    private ConsentVerificationService consentVerificationService;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Check consent status for a patient and organization
     * 
     * @param patientId The patient identifier
     * @param organizationId The organization identifier (optional, defaults to current organization)
     * @return ResponseEntity with consent verification result
     */
    @Operation(
        summary = "Check patient consent status",
        description = """
            Verifies the consent status for a specific patient and organization.
            Returns detailed consent information including allowed/denied data categories,
            effective dates, and expiration information.
            """,
        tags = {"Consent Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Consent status retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ConsentStatusResponse.class),
                examples = @ExampleObject(
                    name = "Valid consent",
                    value = """
                        {
                          "requestId": "123e4567-e89b-12d3-a456-426614174000",
                          "patientId": "patient-123",
                          "organizationId": "org-456",
                          "consentValid": true,
                          "consentStatus": "ACTIVE",
                          "allowedCategories": ["DEMOGRAPHICS", "CLINICAL"],
                          "deniedCategories": ["FINANCIAL"],
                          "effectiveDate": "2025-01-01T00:00:00",
                          "expirationDate": "2026-01-01T00:00:00",
                          "reason": "Valid consent found",
                          "verificationTimestamp": "2025-01-15T10:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions - requires CONSENT_READER or ADMIN role"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/status/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CONSENT_READER') or hasRole('ADMIN')")
    public ResponseEntity<?> getConsentStatus(
            @Parameter(description = "Patient identifier", required = true, example = "patient-123")
            @PathVariable String patientId,
            @Parameter(description = "Organization identifier (optional)", example = "org-456")
            @RequestParam(required = false) String organizationId) {
        
        String requestId = UUID.randomUUID().toString();
        logger.info("Checking consent status for patient: {}, organization: {}, requestId: {}", 
            patientId, organizationId, requestId);
        
        try {
            // Use default organization if not provided
            if (organizationId == null || organizationId.trim().isEmpty()) {
                organizationId = "DEFAULT_ORG"; // This would typically come from security context
            }
            
            ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);
            
            // Audit the consent check
            auditService.logConsentVerification("SYSTEM", patientId, "CONSENT_STATUS_CHECK", 
                result.isValid() ? "SUCCESS" : "DENIED", 
                createAuditDetails("organization_id", organizationId, "consent_status", result.getConsentStatus()));
            
            ConsentStatusResponse response = new ConsentStatusResponse();
            response.setRequestId(requestId);
            response.setPatientId(patientId);
            response.setOrganizationId(organizationId);
            response.setConsentValid(result.isValid());
            response.setConsentStatus(result.getConsentStatus());
            response.setAllowedCategories(result.getAllowedCategories());
            response.setDeniedCategories(result.getDeniedCategories());
            response.setEffectiveDate(result.getEffectiveDate());
            response.setExpirationDate(result.getExpirationDate());
            response.setReason(result.getReason());
            response.setVerificationTimestamp(result.getVerificationTimestamp());
            
            logger.info("Consent status check completed, patient: {}, valid: {}, requestId: {}", 
                patientId, result.isValid(), requestId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking consent status for patient: {}, requestId: {}", patientId, requestId, e);
            
            auditService.logConsentVerification("SYSTEM", patientId, "CONSENT_STATUS_CHECK_ERROR", 
                "ERROR", createAuditDetails("error_message", e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                    "CONSENT_CHECK_ERROR", 
                    "Failed to check consent status", 
                    e.getMessage(),
                    requestId
                ));
        }
    }
    
    /**
     * Check consent for specific data categories
     * 
     * @param patientId The patient identifier
     * @param request The consent check request with categories
     * @return ResponseEntity with consent verification result for categories
     */
    @PostMapping("/check/{patientId}")
    @PreAuthorize("hasRole('CONSENT_READER') or hasRole('ADMIN')")
    public ResponseEntity<?> checkConsentForCategories(
            @PathVariable String patientId,
            @Valid @RequestBody ConsentCheckRequest request) {
        
        String requestId = UUID.randomUUID().toString();
        logger.info("Checking consent for categories, patient: {}, categories: {}, requestId: {}", 
            patientId, request.getDataCategories(), requestId);
        
        try {
            String organizationId = request.getOrganizationId();
            if (organizationId == null || organizationId.trim().isEmpty()) {
                organizationId = "DEFAULT_ORG";
            }
            
            ConsentVerificationResult result = consentVerificationService.verifyConsentForCategories(
                patientId, organizationId, request.getDataCategories());
            
            // Audit the consent check
            auditService.logConsentVerification("SYSTEM", patientId, "CONSENT_CATEGORY_CHECK", 
                result.isValid() ? "SUCCESS" : "DENIED", 
                createAuditDetails("categories", request.getDataCategories(), "organization_id", organizationId));
            
            ConsentCategoryResponse response = new ConsentCategoryResponse();
            response.setRequestId(requestId);
            response.setPatientId(patientId);
            response.setOrganizationId(organizationId);
            response.setRequestedCategories(request.getDataCategories());
            response.setAllowedCategories(result.getAllowedCategories());
            response.setDeniedCategories(result.getDeniedCategories());
            response.setConsentValid(result.isValid());
            response.setVerificationTimestamp(result.getVerificationTimestamp());
            
            logger.info("Consent category check completed, patient: {}, valid: {}, requestId: {}", 
                patientId, result.isValid(), requestId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking consent for categories, patient: {}, requestId: {}", patientId, requestId, e);
            
            auditService.logConsentVerification("SYSTEM", patientId, "CONSENT_CATEGORY_CHECK_ERROR", 
                "ERROR", createAuditDetails("error_message", e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                    "CONSENT_CATEGORY_CHECK_ERROR", 
                    "Failed to check consent for categories", 
                    e.getMessage(),
                    requestId
                ));
        }
    }
    
    /**
     * Update consent record for a patient
     * 
     * @param patientId The patient identifier
     * @param request The consent update request
     * @return ResponseEntity with update result
     */
    @PutMapping("/update/{patientId}")
    @PreAuthorize("hasRole('CONSENT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateConsent(
            @PathVariable String patientId,
            @Valid @RequestBody ConsentUpdateRequest request) {
        
        String requestId = UUID.randomUUID().toString();
        logger.info("Updating consent for patient: {}, status: {}, requestId: {}", 
            patientId, request.getStatus(), requestId);
        
        try {
            // For now, return a placeholder response since we don't have a repository implementation
            // This would typically update the consent record in the database
            
            auditService.logConsentVerification("SYSTEM", patientId, "CONSENT_UPDATE", 
                "SUCCESS", createAuditDetails("new_status", request.getStatus(), 
                "categories", request.getAllowedCategories()));
            
            ConsentUpdateResponse response = new ConsentUpdateResponse();
            response.setRequestId(requestId);
            response.setPatientId(patientId);
            response.setOrganizationId(request.getOrganizationId());
            response.setStatus(request.getStatus());
            response.setAllowedCategories(request.getAllowedCategories());
            response.setUpdateTimestamp(LocalDateTime.now());
            response.setMessage("Consent update endpoint - implementation pending");
            
            logger.info("Consent update completed for patient: {}, requestId: {}", patientId, requestId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating consent for patient: {}, requestId: {}", patientId, requestId, e);
            
            auditService.logConsentVerification("SYSTEM", patientId, "CONSENT_UPDATE_ERROR", 
                "ERROR", createAuditDetails("error_message", e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                    "CONSENT_UPDATE_ERROR", 
                    "Failed to update consent", 
                    e.getMessage(),
                    requestId
                ));
        }
    }
    
    /**
     * Get consent history for a patient
     * 
     * @param patientId The patient identifier
     * @param organizationId The organization identifier (optional)
     * @return ResponseEntity with consent history
     */
    @GetMapping("/history/{patientId}")
    @PreAuthorize("hasRole('CONSENT_READER') or hasRole('ADMIN')")
    public ResponseEntity<?> getConsentHistory(
            @PathVariable String patientId,
            @RequestParam(required = false) String organizationId) {
        
        String requestId = UUID.randomUUID().toString();
        logger.info("Retrieving consent history for patient: {}, organization: {}, requestId: {}", 
            patientId, organizationId, requestId);
        
        try {
            // For now, return a placeholder response since we don't have a repository implementation
            // This would typically query consent history from the database
            
            auditService.logConsentVerification("SYSTEM", patientId, "CONSENT_HISTORY_ACCESS", 
                "SUCCESS", createAuditDetails("organization_id", organizationId));
            
            ConsentHistoryResponse response = new ConsentHistoryResponse();
            response.setRequestId(requestId);
            response.setPatientId(patientId);
            response.setOrganizationId(organizationId);
            response.setMessage("Consent history endpoint - implementation pending");
            response.setRetrievalTimestamp(LocalDateTime.now());
            
            logger.info("Consent history retrieval completed for patient: {}, requestId: {}", patientId, requestId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving consent history for patient: {}, requestId: {}", patientId, requestId, e);
            
            auditService.logConsentVerification("SYSTEM", patientId, "CONSENT_HISTORY_ACCESS_ERROR", 
                "ERROR", createAuditDetails("error_message", e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                    "CONSENT_HISTORY_ERROR", 
                    "Failed to retrieve consent history", 
                    e.getMessage(),
                    requestId
                ));
        }
    }
    
    /**
     * Get all active consents for a patient
     * 
     * @param patientId The patient identifier
     * @return ResponseEntity with active consents
     */
    @GetMapping("/active/{patientId}")
    @PreAuthorize("hasRole('CONSENT_READER') or hasRole('ADMIN')")
    public ResponseEntity<?> getActiveConsents(@PathVariable String patientId) {
        
        String requestId = UUID.randomUUID().toString();
        logger.info("Retrieving active consents for patient: {}, requestId: {}", patientId, requestId);
        
        try {
            List<ConsentRecord> activeConsents = consentVerificationService.getActiveConsentsForPatient(patientId);
            
            auditService.logConsentVerification("SYSTEM", patientId, "ACTIVE_CONSENTS_ACCESS", 
                "SUCCESS", createAuditDetails("consent_count", activeConsents.size()));
            
            ActiveConsentsResponse response = new ActiveConsentsResponse();
            response.setRequestId(requestId);
            response.setPatientId(patientId);
            response.setActiveConsents(activeConsents);
            response.setConsentCount(activeConsents.size());
            response.setRetrievalTimestamp(LocalDateTime.now());
            
            logger.info("Active consents retrieval completed for patient: {}, count: {}, requestId: {}", 
                patientId, activeConsents.size(), requestId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving active consents for patient: {}, requestId: {}", patientId, requestId, e);
            
            auditService.logConsentVerification("SYSTEM", patientId, "ACTIVE_CONSENTS_ACCESS_ERROR", 
                "ERROR", createAuditDetails("error_message", e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                    "ACTIVE_CONSENTS_ERROR", 
                    "Failed to retrieve active consents", 
                    e.getMessage(),
                    requestId
                ));
        }
    }
    
    // Helper methods
    
    private Map<String, Object> createErrorResponse(String code, String message, Object details, String requestId) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of(
            "code", code,
            "message", message,
            "details", details,
            "timestamp", LocalDateTime.now(),
            "requestId", requestId
        ));
        return error;
    }
    
    private Map<String, Object> createAuditDetails(String key1, Object value1) {
        Map<String, Object> details = new HashMap<>();
        details.put(key1, value1);
        return details;
    }
    
    private Map<String, Object> createAuditDetails(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> details = new HashMap<>();
        details.put(key1, value1);
        details.put(key2, value2);
        return details;
    }
    
    // Inner classes for request/response DTOs
    
    public static class ConsentCheckRequest {
        private String organizationId;
        private List<DataCategory> dataCategories;
        
        // Getters and setters
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        
        public List<DataCategory> getDataCategories() { return dataCategories; }
        public void setDataCategories(List<DataCategory> dataCategories) { this.dataCategories = dataCategories; }
    }
    
    public static class ConsentUpdateRequest {
        private String organizationId;
        private ConsentStatus status;
        private List<DataCategory> allowedCategories;
        private LocalDateTime expirationDate;
        private String policyReference;
        
        // Getters and setters
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        
        public ConsentStatus getStatus() { return status; }
        public void setStatus(ConsentStatus status) { this.status = status; }
        
        public List<DataCategory> getAllowedCategories() { return allowedCategories; }
        public void setAllowedCategories(List<DataCategory> allowedCategories) { this.allowedCategories = allowedCategories; }
        
        public LocalDateTime getExpirationDate() { return expirationDate; }
        public void setExpirationDate(LocalDateTime expirationDate) { this.expirationDate = expirationDate; }
        
        public String getPolicyReference() { return policyReference; }
        public void setPolicyReference(String policyReference) { this.policyReference = policyReference; }
    }
    
    public static class ConsentStatusResponse {
        private String requestId;
        private String patientId;
        private String organizationId;
        private boolean consentValid;
        private ConsentStatus consentStatus;
        private List<DataCategory> allowedCategories;
        private List<DataCategory> deniedCategories;
        private LocalDateTime effectiveDate;
        private LocalDateTime expirationDate;
        private String reason;
        private LocalDateTime verificationTimestamp;
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        
        public boolean isConsentValid() { return consentValid; }
        public void setConsentValid(boolean consentValid) { this.consentValid = consentValid; }
        
        public ConsentStatus getConsentStatus() { return consentStatus; }
        public void setConsentStatus(ConsentStatus consentStatus) { this.consentStatus = consentStatus; }
        
        public List<DataCategory> getAllowedCategories() { return allowedCategories; }
        public void setAllowedCategories(List<DataCategory> allowedCategories) { this.allowedCategories = allowedCategories; }
        
        public List<DataCategory> getDeniedCategories() { return deniedCategories; }
        public void setDeniedCategories(List<DataCategory> deniedCategories) { this.deniedCategories = deniedCategories; }
        
        public LocalDateTime getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(LocalDateTime effectiveDate) { this.effectiveDate = effectiveDate; }
        
        public LocalDateTime getExpirationDate() { return expirationDate; }
        public void setExpirationDate(LocalDateTime expirationDate) { this.expirationDate = expirationDate; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public LocalDateTime getVerificationTimestamp() { return verificationTimestamp; }
        public void setVerificationTimestamp(LocalDateTime verificationTimestamp) { this.verificationTimestamp = verificationTimestamp; }
    }
    
    public static class ConsentCategoryResponse {
        private String requestId;
        private String patientId;
        private String organizationId;
        private List<DataCategory> requestedCategories;
        private List<DataCategory> allowedCategories;
        private List<DataCategory> deniedCategories;
        private boolean consentValid;
        private LocalDateTime verificationTimestamp;
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        
        public List<DataCategory> getRequestedCategories() { return requestedCategories; }
        public void setRequestedCategories(List<DataCategory> requestedCategories) { this.requestedCategories = requestedCategories; }
        
        public List<DataCategory> getAllowedCategories() { return allowedCategories; }
        public void setAllowedCategories(List<DataCategory> allowedCategories) { this.allowedCategories = allowedCategories; }
        
        public List<DataCategory> getDeniedCategories() { return deniedCategories; }
        public void setDeniedCategories(List<DataCategory> deniedCategories) { this.deniedCategories = deniedCategories; }
        
        public boolean isConsentValid() { return consentValid; }
        public void setConsentValid(boolean consentValid) { this.consentValid = consentValid; }
        
        public LocalDateTime getVerificationTimestamp() { return verificationTimestamp; }
        public void setVerificationTimestamp(LocalDateTime verificationTimestamp) { this.verificationTimestamp = verificationTimestamp; }
    }
    
    public static class ConsentUpdateResponse {
        private String requestId;
        private String patientId;
        private String organizationId;
        private ConsentStatus status;
        private List<DataCategory> allowedCategories;
        private LocalDateTime updateTimestamp;
        private String message;
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        
        public ConsentStatus getStatus() { return status; }
        public void setStatus(ConsentStatus status) { this.status = status; }
        
        public List<DataCategory> getAllowedCategories() { return allowedCategories; }
        public void setAllowedCategories(List<DataCategory> allowedCategories) { this.allowedCategories = allowedCategories; }
        
        public LocalDateTime getUpdateTimestamp() { return updateTimestamp; }
        public void setUpdateTimestamp(LocalDateTime updateTimestamp) { this.updateTimestamp = updateTimestamp; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class ConsentHistoryResponse {
        private String requestId;
        private String patientId;
        private String organizationId;
        private String message;
        private LocalDateTime retrievalTimestamp;
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public LocalDateTime getRetrievalTimestamp() { return retrievalTimestamp; }
        public void setRetrievalTimestamp(LocalDateTime retrievalTimestamp) { this.retrievalTimestamp = retrievalTimestamp; }
    }
    
    public static class ActiveConsentsResponse {
        private String requestId;
        private String patientId;
        private List<ConsentRecord> activeConsents;
        private int consentCount;
        private LocalDateTime retrievalTimestamp;
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public List<ConsentRecord> getActiveConsents() { return activeConsents; }
        public void setActiveConsents(List<ConsentRecord> activeConsents) { this.activeConsents = activeConsents; }
        
        public int getConsentCount() { return consentCount; }
        public void setConsentCount(int consentCount) { this.consentCount = consentCount; }
        
        public LocalDateTime getRetrievalTimestamp() { return retrievalTimestamp; }
        public void setRetrievalTimestamp(LocalDateTime retrievalTimestamp) { this.retrievalTimestamp = retrievalTimestamp; }
    }
}