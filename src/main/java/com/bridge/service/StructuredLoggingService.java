package com.bridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for structured logging with consistent format and metadata
 */
@Service
public class StructuredLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredLoggingService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Log a structured event with metadata
     */
    public void logEvent(String eventType, String action, String level, Map<String, Object> data) {
        Map<String, Object> logEntry = createBaseLogEntry(eventType, action);
        
        if (data != null) {
            logEntry.putAll(data);
        }
        
        String jsonLog = toJsonString(logEntry);
        
        switch (level.toUpperCase()) {
            case "ERROR" -> logger.error("STRUCTURED_LOG: {}", jsonLog);
            case "WARN" -> logger.warn("STRUCTURED_LOG: {}", jsonLog);
            case "INFO" -> logger.info("STRUCTURED_LOG: {}", jsonLog);
            case "DEBUG" -> logger.debug("STRUCTURED_LOG: {}", jsonLog);
            default -> logger.info("STRUCTURED_LOG: {}", jsonLog);
        }
    }
    
    /**
     * Log transformation events with specific metadata
     */
    public void logTransformationEvent(String sourceFormat, String targetFormat, String status, 
                                     long processingTimeMs, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("sourceFormat", sourceFormat);
        data.put("targetFormat", targetFormat);
        data.put("status", status);
        data.put("processingTimeMs", processingTimeMs);
        data.put("category", "TRANSFORMATION");
        
        if (additionalData != null) {
            data.putAll(additionalData);
        }
        
        String level = "SUCCESS".equals(status) ? "INFO" : "ERROR";
        logEvent("TRANSFORMATION", "PROCESS", level, data);
    }
    
    /**
     * Log API request/response events
     */
    public void logApiEvent(String method, String path, int statusCode, long responseTimeMs, 
                           String userId, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("httpMethod", method);
        data.put("path", path);
        data.put("statusCode", statusCode);
        data.put("responseTimeMs", responseTimeMs);
        data.put("userId", userId);
        data.put("category", "API");
        
        if (additionalData != null) {
            data.putAll(additionalData);
        }
        
        String level = statusCode >= 400 ? "WARN" : "INFO";
        logEvent("API_REQUEST", "PROCESS", level, data);
    }
    
    /**
     * Log security events
     */
    public void logSecurityEvent(String eventType, String action, String outcome, String userId, 
                                String ipAddress, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("outcome", outcome);
        data.put("userId", userId);
        data.put("ipAddress", ipAddress);
        data.put("category", "SECURITY");
        
        if (additionalData != null) {
            data.putAll(additionalData);
        }
        
        String level = "SUCCESS".equals(outcome) ? "INFO" : "WARN";
        logEvent(eventType, action, level, data);
    }
    
    /**
     * Log business events
     */
    public void logBusinessEvent(String eventType, String action, String entityType, String entityId, 
                                Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityType", entityType);
        data.put("entityId", entityId);
        data.put("category", "BUSINESS");
        
        if (additionalData != null) {
            data.putAll(additionalData);
        }
        
        logEvent(eventType, action, "INFO", data);
    }
    
    /**
     * Log performance metrics
     */
    public void logPerformanceMetrics(String operation, long executionTimeMs, String status, 
                                    Map<String, Object> metrics) {
        Map<String, Object> data = new HashMap<>();
        data.put("operation", operation);
        data.put("executionTimeMs", executionTimeMs);
        data.put("status", status);
        data.put("category", "PERFORMANCE");
        
        if (metrics != null) {
            data.putAll(metrics);
        }
        
        String level = executionTimeMs > 1000 ? "WARN" : "INFO";
        logEvent("PERFORMANCE", "MEASURE", level, data);
    }
    
    /**
     * Log system health events
     */
    public void logHealthEvent(String component, String status, Map<String, Object> healthData) {
        Map<String, Object> data = new HashMap<>();
        data.put("component", component);
        data.put("status", status);
        data.put("category", "HEALTH");
        
        if (healthData != null) {
            data.putAll(healthData);
        }
        
        String level = "UP".equals(status) ? "INFO" : "ERROR";
        logEvent("HEALTH_CHECK", "STATUS", level, data);
    }
    
    /**
     * Create base log entry with common metadata
     */
    private Map<String, Object> createBaseLogEntry(String eventType, String action) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        logEntry.put("eventType", eventType);
        logEntry.put("action", action);
        logEntry.put("service", "fhir-bridge");
        logEntry.put("version", "1.0.0");
        
        // Add MDC context
        String correlationId = MDC.get("correlationId");
        String requestId = MDC.get("requestId");
        String operation = MDC.get("operation");
        String userId = MDC.get("userId");
        
        if (correlationId != null) {
            logEntry.put("correlationId", correlationId);
        }
        if (requestId != null) {
            logEntry.put("requestId", requestId);
        }
        if (operation != null) {
            logEntry.put("operation", operation);
        }
        if (userId != null) {
            logEntry.put("userId", userId);
        }
        
        return logEntry;
    }
    
    /**
     * Convert log entry to JSON string
     */
    private String toJsonString(Map<String, Object> logEntry) {
        try {
            return objectMapper.writeValueAsString(logEntry);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize log entry to JSON: {}", e.getMessage());
            return logEntry.toString();
        }
    }
}