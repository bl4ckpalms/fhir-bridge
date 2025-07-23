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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CacheConfig.class, CacheService.class, ConsentVerificationServiceImpl.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "fhir-bridge.consent.cache-ttl=300s"
})
class CacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ConsentVerificationService consentVerificationService;

    @MockBean
    private ConsentRepository consentRepository;

    private ConsentRecord testConsentRecord;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // Setup test consent record
        testConsentRecord = new ConsentRecord();
        testConsentRecord.setPatientId("patient123");
        testConsentRecord.setOrganizationId("org456");
        testConsentRecord.setStatus(ConsentStatus.ACTIVE);
        testConsentRecord.setEffectiveDate(LocalDateTime.now().minusDays(1));
        testConsentRecord.setExpirationDate(LocalDateTime.now().plusDays(30));
        testConsentRecord.setAllowedCategories(List.of(DataCategory.CLINICAL_NOTES, DataCategory.DEMOGRAPHICS));
    }

    @Test
    void testConsentVerificationCaching() {
        // Given
        String patientId = "patient123";
        String organizationId = "org456";
        
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(createConsentEntity()));

        // When - First call should hit the database
        ConsentVerificationResult result1 = consentVerificationService.verifyConsent(patientId, organizationId);
        
        // When - Second call should hit the cache
        ConsentVerificationResult result2 = consentVerificationService.verifyConsent(patientId, organizationId);

        // Then
        assertTrue(result1.isValid());
        assertTrue(result2.isValid());
        assertEquals(result1.getPatientId(), result2.getPatientId());
        assertEquals(result1.getOrganizationId(), result2.getOrganizationId());
        
        // Verify repository was called only once (first call)
        verify(consentRepository, times(1)).findByPatientIdAndOrganizationId(patientId, organizationId);
    }

    @Test
    void testCacheInvalidationOnConsentUpdate() {
        // Given
        String patientId = "patient123";
        String organizationId = "org456";
        
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(createConsentEntity()));

        // When - First call to populate cache
        consentVerificationService.verifyConsent(patientId, organizationId);
        
        // When - Invalidate cache
        cacheService.invalidateConsentCaches(patientId, organizationId);
        
        // When - Second call should hit database again
        consentVerificationService.verifyConsent(patientId, organizationId);

        // Then - Repository should be called twice
        verify(consentRepository, times(2)).findByPatientIdAndOrganizationId(patientId, organizationId);
    }

    @Test
    void testCacheStatsCollection() {
        // Given
        String cacheName = "consent-records";
        String patientId = "patient123";
        String organizationId = "org456";
        
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(createConsentEntity()));

        // When - Populate cache
        consentVerificationService.verifyConsent(patientId, organizationId);
        
        // When - Get cache stats
        CacheService.CacheStats stats = cacheService.getCacheStats(cacheName);

        // Then
        assertNotNull(stats);
        assertEquals(cacheName, stats.getCacheName());
        assertTrue(stats.getKeyCount() >= 0);
        assertTrue(stats.getMemoryUsage() >= 0);
        assertTrue(stats.getHitRatio() >= 0);
    }

    @Test
    void testCacheWarmUp() {
        // Given
        String cacheName = "consent-records";
        String key = "patient:warm123:org:warm456";
        ConsentVerificationResult warmUpValue = ConsentVerificationResult.valid(
            "warm123", "warm456", testConsentRecord);

        // When
        cacheService.warmUpCache(cacheName, key, warmUpValue, 300);

        // Then
        assertTrue(cacheService.existsInCache(cacheName, key));
    }

    @Test
    void testPatientCacheInvalidation() {
        // Given
        String patientId = "patient123";
        String organizationId = "org456";
        
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(createConsentEntity()));

        // When - Populate cache
        consentVerificationService.verifyConsent(patientId, organizationId);
        
        // When - Invalidate all patient caches
        cacheService.invalidatePatientCaches(patientId);
        
        // When - Call again should hit database
        consentVerificationService.verifyConsent(patientId, organizationId);

        // Then - Repository should be called twice
        verify(consentRepository, times(2)).findByPatientIdAndOrganizationId(patientId, organizationId);
    }

    @Test
    void testCacheExistsCheck() {
        // Given
        String cacheName = "consent-records";
        String patientId = "patient123";
        String organizationId = "org456";
        String cacheKey = "patient:" + patientId + ":org:" + organizationId;
        
        when(consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId))
            .thenReturn(Optional.of(createConsentEntity()));

        // When - Cache should be empty initially
        boolean existsBefore = cacheService.existsInCache(cacheName, cacheKey);
        
        // When - Populate cache
        consentVerificationService.verifyConsent(patientId, organizationId);
        
        // When - Check cache exists after population
        boolean existsAfter = cacheService.existsInCache(cacheName, cacheKey);

        // Then
        assertFalse(existsBefore);
        assertTrue(existsAfter);
    }

    @Test
    void testMultipleCacheOperations() {
        // Given
        String patientId1 = "patient123";
        String patientId2 = "patient456";
        String organizationId = "org789";
        
        when(consentRepository.findByPatientIdAndOrganizationId(eq(patientId1), eq(organizationId)))
            .thenReturn(Optional.of(createConsentEntity()));
        when(consentRepository.findByPatientIdAndOrganizationId(eq(patientId2), eq(organizationId)))
            .thenReturn(Optional.of(createConsentEntity()));

        // When - Populate cache for multiple patients
        consentVerificationService.verifyConsent(patientId1, organizationId);
        consentVerificationService.verifyConsent(patientId2, organizationId);
        
        // When - Get cache stats
        CacheService.CacheStats stats = cacheService.getCacheStats("consent-records");
        
        // When - Invalidate one patient's cache
        cacheService.invalidatePatientCaches(patientId1);
        
        // When - Call again for both patients
        consentVerificationService.verifyConsent(patientId1, organizationId); // Should hit database
        consentVerificationService.verifyConsent(patientId2, organizationId); // Should hit cache

        // Then
        assertNotNull(stats);
        // Patient1 should have been called twice (initial + after invalidation)
        verify(consentRepository, times(2)).findByPatientIdAndOrganizationId(patientId1, organizationId);
        // Patient2 should have been called once (initial only, second from cache)
        verify(consentRepository, times(1)).findByPatientIdAndOrganizationId(patientId2, organizationId);
    }

    private com.bridge.entity.ConsentEntity createConsentEntity() {
        com.bridge.entity.ConsentEntity entity = new com.bridge.entity.ConsentEntity();
        entity.setPatientId(testConsentRecord.getPatientId());
        entity.setOrganizationId(testConsentRecord.getOrganizationId());
        entity.setStatus(testConsentRecord.getStatus());
        entity.setEffectiveDate(testConsentRecord.getEffectiveDate());
        entity.setExpirationDate(testConsentRecord.getExpirationDate());
        entity.setAllowedCategories(testConsentRecord.getAllowedCategories());
        entity.setPolicyReference("test-policy");
        return entity;
    }
}