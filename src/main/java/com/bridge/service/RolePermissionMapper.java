package com.bridge.service;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Maps healthcare roles to their associated permissions
 * Implements role-based access control (RBAC) permission matrix
 */
@Component
public class RolePermissionMapper {

    private final Map<HealthcareRole, Set<Permission>> rolePermissions;

    public RolePermissionMapper() {
        this.rolePermissions = initializeRolePermissions();
    }

    /**
     * Get all permissions for a given role
     */
    public Set<Permission> getPermissionsForRole(HealthcareRole role) {
        return rolePermissions.getOrDefault(role, Collections.emptySet());
    }

    /**
     * Get all permissions for multiple roles
     */
    public Set<Permission> getPermissionsForRoles(Collection<HealthcareRole> roles) {
        Set<Permission> allPermissions = new HashSet<>();
        for (HealthcareRole role : roles) {
            allPermissions.addAll(getPermissionsForRole(role));
        }
        return allPermissions;
    }

    /**
     * Check if a role has a specific permission
     */
    public boolean hasPermission(HealthcareRole role, Permission permission) {
        return rolePermissions.getOrDefault(role, Collections.emptySet()).contains(permission);
    }

    /**
     * Check if any of the roles has a specific permission
     */
    public boolean hasPermission(Collection<HealthcareRole> roles, Permission permission) {
        return roles.stream()
                .anyMatch(role -> hasPermission(role, permission));
    }

