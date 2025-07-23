package com.bridge.service;

import com.bridge.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for comprehensive audit logging and retrieval
 */
public interface AuditService {

    // Core audit logging methods
    
    /**
     * Log a data transformation event
     */
    void logTransformation(String userId, String sourceType, String sourceId, 
                          String targetType, String targetId, String outcome, 
                          Map<String, Object> details);

    /**
     * Log an authentication event
     */
    void logAuthentication(String userId, String method, String outcome, 
                          String ipAddress, Map<String, Object> details);

    /**
     * Log an authorization event
     */
    void logAuthorization(String userId, String resource, String action, 
                         String outcome, Map<String, Object> details);

    /**
     * Log a consent verification event
     */
    void logConsentVerification(String userId, String patientId, String action, 
                               String outcome, Map<String, Object> details);

    /**
     * Log a security event
     */
    void logSecurityEvent(String userId, String eventType, String outcome, 
                         String severity, Map<String, Object> details);

    /**
     * Log a system event
     */
    void logSystemEvent(String component, String action, String outcome, 
                       Map<String, Object> details);

    /**
     * Log a generic audit event
     */
    AuditEvent logEvent(AuditEvent auditEvent);

    // Audit retrieval methods
    
    /**
     * Find audit event by ID
     */
    Optional<AuditEvent> findById(String eventId);

    /**
     * Find audit events by user ID
     */
    List<AuditEvent> findByUserId(String userId);

    /**
     * Find audit events by user ID with pagination
     */
    Page<AuditEvent> findByUserId(String userId, Pageable pageable);

    /**
     * Find audit events by action
     */
    List<AuditEvent> findByAction(String action);

    /**
     * Find audit events by action with pagination
     */
    Page<AuditEvent> findByAction(String action, Pageable pageable);

    /**
     * Find audit events by resource
     */
    List<AuditEvent> findByResource(String resourceType, String resourceId);

    /**
     * Find audit events within time range
     */
    List<AuditEvent> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find audit events within time range with pagination
     */
    Page<AuditEvent> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find failed audit events
     */
    List<AuditEvent> findFailedEvents();

    /**
     * Find failed audit events with pagination
     */
    Page<AuditEvent> findFailedEvents(Pageable pageable);

    /**
     * Find recent audit events (last N hours)
     */
    List<AuditEvent> findRecentEvents(int hours);

    /**
     * Find recent audit events with pagination
     */
    Page<AuditEvent> findRecentEvents(int hours, Pageable pageable);

    // Audit statistics and reporting methods
    
    /**
     * Count audit events by action within time range
     */
    long countByAction(String action, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Count audit events by user within time range
     */
    long countByUser(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Count failed events within time range
     */
    long countFailedEvents(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Generate audit report for compliance
     */
    Map<String, Object> generateComplianceReport(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Generate security events summary
     */
    Map<String, Object> generateSecuritySummary(LocalDateTime startTime, LocalDateTime endTime);

    // Audit maintenance methods
    
    /**
     * Clean up old audit events before specified date
     */
    void cleanupOldEvents(LocalDateTime cutoffDate);

    /**
     * Archive audit events to external storage
     */
    void archiveEvents(LocalDateTime cutoffDate);
}