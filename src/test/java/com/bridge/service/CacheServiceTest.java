package com.bridge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cache cache;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService(cacheManager, redisTemplate);
    }

    @Test
    void testInvalidateUserAuthCache() {
        // Given
        String userId = "user123";
        when(cacheManager.getCache("user-auth")).thenReturn(cache);

        // When
        cacheService.invalidateUserAuthCache(userId);

        // Then
        verify(cache).evict("user:" + userId);
    }

    @Test
    void testInvalidateTransformationCaches() {
        // Given
        when(cacheManager.getCache("fhir-transformations")).thenReturn(cache);
        when(cacheManager.getCache("validation-results")).thenReturn(cache);

        // When
        cacheService.invalidateTransformationCaches();

        // Then
        verify(cache, times(2)).clear();
    }

    @Test
    void testGetCacheStatsForNonExistentCache() {
        // Given
        String cacheName = "non-existent-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(null);

        // When
        CacheService.CacheStats stats = cacheService.getCacheStats(cacheName);

        // Then
        assertNotNull(stats);
        assertEquals(cacheName, stats.getCacheName());
        assertEquals(0, stats.getKeyCount());
        assertEquals(0, stats.getMemoryUsage());
        assertEquals(0.0, stats.getHitRatio());
    }

    @Test
    void testWarmUpCache() {
        // Given
        String cacheName = "test-cache";
        String key = "test-key";
        Object value = "test-value";
        long ttlSeconds = 300;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        cacheService.warmUpCache(cacheName, key, value, ttlSeconds);

        // Then
        verify(valueOperations).set(cacheName + "::" + key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Test
    void testExistsInCacheReturnsFalseWhenCacheNotFound() {
        // Given
        String cacheName = "non-existent-cache";
        String key = "test-key";
        when(cacheManager.getCache(cacheName)).thenReturn(null);

        // When
        boolean exists = cacheService.existsInCache(cacheName, key);

        // Then
        assertFalse(exists);
    }
}