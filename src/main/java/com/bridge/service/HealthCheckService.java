package com.bridge.service;

import java.util.Map;
import java.util.HashMap;

public interface HealthCheckService {
    Map<String, Object> checkReadiness();
    Map<String, Object> checkLiveness();
    Map<String, Object> getDetailedHealth();
}