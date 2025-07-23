package com.bridge.service;

import com.bridge.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive monitoring service implementation
 */
@Service
public class MonitoringServiceImpl implements MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringServiceImpl.class);

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditEventRepository auditEventRepository;
    private final AuditService auditService;

    // System startup time
    private final LocalDateTime systemStartTime;
    
    // Alert storage (in production, this would be persisted)
    private final Map<String, List<Map<String, Object>>> alertHistory = new ConcurrentHashMap<>();

    @Autowired
    public MonitoringServiceImpl(DataSource dataSource, 
                                RedisTemplate<String, Object> redisTemplate,
                                AuditEventRepository auditEventRepository,
                                AuditService auditService) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.auditEventRepository = auditEventRepository;
        this.auditService = auditService;
        this.systemStartTime = LocalDateTime.now();
    }

    @Override
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("uptime", getSystemUptime());
            
            // Component health checks
            Map<String, Object> components = new HashMap<>();
            components.put("database", getDatabaseHealth());
            components.put("cache", getCacheHealth());
            components.put("fhirService", getFhirServiceHealth());
            components.put("hl7Service", getHl7ServiceHealth());
            components.put("authService", getAuthServiceHealth());
            
            health.put("components", components);
            
            // Overall status based on components
            boolean allHealthy = components.values().stream()
                .allMatch(component -> {
                    if (component instanceof Map) {
                        return "UP".equals(((Map<?, ?>) component).get("status"));
                    }
                    return false;
                });
            
            health.put("status", allHealthy ? "UP" : "DOWN");
            
        } catch (Exception e) {
            logger.error("Error getting system health", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }

    @Override
    public Map<String, Object> getDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test database connection
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5 second timeout
                health.put("status", isValid ? "UP" : "DOWN");
                health.put("database", connection.getMetaData().getDatabaseProductName());
                health.put("url", connection.getMetaData().getURL());
                
                // Test a simple query
                long auditCount = auditEventRepository.count();
                health.put("auditEventCount", auditCount);
                health.put("lastChecked", LocalDateTime.now());
            }
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("lastChecked", LocalDateTime.now());
        }
        
        return health;
    }

    @Override
    public Map<String, Object> getCacheHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test Redis connection
            String testKey = "health_check_" + System.currentTimeMillis();
            String testValue = "test";
            
            redisTemplate.opsForValue().set(testKey, testValue);
            String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            boolean isHealthy = testValue.equals(retrievedValue);
            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("type", "Redis");
            health.put("lastChecked", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Cache health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("lastChecked", LocalDateTime.now());
        }
        
        return health;
    }

    @Override
    public Map<String, Object> getFhirServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test FHIR service functionality
            // This is a placeholder - in real implementation, you'd test FHIR validation
            health.put("status", "UP");
            health.put("version", "R4");
            health.put("profiles", Arrays.asList("US Core", "TEFCA"));
            health.put("lastChecked", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("FHIR service health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("lastChecked", LocalDateTime.now());
        }
        
        return health;
    }

    @Override
    public Map<String, Object> getHl7ServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test HL7 service functionality
            // This is a placeholder - in real implementation, you'd test HL7 parsing
            health.put("status", "UP");
            health.put("version", "2.5");
            health.put("supportedMessages", Arrays.asList("ADT", "ORM", "ORU"));
            health.put("lastChecked", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("HL7 service health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("lastChecked", LocalDateTime.now());
        }
        
        return health;
    }

    @Override
    public Map<String, Object> getAuthServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test authentication service
            // This is a placeholder - in real implementation, you'd test JWT validation
            health.put("status", "UP");
            health.put("authMethods", Arrays.asList("JWT", "OAuth2"));
            health.put("lastChecked", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Auth service health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("lastChecked", LocalDateTime.now());
        }
        
        return health;
    }

    @Override
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // JVM metrics
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            
            Map<String, Object> jvm = new HashMap<>();
            jvm.put("uptime", runtimeBean.getUptime());
            jvm.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
            jvm.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
            jvm.put("heapCommitted", memoryBean.getHeapMemoryUsage().getCommitted());
            jvm.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
            
            metrics.put("jvm", jvm);
            metrics.put("timestamp", LocalDateTime.now());
            
            // System metrics
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> system = new HashMap<>();
            system.put("availableProcessors", runtime.availableProcessors());
            system.put("totalMemory", runtime.totalMemory());
            system.put("freeMemory", runtime.freeMemory());
            system.put("maxMemory", runtime.maxMemory());
            
            metrics.put("system", system);
            
        } catch (Exception e) {
            logger.error("Error getting performance metrics", e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }

    @Override
    public Map<String, Object> getTransformationMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            LocalDateTime now = LocalDateTime.now();
            
            // Get transformation counts
            long totalTransformations = auditService.countByAction("DATA_TRANSFORMATION", last24Hours, now);
            long failedTransformations = auditEventRepository.findByTimestampBetween(last24Hours, now)
                .stream()
                .filter(event -> "DATA_TRANSFORMATION".equals(event.getAction()) && 
                               ("FAILURE".equals(event.getOutcome()) || "ERROR".equals(event.getOutcome())))
                .count();
            
            metrics.put("totalTransformations24h", totalTransformations);
            metrics.put("failedTransformations24h", failedTransformations);
            metrics.put("successRate", totalTransformations > 0 ? 
                (double)(totalTransformations - failedTransformations) / totalTransformations * 100 : 100.0);
            metrics.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting transformation metrics", e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }

    @Override
    public Map<String, Object> getSecurityMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            LocalDateTime now = LocalDateTime.now();
            
            // Get security event counts
            long authEvents = auditService.countByAction("AUTHENTICATION", last24Hours, now);
            long authzEvents = auditService.countByAction("AUTHORIZATION", last24Hours, now);
            long securityEvents = auditService.countByAction("SECURITY_EVENT", last24Hours, now);
            long failedSecurityEvents = auditEventRepository.findByTimestampBetween(last24Hours, now)
                .stream()
                .filter(event -> ("AUTHENTICATION".equals(event.getAction()) || 
                                "AUTHORIZATION".equals(event.getAction()) ||
                                "SECURITY_EVENT".equals(event.getAction())) &&
                               ("FAILURE".equals(event.getOutcome()) || "ERROR".equals(event.getOutcome())))
                .count();
            
            metrics.put("authenticationEvents24h", authEvents);
            metrics.put("authorizationEvents24h", authzEvents);
            metrics.put("securityEvents24h", securityEvents);
            metrics.put("failedSecurityEvents24h", failedSecurityEvents);
            metrics.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting security metrics", e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }

    @Override
    public Map<String, Object> getAuditMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            LocalDateTime now = LocalDateTime.now();
            
            // Get audit statistics
            long totalEvents = auditEventRepository.findByTimestampBetween(last24Hours, now).size();
            long failedEvents = auditService.countFailedEvents(last24Hours, now);
            
            metrics.put("totalAuditEvents24h", totalEvents);
            metrics.put("failedAuditEvents24h", failedEvents);
            metrics.put("totalAuditEvents", auditEventRepository.count());
            metrics.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting audit metrics", e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }

    @Override
    public boolean isSystemReady() {
        try {
            Map<String, Object> health = getSystemHealth();
            return "UP".equals(health.get("status"));
        } catch (Exception e) {
            logger.error("Error checking system readiness", e);
            return false;
        }
    }

    @Override
    public boolean isSystemAlive() {
        try {
            // Basic liveness check - just verify the service is running
            return true;
        } catch (Exception e) {
            logger.error("Error checking system liveness", e);
            return false;
        }
    }

    @Override
    public void generateAlert(String alertType, String severity, String message, Map<String, Object> details) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", UUID.randomUUID().toString());
            alert.put("type", alertType);
            alert.put("severity", severity);
            alert.put("message", message);
            alert.put("details", details != null ? details : new HashMap<>());
            alert.put("timestamp", LocalDateTime.now());
            alert.put("resolved", false);
            
            // Store alert in memory (in production, persist to database)
            alertHistory.computeIfAbsent(alertType, k -> new ArrayList<>()).add(alert);
            
            // Log alert
            logger.warn("ALERT [{}] {}: {} - {}", severity, alertType, message, details);
            
            // Audit the alert
            auditService.logSystemEvent("MONITORING", "ALERT_GENERATED", "SUCCESS", 
                Map.of("alertType", alertType, "severity", severity, "message", message));
            
        } catch (Exception e) {
            logger.error("Error generating alert", e);
        }
    }

    @Override
    public Map<String, Object> getRecentAlerts(int hours) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
            List<Map<String, Object>> recentAlerts = new ArrayList<>();
            
            for (List<Map<String, Object>> alerts : alertHistory.values()) {
                for (Map<String, Object> alert : alerts) {
                    LocalDateTime alertTime = (LocalDateTime) alert.get("timestamp");
                    if (alertTime.isAfter(cutoff)) {
                        recentAlerts.add(alert);
                    }
                }
            }
            
            // Sort by timestamp descending
            recentAlerts.sort((a, b) -> {
                LocalDateTime timeA = (LocalDateTime) a.get("timestamp");
                LocalDateTime timeB = (LocalDateTime) b.get("timestamp");
                return timeB.compareTo(timeA);
            });
            
            result.put("alerts", recentAlerts);
            result.put("count", recentAlerts.size());
            result.put("periodHours", hours);
            result.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting recent alerts", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getSystemUptime() {
        Map<String, Object> uptime = new HashMap<>();
        
        try {
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long uptimeMs = runtimeBean.getUptime();
            
            uptime.put("startTime", systemStartTime);
            uptime.put("currentTime", LocalDateTime.now());
            uptime.put("uptimeMilliseconds", uptimeMs);
            uptime.put("uptimeSeconds", uptimeMs / 1000);
            uptime.put("uptimeMinutes", uptimeMs / (1000 * 60));
            uptime.put("uptimeHours", uptimeMs / (1000 * 60 * 60));
            uptime.put("uptimeDays", uptimeMs / (1000 * 60 * 60 * 24));
            
        } catch (Exception e) {
            logger.error("Error getting system uptime", e);
            uptime.put("error", e.getMessage());
        }
        
        return uptime;
    }

    @Override
    public Map<String, Object> getResourceUtilization() {
        Map<String, Object> utilization = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // Memory utilization
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            Map<String, Object> memory = new HashMap<>();
            memory.put("used", usedMemory);
            memory.put("free", freeMemory);
            memory.put("total", totalMemory);
            memory.put("max", maxMemory);
            memory.put("usagePercent", (double) usedMemory / totalMemory * 100);
            
            utilization.put("memory", memory);
            
            // Heap memory
            Map<String, Object> heap = new HashMap<>();
            heap.put("used", memoryBean.getHeapMemoryUsage().getUsed());
            heap.put("committed", memoryBean.getHeapMemoryUsage().getCommitted());
            heap.put("max", memoryBean.getHeapMemoryUsage().getMax());
            heap.put("usagePercent", 
                (double) memoryBean.getHeapMemoryUsage().getUsed() / 
                memoryBean.getHeapMemoryUsage().getMax() * 100);
            
            utilization.put("heap", heap);
            
            // CPU information
            Map<String, Object> cpu = new HashMap<>();
            cpu.put("availableProcessors", runtime.availableProcessors());
            
            utilization.put("cpu", cpu);
            utilization.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting resource utilization", e);
            utilization.put("error", e.getMessage());
        }
        
        return utilization;
    }
}