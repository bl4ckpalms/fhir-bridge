package com.bridge.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StructuredLoggingServiceTest {
    
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @InjectMocks
    private StructuredLoggingService structuredLoggingService;
    
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    
    @BeforeEach
    void setUp() {
        // Set up test logger
        logger = (Logger) LoggerFactory.getLogger(StructuredLoggingService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        // Clear MDC
        MDC.clear();
    }
    
    @Test
    void logEvent_ShouldCreateStructuredLogEntry() throws Exception {
        // Given
        String eventType = "TEST_EVENT";
        String action = "TEST_ACTION";
        String level = "INFO";
        Map<String, Object> data = new HashMap<>();
        data.put("testKey", "testValue");
        data.put("testNumber", 123);
        
        // When
        structuredLoggingService.logEvent(eventType, action, level, data);
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        ILoggingEvent logEvent = logEvents.get(0);
        assertEquals(ch.qos.logback.classic.Level.INFO, logEvent.getLevel());
        assertTrue(logEvent.getMessage().startsWith("STRUCTURED_LOG:"));
        
        // Parse JSON log
        String jsonLog = logEvent.getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals(eventType, logNode.get("eventType").asText());
        assertEquals(action, logNode.get("action").asText());
        assertEquals("fhir-bridge", logNode.get("service").asText());
        assertEquals("1.0.0", logNode.get("version").asText());
        assertEquals("testValue", logNode.get("testKey").asText());
        assertEquals(123, logNode.get("testNumber").asInt());
        assertNotNull(logNode.get("timestamp"));
    }
    
    @Test
    void logEvent_ShouldIncludeMDCContext() throws Exception {
        // Given
        MDC.put("requestId", "test-request-123");
        MDC.put("userId", "test-user");
        MDC.put("correlationId", "test-correlation-456");
        
        // When
        structuredLoggingService.logEvent("TEST_EVENT", "TEST_ACTION", "INFO", null);
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        String jsonLog = logEvents.get(0).getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals("test-request-123", logNode.get("requestId").asText());
        assertEquals("test-user", logNode.get("userId").asText());
        assertEquals("test-correlation-456", logNode.get("correlationId").asText());
    }
    
    @Test
    void logTransformationEvent_ShouldLogWithCorrectStructure() throws Exception {
        // Given
        String sourceFormat = "HL7v2";
        String targetFormat = "FHIR";
        String status = "SUCCESS";
        long processingTimeMs = 150;
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("resourceCount", 3);
        
        // When
        structuredLoggingService.logTransformationEvent(
            sourceFormat, targetFormat, status, processingTimeMs, additionalData
        );
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        String jsonLog = logEvents.get(0).getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals("TRANSFORMATION", logNode.get("eventType").asText());
        assertEquals("PROCESS", logNode.get("action").asText());
        assertEquals(sourceFormat, logNode.get("sourceFormat").asText());
        assertEquals(targetFormat, logNode.get("targetFormat").asText());
        assertEquals(status, logNode.get("status").asText());
        assertEquals(processingTimeMs, logNode.get("processingTimeMs").asLong());
        assertEquals("TRANSFORMATION", logNode.get("category").asText());
        assertEquals(3, logNode.get("resourceCount").asInt());
    }
    
    @Test
    void logApiEvent_ShouldLogWithCorrectStructure() throws Exception {
        // Given
        String method = "POST";
        String path = "/api/v1/transform";
        int statusCode = 200;
        long responseTimeMs = 250;
        String userId = "test-user";
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("requestSize", 1024);
        
        // When
        structuredLoggingService.logApiEvent(
            method, path, statusCode, responseTimeMs, userId, additionalData
        );
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        String jsonLog = logEvents.get(0).getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals("API_REQUEST", logNode.get("eventType").asText());
        assertEquals("PROCESS", logNode.get("action").asText());
        assertEquals(method, logNode.get("httpMethod").asText());
        assertEquals(path, logNode.get("path").asText());
        assertEquals(statusCode, logNode.get("statusCode").asInt());
        assertEquals(responseTimeMs, logNode.get("responseTimeMs").asLong());
        assertEquals(userId, logNode.get("userId").asText());
        assertEquals("API", logNode.get("category").asText());
        assertEquals(1024, logNode.get("requestSize").asInt());
    }
    
    @Test
    void logSecurityEvent_ShouldLogWithCorrectStructure() throws Exception {
        // Given
        String eventType = "AUTHENTICATION";
        String action = "LOGIN";
        String outcome = "SUCCESS";
        String userId = "test-user";
        String ipAddress = "192.168.1.100";
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("authMethod", "JWT");
        
        // When
        structuredLoggingService.logSecurityEvent(
            eventType, action, outcome, userId, ipAddress, additionalData
        );
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        String jsonLog = logEvents.get(0).getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals(eventType, logNode.get("eventType").asText());
        assertEquals(action, logNode.get("action").asText());
        assertEquals(outcome, logNode.get("outcome").asText());
        assertEquals(userId, logNode.get("userId").asText());
        assertEquals(ipAddress, logNode.get("ipAddress").asText());
        assertEquals("SECURITY", logNode.get("category").asText());
        assertEquals("JWT", logNode.get("authMethod").asText());
    }
    
    @Test
    void logBusinessEvent_ShouldLogWithCorrectStructure() throws Exception {
        // Given
        String eventType = "CONSENT_UPDATE";
        String action = "MODIFY";
        String entityType = "ConsentRecord";
        String entityId = "consent-123";
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("previousStatus", "ACTIVE");
        additionalData.put("newStatus", "REVOKED");
        
        // When
        structuredLoggingService.logBusinessEvent(
            eventType, action, entityType, entityId, additionalData
        );
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        String jsonLog = logEvents.get(0).getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals(eventType, logNode.get("eventType").asText());
        assertEquals(action, logNode.get("action").asText());
        assertEquals(entityType, logNode.get("entityType").asText());
        assertEquals(entityId, logNode.get("entityId").asText());
        assertEquals("BUSINESS", logNode.get("category").asText());
        assertEquals("ACTIVE", logNode.get("previousStatus").asText());
        assertEquals("REVOKED", logNode.get("newStatus").asText());
    }
    
    @Test
    void logPerformanceMetrics_ShouldLogWithCorrectStructure() throws Exception {
        // Given
        String operation = "HL7ToFhirTransformation";
        long executionTimeMs = 1500;
        String status = "SUCCESS";
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("memoryUsedMB", 128);
        metrics.put("cpuUsagePercent", 45.5);
        
        // When
        structuredLoggingService.logPerformanceMetrics(operation, executionTimeMs, status, metrics);
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        // Should log as WARN because execution time > 1000ms
        assertEquals(ch.qos.logback.classic.Level.WARN, logEvents.get(0).getLevel());
        
        String jsonLog = logEvents.get(0).getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals("PERFORMANCE", logNode.get("eventType").asText());
        assertEquals("MEASURE", logNode.get("action").asText());
        assertEquals(operation, logNode.get("operation").asText());
        assertEquals(executionTimeMs, logNode.get("executionTimeMs").asLong());
        assertEquals(status, logNode.get("status").asText());
        assertEquals("PERFORMANCE", logNode.get("category").asText());
        assertEquals(128, logNode.get("memoryUsedMB").asInt());
        assertEquals(45.5, logNode.get("cpuUsagePercent").asDouble());
    }
    
    @Test
    void logHealthEvent_ShouldLogWithCorrectStructure() throws Exception {
        // Given
        String component = "DATABASE";
        String status = "UP";
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("connectionCount", 5);
        healthData.put("responseTimeMs", 25);
        
        // When
        structuredLoggingService.logHealthEvent(component, status, healthData);
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        String jsonLog = logEvents.get(0).getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        assertEquals("HEALTH_CHECK", logNode.get("eventType").asText());
        assertEquals("STATUS", logNode.get("action").asText());
        assertEquals(component, logNode.get("component").asText());
        assertEquals(status, logNode.get("status").asText());
        assertEquals("HEALTH", logNode.get("category").asText());
        assertEquals(5, logNode.get("connectionCount").asInt());
        assertEquals(25, logNode.get("responseTimeMs").asInt());
    }
    
    @Test
    void logEvent_ShouldHandleDifferentLogLevels() {
        // Test ERROR level
        structuredLoggingService.logEvent("TEST", "ACTION", "ERROR", null);
        assertEquals(ch.qos.logback.classic.Level.ERROR, listAppender.list.get(0).getLevel());
        
        listAppender.list.clear();
        
        // Test WARN level
        structuredLoggingService.logEvent("TEST", "ACTION", "WARN", null);
        assertEquals(ch.qos.logback.classic.Level.WARN, listAppender.list.get(0).getLevel());
        
        listAppender.list.clear();
        
        // Test DEBUG level
        structuredLoggingService.logEvent("TEST", "ACTION", "DEBUG", null);
        assertEquals(ch.qos.logback.classic.Level.DEBUG, listAppender.list.get(0).getLevel());
        
        listAppender.list.clear();
        
        // Test default level (INFO)
        structuredLoggingService.logEvent("TEST", "ACTION", "UNKNOWN", null);
        assertEquals(ch.qos.logback.classic.Level.INFO, listAppender.list.get(0).getLevel());
    }
    
    @Test
    void logEvent_ShouldHandleNullData() throws Exception {
        // When
        structuredLoggingService.logEvent("TEST_EVENT", "TEST_ACTION", "INFO", null);
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        String jsonLog = logEvents.get(0).getMessage().substring("STRUCTURED_LOG: ".length());
        JsonNode logNode = objectMapper.readTree(jsonLog);
        
        // Should still have base fields
        assertEquals("TEST_EVENT", logNode.get("eventType").asText());
        assertEquals("TEST_ACTION", logNode.get("action").asText());
        assertEquals("fhir-bridge", logNode.get("service").asText());
        assertNotNull(logNode.get("timestamp"));
    }
}