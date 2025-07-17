package com.bridge.service;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import com.bridge.model.UserPrincipal;

import java.util.Set;

/**
 * Service interface for role-based authorization and access control
 * Implements healthcare-specific RBAC with TEFCA compliance
 */
public interface AuthorizationService {

    /**
     * Check if user has a specific permission
     */
    boolean hasPermission(UserPrincipal user, Permission permission);

    /**
     * Check if user has any of the specified permissions
     */
    boolean hasAnyPermission(UserPrincipal user, Permission... permissions);

    /**
     * Check if user has all of the specified permissions
     */
    boolean hasAllPermissions(UserPrincipal user, Permission... permissions);

    /**
     * Check if user has a specific role
     */
    boolean hasRole(UserPrincipal user, HealthcareRole role);

    /**
     * Check if user has any of the specified roles
     */
    boolean hasAnyRole(UserPrincipal user, HealthcareRole... roles);

    /**
     * Get all permissions for a user based on their roles
     */
    Set<Permission> getUserPermissions(UserPrincipal user);

    /**
     * Check if user can access patient data for a specific organization
     */
    boolean canAccessPatientData(UserPrincipal user, String organizationId);

    /**
     * Check if user can access patient data for a specific patient
     */
    boolean canAccessPatientData(UserPrincipal user, String patientId, String organizationId);

    /**
     * Check if user can modify patient data
     */
    boolean canModifyPatientData(UserPrincipal user, String organizationId);

    /**
     * Check if user can manage consent records
     */
    boolean canManageConsent(UserPrincipal user, String patientId);

    /**
     * Check if user can access audit logs
     */
    boolean canAccessAuditLogs(UserPrincipal user);

    /**
     * Check if user can perform TEFCA operations
     */
    boolean canPerformTefcaOperations(UserPrincipal user);

    /**
     * Validate user access for API endpoint
     */
    boolean validateEndpointAccess(UserPrincipal user, String endpoint, String httpMethod);
}