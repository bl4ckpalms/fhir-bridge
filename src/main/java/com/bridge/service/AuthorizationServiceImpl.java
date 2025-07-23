package com.bridge.service;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import com.bridge.model.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of AuthorizationService with healthcare-specific RBAC
 * Provides role-based access control with TEFCA compliance
 */
@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

    private final RolePermissionMapper rolePermissionMapper;
    private final AuditService auditService;

    public AuthorizationServiceImpl(RolePermissionMapper rolePermissionMapper, AuditService auditService) {
        this.rolePermissionMapper = rolePermissionMapper;
        this.auditService = auditService;
    }

    @Override
    public boolean hasPermission(UserPrincipal user, Permission permission) {
        if (user == null || permission == null) {
            return false;
        }

        Set<HealthcareRole> userRoles = getUserHealthcareRoles(user);
        boolean hasPermission = rolePermissionMapper.hasPermission(userRoles, permission);

        // Log authorization check for audit
        auditService.logAuthorization(user.getUserId(), permission.getPermissionCode(), "CHECK", 
            hasPermission ? "SUCCESS" : "FAILURE", 
            Map.of("permission", permission.getPermissionCode(), "roles", userRoles.toString()));

        return hasPermission;
    }

    @Override
    public boolean hasAnyPermission(UserPrincipal user, Permission... permissions) {
        if (user == null || permissions == null || permissions.length == 0) {
            return false;
        }

        return Arrays.stream(permissions)
                .anyMatch(permission -> hasPermission(user, permission));
    }

    @Override
    public boolean hasAllPermissions(UserPrincipal user, Permission... permissions) {
        if (user == null || permissions == null || permissions.length == 0) {
            return false;
        }

        return Arrays.stream(permissions)
                .allMatch(permission -> hasPermission(user, permission));
    }

    @Override
    public boolean hasRole(UserPrincipal user, HealthcareRole role) {
        if (user == null || role == null) {
            return false;
        }

        return getUserHealthcareRoles(user).contains(role);
    }

    @Override
    public boolean hasAnyRole(UserPrincipal user, HealthcareRole... roles) {
        if (user == null || roles == null || roles.length == 0) {
            return false;
        }

        Set<HealthcareRole> userRoles = getUserHealthcareRoles(user);
        return Arrays.stream(roles)
                .anyMatch(userRoles::contains);
    }

    @Override
    public Set<Permission> getUserPermissions(UserPrincipal user) {
        if (user == null) {
            return Collections.emptySet();
        }

        Set<HealthcareRole> userRoles = getUserHealthcareRoles(user);
        return rolePermissionMapper.getPermissionsForRoles(userRoles);
    }

    @Override
    public boolean canAccessPatientData(UserPrincipal user, String organizationId) {
        if (user == null || organizationId == null) {
            return false;
        }

        // Check if user has permission to read patient data
        if (!hasPermission(user, Permission.READ_PATIENT_DATA)) {
            return false;
        }

        // Check organization-level access
        return canAccessOrganizationData(user, organizationId);
    }

    @Override
    public boolean canAccessPatientData(UserPrincipal user, String patientId, String organizationId) {
        if (user == null || patientId == null || organizationId == null) {
            return false;
        }

        // First check general patient data access
        if (!canAccessPatientData(user, organizationId)) {
            return false;
        }

        // For patient role, only allow access to their own data
        if (hasRole(user, HealthcareRole.PATIENT)) {
            return user.getUserId().equals(patientId);
        }

        // For patient proxy, check if they're authorized for this patient
        if (hasRole(user, HealthcareRole.PATIENT_PROXY)) {
            return isAuthorizedProxy(user.getUserId(), patientId);
        }

        // Other healthcare roles can access patient data within their organization
        return true;
    }

    @Override
    public boolean canModifyPatientData(UserPrincipal user, String organizationId) {
        if (user == null || organizationId == null) {
            return false;
        }

        // Check if user has permission to write patient data
        if (!hasPermission(user, Permission.WRITE_PATIENT_DATA)) {
            return false;
        }

        // Check organization-level access
        return canAccessOrganizationData(user, organizationId);
    }

    @Override
    public boolean canManageConsent(UserPrincipal user, String patientId) {
        if (user == null || patientId == null) {
            return false;
        }

        // Check if user has consent management permissions
        if (!hasAnyPermission(user, Permission.READ_CONSENT, Permission.WRITE_CONSENT, Permission.MANAGE_CONSENT)) {
            return false;
        }

        // Patients can only manage their own consent
        if (hasRole(user, HealthcareRole.PATIENT)) {
            return user.getUserId().equals(patientId);
        }

        // Patient proxies can manage consent for authorized patients
        if (hasRole(user, HealthcareRole.PATIENT_PROXY)) {
            return isAuthorizedProxy(user.getUserId(), patientId);
        }

        // Healthcare providers and administrators can manage consent within their organization
        return hasAnyRole(user, HealthcareRole.PHYSICIAN, HealthcareRole.NURSE, 
                         HealthcareRole.HEALTH_INFO_MANAGER, HealthcareRole.SYSTEM_ADMIN);
    }

    @Override
    public boolean canAccessAuditLogs(UserPrincipal user) {
        if (user == null) {
            return false;
        }

        return hasPermission(user, Permission.READ_AUDIT_LOGS);
    }

    @Override
    public boolean canPerformTefcaOperations(UserPrincipal user) {
        if (user == null) {
            return false;
        }

        return hasAnyPermission(user, Permission.TEFCA_QUERY, Permission.TEFCA_RESPOND, Permission.TEFCA_ADMIN);
    }

    @Override
    public boolean validateEndpointAccess(UserPrincipal user, String endpoint, String httpMethod) {
        if (user == null || endpoint == null || httpMethod == null) {
            return false;
        }

        // Define endpoint-permission mappings
        Map<String, Permission> endpointPermissions = getEndpointPermissionMappings();
        
        // Check if endpoint requires specific permission
        String endpointKey = httpMethod.toUpperCase() + ":" + endpoint;
        Permission requiredPermission = endpointPermissions.get(endpointKey);
        
        if (requiredPermission != null) {
            return hasPermission(user, requiredPermission);
        }

        // Default to requiring API access for any endpoint
        return hasPermission(user, Permission.API_ACCESS);
    }

    /**
     * Convert user role strings to HealthcareRole enums
     */
    private Set<HealthcareRole> getUserHealthcareRoles(UserPrincipal user) {
        return user.getRoles().stream()
                .map(this::parseHealthcareRole)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Parse role string to HealthcareRole enum
     */
    private HealthcareRole parseHealthcareRole(String roleString) {
        try {
            // Remove ROLE_ prefix if present
            String cleanRole = roleString.startsWith("ROLE_") ? roleString.substring(5) : roleString;
            return HealthcareRole.valueOf(cleanRole);
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown healthcare role: {}", roleString);
            return null;
        }
    }

    /**
     * Check if user can access data for a specific organization
     */
    private boolean canAccessOrganizationData(UserPrincipal user, String organizationId) {
        // System admin can access all organizations
        if (hasRole(user, HealthcareRole.SYSTEM_ADMIN)) {
            return true;
        }

        // TEFCA participants can access data across organizations
        if (hasRole(user, HealthcareRole.TEFCA_PARTICIPANT) || hasRole(user, HealthcareRole.TEFCA_ADMIN)) {
            return true;
        }

        // Other users can only access data within their organization
        return user.getOrganizationId() != null && user.getOrganizationId().equals(organizationId);
    }

    /**
     * Check if user is an authorized proxy for a patient
     * In a real implementation, this would check a database of proxy relationships
     */
    private boolean isAuthorizedProxy(String proxyUserId, String patientId) {
        // TODO: Implement actual proxy authorization check
        // For now, return false as a safe default
        logger.debug("Proxy authorization check needed for proxy {} and patient {}", proxyUserId, patientId);
        return false;
    }

    /**
     * Define endpoint to permission mappings
     */
    private Map<String, Permission> getEndpointPermissionMappings() {
        Map<String, Permission> mappings = new HashMap<>();
        
        // Transformation endpoints
        mappings.put("POST:/api/v1/transform/hl7v2-to-fhir", Permission.TRANSFORM_HL7_TO_FHIR);
        mappings.put("POST:/api/v1/validate/hl7v2", Permission.VALIDATE_HL7_MESSAGE);
        mappings.put("POST:/api/v1/validate/fhir", Permission.VALIDATE_FHIR_RESOURCE);
        
        // Patient data endpoints
        mappings.put("GET:/api/v1/fhir/Patient", Permission.READ_PATIENT_DATA);
        mappings.put("POST:/api/v1/fhir/Patient", Permission.WRITE_PATIENT_DATA);
        mappings.put("PUT:/api/v1/fhir/Patient", Permission.WRITE_PATIENT_DATA);
        mappings.put("DELETE:/api/v1/fhir/Patient", Permission.DELETE_PATIENT_DATA);
        
        // Consent endpoints
        mappings.put("GET:/api/v1/consent", Permission.READ_CONSENT);
        mappings.put("POST:/api/v1/consent", Permission.WRITE_CONSENT);
        mappings.put("PUT:/api/v1/consent", Permission.WRITE_CONSENT);
        
        // Audit endpoints
        mappings.put("GET:/api/v1/audit", Permission.READ_AUDIT_LOGS);
        
        // System endpoints
        mappings.put("GET:/api/v1/system/metrics", Permission.VIEW_SYSTEM_METRICS);
        mappings.put("POST:/api/v1/system/config", Permission.MANAGE_SYSTEM);
        
        // TEFCA endpoints
        mappings.put("POST:/api/v1/tefca/query", Permission.TEFCA_QUERY);
        mappings.put("POST:/api/v1/tefca/respond", Permission.TEFCA_RESPOND);
        
        return mappings;
    }
}