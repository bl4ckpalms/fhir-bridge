package com.bridge.controller;

import com.bridge.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for monitoring and health check endpoints
 */
@RestController
@RequestMapping("/api/v1/monitoring")
@Tag(name = "Monitoring", description = "System health checks and monitoring endpoints")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @Autowired
    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * Get overall system health status
     */
    @Operation(
        summary = "Get overall system health status",
        description = """
            Returns the overall health status of the FHIR Bridge system including all components.
            This endpoint provides a comprehensive view of system health for monitoring purposes.
            """,
        tags = {"Monitoring"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "System is healthy",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Healthy system",
                    value = """
                        {
                          "status": "UP",
                          "components": {
                            "database": {"status": "UP"},
                            "cache": {"status": "UP"},
                            "fhir": {"status": "UP"}
                          },
                          "timestamp": "2025-01-15T10:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "System is unhealthy",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Unhealthy system",
                    value = """
                        {
                          "status": "DOWN",
                          "components": {
                            "database": {"status": "DOWN", "error": "Connection timeout"}
                          },
                          "timestamp": "2025-01-15T10:30:00"
                        }
                        """
                )
            )
        )
    })
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = monitoringService.getSystemHealth();
        boolean isHealthy = "UP".equals(health.get("status"));
        return ResponseEntity.status(isHealthy ? 200 : 503).body(health);
    }

    /**
     * Kubernetes/Docker liveness probe endpoint
     */
    @GetMapping("/health/live")
    public ResponseEntity<Map<String, Object>> getLivenessProbe() {
        boolean isAlive = monitoringService.isSystemAlive();
        Map<String, Object> response = Map.of(
            "status", isAlive ? "UP" : "DOWN",
            "timestamp", java.time.LocalDateTime.now()
        );
        return ResponseEntity.status(isAlive ? 200 : 503).body(response);
    }

    /**
     * Kubernetes/Docker readiness probe endpoint
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> getReadinessProbe() {
        boolean isReady = monitoringService.isSystemReady();
        Map<String, Object> response = Map.of(
            "status", isReady ? "UP" : "DOWN",
            "timestamp", java.time.LocalDateTime.now()
        );
        return ResponseEntity.status(isReady ? 200 : 503).body(response);
    }

    /**
     * Get database health status
     */
    @GetMapping("/health/database")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        Map<String, Object> health = monitoringService.getDatabaseHealth();
        boolean isHealthy = "UP".equals(health.get("status"));
        return ResponseEntity.status(isHealthy ? 200 : 503).body(health);
    }

    /**
     * Get cache health status
     */
    @GetMapping("/health/cache")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        Map<String, Object> health = monitoringService.getCacheHealth();
        boolean isHealthy = "UP".equals(health.get("status"));
        return ResponseEntity.status(isHealthy ? 200 : 503).body(health);
    }

    /**
     * Get FHIR service health status
     */
    @GetMapping("/health/fhir")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getFhirServiceHealth() {
        Map<String, Object> health = monitoringService.getFhirServiceHealth();
        boolean isHealthy = "UP".equals(health.get("status"));
        return ResponseEntity.status(isHealthy ? 200 : 503).body(health);
    }

    /**
     * Get HL7 service health status
     */
    @GetMapping("/health/hl7")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getHl7ServiceHealth() {
        Map<String, Object> health = monitoringService.getHl7ServiceHealth();
        boolean isHealthy = "UP".equals(health.get("status"));
        return ResponseEntity.status(isHealthy ? 200 : 503).body(health);
    }

    /**
     * Get authentication service health status
     */
    @GetMapping("/health/auth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getAuthServiceHealth() {
        Map<String, Object> health = monitoringService.getAuthServiceHealth();
        boolean isHealthy = "UP".equals(health.get("status"));
        return ResponseEntity.status(isHealthy ? 200 : 503).body(health);
    }

    /**
     * Get system performance metrics
     */
    @GetMapping("/metrics/performance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> metrics = monitoringService.getPerformanceMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get transformation metrics
     */
    @GetMapping("/metrics/transformations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getTransformationMetrics() {
        Map<String, Object> metrics = monitoringService.getTransformationMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get security metrics
     */
    @GetMapping("/metrics/security")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getSecurityMetrics() {
        Map<String, Object> metrics = monitoringService.getSecurityMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get audit metrics
     */
    @GetMapping("/metrics/audit")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getAuditMetrics() {
        Map<String, Object> metrics = monitoringService.getAuditMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get system uptime information
     */
    @GetMapping("/uptime")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getSystemUptime() {
        Map<String, Object> uptime = monitoringService.getSystemUptime();
        return ResponseEntity.ok(uptime);
    }

    /**
     * Get resource utilization metrics
     */
    @GetMapping("/resources")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getResourceUtilization() {
        Map<String, Object> resources = monitoringService.getResourceUtilization();
        return ResponseEntity.ok(resources);
    }

    /**
     * Get recent alerts
     */
    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getRecentAlerts(
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> alerts = monitoringService.getRecentAlerts(hours);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Generate a test alert (for testing purposes)
     */
    @PostMapping("/alerts/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateTestAlert(
            @RequestParam String type,
            @RequestParam String severity,
            @RequestParam String message) {
        
        monitoringService.generateAlert(type, severity, message, 
            Map.of("source", "manual_test", "user", "admin"));
        
        Map<String, Object> response = Map.of(
            "message", "Test alert generated successfully",
            "type", type,
            "severity", severity,
            "timestamp", java.time.LocalDateTime.now()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get comprehensive system status dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public ResponseEntity<Map<String, Object>> getSystemDashboard() {
        Map<String, Object> dashboard = Map.of(
            "health", monitoringService.getSystemHealth(),
            "performance", monitoringService.getPerformanceMetrics(),
            "transformations", monitoringService.getTransformationMetrics(),
            "security", monitoringService.getSecurityMetrics(),
            "audit", monitoringService.getAuditMetrics(),
            "uptime", monitoringService.getSystemUptime(),
            "resources", monitoringService.getResourceUtilization(),
            "recentAlerts", monitoringService.getRecentAlerts(24)
        );
        
        return ResponseEntity.ok(dashboard);
    }
}