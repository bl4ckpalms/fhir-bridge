package com.bridge.controller;

import com.bridge.service.MonitoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitoringController.class)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitoringService monitoringService;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> healthyResponse;
    private Map<String, Object> unhealthyResponse;

    @BeforeEach
    void setUp() {
        healthyResponse = new HashMap<>();
        healthyResponse.put("status", "UP");
        healthyResponse.put("timestamp", LocalDateTime.now());

        unhealthyResponse = new HashMap<>();
        unhealthyResponse.put("status", "DOWN");
        unhealthyResponse.put("timestamp", LocalDateTime.now());
        unhealthyResponse.put("error", "Service unavailable");
    }

    @Test
    void testGetSystemHealth_Healthy() throws Exception {
        // Given
        when(monitoringService.getSystemHealth()).thenReturn(healthyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetSystemHealth_Unhealthy() throws Exception {
        // Given
        when(monitoringService.getSystemHealth()).thenReturn(unhealthyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.error").value("Service unavailable"));
    }

    @Test
    void testGetLivenessProbe_Alive() throws Exception {
        // Given
        when(monitoringService.isSystemAlive()).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/live"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetLivenessProbe_NotAlive() throws Exception {
        // Given
        when(monitoringService.isSystemAlive()).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/live"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void testGetReadinessProbe_Ready() throws Exception {
        // Given
        when(monitoringService.isSystemReady()).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/ready"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetReadinessProbe_NotReady() throws Exception {
        // Given
        when(monitoringService.isSystemReady()).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/ready"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetDatabaseHealth_WithAdminRole() throws Exception {
        // Given
        when(monitoringService.getDatabaseHealth()).thenReturn(healthyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/database"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser(roles = "MONITOR")
    void testGetDatabaseHealth_WithMonitorRole() throws Exception {
        // Given
        when(monitoringService.getDatabaseHealth()).thenReturn(healthyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/database"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetDatabaseHealth_WithUserRole_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/database"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetCacheHealth() throws Exception {
        // Given
        when(monitoringService.getCacheHealth()).thenReturn(healthyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/cache"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetFhirServiceHealth() throws Exception {
        // Given
        Map<String, Object> fhirHealth = new HashMap<>(healthyResponse);
        fhirHealth.put("version", "R4");
        when(monitoringService.getFhirServiceHealth()).thenReturn(fhirHealth);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/fhir"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("R4"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetHl7ServiceHealth() throws Exception {
        // Given
        Map<String, Object> hl7Health = new HashMap<>(healthyResponse);
        hl7Health.put("version", "2.5");
        when(monitoringService.getHl7ServiceHealth()).thenReturn(hl7Health);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/hl7"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("2.5"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAuthServiceHealth() throws Exception {
        // Given
        when(monitoringService.getAuthServiceHealth()).thenReturn(healthyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/health/auth"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetPerformanceMetrics() throws Exception {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("jvm", Map.of("heapUsed", 1024000L, "heapMax", 2048000L));
        metrics.put("system", Map.of("availableProcessors", 4));
        metrics.put("timestamp", LocalDateTime.now());
        when(monitoringService.getPerformanceMetrics()).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/metrics/performance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.jvm.heapUsed").value(1024000L))
                .andExpect(jsonPath("$.system.availableProcessors").value(4));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTransformationMetrics() throws Exception {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalTransformations24h", 100L);
        metrics.put("failedTransformations24h", 5L);
        metrics.put("successRate", 95.0);
        when(monitoringService.getTransformationMetrics()).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/metrics/transformations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalTransformations24h").value(100L))
                .andExpect(jsonPath("$.successRate").value(95.0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSecurityMetrics() throws Exception {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("authenticationEvents24h", 200L);
        metrics.put("authorizationEvents24h", 150L);
        metrics.put("failedSecurityEvents24h", 10L);
        when(monitoringService.getSecurityMetrics()).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/metrics/security"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authenticationEvents24h").value(200L))
                .andExpect(jsonPath("$.failedSecurityEvents24h").value(10L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAuditMetrics() throws Exception {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalAuditEvents", 1000L);
        metrics.put("totalAuditEvents24h", 50L);
        metrics.put("failedAuditEvents24h", 2L);
        when(monitoringService.getAuditMetrics()).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/metrics/audit"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalAuditEvents").value(1000L))
                .andExpect(jsonPath("$.failedAuditEvents24h").value(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSystemUptime() throws Exception {
        // Given
        Map<String, Object> uptime = new HashMap<>();
        uptime.put("uptimeHours", 24L);
        uptime.put("uptimeDays", 1L);
        uptime.put("startTime", LocalDateTime.now().minusDays(1));
        when(monitoringService.getSystemUptime()).thenReturn(uptime);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/uptime"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uptimeHours").value(24L))
                .andExpect(jsonPath("$.uptimeDays").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetResourceUtilization() throws Exception {
        // Given
        Map<String, Object> resources = new HashMap<>();
        resources.put("memory", Map.of("usagePercent", 75.5));
        resources.put("heap", Map.of("usagePercent", 60.0));
        when(monitoringService.getResourceUtilization()).thenReturn(resources);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/resources"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.memory.usagePercent").value(75.5))
                .andExpect(jsonPath("$.heap.usagePercent").value(60.0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetRecentAlerts() throws Exception {
        // Given
        Map<String, Object> alerts = new HashMap<>();
        alerts.put("count", 3);
        alerts.put("periodHours", 24);
        alerts.put("alerts", java.util.Arrays.asList(
            Map.of("type", "DATABASE_ERROR", "severity", "HIGH"),
            Map.of("type", "CACHE_WARNING", "severity", "MEDIUM")
        ));
        when(monitoringService.getRecentAlerts(24)).thenReturn(alerts);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.periodHours").value(24));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetRecentAlertsWithCustomHours() throws Exception {
        // Given
        Map<String, Object> alerts = new HashMap<>();
        alerts.put("count", 1);
        alerts.put("periodHours", 12);
        when(monitoringService.getRecentAlerts(12)).thenReturn(alerts);

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/alerts?hours=12"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.periodHours").value(12));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateTestAlert() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/monitoring/alerts/test")
                        .param("type", "TEST_ALERT")
                        .param("severity", "LOW")
                        .param("message", "This is a test alert")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Test alert generated successfully"))
                .andExpect(jsonPath("$.type").value("TEST_ALERT"))
                .andExpect(jsonPath("$.severity").value("LOW"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGenerateTestAlert_WithUserRole_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/monitoring/alerts/test")
                        .param("type", "TEST_ALERT")
                        .param("severity", "LOW")
                        .param("message", "This is a test alert")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSystemDashboard() throws Exception {
        // Given
        when(monitoringService.getSystemHealth()).thenReturn(healthyResponse);
        when(monitoringService.getPerformanceMetrics()).thenReturn(Map.of("jvm", Map.of("heapUsed", 1024L)));
        when(monitoringService.getTransformationMetrics()).thenReturn(Map.of("totalTransformations24h", 100L));
        when(monitoringService.getSecurityMetrics()).thenReturn(Map.of("authenticationEvents24h", 200L));
        when(monitoringService.getAuditMetrics()).thenReturn(Map.of("totalAuditEvents", 1000L));
        when(monitoringService.getSystemUptime()).thenReturn(Map.of("uptimeHours", 24L));
        when(monitoringService.getResourceUtilization()).thenReturn(Map.of("memory", Map.of("usagePercent", 75.0)));
        when(monitoringService.getRecentAlerts(24)).thenReturn(Map.of("count", 0));

        // When & Then
        mockMvc.perform(get("/api/v1/monitoring/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.health.status").value("UP"))
                .andExpect(jsonPath("$.performance.jvm.heapUsed").value(1024L))
                .andExpect(jsonPath("$.transformations.totalTransformations24h").value(100L))
                .andExpect(jsonPath("$.security.authenticationEvents24h").value(200L))
                .andExpect(jsonPath("$.audit.totalAuditEvents").value(1000L))
                .andExpect(jsonPath("$.uptime.uptimeHours").value(24L))
                .andExpect(jsonPath("$.resources.memory.usagePercent").value(75.0))
                .andExpect(jsonPath("$.recentAlerts.count").value(0));
    }
}