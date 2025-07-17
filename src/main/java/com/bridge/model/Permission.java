package com.bridge.model;

/**
 * Granular permissions for healthcare data access
 * Used in conjunction with roles for fine-grained authorization
 */
public enum Permission {
    
    // Data transformation permissions
    TRANSFORM_HL7_TO_FHIR("transform:hl7-to-fhir", "Transform HL7 v2 messages to FHIR"),
    VALIDATE_HL7_MESSAGE("validate:hl7-message", "Validate HL7 v2 message format"),
    VALIDATE_FHIR_RESOURCE("validate:fhir-resource", "Validate FHIR resource"),
    
    // Patient data permissions
    READ_PATIENT_DATA("read:patient-data", "Read patient clinical data"),
    WRITE_PATIENT_DATA("write:patient-data", "Create or update patient data"),
    DELETE_PATIENT_DATA("delete:patient-data", "Delete patient data"),
    
    // Consent management permissions
    READ_CONSENT("read:consent", "Read patient consent records"),
    WRITE_CONSENT("write:consent", "Create or update consent records"),
    MANAGE_CONSENT("manage:consent", "Full consent management access"),
    
    // Audit and monitoring permissions
    READ_AUDIT_LOGS("read:audit-logs", "Read system audit logs"),
    WRITE_AUDIT_LOGS("write:audit-logs", "Create audit log entries"),
    MANAGE_AUDIT("manage:audit", "Full audit management access"),
    
    // System administration permissions
    MANAGE_USERS("manage:users", "Manage user accounts and roles"),
    MANAGE_SYSTEM("manage:system", "System configuration and maintenance"),
    VIEW_SYSTEM_METRICS("view:system-metrics", "View system performance metrics"),
    
    // API access permissions
    API_ACCESS("api:access", "General API access"),
    BULK_DATA_ACCESS("api:bulk-data", "Bulk data export access"),
    SYSTEM_API_ACCESS("api:system", "System-to-system API access"),
    
    // TEFCA-specific permissions
    TEFCA_QUERY("tefca:query", "Query TEFCA network participants"),
    TEFCA_RESPOND("tefca:respond", "Respond to TEFCA queries"),
    TEFCA_ADMIN("tefca:admin", "TEFCA network administration");

    private final String permissionCode;
    private final String description;

    Permission(String permissionCode, String description) {
        this.permissionCode = permissionCode;
        this.description = description;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parse permission from string code
     */
    public static Permission fromCode(String code) {
        for (Permission permission : values()) {
            if (permission.permissionCode.equals(code)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Unknown permission code: " + code);
    }
}