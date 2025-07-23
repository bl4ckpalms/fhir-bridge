package com.bridge.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bridge.service.StructuredLoggingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoggingIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private StructuredLoggingService structuredLoggingService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ListAppender<ILoggingEvent> listAppender;
    private Logger rootLogger;
    
    @BeforeEach
    void setUp() {
        // Set up test logger to capture logs
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        listAppender = new ListAppender<>();
        listAppender.start();
        rootLogger.addAppender(listAppender);
        
        // Clear MDC
        MDC.clear();
    }
    
    @Test
    void shouldLogRequestsWithCorrelationIds() throws Exception {
        String correlationId = "test-correlation-123";
        
        mockMvc.perform(get("/api/v1/health")
                .header("X-Correlation-ID", correlationId))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Correlation-ID", correlationId))
            .andExpect(header().exists("X-Request-ID"));
        
        // Verify logs contain correlation ID
        List<ILoggingEvent> logEvents = listAppender.list;
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("STRUCTURED_LOG") && 
            event.getMessage().contains(correlationId)));
    }
    
    @Test
    void shouldLogErrorsWithStructuredFormat() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.requestId").exists());
        
        // Verify error logging
        List<ILoggingEvent> logEvents = listAppender.list;
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("STRUCTURED_LOG") && 
            event.getMessage().contains("HTTP_REQUEST")));
    }
    
    @Test
    @WithMockUser(roles = {"TRANSFORMER"})
    void shouldLogTransformationRequests() throws Exception {
        String validRequest = """
            {
                "hl7Message": "MSH|^~\\\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20230101120000||ADT^A01|12345|P|2.5",
                "sendingApplication": "TEST_APP",
                "receivingApplication": "FHIR_BRIDGE"
            }
            """;
        
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
            .andExpect(status().isOk());
        
        // Verify transformation logging
        List<ILoggingEvent> logEvents = listAppender.list;
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Starting HL7 to FHIR transformation")));
    }
    
    @Test
    void shouldLogPerformanceMetrics() throws Exception {
        // Test structured logging service directly
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("memoryUsedMB", 256);
        metrics.put("cpuUsagePercent", 75.5);
        
        structuredLoggingService.logPerformanceMetrics(
            "TestOperation", 500, "SUCCESS", metrics
        );
        
        // Verify performance logging
        List<ILoggingEvent> logEvents = listAppender.list;
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("STRUCTURED_LOG") && 
            event.getMessage().contains("PERFORMANCE")));
        
        // Parse and verify JSON structure
        ILoggingEvent performanceEvent = logEvents.stream()
            .filter(event -> event.getMessage().contains("PERFORMANCE"))
            .findFirst()
            .orElseThrow();
        
        String jsonLog = performanceEvent.getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals("PERFORMANCE", logNode.get("eventType").asText());
        assertEquals("TestOperation", logNode.get("operation").asText());
        assertEquals(500, logNode.get("executionTimeMs").asLong());
        assertEquals("SUCCESS", logNode.get("status").asText());
        assertEquals(256, logNode.get("memoryUsedMB").asInt());
        assertEquals(75.5, logNode.get("cpuUsagePercent").asDouble());
    }
    
    @Test
    void shouldLogSecurityEvents() throws Exception {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("authMethod", "JWT");
        additionalData.put("userAgent", "TestClient/1.0");
        
        structuredLoggingService.logSecurityEvent(
            "AUTHENTICATION", "LOGIN_ATTEMPT", "SUCCESS", 
            "test-user", "192.168.1.100", additionalData
        );
        
        // Verify security logging
        List<ILoggingEvent> logEvents = listAppender.list;
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("STRUCTURED_LOG") && 
            event.getMessage().contains("AUTHENTICATION")));
        
        // Parse and verify JSON structure
        ILoggingEvent securityEvent = logEvents.stream()
            .filter(event -> event.getMessage().contains("AUTHENTICATION"))
            .findFirst()
            .orElseThrow();
        
        String jsonLog = securityEvent.getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals("AUTHENTICATION", logNode.get("eventType").asText());
        assertEquals("LOGIN_ATTEMPT", logNode.get("action").asText());
        assertEquals("SUCCESS", logNode.get("outcome").asText());
        assertEquals("test-user", logNode.get("userId").asText());
        assertEquals("192.168.1.100", logNode.get("ipAddress").asText());
        assertEquals("SECURITY", logNode.get("category").asText());
        assertEquals("JWT", logNode.get("authMethod").asText());
    }
    
    @Test
    void shouldLogBusinessEvents() throws Exception {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("previousStatus", "ACTIVE");
        additionalData.put("newStatus", "REVOKED");
        additionalData.put("reason", "Patient request");
        
        structuredLoggingService.logBusinessEvent(
            "CONSENT_UPDATE", "STATUS_CHANGE", "ConsentRecord", 
            "consent-12345", additionalData
        );
        
        // Verify business event logging
        List<ILoggingEvent> logEvents = listAppender.list;
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("STRUCTURED_LOG") && 
            event.getMessage().contains("CONSENT_UPDATE")));
        
        // Parse and verify JSON structure
        ILoggingEvent businessEvent = logEvents.stream()
            .filter(event -> event.getMessage().contains("CONSENT_UPDATE"))
            .findFirst()
            .orElseThrow();
        
        String jsonLog = businessEvent.getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals("CONSENT_UPDATE", logNode.get("eventType").asText());
        assertEquals("STATUS_CHANGE", logNode.get("action").asText());
        assertEquals("ConsentRecord", logNode.get("entityType").asText());
        assertEquals("consent-12345", logNode.get("entityId").asText());
        assertEquals("BUSINESS", logNode.get("category").asText());
        assertEquals("ACTIVE", logNode.get("previousStatus").asText());
        assertEquals("REVOKED", logNode.get("newStatus").asText());
        assertEquals("Patient request", logNode.get("reason").asText());
    }
    
    @Test
    void shouldLogHealthEvents() throws Exception {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("connectionCount", 10);
        healthData.put("responseTimeMs", 45);
        healthData.put("availableConnections", 15);
        
        structuredLoggingService.logHealthEvent("DATABASE", "UP", healthData);
        
        // Verify health event logging
        List<ILoggingEvent> logEvents = listAppender.list;
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("STRUCTURED_LOG") && 
            event.getMessage().contains("HEALTH_CHECK")));
        
        // Parse and verify JSON structure
        ILoggingEvent healthEvent = logEvents.stream()
            .filter(event -> event.getMessage().contains("HEALTH_CHECK"))
            .findFirst()
            .orElseThrow();
        
        String jsonLog = healthEvent.getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals("HEALTH_CHECK", logNode.get("eventType").asText());
        assertEquals("STATUS", logNode.get("action").asText());
        assertEquals("DATABASE", logNode.get("component").asText());
        assertEquals("UP", logNode.get("status").asText());
        assertEquals("HEALTH", logNode.get("category").asText());
        assertEquals(10, logNode.get("connectionCount").asInt());
        assertEquals(45, logNode.get("responseTimeMs").asInt());
        assertEquals(15, logNode.get("availableConnections").asInt());
    }
    
    @Test
    void shouldMaintainLogContextAcrossRequests() throws Exception {
        String correlationId = "test-correlation-456";
        
        mockMvc.perform(get("/api/v1/health")
                .header("X-Correlation-ID", correlationId))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Correlation-ID", correlationId));
        
        // Verify all logs for this request contain the correlation ID
        List<ILoggingEvent> logEvents = listAppender.list;
        List<ILoggingEvent> requestLogs = logEvents.stream()
            .filter(event -> event.getMessage().contains(correlationId))
            .toList();
        
        assertFalse(requestLogs.isEmpty());
        
        // All request-related logs should have the correlation ID
        requestLogs.forEach(event -> 
            assertTrue(event.getMessage().contains(correlationId)));
    }
    
    @Test
    void shouldLogWithTimestamps() throws Exception {
        structuredLoggingService.logEvent("TEST_EVENT", "TEST_ACTION", "INFO", null);
        
        List<ILoggingEvent> logEvents = listAppender.list;
        ILoggingEvent testEvent = logEvents.stream()
            .filter(event -> event.getMessage().contains("TEST_EVENT"))
            .findFirst()
            .orElseThrow();
        
        String jsonLog = testEvent.getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        // Verify timestamp format
        String timestamp = logNode.get("timestamp").asText();
        assertNotNull(timestamp);
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }
    
    @Test
    void shouldLogServiceMetadata() throws Exception {
        structuredLoggingService.logEvent("TEST_EVENT", "TEST_ACTION", "INFO", null);
        
        List<ILoggingEvent> logEvents = listAppender.list;
        ILoggingEvent testEvent = logEvents.stream()
            .filter(event -> event.getMessage().contains("TEST_EVENT"))
            .findFirst()
            .orElseThrow();
        
        String jsonLog = testEvent.getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        // Verify service metadata
        assertEquals("fhir-bridge", logNode.get("service").asText());
        assertEquals("1.0.0", logNode.get("version").asText());
    }
}