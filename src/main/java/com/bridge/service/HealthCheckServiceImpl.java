package com.bridge.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;

@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> checkReadiness() {
        Map<String, Object> response = new HashMap<>();
        boolean ready = true;
        
        try {
            // Check database connectivity
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            response.put("database", "UP");
        } catch (Exception e) {
            response.put("database", "DOWN");
            response.put("database_error", e.getMessage());
            ready = false;
        }
        
        response.put("ready", ready);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    @Override
    public Map<String, Object> checkLiveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("alive", true);
        response.put("timestamp", System.currentTimeMillis());
        response.put("uptime", getUptime());
        
        return response;
    }

    @Override
    public Map<String, Object> getDetailedHealth() {
        Map<String, Object> response = new HashMap<>();
        boolean healthy = true;
        
        // Basic liveness check
        Map<String, Object> liveness = checkLiveness();
        response.put("liveness", liveness);
        
        // Readiness check
        Map<String, Object> readiness = checkReadiness();
        response.put("readiness", readiness);
        
        // Database detailed check
        try {
            Map<String, Object> dbDetails = getDatabaseDetails();
            response.put("database_details", dbDetails);
        } catch (Exception e) {
            response.put("database_details", Map.of("error", e.getMessage()));
            healthy = false;
        }
        
        // JVM metrics
        Map<String, Object> jvmMetrics = getJvmMetrics();
        response.put("jvm", jvmMetrics);
        
        response.put("healthy", healthy);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
    
    private Map<String, Object> getDatabaseDetails() {
        Map<String, Object> details = new HashMap<>();
        
        try {
            // Connection pool info
            List<Map<String, Object>> connections = jdbcTemplate.queryForList(
                "SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = 'active'"
            );
            details.put("active_connections", connections.get(0).get("active_connections"));
            
            // Database size
            Long dbSize = jdbcTemplate.queryForObject(
                "SELECT pg_database_size(current_database())", Long.class
            );
            details.put("database_size_bytes", dbSize);
            
            // Transaction stats
            List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                "SELECT xact_commit, xact_rollback, blks_read, blks_hit FROM pg_stat_database WHERE datname = current_database()"
            );
            if (!stats.isEmpty()) {
                details.putAll(stats.get(0));
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get database details", e);
        }
        
        return details;
    }
    
    private Map<String, Object> getJvmMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        metrics.put("available_processors", runtime.availableProcessors());
        
        // Memory metrics
        metrics.put("heap_memory_used", memoryBean.getHeapMemoryUsage().getUsed());
        metrics.put("heap_memory_max", memoryBean.getHeapMemoryUsage().getMax());
        metrics.put("non_heap_memory_used", memoryBean.getNonHeapMemoryUsage().getUsed());
        
        // Runtime metrics
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        metrics.put("uptime", runtimeBean.getUptime());
        metrics.put("start_time", runtimeBean.getStartTime());
        
        // Memory usage percentage
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        if (maxMemory > 0) {
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            metrics.put("memory_usage_percent", memoryUsage);
        }
        
        return metrics;
    }
    
    private long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }
}