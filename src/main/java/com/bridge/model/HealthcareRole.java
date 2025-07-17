package com.bridge.model;

/**
 * Healthcare-specific roles for RBAC system
 * Defines roles based on TEFCA and healthcare industry standards
 */
public enum HealthcareRole {
    
    // System roles
    SYSTEM_ADMIN("SYSTEM_ADMIN", "Full system administration access"),
    API_CLIENT("API_CLIENT", "System-to-system API access"),
    
    // Healthcare provider roles
    PHYSICIAN("PHYSICIAN", "Licensed physician with full patient data access"),
    NURSE("NURSE", "Registered nurse with patient care access"),
    PHARMACIST("PHARMACIST", "Licensed pharmacist with medication access"),
    TECHNICIAN("TECHNICIAN", "Healthcare technician with limited access"),
    
    // Administrative roles
    HEALTH_INFO_MANAGER("HEALTH_INFO_MANAGER", "Health information management access"),
    COMPLIANCE_OFFICER("COMPLIANCE_OFFICER", "Compliance and audit access"),
    DATA_ANALYST("DATA_ANALYST", "De-identified data analysis access"),
    
    // TEFCA-specific roles
    TEFCA_PARTICIPANT("TEFCA_PARTICIPANT", "TEFCA network participant access"),
    TEFCA_ADMIN("TEFCA_ADMIN", "TEFCA network administration access"),
    
    // Patient access
    PATIENT("PATIENT", "Patient self-access to own data"),
    PATIENT_PROXY("PATIENT_PROXY", "Authorized patient representative access");

    private final String roleName;
    private final String description;

    HealthcareRole(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this role has administrative privileges
     */
    public boolean isAdministrative() {
        return this == SYSTEM_ADMIN || this == TEFCA_ADMIN || this == COMPLIANCE_OFFICER;
    }

    /**
     * Check if this role can access patient clinical data
     */
    public boolean canAccessClinicalData() {
        return this == PHYSICIAN || this == NURSE || this == PHARMACIST || 
               this == HEALTH_INFO_MANAGER || this == SYSTEM_ADMIN;
    }

    /**
     * Check if this role can modify patient data
     */
    public boolean canModifyPatientData() {
        return this == PHYSICIAN || this == NURSE || this == SYSTEM_ADMIN;
    }

    /**
     * Check if this role can access audit logs
     */
    public boolean canAccessAuditLogs() {
        return this == SYSTEM_ADMIN || this == COMPLIANCE_OFFICER || this == TEFCA_ADMIN;
    }

    /**
     * Check if this role can manage consent records
     */
    public boolean canManageConsent() {
        return this == PHYSICIAN || this == NURSE || this == HEALTH_INFO_MANAGER || 
               this == SYSTEM_ADMIN || this == PATIENT || this == PATIENT_PROXY;
    }
}