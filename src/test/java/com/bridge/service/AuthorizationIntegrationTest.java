package com.bridge.service;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import com.bridge.model.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the authorization system
 * Tests the complete RBAC system with real Spring context
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthorizationIntegrationTest {

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @MockBean
    private AuditService auditService;

    private UserPrincipal physicianUser;
    private UserPrincipal nurseUser;
    private UserPrincipal patientUser;
    private UserPrincipal systemAdminUser;
    private UserPrincipal tefcaParticipantUser;
    private UserPrincipal complianceOfficerUser;
    private UserPrincipal apiClientUser;

    @BeforeEach
    void setUp() {
        // Create test users representing different healthcare roles
        physicianUser = new UserPrincipal(
                "physician-1", "dr.smith", "hospital-a", 
                Arrays.asList("PHYSICIAN"), true);

        nurseUser = new UserPrincipal(
                "nurse-1", "nurse.jones", "hospital-a", 
                Arrays.asList("NURSE"), true);

        patientUser = new UserPrincipal(
                "patient-1", "john.doe", "hospital-a", 
                Arrays.asList("PATIENT"), true);

        systemAdminUser = new UserPrincipal(
                "admin-1", "admin", "system", 
                Arrays.asList("SYSTEM_ADMIN"), true);

        tefcaParticipantUser = new UserPrincipal(
                "tefca-1", "tefca.user", "network-org", 
                Arrays.asList("TEFCA_PARTICIPANT"), true);

        complianceOfficerUser = new UserPrincipal(
                "compliance-1", "compliance.officer", "hospital-a", 
                Arrays.asList("COMPLIANCE_OFFICER"), true);

        apiClientUser = new UserPrincipal(
                "api-client-1", "api-client", "system", 
                Arrays.asList("API_CLIENT"), true);
    }

    @Test
    void testPhysicianWorkflow_CanPerformClinicalOperations() {
        // Test physician can read and write patient data
        assertTrue(authorizationService.hasPermission(physicianUser, Permission.READ_PATIENT_DATA));
        assertTrue(authorizationService.hasPermission(physicianUser, Permission.WRITE_PATIENT_DATA));
        assertTrue(authorizationService.hasPermission(physicianUser, Permission.TRANSFORM_HL7_TO_FHIR));
        
        // Test physician can manage consent
        assertTrue(authorizationService.canManageConsent(physicianUser, "patient-1"));
        
        // Test physician can access patient data in their organization
        assertTrue(authorizationService.canAccessPatientData(physicianUser, "hospital-a"));
        
        // Test physician cannot access other organization's data
        assertFalse(authorizationService.canAccessPatientData(physicianUser, "hospital-b"));
        
        // Test physician cannot perform system administration
        assertFalse(authorizationService.hasPermission(physicianUser, Permission.MANAGE_SYSTEM));
        assertFalse(authorizationService.canAccessAuditLogs(physicianUser));
    }

    @Test
    void testNurseWorkflow_HasLimitedClinicalAccess() {
        // Test nurse can read and write patient data
        assertTrue(authorizationService.hasPermission(nurseUser, Permission.READ_PATIENT_DATA));
        assertTrue(authorizationService.hasPermission(nurseUser, Permission.WRITE_PATIENT_DATA));
        
        // Test nurse can manage consent
        assertTrue(authorizationService.canManageConsent(nurseUser, "patient-1"));
        
        // Test nurse can access patient data in their organization
        assertTrue(authorizationService.canAccessPatientData(nurseUser, "hospital-a"));
        
        // Test nurse cannot perform TEFCA operations
        assertFalse(authorizationService.canPerformTefcaOperations(nurseUser));
        
        // Test nurse cannot delete patient data
        assertFalse(authorizationService.hasPermission(nurseUser, Permission.DELETE_PATIENT_DATA));
    }

    @Test
    void testPatientWorkflow_CanOnlyAccessOwnData() {
        // Test patient can read their own data
        assertTrue(authorizationService.canAccessPatientData(patientUser, "patient-1", "hospital-a"));
        
        // Test patient cannot access other patient's data
        assertFalse(authorizationService.canAccessPatientData(patientUser, "patient-2", "hospital-a"));
        
        // Test patient can manage their own consent
        assertTrue(authorizationService.canManageConsent(patientUser, "patient-1"));
        
        // Test patient cannot manage other's consent
        assertFalse(authorizationService.canManageConsent(patientUser, "patient-2"));
        
        // Test patient cannot write patient data
        assertFalse(authorizationService.hasPermission(patientUser, Permission.WRITE_PATIENT_DATA));
        
        // Test patient cannot access audit logs
        assertFalse(authorizationService.canAccessAuditLogs(patientUser));
    }

    @Test
    void testSystemAdminWorkflow_HasFullAccess() {
        // Test system admin has all permissions
        assertTrue(authorizationService.hasPermission(systemAdminUser, Permission.MANAGE_SYSTEM));
        assertTrue(authorizationService.hasPermission(systemAdminUser, Permission.READ_PATIENT_DATA));
        assertTrue(authorizationService.hasPermission(systemAdminUser, Permission.WRITE_PATIENT_DATA));
        assertTrue(authorizationService.hasPermission(systemAdminUser, Permission.DELETE_PATIENT_DATA));
        
        // Test system admin can access any organization's data
        assertTrue(authorizationService.canAccessPatientData(systemAdminUser, "any-organization"));
        
        // Test system admin can access audit logs
        assertTrue(authorizationService.canAccessAuditLogs(systemAdminUser));
        
        // Test system admin can perform TEFCA operations
        assertTrue(authorizationService.canPerformTefcaOperations(systemAdminUser));
    }

    @Test
    void testTefcaParticipantWorkflow_CanAccessCrossOrganizationData() {
        // Test TEFCA participant can perform TEFCA operations
        assertTrue(authorizationService.canPerformTefcaOperations(tefcaParticipantUser));
        
        // Test TEFCA participant can access data across organizations
        assertTrue(authorizationService.canAccessPatientData(tefcaParticipantUser, "hospital-a"));
        assertTrue(authorizationService.canAccessPatientData(tefcaParticipantUser, "hospital-b"));
        
        // Test TEFCA participant can transform data
        assertTrue(authorizationService.hasPermission(tefcaParticipantUser, Permission.TRANSFORM_HL7_TO_FHIR));
        
        // Test TEFCA participant cannot perform system administration
        assertFalse(authorizationService.hasPermission(tefcaParticipantUser, Permission.MANAGE_SYSTEM));
    }

    @Test
    void testComplianceOfficerWorkflow_HasAuditAccess() {
        // Test compliance officer can access audit logs
        assertTrue(authorizationService.canAccessAuditLogs(complianceOfficerUser));
        
        // Test compliance officer can read patient data for compliance
        assertTrue(authorizationService.hasPermission(complianceOfficerUser, Permission.READ_PATIENT_DATA));
        
        // Test compliance officer can view system metrics
        assertTrue(authorizationService.hasPermission(complianceOfficerUser, Permission.VIEW_SYSTEM_METRICS));
        
        // Test compliance officer cannot write patient data
        assertFalse(authorizationService.hasPermission(complianceOfficerUser, Permission.WRITE_PATIENT_DATA));
        
        // Test compliance officer cannot perform system administration
        assertFalse(authorizationService.hasPermission(complianceOfficerUser, Permission.MANAGE_SYSTEM));
    }

    @Test
    void testApiClientWorkflow_HasSystemToSystemAccess() {
        // Test API client can perform transformations
        assertTrue(authorizationService.hasPermission(apiClientUser, Permission.TRANSFORM_HL7_TO_FHIR));
        assertTrue(authorizationService.hasPermission(apiClientUser, Permission.VALIDATE_HL7_MESSAGE));
        assertTrue(authorizationService.hasPermission(apiClientUser, Permission.VALIDATE_FHIR_RESOURCE));
        
        // Test API client has system API access
        assertTrue(authorizationService.hasPermission(apiClientUser, Permission.SYSTEM_API_ACCESS));
        
        // Test API client cannot access patient data directly
        assertFalse(authorizationService.hasPermission(apiClientUser, Permission.READ_PATIENT_DATA));
        assertFalse(authorizationService.hasPermission(apiClientUser, Permission.WRITE_PATIENT_DATA));
    }

    @Test
    void testEndpointAccessValidation() {
        // Test transformation endpoint access
        assertTrue(authorizationService.validateEndpointAccess(
                physicianUser, "/api/v1/transform/hl7v2-to-fhir", "POST"));
        
        assertFalse(authorizationService.validateEndpointAccess(
                patientUser, "/api/v1/transform/hl7v2-to-fhir", "POST"));
        
        // Test patient data endpoint access
        assertTrue(authorizationService.validateEndpointAccess(
                physicianUser, "/api/v1/fhir/Patient", "GET"));
        
        assertTrue(authorizationService.validateEndpointAccess(
                patientUser, "/api/v1/fhir/Patient", "GET"));
        
        assertFalse(authorizationService.validateEndpointAccess(
                patientUser, "/api/v1/fhir/Patient", "POST"));
        
        // Test audit endpoint access
        assertTrue(authorizationService.validateEndpointAccess(
                complianceOfficerUser, "/api/v1/audit", "GET"));
        
        assertFalse(authorizationService.validateEndpointAccess(
                physicianUser, "/api/v1/audit", "GET"));
        
        // Test system endpoint access
        assertTrue(authorizationService.validateEndpointAccess(
                systemAdminUser, "/api/v1/system/metrics", "GET"));
        
        assertFalse(authorizationService.validateEndpointAccess(
                nurseUser, "/api/v1/system/metrics", "GET"));
    }

    @Test
    void testMultiRoleUser_HasCombinedPermissions() {
        // Create user with multiple roles
        UserPrincipal multiRoleUser = new UserPrincipal(
                "multi-1", "multi.user", "hospital-a", 
                Arrays.asList("PHYSICIAN", "COMPLIANCE_OFFICER"), true);
        
        // Test user has permissions from both roles
        assertTrue(authorizationService.hasPermission(multiRoleUser, Permission.READ_PATIENT_DATA));
        assertTrue(authorizationService.hasPermission(multiRoleUser, Permission.WRITE_PATIENT_DATA));
        assertTrue(authorizationService.canAccessAuditLogs(multiRoleUser));
        assertTrue(authorizationService.hasPermission(multiRoleUser, Permission.VIEW_SYSTEM_METRICS));
        
        // Test user still cannot perform system administration
        assertFalse(authorizationService.hasPermission(multiRoleUser, Permission.MANAGE_SYSTEM));
    }

    @Test
    void testPermissionAggregation() {
        // Test that user permissions are correctly aggregated from roles
        Set<Permission> physicianPermissions = authorizationService.getUserPermissions(physicianUser);
        
        assertTrue(physicianPermissions.contains(Permission.READ_PATIENT_DATA));
        assertTrue(physicianPermissions.contains(Permission.WRITE_PATIENT_DATA));
        assertTrue(physicianPermissions.contains(Permission.TRANSFORM_HL7_TO_FHIR));
        assertTrue(physicianPermissions.contains(Permission.TEFCA_QUERY));
        
        assertFalse(physicianPermissions.contains(Permission.MANAGE_SYSTEM));
        assertFalse(physicianPermissions.contains(Permission.DELETE_PATIENT_DATA));
    }

    @Test
    void testAuditLogging() {
        // Test that authorization checks are logged
        authorizationService.hasPermission(physicianUser, Permission.READ_PATIENT_DATA);
        
        verify(auditService).logAuthorization(
                eq("physician-1"), eq("read:patient-data"), eq("CHECK"), anyString(), any(Map.class));
    }
}