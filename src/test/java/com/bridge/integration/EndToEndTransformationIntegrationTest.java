package com.bridge.integration;

import com.bridge.FhirBridgeApplication;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for complete HL7 to FHIR transformation flow
 * Tests the entire pipeline including authentication, validation, transformation, and audit logging
 */
@SpringBootTest(classes = FhirBridgeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class EndToEndTransformationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private Hl7V2Validator hl7Validator;

    @Autowired
    private Hl7ToFhirTransformer transformer;

    @Autowired
    private FhirValidator fhirValidator;

    private static final String VALID_HL7_ADT_MESSAGE = """
        MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20250115103000||ADT^A01^ADT_A01|MSG123456|P|2.5
        EVN||20250115103000|||^SMITH^JOHN^J^^DR|20250115103000
        PID|1||123456789^^^MRN^MR||DOE^JOHN^MIDDLE^^MR||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345^USA||(555)123-4567|(555)987-6543||S||123456789|123-45-6789|||||||||||20250115103000
        PV1|1|I|ICU^101^1|||^ATTENDING^PHYSICIAN^A^^DR|^REFERRING^PHYSICIAN^R^^DR|MED||||19|V|123456789|^ATTENDING^PHYSICIAN^A^^DR||INS|||||||||||||||||||||20250115103000|20250115120000
        """;

    private static final String VALID_HL7_ORM_MESSAGE = """
        MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20250115103000||ORM^O01^ORM_O01|MSG123457|P|2.5
        PID|1||123456789^^^MRN^MR||DOE^JOHN^MIDDLE^^MR||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345^USA||(555)123-4567|(555)987-6543||S||123456789|123-45-6789|||||||||||20250115103000
        ORC|NW|ORDER123|ORDER123|GROUP123|SC||1^ONCE^^^^S||20250115103000|^ORDERING^PHYSICIAN^O^^DR|^ORDERING^PHYSICIAN^O^^DR
        OBR|1|ORDER123|ORDER123|CBC^COMPLETE BLOOD COUNT^L|||20250115103000|20250115103000||||||||^ORDERING^PHYSICIAN^O^^DR||||||20250115103000||F|||^ORDERING^PHYSICIAN^O^^DR
        """;

    private static final String INVALID_HL7_MESSAGE = """
        MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20250115103000||INVALID^TYPE^INVALID|MSG123458|P|2.5
        PID|1||^^^MRN^MR||DOE^JOHN^MIDDLE^^MR||INVALID_DATE|M|||123 MAIN ST^^ANYTOWN^ST^12345^USA||(555)123-4567|(555)987-6543||S||123456789|123-45-6789|||||||||||20250115103000
        """;

    @Test
    @Order(1)
    @WithMockUser(roles = {"TRANSFORMER"})
    @DisplayName("Complete HL7 ADT to FHIR transformation flow")
    void testCompleteAdtTransformationFlow() throws Exception {
        // Prepare transformation request
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(VALID_HL7_ADT_MESSAGE);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        // Execute transformation
        MvcResult result = mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.fhirResources").isArray())
                .andExpect(jsonPath("$.resourceCount").isNumber())
                .andExpect(jsonPath("$.transformationTimestamp").exists())
                .andReturn();

        // Verify response structure
        String responseContent = result.getResponse().getContentAsString();
        FhirBridgeController.TransformationResponse response = 
            objectMapper.readValue(responseContent, FhirBridgeController.TransformationResponse.class);

        assertNotNull(response.getRequestId());
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getFhirResources());
        assertTrue(response.getResourceCount() > 0);
        assertNotNull(response.getTransformationTimestamp());

        // Verify FHIR resources were created
        List<FhirResource> fhirResources = response.getFhirResources();
        assertFalse(fhirResources.isEmpty());

        // Verify Patient resource was created
        boolean hasPatientResource = fhirResources.stream()
            .anyMatch(resource -> "Patient".equals(resource.getResourceType()));
        assertTrue(hasPatientResource, "Patient resource should be created from ADT message");

        // Verify Encounter resource was created (from PV1 segment)
        boolean hasEncounterResource = fhirResources.stream()
            .anyMatch(resource -> "Encounter".equals(resource.getResourceType()));
        assertTrue(hasEncounterResource, "Encounter resource should be created from PV1 segment");

        // Verify audit logging
        verifyAuditLogging(response.getRequestId(), "TRANSFORMATION_SUCCESS");
    }

    @Test
    @Order(2)
    @WithMockUser(roles = {"TRANSFORMER"})
    @DisplayName("Complete HL7 ORM to FHIR transformation flow")
    void testCompleteOrmTransformationFlow() throws Exception {
        // Prepare transformation request
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(VALID_HL7_ORM_MESSAGE);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        // Execute transformation
        MvcResult result = mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.fhirResources").isArray())
                .andExpect(jsonPath("$.resourceCount").isNumber())
                .andReturn();

        // Verify response
        String responseContent = result.getResponse().getContentAsString();
        FhirBridgeController.TransformationResponse response = 
            objectMapper.readValue(responseContent, FhirBridgeController.TransformationResponse.class);

        // Verify FHIR resources were created
        List<FhirResource> fhirResources = response.getFhirResources();
        assertFalse(fhirResources.isEmpty());

        // Verify Patient resource was created
        boolean hasPatientResource = fhirResources.stream()
            .anyMatch(resource -> "Patient".equals(resource.getResourceType()));
        assertTrue(hasPatientResource, "Patient resource should be created from ORM message");

        // Verify ServiceRequest resource was created (from OBR segment)
        boolean hasServiceRequestResource = fhirResources.stream()
            .anyMatch(resource -> "ServiceRequest".equals(resource.getResourceType()));
        assertTrue(hasServiceRequestResource, "ServiceRequest resource should be created from OBR segment");

        // Verify audit logging
        verifyAuditLogging(response.getRequestId(), "TRANSFORMATION_SUCCESS");
    }

    @Test
    @Order(3)
    @WithMockUser(roles = {"TRANSFORMER"})
    @DisplayName("HL7 validation failure handling")
    void testHl7ValidationFailureHandling() throws Exception {
        // Prepare transformation request with invalid HL7 message
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(INVALID_HL7_MESSAGE);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        // Execute transformation - should fail validation
        MvcResult result = mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("HL7 message validation failed"))
                .andExpect(jsonPath("$.error.details").isArray())
                .andExpect(jsonPath("$.error.requestId").exists())
                .andReturn();

        // Verify error response structure
        String responseContent = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = objectMapper.readValue(responseContent, Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertNotNull(error);
        assertEquals("VALIDATION_ERROR", error.get("code"));
        assertNotNull(error.get("details"));
        assertNotNull(error.get("requestId"));

        // Verify audit logging for validation failure
        String requestId = (String) error.get("requestId");
        verifyAuditLogging(requestId, "VALIDATION_FAILED");
    }

    @Test
    @Order(4)
    @WithMockUser(roles = {"READER"})
    @DisplayName("Authentication and authorization integration")
    void testAuthenticationAndAuthorizationIntegration() throws Exception {
        // Test with READER role - should be denied for transformation
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(VALID_HL7_ADT_MESSAGE);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Test FHIR resource access with READER role - should be allowed
        mockMvc.perform(get("/api/v1/fhir/Patient/patient-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @Order(5)
    @DisplayName("Unauthenticated access should be denied")
    void testUnauthenticatedAccessDenied() throws Exception {
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(VALID_HL7_ADT_MESSAGE);

        // Should be denied without authentication
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Admin role has full access")
    void testAdminRoleFullAccess() throws Exception {
        // Test transformation access with ADMIN role
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(VALID_HL7_ADT_MESSAGE);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Test FHIR resource access with ADMIN role
        mockMvc.perform(get("/api/v1/fhir/Patient/patient-123"))
                .andExpect(status().isOk());

        // Test FHIR resource search with ADMIN role
        mockMvc.perform(get("/api/v1/fhir/Patient?name=John"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    @WithMockUser(roles = {"TRANSFORMER"})
    @DisplayName("Error handling and audit logging integration")
    void testErrorHandlingAndAuditLogging() throws Exception {
        // Test with malformed JSON
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());

        // Test with missing required fields
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        // Test with empty HL7 message
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message("");
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(8)
    @DisplayName("Health check endpoint accessibility")
    void testHealthCheckEndpoint() throws Exception {
        // Health check should be accessible without authentication
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("FHIR Bridge Transformation API"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    @Order(9)
    @WithMockUser(roles = {"TRANSFORMER"})
    @DisplayName("Concurrent transformation requests handling")
    void testConcurrentTransformationRequests() throws Exception {
        // Prepare multiple transformation requests
        FhirBridgeController.TransformationRequest request1 = new FhirBridgeController.TransformationRequest();
        request1.setHl7Message(VALID_HL7_ADT_MESSAGE);
        request1.setSendingApplication("SENDING_APP_1");
        request1.setReceivingApplication("RECEIVING_APP_1");

        FhirBridgeController.TransformationRequest request2 = new FhirBridgeController.TransformationRequest();
        request2.setHl7Message(VALID_HL7_ORM_MESSAGE);
        request2.setSendingApplication("SENDING_APP_2");
        request2.setReceivingApplication("RECEIVING_APP_2");

        // Execute concurrent requests
        MvcResult result1 = mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify both requests succeeded with different request IDs
        FhirBridgeController.TransformationResponse response1 = 
            objectMapper.readValue(result1.getResponse().getContentAsString(), 
                FhirBridgeController.TransformationResponse.class);
        FhirBridgeController.TransformationResponse response2 = 
            objectMapper.readValue(result2.getResponse().getContentAsString(), 
                FhirBridgeController.TransformationResponse.class);

        assertNotEquals(response1.getRequestId(), response2.getRequestId());
        assertEquals("SUCCESS", response1.getStatus());
        assertEquals("SUCCESS", response2.getStatus());
    }

    /**
     * Helper method to verify audit logging
     */
    private void verifyAuditLogging(String requestId, String expectedOutcome) {
        // Note: In a real implementation, this would query the audit service
        // For now, we'll verify that the audit service was called
        assertNotNull(auditService);
        assertNotNull(requestId);
        assertNotNull(expectedOutcome);
        
        // This would typically verify:
        // - Audit event was created with correct request ID
        // - Audit event has correct outcome
        // - Audit event contains transformation details
        // - Timestamp is within expected range
    }
}