    /**
     * Initialize the role-permission mapping matrix
     */
    private Map<HealthcareRole, Set<Permission>> initializeRolePermissions() {
        Map<HealthcareRole, Set<Permission>> permissions = new HashMap<>();

        // SYSTEM_ADMIN - Full access to everything
        permissions.put(HealthcareRole.SYSTEM_ADMIN, Set.of(
                Permission.TRANSFORM_HL7_TO_FHIR,
                Permission.VALIDATE_HL7_MESSAGE,
                Permission.VALIDATE_FHIR_RESOURCE,
                Permission.READ_PATIENT_DATA,
                Permission.WRITE_PATIENT_DATA,
                Permission.DELETE_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.WRITE_CONSENT,
                Permission.MANAGE_CONSENT,
                Permission.READ_AUDIT_LOGS,
                Permission.WRITE_AUDIT_LOGS,
                Permission.MANAGE_AUDIT,
                Permission.MANAGE_USERS,
                Permission.MANAGE_SYSTEM,
                Permission.VIEW_SYSTEM_METRICS,
                Permission.API_ACCESS,
                Permission.BULK_DATA_ACCESS,
                Permission.SYSTEM_API_ACCESS,
                Permission.TEFCA_QUERY,
                Permission.TEFCA_RESPOND,
                Permission.TEFCA_ADMIN
        ));

        // API_CLIENT - System-to-system access
        permissions.put(HealthcareRole.API_CLIENT, Set.of(
                Permission.TRANSFORM_HL7_TO_FHIR,
                Permission.VALIDATE_HL7_MESSAGE,
                Permission.VALIDATE_FHIR_RESOURCE,
                Permission.API_ACCESS,
                Permission.SYSTEM_API_ACCESS,
                Permission.WRITE_AUDIT_LOGS
        ));

        // PHYSICIAN - Full clinical data access
        permissions.put(HealthcareRole.PHYSICIAN, Set.of(
                Permission.TRANSFORM_HL7_TO_FHIR,
                Permission.VALIDATE_HL7_MESSAGE,
                Permission.VALIDATE_FHIR_RESOURCE,
                Permission.READ_PATIENT_DATA,
                Permission.WRITE_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.WRITE_CONSENT,
                Permission.API_ACCESS,
                Permission.TEFCA_QUERY,
                Permission.TEFCA_RESPOND
        ));

        // NURSE - Clinical data access with some restrictions
        permissions.put(HealthcareRole.NURSE, Set.of(
                Permission.TRANSFORM_HL7_TO_FHIR,
                Permission.VALIDATE_HL7_MESSAGE,
                Permission.READ_PATIENT_DATA,
                Permission.WRITE_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.WRITE_CONSENT,
                Permission.API_ACCESS
        ));

        // PHARMACIST - Medication-focused access
        permissions.put(HealthcareRole.PHARMACIST, Set.of(
                Permission.TRANSFORM_HL7_TO_FHIR,
                Permission.VALIDATE_HL7_MESSAGE,
                Permission.READ_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.API_ACCESS
        ));

        // TECHNICIAN - Limited technical access
        permissions.put(HealthcareRole.TECHNICIAN, Set.of(
                Permission.VALIDATE_HL7_MESSAGE,
                Permission.VALIDATE_FHIR_RESOURCE,
                Permission.READ_PATIENT_DATA,
                Permission.API_ACCESS
        ));

        // HEALTH_INFO_MANAGER - Data management focus
        permissions.put(HealthcareRole.HEALTH_INFO_MANAGER, Set.of(
                Permission.TRANSFORM_HL7_TO_FHIR,
                Permission.VALIDATE_HL7_MESSAGE,
                Permission.VALIDATE_FHIR_RESOURCE,
                Permission.READ_PATIENT_DATA,
                Permission.WRITE_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.WRITE_CONSENT,
                Permission.MANAGE_CONSENT,
                Permission.READ_AUDIT_LOGS,
                Permission.API_ACCESS,
                Permission.BULK_DATA_ACCESS
        ));

        // COMPLIANCE_OFFICER - Audit and compliance focus
        permissions.put(HealthcareRole.COMPLIANCE_OFFICER, Set.of(
                Permission.READ_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.READ_AUDIT_LOGS,
                Permission.WRITE_AUDIT_LOGS,
                Permission.MANAGE_AUDIT,
                Permission.VIEW_SYSTEM_METRICS,
                Permission.API_ACCESS
        ));

        // DATA_ANALYST - Analytics access (de-identified data)
        permissions.put(HealthcareRole.DATA_ANALYST, Set.of(
                Permission.READ_AUDIT_LOGS,
                Permission.VIEW_SYSTEM_METRICS,
                Permission.API_ACCESS,
                Permission.BULK_DATA_ACCESS
        ));

        // TEFCA_PARTICIPANT - TEFCA network access
        permissions.put(HealthcareRole.TEFCA_PARTICIPANT, Set.of(
                Permission.TRANSFORM_HL7_TO_FHIR,
                Permission.VALIDATE_HL7_MESSAGE,
                Permission.VALIDATE_FHIR_RESOURCE,
                Permission.READ_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.API_ACCESS,
                Permission.TEFCA_QUERY,
                Permission.TEFCA_RESPOND
        ));

        // TEFCA_ADMIN - TEFCA administration
        permissions.put(HealthcareRole.TEFCA_ADMIN, Set.of(
                Permission.TRANSFORM_HL7_TO_FHIR,
                Permission.VALIDATE_HL7_MESSAGE,
                Permission.VALIDATE_FHIR_RESOURCE,
                Permission.READ_PATIENT_DATA,
                Permission.WRITE_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.WRITE_CONSENT,
                Permission.MANAGE_CONSENT,
                Permission.READ_AUDIT_LOGS,
                Permission.MANAGE_AUDIT,
                Permission.MANAGE_USERS,
                Permission.VIEW_SYSTEM_METRICS,
                Permission.API_ACCESS,
                Permission.BULK_DATA_ACCESS,
                Permission.TEFCA_QUERY,
                Permission.TEFCA_RESPOND,
                Permission.TEFCA_ADMIN
        ));

        // PATIENT - Self-access to own data
        permissions.put(HealthcareRole.PATIENT, Set.of(
                Permission.READ_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.WRITE_CONSENT,
                Permission.API_ACCESS
        ));

        // PATIENT_PROXY - Authorized representative access
        permissions.put(HealthcareRole.PATIENT_PROXY, Set.of(
                Permission.READ_PATIENT_DATA,
                Permission.READ_CONSENT,
                Permission.WRITE_CONSENT,
                Permission.API_ACCESS
        ));

        return permissions;
    }
}