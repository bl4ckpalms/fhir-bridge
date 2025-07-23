package com.bridge.service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service interface for system monitoring and health checks
 */
public interface MonitoringService {

    /**
     * Get overall system health status
     */
    Map<String, Object> getSystemHealth();

    /**
     * Get database health status
     */
    Map<String, Object> getDatabaseHealth();

    /**
     * Get cache health status
     */
    Map<String, Object> getCacheHealth();

    /**
     * Get FHIR service health status
     */
    Map<String, Object> getFhirServiceHealth();

    /**
     * Get HL7 service health status
     */
    Map<String, Object> getHl7ServiceHealth();

    /**
     * Get authentication service health status
     */
    Map<String, Object> getAuthServiceHealth();

    /**
     * Get system performance metrics
     */
    Map<String, Object> getPerformanceMetrics();

    /**
     * Get transformation metrics
     */
    Map<String, Object> getTransformationMetrics();

    /**
     * Get security metrics
     */
    Map<String, Object> getSecurityMetrics();

    /**
     * Get audit metrics
     */
    Map<String, Object> getAuditMetrics();

    /**
     * Check if system is ready to serve requests
     */
    boolean isSystemReady();

    /**
     * Check if system is alive
     */
    boolean isSystemAlive();

    /**
     * Generate alert for system issues
     */
    void generateAlert(String alertType, String severity, String message, Map<String, Object> details);

    /**
     * Get recent alerts
     */
    Map<String, Object> getRecentAlerts(int hours);

    /**
     * Get system uptime
     */
    Map<String, Object> getSystemUptime();

    /**
     * Get resource utilization metrics
     */
    Map<String, Object> getResourceUtilization();
}