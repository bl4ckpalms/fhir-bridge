package com.bridge.service;

import com.bridge.entity.AuditEventEntity;
import com.bridge.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceImplTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private MonitoringServiceImpl monitoringService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetSystemHealth_AllComponentsHealthy() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(databaseMetaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/test");
        when(auditEventRepository.count()).thenReturn(100L);

        when(valueOperations.get(anyString())).thenReturn("test");

        // When
        Map<String, Object> health = monitoringService.getSystemHealth();

        // Then
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertTrue(health.containsKey("components"));
        assertTrue(health.containsKey("uptime"));
        assertTrue(health.containsKey("timestamp"));

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) health.get("components");
        assertNotNull(components);
        assertTrue(components.containsKey("database"));
        assertTrue(components.containsKey("cache"));
    }

    @Test
    void testGetDatabaseHealth_Healthy() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(databaseMetaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/test");
        when(auditEventRepository.count()).thenReturn(100L);

        // When
        Map<String, Object> health = monitoringService.getDatabaseHealth();

        // Then
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertEquals("PostgreSQL", health.get("database"));
        assertEquals(100L, health.get("auditEventCount"));
        assertTrue(health.containsKey("lastChecked"));
    }

    @Test
    void testGetDatabaseHealth_Unhealthy() throws Exception {
        // Given
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failed"));

        // When
        Map<String, Object> health = monitoringService.getDatabaseHealth();

        // Then
        assertNotNull(health);
        assertEquals("DOWN", health.get("status"));
        assertTrue(health.containsKey("error"));
        assertTrue(health.containsKey("lastChecked"));
    }

    @Test
    void testGetCacheHealth_Healthy() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("test");

        // When
        Map<String, Object> health = monitoringService.getCacheHealth();

        // Then
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertEquals("Redis", health.get("type"));
        assertTrue(health.containsKey("lastChecked"));
    }

    @Test
    void testGetCacheHealth_Unhealthy() {
        // Given
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        Map<String, Object> health = monitoringService.getCacheHealth();

        // Then
        assertNotNull(health);
        assertEquals("DOWN", health.get("status"));
        assertTrue(health.containsKey("error"));
        assertTrue(health.containsKey("lastChecked"));
    }

    @Test
    void testGetFhirServiceHealth() {
        // When
        Map<String, Object> health = monitoringService.getFhirServiceHealth();

        // Then
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertEquals("R4", health.get("version"));
        assertTrue(health.containsKey("profiles"));
        assertTrue(health.containsKey("lastChecked"));
    }

    @Test
    void testGetHl7ServiceHealth() {
        // When
        Map<String, Object> health = monitoringService.getHl7ServiceHealth();

        // Then
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertEquals("2.5", health.get("version"));
        assertTrue(health.containsKey("supportedMessages"));
        assertTrue(health.containsKey("lastChecked"));
    }

    @Test
    void testGetAuthServiceHealth() {
        // When
        Map<String, Object> health = monitoringService.getAuthServiceHealth();

        // Then
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertTrue(health.containsKey("authMethods"));
        assertTrue(health.containsKey("lastChecked"));
    }

    @Test
    void testGetPerformanceMetrics() {
        // When
        Map<String, Object> metrics = monitoringService.getPerformanceMetrics();

        // Then
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("jvm"));
        assertTrue(metrics.containsKey("system"));
        assertTrue(metrics.containsKey("timestamp"));

        @SuppressWarnings("unchecked")
        Map<String, Object> jvm = (Map<String, Object>) metrics.get("jvm");
        assertNotNull(jvm);
        assertTrue(jvm.containsKey("uptime"));
        assertTrue(jvm.containsKey("heapUsed"));
        assertTrue(jvm.containsKey("heapMax"));

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) metrics.get("system");
        assertNotNull(system);
        assertTrue(system.containsKey("availableProcessors"));
        assertTrue(system.containsKey("totalMemory"));
    }

    @Test
    void testGetTransformationMetrics() {
        // Given
        lenient().when(auditService.countByAction(eq("DATA_TRANSFORMATION"), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(50L);
        
        AuditEventEntity failedEvent = new AuditEventEntity("user1", "DATA_TRANSFORMATION", "FHIR", "res1", "FAILURE");
        lenient().when(auditEventRepository.findByTimestampBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(failedEvent));

        // When
        Map<String, Object> metrics = monitoringService.getTransformationMetrics();

        // Then
        assertNotNull(metrics);
        assertEquals(50L, metrics.get("totalTransformations24h"));
        assertTrue(metrics.containsKey("failedTransformations24h"));
        assertTrue(metrics.containsKey("successRate"));
        assertTrue(metrics.containsKey("timestamp"));
    }

    @Test
    void testGetSecurityMetrics() {
        // Given
        lenient().when(auditService.countByAction(eq("AUTHENTICATION"), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(100L);
        lenient().when(auditService.countByAction(eq("AUTHORIZATION"), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(80L);
        lenient().when(auditService.countByAction(eq("SECURITY_EVENT"), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(5L);
        
        AuditEventEntity failedAuthEvent = new AuditEventEntity("user1", "AUTHENTICATION", "USER", "user1", "FAILURE");
        lenient().when(auditEventRepository.findByTimestampBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(failedAuthEvent));

        // When
        Map<String, Object> metrics = monitoringService.getSecurityMetrics();

        // Then
        assertNotNull(metrics);
        assertEquals(100L, metrics.get("authenticationEvents24h"));
        assertEquals(80L, metrics.get("authorizationEvents24h"));
        assertEquals(5L, metrics.get("securityEvents24h"));
        assertTrue(metrics.containsKey("failedSecurityEvents24h"));
        assertTrue(metrics.containsKey("timestamp"));
    }

    @Test
    void testGetAuditMetrics() {
        // Given
        when(auditEventRepository.count()).thenReturn(1000L);
        when(auditService.countFailedEvents(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10L);
        when(auditEventRepository.findByTimestampBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(new AuditEventEntity()));

        // When
        Map<String, Object> metrics = monitoringService.getAuditMetrics();

        // Then
        assertNotNull(metrics);
        assertEquals(1000L, metrics.get("totalAuditEvents"));
        assertEquals(10L, metrics.get("failedAuditEvents24h"));
        assertTrue(metrics.containsKey("totalAuditEvents24h"));
        assertTrue(metrics.containsKey("timestamp"));
    }

    @Test
    void testIsSystemReady_True() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(databaseMetaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/test");
        when(auditEventRepository.count()).thenReturn(100L);
        when(valueOperations.get(anyString())).thenReturn("test");

        // When
        boolean isReady = monitoringService.isSystemReady();

        // Then
        assertTrue(isReady);
    }

    @Test
    void testIsSystemReady_False() throws Exception {
        // Given
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Database down"));

        // When
        boolean isReady = monitoringService.isSystemReady();

        // Then
        assertFalse(isReady);
    }

    @Test
    void testIsSystemAlive() {
        // When
        boolean isAlive = monitoringService.isSystemAlive();

        // Then
        assertTrue(isAlive);
    }

    @Test
    void testGenerateAlert() {
        // Given
        Map<String, Object> details = Map.of("component", "database", "error", "connection timeout");

        // When
        monitoringService.generateAlert("DATABASE_ERROR", "HIGH", "Database connection failed", details);

        // Then
        verify(auditService).logSystemEvent(eq("MONITORING"), eq("ALERT_GENERATED"), eq("SUCCESS"), any(Map.class));
    }

    @Test
    void testGetRecentAlerts() {
        // Given
        monitoringService.generateAlert("TEST_ALERT", "LOW", "Test message", Map.of("test", "value"));

        // When
        Map<String, Object> result = monitoringService.getRecentAlerts(24);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("alerts"));
        assertTrue(result.containsKey("count"));
        assertEquals(24, result.get("periodHours"));
        assertTrue(result.containsKey("timestamp"));
    }

    @Test
    void testGetSystemUptime() {
        // When
        Map<String, Object> uptime = monitoringService.getSystemUptime();

        // Then
        assertNotNull(uptime);
        assertTrue(uptime.containsKey("startTime"));
        assertTrue(uptime.containsKey("currentTime"));
        assertTrue(uptime.containsKey("uptimeMilliseconds"));
        assertTrue(uptime.containsKey("uptimeSeconds"));
        assertTrue(uptime.containsKey("uptimeMinutes"));
        assertTrue(uptime.containsKey("uptimeHours"));
        assertTrue(uptime.containsKey("uptimeDays"));
    }

    @Test
    void testGetResourceUtilization() {
        // When
        Map<String, Object> utilization = monitoringService.getResourceUtilization();

        // Then
        assertNotNull(utilization);
        assertTrue(utilization.containsKey("memory"));
        assertTrue(utilization.containsKey("heap"));
        assertTrue(utilization.containsKey("cpu"));
        assertTrue(utilization.containsKey("timestamp"));

        @SuppressWarnings("unchecked")
        Map<String, Object> memory = (Map<String, Object>) utilization.get("memory");
        assertNotNull(memory);
        assertTrue(memory.containsKey("used"));
        assertTrue(memory.containsKey("free"));
        assertTrue(memory.containsKey("total"));
        assertTrue(memory.containsKey("usagePercent"));

        @SuppressWarnings("unchecked")
        Map<String, Object> heap = (Map<String, Object>) utilization.get("heap");
        assertNotNull(heap);
        assertTrue(heap.containsKey("used"));
        assertTrue(heap.containsKey("committed"));
        assertTrue(heap.containsKey("max"));
        assertTrue(heap.containsKey("usagePercent"));
    }
}