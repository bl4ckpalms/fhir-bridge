package com.bridge.service;

import com.bridge.entity.AuditEventEntity;
import com.bridge.model.AuditEvent;
import com.bridge.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    private AuditEvent testAuditEvent;
    private AuditEventEntity testAuditEventEntity;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = LocalDateTime.now();
        
        testAuditEvent = new AuditEvent("user123", "TEST_ACTION", "RESOURCE", "resource123", "SUCCESS");
        testAuditEvent.setTimestamp(testTimestamp);
        testAuditEvent.addDetail("testKey", "testValue");

        testAuditEventEntity = new AuditEventEntity("user123", "TEST_ACTION", "RESOURCE", "resource123", "SUCCESS");
        testAuditEventEntity.setId(1L);
        testAuditEventEntity.setEventId(testAuditEvent.getEventId());
        testAuditEventEntity.setTimestamp(testTimestamp);
        testAuditEventEntity.addDetail("testKey", "testValue");
    }

    @Test
    void testLogTransformation() {
        // Given
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(testAuditEventEntity);

        Map<String, Object> details = new HashMap<>();
        details.put("messageSize", 1024);

        // When
        auditService.logTransformation("user123", "HL7", "msg123", "FHIR", "fhir123", "SUCCESS", details);

        // Then
        verify(auditEventRepository).save(argThat(entity -> 
            "DATA_TRANSFORMATION".equals(entity.getAction()) &&
            "user123".equals(entity.getUserId()) &&
            "FHIR".equals(entity.getResourceType()) &&
            "fhir123".equals(entity.getResourceId()) &&
            "SUCCESS".equals(entity.getOutcome()) &&
            entity.getDetails().containsKey("sourceType") &&
            entity.getDetails().containsKey("targetType")
        ));
    }

    @Test
    void testLogAuthentication() {
        // Given
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(testAuditEventEntity);

        Map<String, Object> details = new HashMap<>();
        details.put("userAgent", "Mozilla/5.0");

        // When
        auditService.logAuthentication("user123", "JWT", "SUCCESS", "192.168.1.1", details);

        // Then
        verify(auditEventRepository).save(argThat(entity -> 
            "AUTHENTICATION".equals(entity.getAction()) &&
            "user123".equals(entity.getUserId()) &&
            "USER".equals(entity.getResourceType()) &&
            "SUCCESS".equals(entity.getOutcome()) &&
            entity.getDetails().containsKey("authMethod") &&
            entity.getDetails().containsKey("ipAddress")
        ));
    }

    @Test
    void testLogAuthorization() {
        // Given
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(testAuditEventEntity);

        Map<String, Object> details = new HashMap<>();
        details.put("permissions", Arrays.asList("READ", "WRITE"));

        // When
        auditService.logAuthorization("user123", "patient/123", "READ", "SUCCESS", details);

        // Then
        verify(auditEventRepository).save(argThat(entity -> 
            "AUTHORIZATION".equals(entity.getAction()) &&
            "user123".equals(entity.getUserId()) &&
            "RESOURCE".equals(entity.getResourceType()) &&
            "patient/123".equals(entity.getResourceId()) &&
            "SUCCESS".equals(entity.getOutcome()) &&
            entity.getDetails().containsKey("requestedAction") &&
            entity.getDetails().containsKey("resource")
        ));
    }

    @Test
    void testLogConsentVerification() {
        // Given
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(testAuditEventEntity);

        Map<String, Object> details = new HashMap<>();
        details.put("consentStatus", "ACTIVE");

        // When
        auditService.logConsentVerification("user123", "patient123", "DATA_ACCESS", "SUCCESS", details);

        // Then
        verify(auditEventRepository).save(argThat(entity -> 
            "CONSENT_VERIFICATION".equals(entity.getAction()) &&
            "user123".equals(entity.getUserId()) &&
            "PATIENT".equals(entity.getResourceType()) &&
            "patient123".equals(entity.getResourceId()) &&
            "SUCCESS".equals(entity.getOutcome()) &&
            entity.getDetails().containsKey("patientId") &&
            entity.getDetails().containsKey("consentStatus")
        ));
    }

    @Test
    void testLogSecurityEvent() {
        // Given
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(testAuditEventEntity);

        Map<String, Object> details = new HashMap<>();
        details.put("threatLevel", "HIGH");

        // When
        auditService.logSecurityEvent("user123", "SUSPICIOUS_ACTIVITY", "FAILURE", "HIGH", details);

        // Then
        verify(auditEventRepository).save(argThat(entity -> 
            "SECURITY_EVENT".equals(entity.getAction()) &&
            "user123".equals(entity.getUserId()) &&
            "SYSTEM".equals(entity.getResourceType()) &&
            "SUSPICIOUS_ACTIVITY".equals(entity.getResourceId()) &&
            "FAILURE".equals(entity.getOutcome()) &&
            entity.getDetails().containsKey("eventType") &&
            entity.getDetails().containsKey("severity")
        ));
    }

    @Test
    void testLogSystemEvent() {
        // Given
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(testAuditEventEntity);

        Map<String, Object> details = new HashMap<>();
        details.put("version", "1.0.0");

        // When
        auditService.logSystemEvent("DATABASE", "STARTUP", "SUCCESS", details);

        // Then
        verify(auditEventRepository).save(argThat(entity -> 
            "STARTUP".equals(entity.getAction()) &&
            "SYSTEM".equals(entity.getUserId()) &&
            "COMPONENT".equals(entity.getResourceType()) &&
            "DATABASE".equals(entity.getResourceId()) &&
            "SUCCESS".equals(entity.getOutcome()) &&
            entity.getDetails().containsKey("component") &&
            entity.getDetails().containsKey("systemAction")
        ));
    }

    @Test
    void testLogEvent() {
        // Given
        when(auditEventRepository.save(any(AuditEventEntity.class))).thenReturn(testAuditEventEntity);

        // When
        AuditEvent result = auditService.logEvent(testAuditEvent);

        // Then
        assertNotNull(result);
        assertEquals(testAuditEvent.getEventId(), result.getEventId());
        assertEquals(testAuditEvent.getUserId(), result.getUserId());
        assertEquals(testAuditEvent.getAction(), result.getAction());
        verify(auditEventRepository).save(any(AuditEventEntity.class));
    }

    @Test
    void testLogEventFailure() {
        // Given
        when(auditEventRepository.save(any(AuditEventEntity.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> auditService.logEvent(testAuditEvent));
    }

    @Test
    void testFindById() {
        // Given
        when(auditEventRepository.findByEventId("event123"))
            .thenReturn(Optional.of(testAuditEventEntity));

        // When
        Optional<AuditEvent> result = auditService.findById("event123");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testAuditEventEntity.getEventId(), result.get().getEventId());
        assertEquals(testAuditEventEntity.getUserId(), result.get().getUserId());
    }

    @Test
    void testFindByIdNotFound() {
        // Given
        when(auditEventRepository.findByEventId("nonexistent"))
            .thenReturn(Optional.empty());

        // When
        Optional<AuditEvent> result = auditService.findById("nonexistent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByUserId() {
        // Given
        List<AuditEventEntity> entities = Arrays.asList(testAuditEventEntity);
        when(auditEventRepository.findByUserIdOrderByTimestampDesc("user123"))
            .thenReturn(entities);

        // When
        List<AuditEvent> result = auditService.findByUserId("user123");

        // Then
        assertEquals(1, result.size());
        assertEquals("user123", result.get(0).getUserId());
    }

    @Test
    void testFindByUserIdWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditEventEntity> entityPage = new PageImpl<>(Arrays.asList(testAuditEventEntity));
        when(auditEventRepository.findByUserIdOrderByTimestampDesc("user123", pageable))
            .thenReturn(entityPage);

        // When
        Page<AuditEvent> result = auditService.findByUserId("user123", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("user123", result.getContent().get(0).getUserId());
    }

    @Test
    void testFindByAction() {
        // Given
        List<AuditEventEntity> entities = Arrays.asList(testAuditEventEntity);
        when(auditEventRepository.findByActionOrderByTimestampDesc("TEST_ACTION"))
            .thenReturn(entities);

        // When
        List<AuditEvent> result = auditService.findByAction("TEST_ACTION");

        // Then
        assertEquals(1, result.size());
        assertEquals("TEST_ACTION", result.get(0).getAction());
    }

    @Test
    void testFindByResource() {
        // Given
        List<AuditEventEntity> entities = Arrays.asList(testAuditEventEntity);
        when(auditEventRepository.findByResourceTypeAndResourceIdOrderByTimestampDesc("RESOURCE", "resource123"))
            .thenReturn(entities);

        // When
        List<AuditEvent> result = auditService.findByResource("RESOURCE", "resource123");

        // Then
        assertEquals(1, result.size());
        assertEquals("RESOURCE", result.get(0).getResourceType());
        assertEquals("resource123", result.get(0).getResourceId());
    }

    @Test
    void testFindByTimeRange() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        List<AuditEventEntity> entities = Arrays.asList(testAuditEventEntity);
        when(auditEventRepository.findByTimestampBetween(startTime, endTime))
            .thenReturn(entities);

        // When
        List<AuditEvent> result = auditService.findByTimeRange(startTime, endTime);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    void testFindFailedEvents() {
        // Given
        List<AuditEventEntity> entities = Arrays.asList(testAuditEventEntity);
        when(auditEventRepository.findFailedEvents()).thenReturn(entities);

        // When
        List<AuditEvent> result = auditService.findFailedEvents();

        // Then
        assertEquals(1, result.size());
    }

    @Test
    void testFindRecentEvents() {
        // Given
        List<AuditEventEntity> entities = Arrays.asList(testAuditEventEntity);
        when(auditEventRepository.findRecentEvents(any(LocalDateTime.class)))
            .thenReturn(entities);

        // When
        List<AuditEvent> result = auditService.findRecentEvents(24);

        // Then
        assertEquals(1, result.size());
        verify(auditEventRepository).findRecentEvents(any(LocalDateTime.class));
    }

    @Test
    void testCountByAction() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        when(auditEventRepository.countByActionAndTimestampBetween("TEST_ACTION", startTime, endTime))
            .thenReturn(5L);

        // When
        long result = auditService.countByAction("TEST_ACTION", startTime, endTime);

        // Then
        assertEquals(5L, result);
    }

    @Test
    void testCountByUser() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        when(auditEventRepository.countByUserIdAndTimestampBetween("user123", startTime, endTime))
            .thenReturn(3L);

        // When
        long result = auditService.countByUser("user123", startTime, endTime);

        // Then
        assertEquals(3L, result);
    }

    @Test
    void testCountFailedEvents() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        when(auditEventRepository.countFailedEventsByTimestampBetween(startTime, endTime))
            .thenReturn(2L);

        // When
        long result = auditService.countFailedEvents(startTime, endTime);

        // Then
        assertEquals(2L, result);
    }

    @Test
    void testGenerateComplianceReport() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        AuditEventEntity event1 = new AuditEventEntity("user1", "ACTION1", "TYPE1", "id1", "SUCCESS");
        AuditEventEntity event2 = new AuditEventEntity("user2", "ACTION2", "TYPE2", "id2", "FAILURE");
        AuditEventEntity event3 = new AuditEventEntity("user1", "SECURITY_EVENT", "SYSTEM", "event1", "SUCCESS");
        
        List<AuditEventEntity> events = Arrays.asList(event1, event2, event3);
        when(auditEventRepository.findByTimestampBetween(startTime, endTime))
            .thenReturn(events);

        // When
        Map<String, Object> report = auditService.generateComplianceReport(startTime, endTime);

        // Then
        assertNotNull(report);
        assertEquals(3, report.get("totalEvents"));
        assertTrue(report.containsKey("eventsByAction"));
        assertTrue(report.containsKey("eventsByOutcome"));
        assertTrue(report.containsKey("eventsByUser"));
        assertEquals(1L, report.get("securityEvents"));
        assertEquals(1L, report.get("failedEvents"));
    }

    @Test
    void testGenerateSecuritySummary() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        AuditEventEntity authEvent = new AuditEventEntity("user1", "AUTHENTICATION", "USER", "user1", "SUCCESS");
        AuditEventEntity authzEvent = new AuditEventEntity("user1", "AUTHORIZATION", "RESOURCE", "res1", "FAILURE");
        AuditEventEntity secEvent = new AuditEventEntity("user2", "SECURITY_EVENT", "SYSTEM", "event1", "SUCCESS");
        
        List<AuditEventEntity> allEvents = Arrays.asList(authEvent, authzEvent, secEvent);
        when(auditEventRepository.findByTimestampBetween(startTime, endTime))
            .thenReturn(allEvents);

        // When
        Map<String, Object> summary = auditService.generateSecuritySummary(startTime, endTime);

        // Then
        assertNotNull(summary);
        assertEquals(3, summary.get("totalSecurityEvents"));
        assertEquals(1L, summary.get("authenticationEvents"));
        assertEquals(1L, summary.get("authorizationEvents"));
        assertEquals(1L, summary.get("failedSecurityEvents"));
        assertTrue(summary.containsKey("userSecurityActivity"));
    }

    @Test
    void testCleanupOldEvents() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        // When
        auditService.cleanupOldEvents(cutoffDate);

        // Then
        verify(auditEventRepository).batchDeleteOldEvents(cutoffDate);
    }

    @Test
    void testArchiveEvents() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<AuditEventEntity> events = Arrays.asList(testAuditEventEntity);
        when(auditEventRepository.findByTimestampBetween(any(LocalDateTime.class), eq(cutoffDate)))
            .thenReturn(events);

        // When
        auditService.archiveEvents(cutoffDate);

        // Then
        verify(auditEventRepository).findByTimestampBetween(any(LocalDateTime.class), eq(cutoffDate));
    }
}