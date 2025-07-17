package com.bridge.service;

/**
 * Service interface for comprehensive audit logging and monitoring
 */
public interface AuditService {
    
    /**
     * Log authorization check for audit purposes
     */
    void logAuthorizationCheck(String userId, String permission, boolean granted);
    
    // Additional interface methods will be completed in later tasks
}