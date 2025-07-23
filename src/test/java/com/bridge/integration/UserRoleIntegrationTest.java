package com.bridge.integration;

import com.bridge.util.TestUserDataLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests demonstrating role-based access control using test user accounts
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class UserRoleIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    void tearDown() {
        TestUserDataLoader.clearSecurityContext();
    }

    @Test
    @DisplayName("System Admin can access system metrics endpoint")
    void testSystemAdminCanAccessSystemMetrics() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-SYSADMIN-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Physician can access patient data transformation endpoint")
    void testPhysicianCanAccessTransformation() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-PHYSICIAN-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();
        
        String sampleHl7Message = "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20250115120000||ADT^A01|MSG001|P|2.4";

        // When & Then
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.TEXT_PLAIN)
                .content(sampleHl7Message))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Nurse can access consent management endpoints")
    void testNurseCanAccessConsentManagement() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-NURSE-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then
        mockMvc.perform(get("/api/v1/consent/status/PAT-TEST-001")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Pharmacist has limited access to patient data")
    void testPharmacistLimitedAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-PHARMACIST-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then - Can read patient data
        mockMvc.perform(get("/api/v1/fhir/Patient/PAT-TEST-001")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // But cannot access system administration endpoints
        mockMvc.perform(get("/api/v1/monitoring/metrics")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Technician has minimal access")
    void testTechnicianMinimalAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-TECHNICIAN-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then - Can access basic health check
        mockMvc.perform(get("/api/v1/monitoring/health")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // But cannot access patient data transformation
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.TEXT_PLAIN)
                .content("MSH|^~\\&|TEST|TEST|TEST|TEST|20250115120000||ADT^A01|MSG001|P|2.4"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Compliance Officer can access audit endpoints")
    void testComplianceOfficerAuditAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-COMPLIANCE-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then
        mockMvc.perform(get("/api/v1/audit/events")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Data Analyst can access bulk data endpoints")
    void testDataAnalystBulkAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-ANALYST-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then
        mockMvc.perform(get("/api/v1/fhir/Patient")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .param("_format", "json")
                .param("_count", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TEFCA Participant can access network endpoints")
    void testTefcaParticipantNetworkAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-TEFCA-PARTICIPANT-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then
        mockMvc.perform(post("/api/v1/tefca/query")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientId\":\"PAT-TEST-001\",\"dataTypes\":[\"Patient\",\"Observation\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Patient can access own consent management")
    void testPatientSelfConsentAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-PATIENT-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then - Can access own consent
        mockMvc.perform(get("/api/v1/consent/status/PAT-TEST-001")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // But cannot access other patient's consent
        mockMvc.perform(get("/api/v1/consent/status/PAT-TEST-002")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Patient Proxy can manage authorized patient consent")
    void testPatientProxyAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-PATIENT-PROXY-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then - Can access authorized patient's consent
        mockMvc.perform(get("/api/v1/consent/status/PAT-TEST-002")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // But cannot access other patient's consent
        mockMvc.perform(get("/api/v1/consent/status/PAT-TEST-001")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API Client can access system integration endpoints")
    void testApiClientSystemAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-API-CLIENT-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.TEXT_PLAIN)
                .content("MSH|^~\\&|EHR_SYSTEM|HOSPITAL|FHIR_BRIDGE|BRIDGE|20250115120000||ADT^A01|MSG001|P|2.4"))
                .andExpect(status().isOk());

        // And can access bulk data operations
        mockMvc.perform(get("/api/v1/fhir/Patient")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .param("_format", "json")
                .param("_count", "1000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Multi-role user has combined permissions")
    void testMultiRoleUserCombinedAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-MULTI-ROLE-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then - Has physician access
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.TEXT_PLAIN)
                .content("MSH|^~\\&|TEST|TEST|TEST|TEST|20250115120000||ADT^A01|MSG001|P|2.4"))
                .andExpect(status().isOk());

        // And has TEFCA participant access
        mockMvc.perform(post("/api/v1/tefca/query")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientId\":\"PAT-TEST-001\",\"dataTypes\":[\"Patient\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Inactive user is denied access")
    void testInactiveUserDeniedAccess() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-INACTIVE-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then - All requests should be unauthorized
        mockMvc.perform(get("/api/v1/monitoring/health")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Limited access user has minimal permissions")
    void testLimitedAccessUserMinimalPermissions() throws Exception {
        // Given
        var token = TestUserDataLoader.getTokenByUserId("TEST-LIMITED-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then - Can access basic health check
        mockMvc.perform(get("/api/v1/monitoring/health")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // But cannot access patient data
        mockMvc.perform(get("/api/v1/fhir/Patient/PAT-TEST-001")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // And cannot access transformation endpoints
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.TEXT_PLAIN)
                .content("MSH|^~\\&|TEST|TEST|TEST|TEST|20250115120000||ADT^A01|MSG001|P|2.4"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-organization access is properly controlled")
    void testCrossOrganizationAccessControl() throws Exception {
        // Given - User from TEST-ORG-002
        var token = TestUserDataLoader.getTokenByUserId("TEST-PHARMACIST-001").orElseThrow();
        String authHeader = token.getTokenType() + " " + token.getSampleToken();

        // When & Then - Should be able to access own organization's data
        mockMvc.perform(get("/api/v1/fhir/Patient")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .param("organization", "TEST-ORG-002")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // But should not be able to access other organization's restricted data
        mockMvc.perform(get("/api/v1/fhir/Patient")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .param("organization", "TEST-ORG-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}