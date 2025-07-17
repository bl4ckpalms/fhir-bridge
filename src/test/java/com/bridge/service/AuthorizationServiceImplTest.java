package com.bridge.service;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import com.bridge.model.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthorizationServiceImpl
 * Tests role-based access control and healthcare-specific authorization logic
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @Mock
    private AuditService auditService;

    private AuthorizationServiceImpl authorizationService;

    private UserPrincipal physicianUser;
    private UserPrincipal nurseUser;
    private UserPrincipal patientUser;
    private UserPrincipal systemAdminUser;
    private UserPrincipal tefcaParticipantUser;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationServiceImpl(rolePermissionMapper, auditService);

        // Create test users with different roles
        physicianUser = new UserPrincipal(
                "physician-1", "dr.smith", "org-1", 
                Arrays.asList("PHYSICIAN"), true);

        nurseUser = new UserPrincipal(
                "nurse-1", "nurse.jones", "org-1", 
                Arrays.asList("NURSE"), true);

        patientUser = new UserPrincipal(
                "patient-1", "john.doe", "org-1", 
                Arrays.asList("PATIENT"), true);

        systemAdminUser = new UserPrincipal(
                "admin-1", "admin", "system", 
                Arrays.asList("SYSTEM_ADMIN"), true);

        tefcaParticipantUser = new UserPrincipal(
                "tefca-1", "tefca.user", "org-2", 
                Arrays.asList("TEFCA_PARTICIPANT"), true);
    }

    @Test
    void testHasPermission_WithValidPermission_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.hasPermission(physicianUser, Permission.READ_PATIENT_DATA);

        // Assert
        assertTrue(result);
        verify(auditService).logAuthorizationCheck(
                eq("physician-1"), eq("read:patient-data"), eq(true));
    }

    @Test
    void testHasPermission_WithInvalidPermission_ReturnsFalse() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.MANAGE_SYSTEM)))
                .thenReturn(false);

        // Act
        boolean result = authorizationService.hasPermission(nurseUser, Permission.MANAGE_SYSTEM);

        // Assert
        assertFalse(result);
        verify(auditService).logAuthorizationCheck(
                eq("nurse-1"), eq("manage:system"), eq(false));
    }

    @Test
    void testHasPermission_WithNullUser_ReturnsFalse() {
        // Act
        boolean result = authorizationService.hasPermission(null, Permission.READ_PATIENT_DATA);

        // Assert
        assertFalse(result);
        verifyNoInteractions(auditService);
    }

    @Test
    void testHasAnyPermission_WithValidPermissions_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.hasAnyPermission(
                physicianUser, Permission.READ_PATIENT_DATA, Permission.WRITE_PATIENT_DATA);

        // Assert
        assertTrue(result);
    }

    @Test
    void testHasAllPermissions_WithAllValidPermissions_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.WRITE_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.hasAllPermissions(
                physicianUser, Permission.READ_PATIENT_DATA, Permission.WRITE_PATIENT_DATA);

        // Assert
        assertTrue(result);
    }

    @Test
    void testHasAllPermissions_WithSomeInvalidPermissions_ReturnsFalse() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.MANAGE_SYSTEM)))
                .thenReturn(false);

        // Act
        boolean result = authorizationService.hasAllPermissions(
                physicianUser, Permission.READ_PATIENT_DATA, Permission.MANAGE_SYSTEM);

        // Assert
        assertFalse(result);
    }

    @Test
    void testCanAccessPatientData_SameOrganization_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canAccessPatientData(physicianUser, "org-1");

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanAccessPatientData_DifferentOrganization_ReturnsFalse() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canAccessPatientData(physicianUser, "org-2");

        // Assert
        assertFalse(result);
    }

    @Test
    void testCanAccessPatientData_SystemAdmin_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canAccessPatientData(systemAdminUser, "any-org");

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanAccessPatientData_TefcaParticipant_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canAccessPatientData(tefcaParticipantUser, "org-1");

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanAccessPatientDataWithPatientId_PatientAccessingOwnData_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canAccessPatientData(
                patientUser, "patient-1", "org-1");

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanAccessPatientDataWithPatientId_PatientAccessingOtherData_ReturnsFalse() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canAccessPatientData(
                patientUser, "patient-2", "org-1");

        // Assert
        assertFalse(result);
    }

    @Test
    void testCanModifyPatientData_WithWritePermission_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.WRITE_PATIENT_DATA)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canModifyPatientData(physicianUser, "org-1");

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanModifyPatientData_WithoutWritePermission_ReturnsFalse() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.WRITE_PATIENT_DATA)))
                .thenReturn(false);

        // Act
        boolean result = authorizationService.canModifyPatientData(patientUser, "org-1");

        // Assert
        assertFalse(result);
    }

    @Test
    void testCanManageConsent_PatientManagingOwnConsent_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_CONSENT)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canManageConsent(patientUser, "patient-1");

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanManageConsent_PhysicianManagingConsent_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_CONSENT)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canManageConsent(physicianUser, "patient-1");

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanAccessAuditLogs_WithAuditPermission_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_AUDIT_LOGS)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canAccessAuditLogs(systemAdminUser);

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanAccessAuditLogs_WithoutAuditPermission_ReturnsFalse() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.READ_AUDIT_LOGS)))
                .thenReturn(false);

        // Act
        boolean result = authorizationService.canAccessAuditLogs(patientUser);

        // Assert
        assertFalse(result);
    }

    @Test
    void testCanPerformTefcaOperations_WithTefcaPermissions_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.TEFCA_QUERY)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.canPerformTefcaOperations(tefcaParticipantUser);

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanPerformTefcaOperations_WithoutTefcaPermissions_ReturnsFalse() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.TEFCA_QUERY)))
                .thenReturn(false);
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.TEFCA_RESPOND)))
                .thenReturn(false);
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.TEFCA_ADMIN)))
                .thenReturn(false);

        // Act
        boolean result = authorizationService.canPerformTefcaOperations(patientUser);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateEndpointAccess_WithRequiredPermission_ReturnsTrue() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.TRANSFORM_HL7_TO_FHIR)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.validateEndpointAccess(
                physicianUser, "/api/v1/transform/hl7v2-to-fhir", "POST");

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateEndpointAccess_WithoutRequiredPermission_ReturnsFalse() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.TRANSFORM_HL7_TO_FHIR)))
                .thenReturn(false);

        // Act
        boolean result = authorizationService.validateEndpointAccess(
                patientUser, "/api/v1/transform/hl7v2-to-fhir", "POST");

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateEndpointAccess_UnknownEndpoint_ChecksApiAccess() {
        // Arrange
        when(rolePermissionMapper.hasPermission(any(Collection.class), eq(Permission.API_ACCESS)))
                .thenReturn(true);

        // Act
        boolean result = authorizationService.validateEndpointAccess(
                physicianUser, "/api/v1/unknown/endpoint", "GET");

        // Assert
        assertTrue(result);
    }

    @Test
    void testGetUserPermissions_ReturnsCorrectPermissions() {
        // Arrange
        Set<Permission> expectedPermissions = Set.of(
                Permission.READ_PATIENT_DATA, Permission.WRITE_PATIENT_DATA);
        when(rolePermissionMapper.getPermissionsForRoles(any()))
                .thenReturn(expectedPermissions);

        // Act
        Set<Permission> result = authorizationService.getUserPermissions(physicianUser);

        // Assert
        assertEquals(expectedPermissions, result);
    }

    @Test
    void testGetUserPermissions_WithNullUser_ReturnsEmptySet() {
        // Act
        Set<Permission> result = authorizationService.getUserPermissions(null);

        // Assert
        assertTrue(result.isEmpty());
    }
}