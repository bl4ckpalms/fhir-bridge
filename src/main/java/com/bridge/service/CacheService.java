package com.bridge.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing cache operations and invalidation strategies
 */
@Service
public class CacheService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(CacheManager cacheManager, RedisTemplate<String, Object> redisTemplate) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Invalidate all caches for a specific patient
     */
    public void invalidatePatientCaches(String patientId) {
        // Invalidate consent records for the patient
        evictFromCache("consent-records", "patient:" + patientId);
        
        // Invalidate FHIR resources for the patient
        evictPatternFromCache("fhir-resources", "patient:" + patientId + ":*");
        
        // Invalidate transformation results for the patient
        evictPatternFromCache("fhir-transformations", "patient:" + patientId + ":*");
    }

    /**
     * Invalidate consent-related caches when consent changes
     */
    public void invalidateConsentCaches(String patientId, String organizationId) {
        String consentKey = String.format("patient:%s:org:%s", patientId, organizationId);
        evictFromCache("consent-records", consentKey);
        
        // Also invalidate any filtered resources that depend on this consent
        evictPatternFromCache("fhir-resources", consentKey + ":*");
    }

    /**
     * Invalidate transformation caches when mapping rules change
     */
    public void invalidateTransformationCaches() {
        clearCache("fhir-transformations");
        clearCache("validation-results");
    }

    /**
     * Invalidate authentication caches for a user
     */
    public void invalidateUserAuthCache(String userId) {
        evictFromCache("user-auth", "user:" + userId);
    }

    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getCacheStats(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return new CacheStats(cacheName, 0, 0, 0.0);
        }

        // Get Redis-specific statistics
        String pattern = cacheName + "::*";
        Set<String> keys = redisTemplate.keys(pattern);
        long keyCount = keys != null ? keys.size() : 0;

        // Calculate approximate memory usage (simplified)
        long memoryUsage = keyCount * 1024; // Rough estimate

        return new CacheStats(cacheName, keyCount, memoryUsage, calculateHitRatio(cacheName));
    }

    /**
     * Warm up cache with frequently accessed data
     */
    public void warmUpCache(String cacheName, String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(cacheName + "::" + key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Check if a key exists in cache
     */
    public boolean existsInCache(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        return cache != null && cache.get(key) != null;
    }

    private void evictFromCache(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void evictPatternFromCache(String cacheName, String pattern) {
        String redisPattern = cacheName + "::" + pattern;
        Set<String> keys = redisTemplate.keys(redisPattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private double calculateHitRatio(String cacheName) {
        // This would require Redis INFO command or custom metrics
        // For now, return a placeholder value
        return 0.85; // 85% hit ratio as example
    }

    /**
     * Cache statistics data class
     */
    public static class CacheStats {
        private final String cacheName;
        private final long keyCount;
        private final long memoryUsage;
        private final double hitRatio;

        public CacheStats(String cacheName, long keyCount, long memoryUsage, double hitRatio) {
            this.cacheName = cacheName;
            this.keyCount = keyCount;
            this.memoryUsage = memoryUsage;
            this.hitRatio = hitRatio;
        }

        public String getCacheName() { return cacheName; }
        public long getKeyCount() { return keyCount; }
        public long getMemoryUsage() { return memoryUsage; }
        public double getHitRatio() { return hitRatio; }
    }
}