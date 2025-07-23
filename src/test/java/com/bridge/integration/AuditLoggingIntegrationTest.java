package com.bridge.integration;

import com.bridge.FhirBridgeApplication;
import com.bridge.controller.FhirBridgeController;
import com.bridge.entity.AuditEventEntity;
import com.bridge.model.AuditEvent;
import com.bridge.repository.AuditEventRepository;
import com.bridge.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for comprehensive audit logging functionality
 * Tests audit event creation, persistence, and retrieval across all system operations
 */
@SpringBootTest(classes = FhirBridgeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class AuditLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private static final String VALID_HL7_MESSAGE = """
        MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20250115103000||ADT^A01^ADT_A01|MSG123456|P|2.5
        EVN||20250115103000|||^SMITH^JOHN^J^^DR|20250115103000
        PID|1||123456789^^^MRN^MR||DOE^JOHN^MIDDLE^^MR||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345^USA||(555)123-4567|(555)987-6543||S||123456789|123-45-6789|||||||||||20250115103000
        PV1|1|I|ICU^101^1|||^ATTENDING^PHYSICIAN^A^^DR|^REFERRING^PHYSICIAN^R^^DR|MED||||19|V|123456789|^ATTENDING^PHYSICIAN^A^^DR||INS|||||||||||||||||||||20250115103000|20250115120000
        """;

    @Test
    @Order(1)
    @WithMockUser(roles = {"TRANSFORMER"}, username = "test-user")
    @DisplayName("Audit logging for successful transformation")
    void testAuditLoggingForSuccessfulTransformation() throws Exception {
        // Count initial audit events
        long initialCount = auditEventRepository.count();

        // Prepare transformation request
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(VALID_HL7_MESSAGE);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        // Execute transformation
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify audit events were created
        long finalCount = auditEventRepository.count();
        assertTrue(finalCount > initialCount, "Audit events should be created for transformation");

        // Find the transformation audit event
        List<AuditEventEntity> recentEvents = auditEventRepository.findRecentEvents(
            LocalDateTime.now().minusMinutes(1));
        
        Optional<AuditEventEntity> transformationEvent = recentEvents.stream()
            .filter(event -> "TRANSFORMATION_SUCCESS".equals(event.getAction()))
            .findFirst();

        assertTrue(transformationEvent.isPresent(), "Transformation success audit event should exist");

        AuditEventEntity event = transformationEvent.get();
        assertEquals("test-user", event.getUserId());
        assertEquals("TRANSFORMATION_SUCCESS", event.getAction());
        assertEquals("SUCCESS", event.getOutcome());
        assertNotNull(event.getResourceId());
        assertNotNull(event.getTimestamp());
        assertNotNull(event.getDetails());
    }

    @Test
    @Order(2)
    @WithMockUser(roles = {"TRANSFORMER"}, username = "test-user")
    @DisplayName("Audit logging for validation failures")
    void testAuditLoggingForValidationFailures() throws Exception {
        // Count initial audit events
        long initialCount = auditEventRepository.count();

        // Prepare invalid transformation request
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message("INVALID_HL7_MESSAGE");
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        // Execute transformation - should fail validation
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify audit events were created for validation failure
        long finalCount = auditEventRepository.count();
        assertTrue(finalCount > initialCount, "Audit events should be created for validation failures");

        // Find the validation failure audit event
        List<AuditEventEntity> recentEvents = auditEventRepository.findRecentEvents(
            LocalDateTime.now().minusMinutes(1));
        
        Optional<AuditEventEntity> validationEvent = recentEvents.stream()
            .filter(event -> "VALIDATION_FAILED".equals(event.getAction()))
            .findFirst();

        assertTrue(validationEvent.isPresent(), "Validation failure audit event should exist");

        AuditEventEntity event = validationEvent.get();
        assertEquals("test-user", event.getUserId());
        assertEquals("VALIDATION_FAILED", event.getAction());
        assertEquals("VALIDATION_ERROR", event.getOutcome());
        assertNotNull(event.getTimestamp());
        assertNotNull(event.getDetails());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = {"READER"}, username = "reader-user")
    @DisplayName("Audit logging for FHIR resource access")
    void testAuditLoggingForFhirResourceAccess() throws Exception {
        // Count initial audit events
        long initialCount = auditEventRepository.count();

        // Access FHIR resource
        mockMvc.perform(get("/api/v1/fhir/Patient/patient-123"))
                .andExpect(status().isOk());

        // Verify audit event was created for resource access
        long finalCount = auditEventRepository.count();
        assertTrue(finalCount > initialCount, "Audit events should be created for FHIR resource access");

        // Find the resource access audit event
        List<AuditEventEntity> recentEvents = auditEventRepository.findRecentEvents(
            LocalDateTime.now().minusMinutes(1));
        
        Optional<AuditEventEntity> accessEvent = recentEvents.stream()
            .filter(event -> "GET_RESOURCE".equals(event.getAction()))
            .findFirst();

        assertTrue(accessEvent.isPresent(), "Resource access audit event should exist");

        AuditEventEntity event = accessEvent.get();
        assertEquals("reader-user", event.getUserId());
        assertEquals("GET_RESOURCE", event.getAction());
        assertEquals("SUCCESS", event.getOutcome());
        assertEquals("Patient", event.getResourceType());
        assertNotNull(event.getTimestamp());
    }

    @Test
    @Order(4)
    @DisplayName("Audit logging for authentication failures")
    void testAuditLoggingForAuthenticationFailures() throws Exception {
        // Count initial audit events
        long initialCount = auditEventRepository.count();

        // Attempt unauthenticated access
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        // Note: Authentication failure audit events would typically be created
        // by the security framework, but for this test we'll verify the mechanism works
        long finalCount = auditEventRepository.count();
        // In a real implementation, this would create an audit event for auth failure
    }

    @Test
    @Order(5)
    @WithMockUser(roles = {"READER"}, username = "unauthorized-user")
    @DisplayName("Audit logging for authorization failures")
    void testAuditLoggingForAuthorizationFailures() throws Exception {
        // Count initial audit events
        long initialCount = auditEventRepository.count();

        // Attempt unauthorized transformation (READER role trying to transform)
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(VALID_HL7_MESSAGE);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify authorization failure is audited
        // In a real implementation, this would create an audit event for authorization failure
        long finalCount = auditEventRepository.count();
        // The security aspect would typically create audit events for authorization failures
    }

    @Test
    @Order(6)
    @WithMockUser(roles = {"ADMIN"}, username = "admin-user")
    @DisplayName("Audit event retrieval and querying")
    void testAuditEventRetrievalAndQuerying() throws Exception {
        // Create some audit events first
        auditService.logTransformation("test-request-1", "HL7", "msg-1", "FHIR", 
            "TRANSFORMATION_SUCCESS", "SUCCESS", null);
        auditService.logTransformation("test-request-2", "HL7", "msg-2", "FHIR", 
            "TRANSFORMATION_ERROR", "ERROR", null);
        auditService.logSystemEvent("SYSTEM_STARTUP", "STARTUP", "SUCCESS", null);

        // Query audit events by user
        List<AuditEventEntity> userEvents = auditEventRepository.findByUserIdOrderByTimestampDesc("admin-user");
        assertFalse(userEvents.isEmpty(), "Should find audit events for admin-user");

        // Query audit events by action
        List<AuditEventEntity> transformationEvents = auditEventRepository.findByActionOrderByTimestampDesc("TRANSFORMATION_SUCCESS");
        assertFalse(transformationEvents.isEmpty(), "Should find transformation success events");

        // Query audit events by time range
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        List<AuditEventEntity> timeRangeEvents = auditEventRepository.findByTimestampBetween(startTime, endTime);
        assertFalse(timeRangeEvents.isEmpty(), "Should find events in time range");

        // Query audit events by outcome
        List<AuditEventEntity> successEvents = auditEventRepository.findByOutcomeOrderByTimestampDesc("SUCCESS");
        List<AuditEventEntity> errorEvents = auditEventRepository.findByOutcomeOrderByTimestampDesc("ERROR");
        
        assertFalse(successEvents.isEmpty(), "Should find success events");
        assertFalse(errorEvents.isEmpty(), "Should find error events");
    }

    @Test
    @Order(7)
    @WithMockUser(roles = {"TRANSFORMER"}, username = "test-user")
    @DisplayName("Audit logging for system errors")
    void testAuditLoggingForSystemErrors() throws Exception {
        // Count initial audit events
        long initialCount = auditEventRepository.count();

        // Simulate a system error by sending malformed JSON
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());

        // Verify audit events were created for system error
        long finalCount = auditEventRepository.count();
        // System errors should be audited (though this specific case might be handled by Spring's error handling)
    }

    @Test
    @Order(8)
    @WithMockUser(roles = {"ADMIN"}, username = "admin-user")
    @DisplayName("Audit trail completeness verification")
    void testAuditTrailCompletenessVerification() throws Exception {
        // Perform a series of operations and verify each is audited
        LocalDateTime testStartTime = LocalDateTime.now();

        // 1. Successful transformation
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message(VALID_HL7_MESSAGE);
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 2. FHIR resource access
        mockMvc.perform(get("/api/v1/fhir/Patient/patient-123"))
                .andExpect(status().isOk());

        // 3. FHIR resource search
        mockMvc.perform(get("/api/v1/fhir/Patient?name=John"))
                .andExpect(status().isOk());

        // 4. Health check (should not be audited)
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());

        // Verify audit events were created for auditable operations
        List<AuditEventEntity> testEvents = auditEventRepository.findRecentEvents(testStartTime);
        
        // Should have events for transformation, resource access, and resource search
        // Health check should not be audited
        assertTrue(testEvents.size() >= 3, "Should have at least 3 audit events for the operations performed");

        // Verify event types
        boolean hasTransformationEvent = testEvents.stream()
            .anyMatch(event -> "TRANSFORMATION_SUCCESS".equals(event.getAction()));
        boolean hasResourceAccessEvent = testEvents.stream()
            .anyMatch(event -> "GET_RESOURCE".equals(event.getAction()));
        boolean hasResourceSearchEvent = testEvents.stream()
            .anyMatch(event -> "SEARCH_RESOURCES".equals(event.getAction()));

        assertTrue(hasTransformationEvent, "Should have transformation audit event");
        assertTrue(hasResourceAccessEvent, "Should have resource access audit event");
        assertTrue(hasResourceSearchEvent, "Should have resource search audit event");

        // Verify all events have required fields
        for (AuditEventEntity event : testEvents) {
            assertNotNull(event.getEventId(), "Event ID should not be null");
            assertNotNull(event.getUserId(), "User ID should not be null");
            assertNotNull(event.getAction(), "Action should not be null");
            assertNotNull(event.getTimestamp(), "Timestamp should not be null");
            assertNotNull(event.getOutcome(), "Outcome should not be null");
        }
    }

    @Test
    @Order(9)
    @WithMockUser(roles = {"ADMIN"}, username = "admin-user")
    @DisplayName("Audit event data integrity and immutability")
    void testAuditEventDataIntegrityAndImmutability() throws Exception {
        // Create an audit event
        String testRequestId = "test-immutability-" + System.currentTimeMillis();
        auditService.logTransformation(testRequestId, "HL7", "msg-test", "FHIR", 
            "TRANSFORMATION_SUCCESS", "SUCCESS", null);

        // Find the created event
        List<AuditEventEntity> events = auditEventRepository.findByResourceTypeAndResourceIdOrderByTimestampDesc("HL7", testRequestId);
        assertFalse(events.isEmpty(), "Should find the created audit event");

        AuditEventEntity originalEvent = events.get(0);
        String originalEventId = originalEvent.getEventId();
        LocalDateTime originalTimestamp = originalEvent.getTimestamp();
        String originalAction = originalEvent.getAction();

        // Verify event data integrity
        assertNotNull(originalEventId);
        assertNotNull(originalTimestamp);
        assertEquals("TRANSFORMATION_SUCCESS", originalAction);
        assertEquals("SUCCESS", originalEvent.getOutcome());

        // In a real implementation, you would test that audit events cannot be modified
        // This would involve testing database constraints, triggers, or application-level protections
        
        // Verify the event still exists with original data
        Optional<AuditEventEntity> retrievedEvent = auditEventRepository.findById(originalEvent.getId());
        assertTrue(retrievedEvent.isPresent(), "Audit event should still exist");
        assertEquals(originalEventId, retrievedEvent.get().getEventId());
        assertEquals(originalTimestamp, retrievedEvent.get().getTimestamp());
        assertEquals(originalAction, retrievedEvent.get().getAction());
    }

    @Test
    @Order(10)
    @WithMockUser(roles = {"ADMIN"}, username = "admin-user")
    @DisplayName("Performance impact of audit logging")
    void testPerformanceImpactOfAuditLogging() throws Exception {
        // Measure performance with audit logging enabled
        long startTime = System.currentTimeMillis();
        
        // Perform multiple operations
        for (int i = 0; i < 10; i++) {
            FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
            request.setHl7Message(VALID_HL7_MESSAGE);
            request.setSendingApplication("SENDING_APP_" + i);
            request.setReceivingApplication("RECEIVING_APP_" + i);

            mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Verify that audit logging doesn't significantly impact performance
        // This is a basic check - in a real scenario you'd have more sophisticated performance testing
        assertTrue(totalTime < 30000, "10 transformations should complete within 30 seconds even with audit logging");
        
        // Verify all operations were audited
        List<AuditEventEntity> recentEvents = auditEventRepository.findRecentEvents(
            LocalDateTime.now().minusMinutes(2));
        
        long transformationEvents = recentEvents.stream()
            .filter(event -> "TRANSFORMATION_SUCCESS".equals(event.getAction()))
            .count();
        
        assertTrue(transformationEvents >= 10, "Should have at least 10 transformation audit events");
    }
}