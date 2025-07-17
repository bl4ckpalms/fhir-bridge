package com.bridge.model;

/**
 * Enumeration representing the status of a patient consent record
 */
public enum ConsentStatus {
    /**
     * Consent is pending approval or activation
     */
    PENDING,
    
    /**
     * Consent is active and valid for data sharing
     */
    ACTIVE,
    
    /**
     * Consent is inactive (alternative to pending)
     */
    INACTIVE,
    
    /**
     * Consent has been revoked by the patient
     */
    REVOKED,
    
    /**
     * Consent has expired based on the expiration date
     */
    EXPIRED,
    
    /**
     * Consent has been suspended temporarily
     */
    SUSPENDED,
    
    /**
     * Consent has been denied or rejected
     */
    DENIED
}