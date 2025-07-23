package com.bridge.service;

import com.bridge.config.DatabaseConfig;
import com.bridge.entity.AuditEventEntity;
import com.bridge.entity.ConsentEntity;
import com.bridge.model.ConsentStatus;
import com.bridge.model.DataCategory;
import com.bridge.repository.AuditEventRepository;
import com.bridge.repository.ConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {DatabaseConfig.class, DatabasePerformanceMonitoringService.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DatabasePerformanceTest {

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private DatabasePerformanceMonitoringService performanceMonitoringService;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        auditEventRepository.deleteAll();
        consentRepository.deleteAll();
    }

    @Test
    @Transactional
    void testBatchConsentOperations() {
        // Given
        int batchSize = 100;
        List<ConsentEntity> consents = new ArrayList<>();
        
        for (int i = 0; i < batchSize; i++) {
            ConsentEntity consent = new ConsentEntity();
            consent.setPatientId("patient" + i);
            consent.setOrganizationId("org" + (i % 10)); // 10 different orgs
            consent.setStatus(ConsentStatus.ACTIVE);
            consent.setEffectiveDate(LocalDateTime.now().minusDays(1));
            consent.setExpirationDate(LocalDateTime.now().plusDays(30));
            consent.setAllowedCategories(List.of(DataCategory.DEMOGRAPHICS, DataCategory.MEDICAL_HISTORY));
            consent.setPolicyReference("policy-" + i);
            consents.add(consent);
        }

        // When - Measure batch insert performance
        long startTime = System.currentTimeMillis();
        consentRepository.saveAll(consents);
        long insertTime = System.currentTimeMillis() - startTime;

        // When - Measure batch query performance
        startTime = System.currentTimeMillis();
        List<String> patientIds = List.of("patient1", "patient2", "patient3", "patient4", "patient5");
        List<ConsentEntity> results = consentRepository.findActiveConsentsByPatientIds(patientIds, LocalDateTime.now());
        long queryTime = System.currentTimeMillis() - startTime;

        // Then
        assertEquals(batchSize, consentRepository.count());
        assertEquals(5, results.size());
        
        System.out.println("Batch insert time for " + batchSize + " records: " + insertTime + "ms");
        System.out.println("Batch query time for " + patientIds.size() + " patients: " + queryTime + "ms");
        
        // Performance assertions
        assertTrue(insertTime < 5000, "Batch insert should complete within 5 seconds");
        assertTrue(queryTime < 1000, "Batch query should complete within 1 second");
    }

    @Test
    @Transactional
    void testOptimizedConsentQueries() {
        // Given
        ConsentEntity consent1 = createTestConsent("patient123", "org456", ConsentStatus.ACTIVE);
        ConsentEntity consent2 = createTestConsent("patient123", "org789", ConsentStatus.EXPIRED);
        ConsentEntity consent3 = createTestConsent("patient456", "org456", ConsentStatus.ACTIVE);
        
        consentRepository.saveAll(List.of(consent1, consent2, consent3));

        // When - Test optimized active consent query
        long startTime = System.currentTimeMillis();
        List<ConsentEntity> activeConsents = consentRepository.findActiveConsentsByPatientId("patient123", LocalDateTime.now());
        long queryTime = System.currentTimeMillis() - startTime;

        // Then
        assertEquals(1, activeConsents.size());
        assertEquals("org456", activeConsents.get(0).getOrganizationId());
        
        System.out.println("Optimized active consent query time: " + queryTime + "ms");
        assertTrue(queryTime < 100, "Optimized query should complete within 100ms");
    }

    @Test
    @Transactional
    void testBatchExpiredConsentUpdate() {
        // Given
        List<ConsentEntity> consents = new ArrayList<>();
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        
        for (int i = 0; i < 50; i++) {
            ConsentEntity consent = createTestConsent("patient" + i, "org" + i, ConsentStatus.ACTIVE);
            consent.setExpirationDate(pastDate); // Make them expired
            consents.add(consent);
        }
        
        consentRepository.saveAll(consents);

        // When - Test batch update of expired consents
        long startTime = System.currentTimeMillis();
        int updatedCount = consentRepository.batchUpdateExpiredConsents(LocalDateTime.now());
        long updateTime = System.currentTimeMillis() - startTime;

        // Then
        assertEquals(50, updatedCount);
        
        System.out.println("Batch update time for " + updatedCount + " expired consents: " + updateTime + "ms");
        assertTrue(updateTime < 1000, "Batch update should complete within 1 second");
    }

    @Test
    @Transactional
    void testAuditEventPerformance() {
        // Given
        int eventCount = 200;
        List<AuditEventEntity> events = new ArrayList<>();
        
        for (int i = 0; i < eventCount; i++) {
            AuditEventEntity event = new AuditEventEntity();
            event.setEventId("event-" + i);
            event.setUserId("user" + (i % 20)); // 20 different users
            event.setAction("TRANSFORM_HL7");
            event.setResourceType("Patient");
            event.setResourceId("patient" + i);
            event.setOutcome(i % 10 == 0 ? "FAILURE" : "SUCCESS"); // 10% failure rate
            event.setTimestamp(LocalDateTime.now().minusMinutes(i));
            event.addDetail("duration", 100 + i);
            events.add(event);
        }

        // When - Measure batch insert performance
        long startTime = System.currentTimeMillis();
        auditEventRepository.saveAll(events);
        long insertTime = System.currentTimeMillis() - startTime;

        // When - Test optimized recent events query
        startTime = System.currentTimeMillis();
        List<AuditEventEntity> recentEvents = auditEventRepository.findRecentEventsOptimized(
            LocalDateTime.now().minusHours(1), 50);
        long queryTime = System.currentTimeMillis() - startTime;

        // Then
        assertEquals(eventCount, auditEventRepository.count());
        assertTrue(recentEvents.size() <= 50);
        
        System.out.println("Audit events batch insert time for " + eventCount + " records: " + insertTime + "ms");
        System.out.println("Optimized recent events query time: " + queryTime + "ms");
        
        assertTrue(insertTime < 3000, "Audit batch insert should complete within 3 seconds");
        assertTrue(queryTime < 500, "Recent events query should complete within 500ms");
    }

    @Test
    void testConcurrentDatabaseAccess() throws InterruptedException {
        // Given
        int threadCount = 10;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - Execute concurrent database operations
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    ConsentEntity consent = createTestConsent(
                        "patient" + threadId + "-" + j, 
                        "org" + threadId, 
                        ConsentStatus.ACTIVE);
                    consentRepository.save(consent);
                    
                    // Also create audit event
                    AuditEventEntity event = new AuditEventEntity();
                    event.setEventId("event-" + threadId + "-" + j);
                    event.setUserId("user" + threadId);
                    event.setAction("CREATE_CONSENT");
                    event.setResourceType("Consent");
                    event.setResourceId(consent.getPatientId());
                    event.setOutcome("SUCCESS");
                    event.setTimestamp(LocalDateTime.now());
                    event.addDetail("operation", "CREATE_CONSENT");
                    auditEventRepository.save(event);
                }
            }, executor);
        }
        
        CompletableFuture.allOf(futures).join();
        long totalTime = System.currentTimeMillis() - startTime;
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then
        long totalOperations = threadCount * operationsPerThread;
        assertEquals(totalOperations, consentRepository.count());
        assertEquals(totalOperations, auditEventRepository.count());
        
        System.out.println("Concurrent operations (" + threadCount + " threads, " + 
                          operationsPerThread + " ops each): " + totalTime + "ms");
        System.out.println("Operations per second: " + (totalOperations * 2 * 1000.0 / totalTime));
        
        assertTrue(totalTime < 10000, "Concurrent operations should complete within 10 seconds");
    }

    @Test
    void testDatabasePerformanceMonitoring() {
        // Given - Create some data to generate metrics
        for (int i = 0; i < 10; i++) {
            ConsentEntity consent = createTestConsent("patient" + i, "org" + i, ConsentStatus.ACTIVE);
            consentRepository.save(consent);
        }

        // When - Get performance metrics
        DatabasePerformanceMonitoringService.ConnectionPoolMetrics poolMetrics = 
            performanceMonitoringService.getConnectionPoolMetrics();
        
        DatabasePerformanceMonitoringService.HibernateMetrics hibernateMetrics = 
            performanceMonitoringService.getHibernateMetrics();
        
        DatabasePerformanceMonitoringService.DatabasePerformanceSummary summary = 
            performanceMonitoringService.getPerformanceSummary();
        
        DatabasePerformanceMonitoringService.PerformanceHealthCheck healthCheck = 
            performanceMonitoringService.checkPerformanceHealth();

        // Then
        assertNotNull(poolMetrics);
        assertNotNull(hibernateMetrics);
        assertNotNull(summary);
        assertNotNull(healthCheck);
        
        assertTrue(hibernateMetrics.getQueryExecutionCount() > 0);
        assertTrue(summary.getConnectionUtilization() >= 0);
        
        System.out.println("Connection pool utilization: " + summary.getConnectionUtilization() + "%");
        System.out.println("Query execution count: " + hibernateMetrics.getQueryExecutionCount());
        System.out.println("Performance health: " + (healthCheck.isHealthy() ? "HEALTHY" : "UNHEALTHY"));
        System.out.println("Health message: " + healthCheck.getMessage());
    }

    @Test
    void testQueryOptimizationRecommendations() {
        // Given - Create scenario that might trigger recommendations
        for (int i = 0; i < 50; i++) {
            ConsentEntity consent = createTestConsent("patient" + i, "org" + (i % 5), ConsentStatus.ACTIVE);
            consentRepository.save(consent);
        }

        // Perform some queries to generate statistics
        for (int i = 0; i < 10; i++) {
            consentRepository.findActiveConsentsByPatientId("patient" + i, LocalDateTime.now());
        }

        // When
        var recommendations = performanceMonitoringService.getPerformanceRecommendations();

        // Then
        assertNotNull(recommendations);
        System.out.println("Performance recommendations:");
        recommendations.forEach((key, value) -> 
            System.out.println("  " + key + ": " + value));
    }

    private ConsentEntity createTestConsent(String patientId, String organizationId, ConsentStatus status) {
        ConsentEntity consent = new ConsentEntity();
        consent.setPatientId(patientId);
        consent.setOrganizationId(organizationId);
        consent.setStatus(status);
        consent.setEffectiveDate(LocalDateTime.now().minusDays(1));
        consent.setExpirationDate(LocalDateTime.now().plusDays(30));
        consent.setAllowedCategories(List.of(DataCategory.DEMOGRAPHICS, DataCategory.MEDICAL_HISTORY));
        consent.setPolicyReference("test-policy");
        return consent;
    }
}