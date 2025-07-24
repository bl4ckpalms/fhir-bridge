package com.bridge.controller;

import com.bridge.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @Autowired
    private HealthCheckService healthCheckService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "fhir-bridge");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> response = healthCheckService.checkReadiness();
        boolean isReady = (Boolean) response.get("ready");
        
        if (isReady) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> livenessCheck() {
        Map<String, Object> response = healthCheckService.checkLiveness();
        boolean isAlive = (Boolean) response.get("alive");
        
        if (isAlive) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> response = healthCheckService.getDetailedHealth();
        boolean isHealthy = (Boolean) response.get("healthy");
        
        if (isHealthy) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
}