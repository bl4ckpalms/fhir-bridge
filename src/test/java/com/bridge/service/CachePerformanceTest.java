package com.bridge.service;

import com.bridge.config.CacheConfig;
import com.bridge.model.ConsentRecord;
import com.bridge.model.ConsentStatus;
import com.bridge.model.ConsentVerificationResult;
import com.bridge.model.DataCategory;
import com.bridge.repository.ConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CacheConfig.class, CacheService.class, ConsentVerificationServiceImpl.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "fhir-bridge.consent.cache-ttl=300s"
})
class CachePerformanceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ConsentVerificationService consentVerificationService;

    @MockBean
    private ConsentRepository consentRepository;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // Setup mock to simulate database delay
        when(consentRepository.findByPatientIdAndOrganizationId(anyString(), anyString()))
            .thenAnswer(invocation -> {
                // Simulate database query delay
                Thread.sleep(50); // 50ms delay
                return Optional.of(createConsentEntity());
            });
    }

    @Test
    void testCachePerformanceImprovement() throws InterruptedException {
        // Given
        String patientId = "patient123";
        String organizationId = "org456";
        int numberOfCalls = 10;

        // When - Measure time without cache (first call)
        long startTimeWithoutCache = System.currentTimeMillis();
        ConsentVerificationResult firstResult = consentVerificationService.verifyConsent(patientId, organizationId);
        long timeWithoutCache = System.currentTimeMillis() - startTimeWithoutCache;

        // When - Measure time with cache (subsequent calls)
        long startTimeWithCache = System.currentTimeMillis();
        for (int i = 0; i < numberOfCalls - 1; i++) {
            consentVerificationService.verifyConsent(patientId, organizationId);
        }
        long timeWithCache = System.currentTimeMillis() - startTimeWithCache;

        // Then
        assertTrue(firstResult.isValid());
        assertTrue(timeWithoutCache > 40); // Should take at least 40ms (database delay)
        assertTrue(timeWithCache < timeWithoutCache); // Cache should be faster
        
        // Verify repository was called only once (first call)
        verify(consentRepository, times(1)).findByPatientIdAndOrganizationId(patientId, organizationId);
        
        System.out.println("Time without cache (first call): " + timeWithoutCache + "ms");
        System.out.println("Time with cache (" + (numberOfCalls - 1) + " calls): " + timeWithCache + "ms");
        System.out.println("Performance improvement: " + (timeWithoutCache / (double) timeWithCache * (numberOfCalls - 1)) + "x");
    }

    @Test
    void testConcurrentCacheAccess() throws InterruptedException {
        // Given
        String patientId = "patient123";
        String organizationId = "org456";
        int numberOfThreads = 10;
        int callsPerThread = 5;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // When - Execute concurrent calls
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] futures = new CompletableFuture[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < callsPerThread; j++) {
                    ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);
                    assertTrue(result.isValid());
                }
            }, executor);
        }
        
        CompletableFuture.allOf(futures).join();
        long totalTime = System.currentTimeMillis() - startTime;
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then
        // Repository should be called only once despite multiple concurrent calls
        verify(consentRepository, times(1)).findByPatientIdAndOrganizationId(patientId, organizationId);
        
        System.out.println("Concurrent execution time (" + numberOfThreads + " threads, " + 
                          callsPerThread + " calls each): " + totalTime + "ms");
        System.out.println("Total calls: " + (numberOfThreads * callsPerThread));
    }

    @Test
    void testCacheMemoryUsage() {
        // Given
        int numberOfPatients = 100;
        String organizationId = "org456";

        // When - Populate cache with multiple patients
        for (int i = 0; i < numberOfPatients; i++) {
            String patientId = "patient" + i;
            consentVerificationService.verifyConsent(patientId, organizationId);
        }

        // When - Get cache statistics
        CacheService.CacheStats stats = cacheService.getCacheStats("consent-records");

        // Then
        assertNotNull(stats);
        assertTrue(stats.getKeyCount() >= numberOfPatients);
        assertTrue(stats.getMemoryUsage() > 0);
        
        System.out.println("Cache entries: " + stats.getKeyCount());
        System.out.println("Estimated memory usage: " + stats.getMemoryUsage() + " bytes");
        System.out.println("Hit ratio: " + (stats.getHitRatio() * 100) + "%");
    }

    @Test
    void testCacheEvictionPerformance() {
        // Given
        int numberOfPatients = 50;
        String organizationId = "org456";

        // When - Populate cache
        for (int i = 0; i < numberOfPatients; i++) {
            String patientId = "patient" + i;
            consentVerificationService.verifyConsent(patientId, organizationId);
        }

        // When - Measure eviction time
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfPatients; i++) {
            String patientId = "patient" + i;
            cacheService.invalidatePatientCaches(patientId);
        }
        long evictionTime = System.currentTimeMillis() - startTime;

        // Then
        assertTrue(evictionTime < 1000); // Should complete within 1 second
        
        System.out.println("Cache eviction time for " + numberOfPatients + " patients: " + evictionTime + "ms");
    }

    @Test
    void testCacheHitRatioImprovement() {
        // Given
        String[] patientIds = {"patient1", "patient2", "patient3"};
        String organizationId = "org456";
        int repeatCalls = 10;

        // When - Make repeated calls to same patients
        for (int round = 0; round < repeatCalls; round++) {
            for (String patientId : patientIds) {
                ConsentVerificationResult result = consentVerificationService.verifyConsent(patientId, organizationId);
                assertTrue(result.isValid());
            }
        }

        // Then - Repository should only be called once per patient (first time)
        for (String patientId : patientIds) {
            verify(consentRepository, times(1)).findByPatientIdAndOrganizationId(patientId, organizationId);
        }

        // Calculate effective hit ratio
        int totalCalls = patientIds.length * repeatCalls;
        int cacheMisses = patientIds.length; // Only first call per patient hits database
        double hitRatio = (double) (totalCalls - cacheMisses) / totalCalls;
        
        assertTrue(hitRatio > 0.8); // Should have > 80% hit ratio
        
        System.out.println("Total calls: " + totalCalls);
        System.out.println("Cache misses: " + cacheMisses);
        System.out.println("Hit ratio: " + (hitRatio * 100) + "%");
    }

    private com.bridge.entity.ConsentEntity createConsentEntity() {
        com.bridge.entity.ConsentEntity entity = new com.bridge.entity.ConsentEntity();
        entity.setPatientId("test-patient");
        entity.setOrganizationId("test-org");
        entity.setStatus(ConsentStatus.ACTIVE);
        entity.setEffectiveDate(LocalDateTime.now().minusDays(1));
        entity.setExpirationDate(LocalDateTime.now().plusDays(30));
        entity.setAllowedCategories(List.of(DataCategory.CLINICAL_NOTES, DataCategory.DEMOGRAPHICS));
        entity.setPolicyReference("test-policy");
        return entity;
    }
}