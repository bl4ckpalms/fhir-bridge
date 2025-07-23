package com.bridge.service;

import com.bridge.entity.AuditEventEntity;
import com.bridge.model.AuditEvent;
import com.bridge.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive audit service implementation for logging and retrieving audit events
 */
@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditEventRepository auditEventRepository;

    @Autowired
    public AuditServiceImpl(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    // Core audit logging methods

    @Override
    public void logTransformation(String userId, String sourceType, String sourceId, 
                                 String targetType, String targetId, String outcome, 
                                 Map<String, Object> details) {
        try {
            AuditEvent auditEvent = new AuditEvent(userId, "DATA_TRANSFORMATION", targetType, targetId, outcome);
            
            Map<String, Object> transformationDetails = new HashMap<>(details != null ? details : new HashMap<>());
            transformationDetails.put("sourceType", sourceType);
            transformationDetails.put("sourceId", sourceId);
            transformationDetails.put("targetType", targetType);
            transformationDetails.put("targetId", targetId);
            transformationDetails.put("transformationType", sourceType + "_TO_" + targetType);
            
            auditEvent.setDetails(transformationDetails);
            logEvent(auditEvent);
            
            logger.info("Logged transformation event: {} -> {} for user: {}", sourceType, targetType, userId);
        } catch (Exception e) {
            logger.error("Failed to log transformation event", e);
        }
    }

    @Override
    public void logAuthentication(String userId, String method, String outcome, 
                                 String ipAddress, Map<String, Object> details) {
        try {
            AuditEvent auditEvent = new AuditEvent(userId, "AUTHENTICATION", "USER", userId, outcome);
            
            Map<String, Object> authDetails = new HashMap<>(details != null ? details : new HashMap<>());
            authDetails.put("authMethod", method);
            authDetails.put("ipAddress", ipAddress);
            authDetails.put("userAgent", authDetails.getOrDefault("userAgent", "Unknown"));
            
            auditEvent.setDetails(authDetails);
            logEvent(auditEvent);
            
            logger.info("Logged authentication event for user: {} with outcome: {}", userId, outcome);
        } catch (Exception e) {
            logger.error("Failed to log authentication event", e);
        }
    }

    @Override
    public void logAuthorization(String userId, String resource, String action, 
                                String outcome, Map<String, Object> details) {
        try {
            AuditEvent auditEvent = new AuditEvent(userId, "AUTHORIZATION", "RESOURCE", resource, outcome);
            
            Map<String, Object> authzDetails = new HashMap<>(details != null ? details : new HashMap<>());
            authzDetails.put("requestedAction", action);
            authzDetails.put("resource", resource);
            authzDetails.put("permissions", authzDetails.getOrDefault("permissions", "Unknown"));
            
            auditEvent.setDetails(authzDetails);
            logEvent(auditEvent);
            
            logger.info("Logged authorization event for user: {} on resource: {} with outcome: {}", 
                       userId, resource, outcome);
        } catch (Exception e) {
            logger.error("Failed to log authorization event", e);
        }
    }

    @Override
    public void logConsentVerification(String userId, String patientId, String action, 
                                      String outcome, Map<String, Object> details) {
        try {
            AuditEvent auditEvent = new AuditEvent(userId, "CONSENT_VERIFICATION", "PATIENT", patientId, outcome);
            
            Map<String, Object> consentDetails = new HashMap<>(details != null ? details : new HashMap<>());
            consentDetails.put("patientId", patientId);
            consentDetails.put("requestedAction", action);
            consentDetails.put("consentStatus", consentDetails.getOrDefault("consentStatus", "Unknown"));
            
            auditEvent.setDetails(consentDetails);
            logEvent(auditEvent);
            
            logger.info("Logged consent verification for patient: {} by user: {} with outcome: {}", 
                       patientId, userId, outcome);
        } catch (Exception e) {
            logger.error("Failed to log consent verification event", e);
        }
    }

    @Override
    public void logSecurityEvent(String userId, String eventType, String outcome, 
                                String severity, Map<String, Object> details) {
        try {
            AuditEvent auditEvent = new AuditEvent(userId, "SECURITY_EVENT", "SYSTEM", eventType, outcome);
            
            Map<String, Object> securityDetails = new HashMap<>(details != null ? details : new HashMap<>());
            securityDetails.put("eventType", eventType);
            securityDetails.put("severity", severity);
            securityDetails.put("threatLevel", securityDetails.getOrDefault("threatLevel", "LOW"));
            
            auditEvent.setDetails(securityDetails);
            logEvent(auditEvent);
            
            logger.warn("Logged security event: {} for user: {} with severity: {}", eventType, userId, severity);
        } catch (Exception e) {
            logger.error("Failed to log security event", e);
        }
    }

    @Override
    public void logSystemEvent(String component, String action, String outcome, 
                              Map<String, Object> details) {
        try {
            AuditEvent auditEvent = new AuditEvent("SYSTEM", action, "COMPONENT", component, outcome);
            
            Map<String, Object> systemDetails = new HashMap<>(details != null ? details : new HashMap<>());
            systemDetails.put("component", component);
            systemDetails.put("systemAction", action);
            
            auditEvent.setDetails(systemDetails);
            logEvent(auditEvent);
            
            logger.info("Logged system event: {} for component: {} with outcome: {}", action, component, outcome);
        } catch (Exception e) {
            logger.error("Failed to log system event", e);
        }
    }

    @Override
    public AuditEvent logEvent(AuditEvent auditEvent) {
        try {
            AuditEventEntity entity = convertToEntity(auditEvent);
            AuditEventEntity savedEntity = auditEventRepository.save(entity);
            
            logger.debug("Saved audit event with ID: {}", savedEntity.getEventId());
            return convertToModel(savedEntity);
        } catch (Exception e) {
            logger.error("Failed to save audit event: {}", auditEvent, e);
            throw new RuntimeException("Failed to save audit event", e);
        }
    }

    // Audit retrieval methods

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditEvent> findById(String eventId) {
        return auditEventRepository.findByEventId(eventId)
                .map(this::convertToModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> findByUserId(String userId) {
        return auditEventRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByUserId(String userId, Pageable pageable) {
        return auditEventRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(this::convertToModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> findByAction(String action) {
        return auditEventRepository.findByActionOrderByTimestampDesc(action)
                .stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByAction(String action, Pageable pageable) {
        return auditEventRepository.findByActionOrderByTimestampDesc(action, pageable)
                .map(this::convertToModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> findByResource(String resourceType, String resourceId) {
        return auditEventRepository.findByResourceTypeAndResourceIdOrderByTimestampDesc(resourceType, resourceId)
                .stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditEventRepository.findByTimestampBetween(startTime, endTime)
                .stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return auditEventRepository.findByTimestampBetween(startTime, endTime, pageable)
                .map(this::convertToModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> findFailedEvents() {
        return auditEventRepository.findFailedEvents()
                .stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> findFailedEvents(Pageable pageable) {
        return auditEventRepository.findFailedEvents(pageable)
                .map(this::convertToModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> findRecentEvents(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditEventRepository.findRecentEvents(since)
                .stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> findRecentEvents(int hours, Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditEventRepository.findRecentEvents(since, pageable)
                .map(this::convertToModel);
    }

    // Audit statistics and reporting methods

    @Override
    @Transactional(readOnly = true)
    public long countByAction(String action, LocalDateTime startTime, LocalDateTime endTime) {
        return auditEventRepository.countByActionAndTimestampBetween(action, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUser(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditEventRepository.countByUserIdAndTimestampBetween(userId, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFailedEvents(LocalDateTime startTime, LocalDateTime endTime) {
        return auditEventRepository.countFailedEventsByTimestampBetween(startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateComplianceReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();
        
        try {
            // Basic statistics
            List<AuditEventEntity> events = auditEventRepository.findByTimestampBetween(startTime, endTime);
            report.put("totalEvents", events.size());
            report.put("reportPeriod", Map.of(
                "startTime", startTime,
                "endTime", endTime
            ));
            
            // Event breakdown by action
            Map<String, Long> actionCounts = events.stream()
                .collect(Collectors.groupingBy(AuditEventEntity::getAction, Collectors.counting()));
            report.put("eventsByAction", actionCounts);
            
            // Event breakdown by outcome
            Map<String, Long> outcomeCounts = events.stream()
                .collect(Collectors.groupingBy(AuditEventEntity::getOutcome, Collectors.counting()));
            report.put("eventsByOutcome", outcomeCounts);
            
            // User activity
            Map<String, Long> userCounts = events.stream()
                .filter(e -> e.getUserId() != null)
                .collect(Collectors.groupingBy(AuditEventEntity::getUserId, Collectors.counting()));
            report.put("eventsByUser", userCounts);
            
            // Security events
            long securityEvents = events.stream()
                .filter(e -> "SECURITY_EVENT".equals(e.getAction()))
                .count();
            report.put("securityEvents", securityEvents);
            
            // Failed events
            long failedEvents = events.stream()
                .filter(e -> "FAILURE".equals(e.getOutcome()) || "ERROR".equals(e.getOutcome()))
                .count();
            report.put("failedEvents", failedEvents);
            
            logger.info("Generated compliance report for period {} to {}", startTime, endTime);
        } catch (Exception e) {
            logger.error("Failed to generate compliance report", e);
            report.put("error", "Failed to generate report: " + e.getMessage());
        }
        
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateSecuritySummary(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            List<AuditEventEntity> securityEvents = auditEventRepository.findByTimestampBetween(startTime, endTime)
                .stream()
                .filter(e -> "SECURITY_EVENT".equals(e.getAction()) || 
                           "AUTHENTICATION".equals(e.getAction()) || 
                           "AUTHORIZATION".equals(e.getAction()))
                .collect(Collectors.toList());
            
            summary.put("totalSecurityEvents", securityEvents.size());
            summary.put("reportPeriod", Map.of(
                "startTime", startTime,
                "endTime", endTime
            ));
            
            // Authentication events
            long authEvents = securityEvents.stream()
                .filter(e -> "AUTHENTICATION".equals(e.getAction()))
                .count();
            summary.put("authenticationEvents", authEvents);
            
            // Authorization events
            long authzEvents = securityEvents.stream()
                .filter(e -> "AUTHORIZATION".equals(e.getAction()))
                .count();
            summary.put("authorizationEvents", authzEvents);
            
            // Failed security events
            long failedSecurityEvents = securityEvents.stream()
                .filter(e -> "FAILURE".equals(e.getOutcome()) || "ERROR".equals(e.getOutcome()))
                .count();
            summary.put("failedSecurityEvents", failedSecurityEvents);
            
            // Top users by security events
            Map<String, Long> userSecurityCounts = securityEvents.stream()
                .filter(e -> e.getUserId() != null)
                .collect(Collectors.groupingBy(AuditEventEntity::getUserId, Collectors.counting()));
            summary.put("userSecurityActivity", userSecurityCounts);
            
            logger.info("Generated security summary for period {} to {}", startTime, endTime);
        } catch (Exception e) {
            logger.error("Failed to generate security summary", e);
            summary.put("error", "Failed to generate summary: " + e.getMessage());
        }
        
        return summary;
    }

    // Audit maintenance methods

    @Override
    public void cleanupOldEvents(LocalDateTime cutoffDate) {
        try {
            auditEventRepository.batchDeleteOldEvents(cutoffDate);
            logger.info("Cleaned up audit events before {}", cutoffDate);
        } catch (Exception e) {
            logger.error("Failed to cleanup old audit events", e);
            throw new RuntimeException("Failed to cleanup old audit events", e);
        }
    }

    @Override
    public void archiveEvents(LocalDateTime cutoffDate) {
        try {
            // This is a placeholder for archiving logic
            // In a real implementation, you would export events to external storage
            // before deleting them from the primary database
            List<AuditEventEntity> eventsToArchive = auditEventRepository.findByTimestampBetween(
                LocalDateTime.of(2000, 1, 1, 0, 0), cutoffDate);
            
            logger.info("Would archive {} events before {}", eventsToArchive.size(), cutoffDate);
            // TODO: Implement actual archiving to S3, file system, or other storage
            
        } catch (Exception e) {
            logger.error("Failed to archive audit events", e);
            throw new RuntimeException("Failed to archive audit events", e);
        }
    }

    // Helper methods for entity conversion

    private AuditEventEntity convertToEntity(AuditEvent auditEvent) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setEventId(auditEvent.getEventId());
        entity.setUserId(auditEvent.getUserId());
        entity.setAction(auditEvent.getAction());
        entity.setResourceType(auditEvent.getResourceType());
        entity.setResourceId(auditEvent.getResourceId());
        entity.setTimestamp(auditEvent.getTimestamp());
        entity.setOutcome(auditEvent.getOutcome());
        entity.setDetails(auditEvent.getDetails());
        return entity;
    }

    private AuditEvent convertToModel(AuditEventEntity entity) {
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventId(entity.getEventId());
        auditEvent.setUserId(entity.getUserId());
        auditEvent.setAction(entity.getAction());
        auditEvent.setResourceType(entity.getResourceType());
        auditEvent.setResourceId(entity.getResourceId());
        auditEvent.setTimestamp(entity.getTimestamp());
        auditEvent.setOutcome(entity.getOutcome());
        auditEvent.setDetails(entity.getDetails());
        return auditEvent;
    }
}