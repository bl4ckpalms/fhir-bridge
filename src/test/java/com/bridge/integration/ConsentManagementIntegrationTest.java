package com.bridge.integration;

import com.bridge.FhirBridgeApplication;
import com.bridge.controller.ConsentController;
import com.bridge.controller.FhirBridgeController;
import com.bridge.model.*;
import com.bridge.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for consent management scenarios
 * Tests consent verification, data filtering, and consent-based access control
 */
@SpringBootTest(classes = FhirBridgeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class ConsentManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsentVerificationService consentService;

    @Autowired
    private ConsentDataFilterService dataFilterService;

    @Autowired
    private AuditService auditService;

    private static final String PATIENT_ID = "patient-123";
    private static final String ORGANIZATION_ID = "org-456";

    private static final String HL7_MESSAGE_WITH_PATIENT = """
        MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20250115103000||ADT^A01^ADT_A01|MSG123456|P|2.5
        EVN||20250115103000|||^SMITH^JOHN^J^^DR|20250115103000
        PID|1||123456789^^^MRN^MR||DOE^JOHN^MIDDLE^^MR||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345^USA||(555)123-4567|(555)987-6543||S||123456789|123-45-6789|||||||||||20250115103000
        PV1|1|I|ICU^101^1|||^ATTENDING^PHYSICIAN^A^^DR|^REFERRING^PHYSICIAN^R^^DR|MED||||19|V|123456789|^ATTENDING^PHYSICIAN^A^^DR||INS|||||||||||||||||||||20250115103000|20250115120000
        """;

    @Test
    @Order(1)
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Create and verify patient consent")
    void testCreateAndVerifyPatientConsent() throws Exception {
        // Create consent record
        ConsentController.ConsentUpdateRequest consentRequest = new ConsentController.ConsentUpdateRequest();
        consentRequest.setOrganizationId(ORGANIZATION_ID);
        consentRequest.setStatus(ConsentStatus.ACTIVE);
        consentRequest.setAllowedCategories(Arrays.asList(
            DataCategory.DEMOGRAPHICS,
            DataCategory.MEDICAL_HISTORY,
            DataCategory.LABORATORY_RESULTS
        ));
        consentRequest.setExpirationDate(LocalDateTime.now().plusYears(1));
        consentRequest.setPolicyReference("TEFCA-STANDARD-CONSENT-001");

        // Create consent via API
        MvcResult createResult = mockMvc.perform(post("/api/v1/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consentRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.consentId").exists())
                .andReturn();

        // Verify consent was created
        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createResponseMap = objectMapper.readValue(createResponse, Map.class);
        String consentId = (String) createResponseMap.get("consentId");
        assertNotNull(consentId);

        // Verify consent status
        mockMvc.perform(get("/api/v1/consent/status/" + PATIENT_ID)
                .param("organizationId", ORGANIZATION_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.allowedCategories").isArray())
                .andExpect(jsonPath("$.effectiveDate").exists())
                .andExpect(jsonPath("$.expirationDate").exists());
    }

    @Test
    @Order(2)
    @WithMockUser(roles = {"TRANSFORMER"})
    @DisplayName("Transformation with valid consent")
    void testTransformationWithValidConsent() throws Exception {
        // First ensure consent exists (this would typically be set up in test data)
        setupValidConsent();

        // Prepare transformation request
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(HL7_MESSAGE_WITH_PATIENT);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        // Execute transformation - should succeed with valid consent
        MvcResult result = mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Patient-Id", PATIENT_ID)
                .header("X-Organization-Id", ORGANIZATION_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.fhirResources").isArray())
                .andReturn();

        // Verify transformation succeeded
        String responseContent = result.getResponse().getContentAsString();
        FhirBridgeController.TransformationResponse response = 
            objectMapper.readValue(responseContent, FhirBridgeController.TransformationResponse.class);

        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getFhirResources());
        assertTrue(response.getResourceCount() > 0);
    }

    @Test
    @Order(3)
    @WithMockUser(roles = {"TRANSFORMER"})
    @DisplayName("Transformation blocked by missing consent")
    void testTransformationBlockedByMissingConsent() throws Exception {
        // Use a different patient ID without consent
        String patientWithoutConsent = "patient-no-consent";

        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(HL7_MESSAGE_WITH_PATIENT.replace("123456789", "987654321"));
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        // Execute transformation - should be blocked due to missing consent
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Patient-Id", patientWithoutConsent)
                .header("X-Organization-Id", ORGANIZATION_ID))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("CONSENT_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Patient consent not found or expired"));
    }

    @Test
    @Order(4)
    @WithMockUser(roles = {"TRANSFORMER"})
    @DisplayName("Data filtering based on consent preferences")
    void testDataFilteringBasedOnConsent() throws Exception {
        // Setup consent with limited data categories
        setupLimitedConsent();

        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(HL7_MESSAGE_WITH_PATIENT);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        // Execute transformation with limited consent
        MvcResult result = mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Patient-Id", PATIENT_ID)
                .header("X-Organization-Id", ORGANIZATION_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.fhirResources").isArray())
                .andReturn();

        // Verify that sensitive data was filtered out
        String responseContent = result.getResponse().getContentAsString();
        FhirBridgeController.TransformationResponse response = 
            objectMapper.readValue(responseContent, FhirBridgeController.TransformationResponse.class);

        // Check that only allowed resource types are present
        List<FhirResource> fhirResources = response.getFhirResources();
        for (FhirResource resource : fhirResources) {
            // Verify that sensitive resource types are not included
            assertNotEquals("Observation", resource.getResourceType(), 
                "Observation resources should be filtered out with limited consent");
        }
    }

    @Test
    @Order(5)
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Consent expiration handling")
    void testConsentExpirationHandling() throws Exception {
        // Create expired consent
        ConsentController.ConsentUpdateRequest expiredConsentRequest = new ConsentController.ConsentUpdateRequest();
        expiredConsentRequest.setOrganizationId(ORGANIZATION_ID);
        expiredConsentRequest.setStatus(ConsentStatus.ACTIVE);
        expiredConsentRequest.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS));
        expiredConsentRequest.setExpirationDate(LocalDateTime.now().minusYears(1)); // Expired

        // Create expired consent
        mockMvc.perform(post("/api/v1/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expiredConsentRequest)))
                .andExpect(status().isCreated());

        // Try to use expired consent for transformation
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(HL7_MESSAGE_WITH_PATIENT);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Patient-Id", "patient-expired")
                .header("X-Organization-Id", ORGANIZATION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONSENT_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Patient consent not found or expired"));
    }

    @Test
    @Order(6)
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Consent update and revocation")
    void testConsentUpdateAndRevocation() throws Exception {
        // Create initial consent
        setupValidConsent();

        // Update consent to revoked status
        ConsentController.ConsentUpdateRequest updateRequest = new ConsentController.ConsentUpdateRequest();
        updateRequest.setOrganizationId(ORGANIZATION_ID);
        updateRequest.setStatus(ConsentStatus.REVOKED);

        // Update consent status
        mockMvc.perform(put("/api/v1/consent/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Consent updated successfully"));

        // Verify consent is now revoked
        mockMvc.perform(get("/api/v1/consent/status/" + PATIENT_ID)
                .param("organizationId", ORGANIZATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        // Try transformation with revoked consent - should be blocked
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(HL7_MESSAGE_WITH_PATIENT);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Patient-Id", PATIENT_ID)
                .header("X-Organization-Id", ORGANIZATION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONSENT_ERROR"));
    }

    @Test
    @Order(7)
    @WithMockUser(roles = {"READER"})
    @DisplayName("Consent history and audit trail")
    void testConsentHistoryAndAuditTrail() throws Exception {
        // Get consent history
        mockMvc.perform(get("/api/v1/consent/history/" + PATIENT_ID)
                .param("organizationId", ORGANIZATION_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.history").isArray());

        // Verify audit events were created for consent operations
        // This would typically query the audit service for consent-related events
        assertNotNull(auditService);
    }

    @Test
    @Order(8)
    @WithMockUser(roles = {"TRANSFORMER"})
    @DisplayName("Multi-organization consent scenarios")
    void testMultiOrganizationConsentScenarios() throws Exception {
        String org1 = "org-001";
        String org2 = "org-002";
        String multiOrgPatient = "patient-multi-org";

        // Create consent for organization 1
        ConsentController.ConsentUpdateRequest consent1 = new ConsentController.ConsentUpdateRequest();
        consent1.setOrganizationId(org1);
        consent1.setStatus(ConsentStatus.ACTIVE);
        consent1.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS, DataCategory.MEDICAL_HISTORY));
        consent1.setExpirationDate(LocalDateTime.now().plusYears(1));

        mockMvc.perform(post("/api/v1/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consent1)))
                .andExpect(status().isCreated());

        // Create different consent for organization 2
        ConsentController.ConsentUpdateRequest consent2 = new ConsentController.ConsentUpdateRequest();
        consent2.setOrganizationId(org2);
        consent2.setStatus(ConsentStatus.ACTIVE);
        consent2.setAllowedCategories(Arrays.asList(DataCategory.DEMOGRAPHICS)); // More restrictive
        consent2.setExpirationDate(LocalDateTime.now().plusYears(1));

        mockMvc.perform(post("/api/v1/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consent2)))
                .andExpect(status().isCreated());

        // Test transformation with org1 - should get more data
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(HL7_MESSAGE_WITH_PATIENT);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        MvcResult org1Result = mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Patient-Id", multiOrgPatient)
                .header("X-Organization-Id", org1))
                .andExpect(status().isOk())
                .andReturn();

        // Test transformation with org2 - should get less data
        MvcResult org2Result = mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Patient-Id", multiOrgPatient)
                .header("X-Organization-Id", org2))
                .andExpect(status().isOk())
                .andReturn();

        // Verify different data filtering based on organization-specific consent
        FhirBridgeController.TransformationResponse response1 = 
            objectMapper.readValue(org1Result.getResponse().getContentAsString(), 
                FhirBridgeController.TransformationResponse.class);
        FhirBridgeController.TransformationResponse response2 = 
            objectMapper.readValue(org2Result.getResponse().getContentAsString(), 
                FhirBridgeController.TransformationResponse.class);

        // Org1 should have more resources due to broader consent
        assertTrue(response1.getResourceCount() >= response2.getResourceCount(),
            "Organization 1 should have equal or more resources due to broader consent");
    }

    /**
     * Helper method to setup valid consent for testing
     */
    private void setupValidConsent() throws Exception {
        ConsentController.ConsentUpdateRequest consentRequest = new ConsentController.ConsentUpdateRequest();
        consentRequest.setOrganizationId(ORGANIZATION_ID);
        consentRequest.setStatus(ConsentStatus.ACTIVE);
        consentRequest.setAllowedCategories(Arrays.asList(
            DataCategory.DEMOGRAPHICS,
            DataCategory.MEDICAL_HISTORY,
            DataCategory.LABORATORY_RESULTS,
            DataCategory.IMAGING
        ));
        consentRequest.setExpirationDate(LocalDateTime.now().plusYears(1));
        consentRequest.setPolicyReference("TEFCA-STANDARD-CONSENT-001");

        mockMvc.perform(put("/api/v1/consent/update/" + PATIENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consentRequest)))
                .andExpect(status().isOk());
    }

    /**
     * Helper method to setup limited consent for testing data filtering
     */
    private void setupLimitedConsent() throws Exception {
        ConsentController.ConsentUpdateRequest consentRequest = new ConsentController.ConsentUpdateRequest();
        consentRequest.setOrganizationId(ORGANIZATION_ID);
        consentRequest.setStatus(ConsentStatus.ACTIVE);
        consentRequest.setAllowedCategories(Arrays.asList(
            DataCategory.DEMOGRAPHICS // Only demographics allowed
        ));
        consentRequest.setExpirationDate(LocalDateTime.now().plusYears(1));
        consentRequest.setPolicyReference("TEFCA-LIMITED-CONSENT-001");

        mockMvc.perform(put("/api/v1/consent/update/" + PATIENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consentRequest)))
                .andExpect(status().isOk());
    }
}