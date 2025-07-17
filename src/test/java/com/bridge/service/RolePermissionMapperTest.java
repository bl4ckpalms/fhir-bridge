package com.bridge.service;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RolePermissionMapper
 * Tests the role-permission mapping matrix for healthcare roles
 */
class RolePermissionMapperTest {

    private RolePermissionMapper rolePermissionMapper;

    @BeforeEach
    void setUp() {
        rolePermissionMapper = new RolePermissionMapper();
    }

    @Test
    void testSystemAdminHasAllPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.SYSTEM_ADMIN);

        // Assert
        assertTrue(permissions.contains(Permission.MANAGE_SYSTEM));
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.DELETE_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.MANAGE_AUDIT));
        assertTrue(permissions.contains(Permission.TEFCA_ADMIN));
    }

    @Test
    void testPhysicianHasClinicalPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.PHYSICIAN);

        // Assert
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.READ_CONSENT));
        assertTrue(permissions.contains(Permission.WRITE_CONSENT));
        assertTrue(permissions.contains(Permission.TRANSFORM_HL7_TO_FHIR));
        assertTrue(permissions.contains(Permission.TEFCA_QUERY));
        assertTrue(permissions.contains(Permission.TEFCA_RESPOND));
        
        // Should not have system admin permissions
        assertFalse(permissions.contains(Permission.MANAGE_SYSTEM));
        assertFalse(permissions.contains(Permission.DELETE_PATIENT_DATA));
    }

    @Test
    void testNurseHasLimitedClinicalPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.NURSE);

        // Assert
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.READ_CONSENT));
        assertTrue(permissions.contains(Permission.WRITE_CONSENT));
        
        // Should not have TEFCA or system permissions
        assertFalse(permissions.contains(Permission.TEFCA_QUERY));
        assertFalse(permissions.contains(Permission.DELETE_PATIENT_DATA));
        assertFalse(permissions.contains(Permission.MANAGE_SYSTEM));
    }

    @Test
    void testPatientHasLimitedSelfAccessPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.PATIENT);

        // Assert
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.READ_CONSENT));
        assertTrue(permissions.contains(Permission.WRITE_CONSENT));
        assertTrue(permissions.contains(Permission.API_ACCESS));
        
        // Should not have write patient data or system permissions
        assertFalse(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertFalse(permissions.contains(Permission.DELETE_PATIENT_DATA));
        assertFalse(permissions.contains(Permission.MANAGE_SYSTEM));
        assertFalse(permissions.contains(Permission.READ_AUDIT_LOGS));
    }

    @Test
    void testApiClientHasSystemToSystemPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.API_CLIENT);

        // Assert
        assertTrue(permissions.contains(Permission.TRANSFORM_HL7_TO_FHIR));
        assertTrue(permissions.contains(Permission.VALIDATE_HL7_MESSAGE));
        assertTrue(permissions.contains(Permission.VALIDATE_FHIR_RESOURCE));
        assertTrue(permissions.contains(Permission.API_ACCESS));
        assertTrue(permissions.contains(Permission.SYSTEM_API_ACCESS));
        
        // Should not have patient data access
        assertFalse(permissions.contains(Permission.READ_PATIENT_DATA));
        assertFalse(permissions.contains(Permission.WRITE_PATIENT_DATA));
    }

    @Test
    void testComplianceOfficerHasAuditPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.COMPLIANCE_OFFICER);

        // Assert
        assertTrue(permissions.contains(Permission.READ_AUDIT_LOGS));
        assertTrue(permissions.contains(Permission.WRITE_AUDIT_LOGS));
        assertTrue(permissions.contains(Permission.MANAGE_AUDIT));
        assertTrue(permissions.contains(Permission.VIEW_SYSTEM_METRICS));
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.READ_CONSENT));
        
        // Should not have write patient data permissions
        assertFalse(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertFalse(permissions.contains(Permission.DELETE_PATIENT_DATA));
    }

    @Test
    void testTefcaParticipantHasTefcaPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.TEFCA_PARTICIPANT);

        // Assert
        assertTrue(permissions.contains(Permission.TEFCA_QUERY));
        assertTrue(permissions.contains(Permission.TEFCA_RESPOND));
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.TRANSFORM_HL7_TO_FHIR));
        
        // Should not have admin permissions
        assertFalse(permissions.contains(Permission.TEFCA_ADMIN));
        assertFalse(permissions.contains(Permission.MANAGE_SYSTEM));
    }

    @Test
    void testTefcaAdminHasFullTefcaPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.TEFCA_ADMIN);

        // Assert
        assertTrue(permissions.contains(Permission.TEFCA_QUERY));
        assertTrue(permissions.contains(Permission.TEFCA_RESPOND));
        assertTrue(permissions.contains(Permission.TEFCA_ADMIN));
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.MANAGE_CONSENT));
        assertTrue(permissions.contains(Permission.READ_AUDIT_LOGS));
        assertTrue(permissions.contains(Permission.MANAGE_AUDIT));
    }

    @Test
    void testDataAnalystHasAnalyticsPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.DATA_ANALYST);

        // Assert
        assertTrue(permissions.contains(Permission.READ_AUDIT_LOGS));
        assertTrue(permissions.contains(Permission.VIEW_SYSTEM_METRICS));
        assertTrue(permissions.contains(Permission.BULK_DATA_ACCESS));
        
        // Should not have patient data write permissions
        assertFalse(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertFalse(permissions.contains(Permission.READ_PATIENT_DATA));
    }

    @Test
    void testHasPermissionForSingleRole() {
        // Act & Assert
        assertTrue(rolePermissionMapper.hasPermission(HealthcareRole.PHYSICIAN, Permission.READ_PATIENT_DATA));
        assertFalse(rolePermissionMapper.hasPermission(HealthcareRole.PATIENT, Permission.WRITE_PATIENT_DATA));
        assertTrue(rolePermissionMapper.hasPermission(HealthcareRole.SYSTEM_ADMIN, Permission.MANAGE_SYSTEM));
    }

    @Test
    void testHasPermissionForMultipleRoles() {
        // Arrange
        Set<HealthcareRole> roles = Set.of(HealthcareRole.NURSE, HealthcareRole.TECHNICIAN);

        // Act & Assert
        assertTrue(rolePermissionMapper.hasPermission(roles, Permission.READ_PATIENT_DATA));
        assertTrue(rolePermissionMapper.hasPermission(roles, Permission.VALIDATE_HL7_MESSAGE));
        assertFalse(rolePermissionMapper.hasPermission(roles, Permission.MANAGE_SYSTEM));
    }

    @Test
    void testGetPermissionsForMultipleRoles() {
        // Arrange
        Set<HealthcareRole> roles = Set.of(HealthcareRole.PHYSICIAN, HealthcareRole.COMPLIANCE_OFFICER);

        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRoles(roles);

        // Assert
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.READ_AUDIT_LOGS));
        assertTrue(permissions.contains(Permission.MANAGE_AUDIT));
        assertTrue(permissions.contains(Permission.TEFCA_QUERY));
    }

    @Test
    void testGetPermissionsForUnknownRole() {
        // This test ensures the mapper handles unknown roles gracefully
        // Since we can't create unknown enum values, we test with empty collection
        
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRoles(Set.of());

        // Assert
        assertTrue(permissions.isEmpty());
    }

    @Test
    void testHealthInfoManagerHasDataManagementPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.HEALTH_INFO_MANAGER);

        // Assert
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.MANAGE_CONSENT));
        assertTrue(permissions.contains(Permission.READ_AUDIT_LOGS));
        assertTrue(permissions.contains(Permission.BULK_DATA_ACCESS));
        assertTrue(permissions.contains(Permission.TRANSFORM_HL7_TO_FHIR));
        
        // Should not have system admin permissions
        assertFalse(permissions.contains(Permission.MANAGE_SYSTEM));
        assertFalse(permissions.contains(Permission.DELETE_PATIENT_DATA));
    }

    @Test
    void testPharmacistHasMedicationFocusedPermissions() {
        // Act
        Set<Permission> permissions = rolePermissionMapper.getPermissionsForRole(HealthcareRole.PHARMACIST);

        // Assert
        assertTrue(permissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(permissions.contains(Permission.READ_CONSENT));
        assertTrue(permissions.contains(Permission.TRANSFORM_HL7_TO_FHIR));
        
        // Should not have write permissions
        assertFalse(permissions.contains(Permission.WRITE_PATIENT_DATA));
        assertFalse(permissions.contains(Permission.WRITE_CONSENT));
        assertFalse(permissions.contains(Permission.DELETE_PATIENT_DATA));
    }
}