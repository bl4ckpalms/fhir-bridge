package com.bridge.security;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import com.bridge.model.UserPrincipal;
import com.bridge.service.AuthorizationService;
import com.bridge.util.TestUserDataLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration tests for role-based authorization using test user accounts
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RoleBasedAuthorizationTest {

    @Mock
    private AuthorizationService authorizationService;

    private SecurityAspect securityAspect;

    @BeforeEach
    void setUp() {
        securityAspect = new SecurityAspect(authorizationService);
    }

    @AfterEach
    void tearDown() {
        TestUserDataLoader.clearSecurityContext();
    }

    @Test
    @DisplayName("System Admin should have access to all operations")
    void testSystemAdminAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-SYSADMIN-001");
        UserPrincipal systemAdmin = TestUserDataLoader.createUserPrincipal("TEST-SYSADMIN-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(systemAdmin), any(Permission.class))).thenReturn(true);
        when(authorizationService.hasAnyRole(eq(systemAdmin), any(HealthcareRole[].class))).thenReturn(true);

        // Then - System admin should have all permissions
        assertTrue(systemAdmin.getRoles().contains("SYSTEM_ADMIN"));
        assertEquals("TEST-ORG-001", systemAdmin.getOrganizationId());
        assertTrue(systemAdmin.isEnabled());
    }

    @Test
    @DisplayName("Physician should have clinical data access")
    void testPhysicianAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-PHYSICIAN-001");
        UserPrincipal physician = TestUserDataLoader.createUserPrincipal("TEST-PHYSICIAN-001");

        // Mock authorization service responses for physician permissions
        when(authorizationService.hasPermission(eq(physician), eq(Permission.READ_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(physician), eq(Permission.WRITE_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(physician), eq(Permission.MANAGE_CONSENT))).thenReturn(true);
        when(authorizationService.hasPermission(eq(physician), eq(Permission.MANAGE_SYSTEM))).thenReturn(false);

        // Then
        assertTrue(physician.getRoles().contains("PHYSICIAN"));
        assertEquals("dr.johnson", physician.getUsername());
        assertEquals("TEST-ORG-001", physician.getOrganizationId());
    }

    @Test
    @DisplayName("Nurse should have limited patient data access")
    void testNurseAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-NURSE-001");
        UserPrincipal nurse = TestUserDataLoader.createUserPrincipal("TEST-NURSE-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(nurse), eq(Permission.READ_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(nurse), eq(Permission.WRITE_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(nurse), eq(Permission.DELETE_PATIENT_DATA))).thenReturn(false);
        when(authorizationService.hasPermission(eq(nurse), eq(Permission.MANAGE_SYSTEM))).thenReturn(false);

        // Then
        assertTrue(nurse.getRoles().contains("NURSE"));
        assertEquals("nurse.williams", nurse.getUsername());
        assertEquals("TEST-ORG-001", nurse.getOrganizationId());
    }

    @Test
    @DisplayName("Pharmacist should have medication-focused access")
    void testPharmacistAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-PHARMACIST-001");
        UserPrincipal pharmacist = TestUserDataLoader.createUserPrincipal("TEST-PHARMACIST-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(pharmacist), eq(Permission.READ_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(pharmacist), eq(Permission.WRITE_PATIENT_DATA))).thenReturn(false);
        when(authorizationService.hasPermission(eq(pharmacist), eq(Permission.READ_CONSENT))).thenReturn(true);

        // Then
        assertTrue(pharmacist.getRoles().contains("PHARMACIST"));
        assertEquals("pharm.davis", pharmacist.getUsername());
        assertEquals("TEST-ORG-002", pharmacist.getOrganizationId());
    }

    @Test
    @DisplayName("Technician should have minimal access")
    void testTechnicianAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-TECHNICIAN-001");
        UserPrincipal technician = TestUserDataLoader.createUserPrincipal("TEST-TECHNICIAN-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(technician), eq(Permission.READ_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(technician), eq(Permission.WRITE_PATIENT_DATA))).thenReturn(false);
        when(authorizationService.hasPermission(eq(technician), eq(Permission.MANAGE_CONSENT))).thenReturn(false);
        when(authorizationService.hasPermission(eq(technician), eq(Permission.READ_AUDIT_LOGS))).thenReturn(false);

        // Then
        assertTrue(technician.getRoles().contains("TECHNICIAN"));
        assertEquals("tech.brown", technician.getUsername());
        assertEquals("TEST-ORG-002", technician.getOrganizationId());
    }

    @Test
    @DisplayName("Health Information Manager should have data management access")
    void testHealthInfoManagerAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-HIM-001");
        UserPrincipal him = TestUserDataLoader.createUserPrincipal("TEST-HIM-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(him), eq(Permission.READ_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(him), eq(Permission.MANAGE_CONSENT))).thenReturn(true);
        when(authorizationService.hasPermission(eq(him), eq(Permission.READ_AUDIT_LOGS))).thenReturn(true);
        when(authorizationService.hasPermission(eq(him), eq(Permission.BULK_DATA_ACCESS))).thenReturn(true);

        // Then
        assertTrue(him.getRoles().contains("HEALTH_INFO_MANAGER"));
        assertEquals("him.manager", him.getUsername());
        assertEquals("TEST-ORG-001", him.getOrganizationId());
    }

    @Test
    @DisplayName("Compliance Officer should have audit access")
    void testComplianceOfficerAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-COMPLIANCE-001");
        UserPrincipal compliance = TestUserDataLoader.createUserPrincipal("TEST-COMPLIANCE-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(compliance), eq(Permission.READ_AUDIT_LOGS))).thenReturn(true);
        when(authorizationService.hasPermission(eq(compliance), eq(Permission.MANAGE_AUDIT))).thenReturn(true);
        when(authorizationService.hasPermission(eq(compliance), eq(Permission.VIEW_SYSTEM_METRICS))).thenReturn(true);
        when(authorizationService.hasPermission(eq(compliance), eq(Permission.WRITE_PATIENT_DATA))).thenReturn(false);

        // Then
        assertTrue(compliance.getRoles().contains("COMPLIANCE_OFFICER"));
        assertEquals("compliance.officer", compliance.getUsername());
        assertEquals("TEST-ORG-001", compliance.getOrganizationId());
    }

    @Test
    @DisplayName("Data Analyst should have limited read access")
    void testDataAnalystAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-ANALYST-001");
        UserPrincipal analyst = TestUserDataLoader.createUserPrincipal("TEST-ANALYST-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(analyst), eq(Permission.READ_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(analyst), eq(Permission.BULK_DATA_ACCESS))).thenReturn(true);
        when(authorizationService.hasPermission(eq(analyst), eq(Permission.WRITE_PATIENT_DATA))).thenReturn(false);
        when(authorizationService.hasPermission(eq(analyst), eq(Permission.MANAGE_CONSENT))).thenReturn(false);

        // Then
        assertTrue(analyst.getRoles().contains("DATA_ANALYST"));
        assertEquals("data.analyst", analyst.getUsername());
        assertEquals("TEST-ORG-003", analyst.getOrganizationId());
    }

    @Test
    @DisplayName("TEFCA Participant should have network access")
    void testTefcaParticipantAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-TEFCA-PARTICIPANT-001");
        UserPrincipal tefcaParticipant = TestUserDataLoader.createUserPrincipal("TEST-TEFCA-PARTICIPANT-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(tefcaParticipant), eq(Permission.TEFCA_QUERY))).thenReturn(true);
        when(authorizationService.hasPermission(eq(tefcaParticipant), eq(Permission.TEFCA_RESPOND))).thenReturn(true);
        when(authorizationService.hasPermission(eq(tefcaParticipant), eq(Permission.READ_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(tefcaParticipant), eq(Permission.TEFCA_ADMIN))).thenReturn(false);

        // Then
        assertTrue(tefcaParticipant.getRoles().contains("TEFCA_PARTICIPANT"));
        assertEquals("tefca.participant", tefcaParticipant.getUsername());
        assertEquals("TEST-ORG-004", tefcaParticipant.getOrganizationId());
    }

    @Test
    @DisplayName("TEFCA Admin should have full network administration access")
    void testTefcaAdminAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-TEFCA-ADMIN-001");
        UserPrincipal tefcaAdmin = TestUserDataLoader.createUserPrincipal("TEST-TEFCA-ADMIN-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(tefcaAdmin), eq(Permission.TEFCA_ADMIN))).thenReturn(true);
        when(authorizationService.hasPermission(eq(tefcaAdmin), eq(Permission.TEFCA_QUERY))).thenReturn(true);
        when(authorizationService.hasPermission(eq(tefcaAdmin), eq(Permission.TEFCA_RESPOND))).thenReturn(true);
        when(authorizationService.hasPermission(eq(tefcaAdmin), eq(Permission.MANAGE_USERS))).thenReturn(true);

        // Then
        assertTrue(tefcaAdmin.getRoles().contains("TEFCA_ADMIN"));
        assertEquals("tefca.admin", tefcaAdmin.getUsername());
        assertEquals("TEST-ORG-005", tefcaAdmin.getOrganizationId());
    }

    @Test
    @DisplayName("Patient should have self-access only")
    void testPatientAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-PATIENT-001");
        UserPrincipal patient = TestUserDataLoader.createUserPrincipal("TEST-PATIENT-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(patient), eq(Permission.READ_CONSENT))).thenReturn(true);
        when(authorizationService.hasPermission(eq(patient), eq(Permission.MANAGE_CONSENT))).thenReturn(true);
        when(authorizationService.hasPermission(eq(patient), eq(Permission.READ_PATIENT_DATA))).thenReturn(false);
        when(authorizationService.hasPermission(eq(patient), eq(Permission.WRITE_PATIENT_DATA))).thenReturn(false);

        // Then
        assertTrue(patient.getRoles().contains("PATIENT"));
        assertEquals("patient.smith", patient.getUsername());
        assertEquals("TEST-ORG-001", patient.getOrganizationId());
    }

    @Test
    @DisplayName("Patient Proxy should have limited proxy access")
    void testPatientProxyAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-PATIENT-PROXY-001");
        UserPrincipal proxy = TestUserDataLoader.createUserPrincipal("TEST-PATIENT-PROXY-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(proxy), eq(Permission.READ_CONSENT))).thenReturn(true);
        when(authorizationService.hasPermission(eq(proxy), eq(Permission.MANAGE_CONSENT))).thenReturn(true);
        when(authorizationService.hasPermission(eq(proxy), eq(Permission.READ_PATIENT_DATA))).thenReturn(false);

        // Then
        assertTrue(proxy.getRoles().contains("PATIENT_PROXY"));
        assertEquals("proxy.jones", proxy.getUsername());
        assertEquals("TEST-ORG-001", proxy.getOrganizationId());
    }

    @Test
    @DisplayName("API Client should have system integration access")
    void testApiClientAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-API-CLIENT-001");
        UserPrincipal apiClient = TestUserDataLoader.createUserPrincipal("TEST-API-CLIENT-001");

        // Mock authorization service responses
        when(authorizationService.hasPermission(eq(apiClient), eq(Permission.SYSTEM_API_ACCESS))).thenReturn(true);
        when(authorizationService.hasPermission(eq(apiClient), eq(Permission.TRANSFORM_HL7_TO_FHIR))).thenReturn(true);
        when(authorizationService.hasPermission(eq(apiClient), eq(Permission.BULK_DATA_ACCESS))).thenReturn(true);
        when(authorizationService.hasPermission(eq(apiClient), eq(Permission.MANAGE_USERS))).thenReturn(false);

        // Then
        assertTrue(apiClient.getRoles().contains("API_CLIENT"));
        assertEquals("api.client.system", apiClient.getUsername());
        assertEquals("TEST-ORG-006", apiClient.getOrganizationId());
    }

    @Test
    @DisplayName("Multi-role user should have combined permissions")
    void testMultiRoleUserAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-MULTI-ROLE-001");
        UserPrincipal multiRole = TestUserDataLoader.createUserPrincipal("TEST-MULTI-ROLE-001");

        // Mock authorization service responses for combined roles
        when(authorizationService.hasPermission(eq(multiRole), eq(Permission.READ_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(multiRole), eq(Permission.WRITE_PATIENT_DATA))).thenReturn(true);
        when(authorizationService.hasPermission(eq(multiRole), eq(Permission.TEFCA_QUERY))).thenReturn(true);
        when(authorizationService.hasPermission(eq(multiRole), eq(Permission.TEFCA_RESPOND))).thenReturn(true);

        // Then
        assertTrue(multiRole.getRoles().contains("PHYSICIAN"));
        assertTrue(multiRole.getRoles().contains("TEFCA_PARTICIPANT"));
        assertEquals("multi.role.user", multiRole.getUsername());
        assertEquals("TEST-ORG-001", multiRole.getOrganizationId());
    }

    @Test
    @DisplayName("Inactive user should be denied access")
    void testInactiveUserAccess() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-INACTIVE-001");
        UserPrincipal inactiveUser = TestUserDataLoader.createUserPrincipal("TEST-INACTIVE-001");

        // Then
        assertFalse(inactiveUser.isEnabled());
        assertFalse(inactiveUser.isAccountNonExpired());
        assertFalse(inactiveUser.isAccountNonLocked());
        assertFalse(inactiveUser.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("Limited access user should have minimal permissions")
    void testLimitedAccessUser() {
        // Given
        TestUserDataLoader.setSecurityContext("TEST-LIMITED-001");
        UserPrincipal limitedUser = TestUserDataLoader.createUserPrincipal("TEST-LIMITED-001");

        // Mock authorization service responses for limited access
        when(authorizationService.hasPermission(eq(limitedUser), eq(Permission.API_ACCESS))).thenReturn(true);
        when(authorizationService.hasPermission(eq(limitedUser), eq(Permission.READ_PATIENT_DATA))).thenReturn(false);
        when(authorizationService.hasPermission(eq(limitedUser), eq(Permission.WRITE_PATIENT_DATA))).thenReturn(false);
        when(authorizationService.hasPermission(eq(limitedUser), eq(Permission.MANAGE_CONSENT))).thenReturn(false);

        // Then
        assertTrue(limitedUser.getRoles().contains("TECHNICIAN"));
        assertEquals("limited.access", limitedUser.getUsername());
        assertEquals("TEST-ORG-003", limitedUser.getOrganizationId());
        assertTrue(limitedUser.isEnabled());
    }

    @Test
    @DisplayName("Test user data loader utility functions")
    void testUserDataLoaderUtilities() {
        // Test getting users by role
        var physicians = TestUserDataLoader.getUsersByRole(HealthcareRole.PHYSICIAN);
        assertFalse(physicians.isEmpty());
        assertTrue(physicians.stream().anyMatch(user -> user.getUsername().equals("dr.johnson")));

        // Test getting users by permission
        var usersWithPatientDataAccess = TestUserDataLoader.getUsersByPermission(Permission.READ_PATIENT_DATA);
        assertFalse(usersWithPatientDataAccess.isEmpty());

        // Test getting active users
        var activeUsers = TestUserDataLoader.getActiveUsers();
        assertTrue(activeUsers.size() > 0);
        assertTrue(activeUsers.stream().allMatch(TestUserDataLoader.TestUser::isActive));

        // Test getting inactive users
        var inactiveUsers = TestUserDataLoader.getInactiveUsers();
        assertTrue(inactiveUsers.size() > 0);
        assertTrue(inactiveUsers.stream().noneMatch(TestUserDataLoader.TestUser::isActive));

        // Test getting JWT tokens
        var systemAdminToken = TestUserDataLoader.getTokenByUserId("TEST-SYSADMIN-001");
        assertTrue(systemAdminToken.isPresent());
        assertEquals("Bearer", systemAdminToken.get().getTokenType());
    }
}