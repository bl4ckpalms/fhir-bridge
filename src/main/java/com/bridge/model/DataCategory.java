package com.bridge.model;

/**
 * Enumeration representing different categories of healthcare data
 * that can be included or excluded from consent agreements
 */
public enum DataCategory {
    /**
     * Basic demographic information (name, address, phone, etc.)
     */
    DEMOGRAPHICS,
    
    /**
     * Medical history and clinical notes
     */
    MEDICAL_HISTORY,
    
    /**
     * Clinical notes and documentation
     */
    CLINICAL_NOTES,
    
    /**
     * Laboratory test results and values
     */
    LAB_RESULTS,
    
    /**
     * Laboratory test results and values (alternative name)
     */
    LABORATORY_RESULTS,
    
    /**
     * Diagnostic imaging and radiology reports
     */
    IMAGING,
    
    /**
     * Prescription and medication information
     */
    MEDICATIONS,
    
    /**
     * Allergies and adverse reactions
     */
    ALLERGIES,
    
    /**
     * Immunization records
     */
    IMMUNIZATIONS,
    
    /**
     * Vital signs and measurements
     */
    VITAL_SIGNS,
    
    /**
     * Mental health and behavioral health records
     */
    MENTAL_HEALTH,
    
    /**
     * Substance abuse treatment records
     */
    SUBSTANCE_ABUSE,
    
    /**
     * Genetic and genomic information
     */
    GENETIC_DATA,
    
    /**
     * Insurance and financial information
     */
    FINANCIAL,
    
    /**
     * Emergency contact information
     */
    EMERGENCY_CONTACTS,
    
    /**
     * All categories of data
     */
    ALL
}