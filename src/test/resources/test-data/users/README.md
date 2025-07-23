# Test User Accounts Documentation

This directory contains comprehensive test user accounts with different roles and permissions for testing the FHIR Bridge application's authorization and security features.

## Files Overview

### `test-users-with-roles.json`
Contains detailed test user accounts with all healthcare roles defined in the system. Each user includes:
- Basic user information (ID, username, email, name)
- Assigned roles from the `HealthcareRole` enum
- Granted permissions from the `Permission` enum
- Organization affiliation
- Role-specific metadata (license numbers, certifications, etc.)
- Account status and activity information

### `test-jwt-tokens.json`
Contains sample JWT tokens for each test user, including:
- Token metadata (type, expiration, scope)
- Sample JWT token strings for testing
- Role and organization information embedded in tokens

### `synthetic-users.json` (Legacy)
Legacy test user data with older role structure. Maintained for backward compatibility but should be migrated to use the new role-based system.

## Test User Categories

### Administrative Users

#### System Administrator (`TEST-SYSADMIN-001`)
- **Username**: `system.admin`
- **Role**: `SYSTEM_ADMIN`
- **Permissions**: All permissions (full system access)
- **Use Case**: Testing system-wide administrative functions

#### TEFCA Administrator (`TEST-TEFCA-ADMIN-001`)
- **Username**: `tefca.admin`
- **Role**: `TEFCA_ADMIN`
- **Permissions**: TEFCA network administration, user management, audit access
- **Use Case**: Testing TEFCA network administration features

### Healthcare Providers

#### Physician (`TEST-PHYSICIAN-001`)
- **Username**: `dr.johnson`
- **Role**: `PHYSICIAN`
- **License**: `MD-12345`
- **Specialty**: Internal Medicine
- **Permissions**: Full patient data access, consent management, clinical operations
- **Use Case**: Testing clinical workflows and patient data access

#### Nurse (`TEST-NURSE-001`)
- **Username**: `nurse.williams`
- **Role**: `NURSE`
- **License**: `RN-67890`
- **Department**: Emergency Department
- **Permissions**: Patient data read/write, consent management, limited administrative access
- **Use Case**: Testing nursing workflows and patient care operations

#### Pharmacist (`TEST-PHARMACIST-001`)
- **Username**: `pharm.davis`
- **Role**: `PHARMACIST`
- **License**: `PharmD-11111`
- **Permissions**: Patient data read access, medication-focused operations
- **Use Case**: Testing pharmacy workflows and medication management

#### Healthcare Technician (`TEST-TECHNICIAN-001`)
- **Username**: `tech.brown`
- **Role**: `TECHNICIAN`
- **Certification**: `MLT-Certified`
- **Department**: Laboratory
- **Permissions**: Limited patient data read access, basic API access
- **Use Case**: Testing technician workflows with restricted access

### Information Management

#### Health Information Manager (`TEST-HIM-001`)
- **Username**: `him.manager`
- **Role**: `HEALTH_INFO_MANAGER`
- **Certification**: `RHIA`
- **Permissions**: Data transformation, consent management, audit access, bulk data operations
- **Use Case**: Testing health information management workflows

#### Compliance Officer (`TEST-COMPLIANCE-001`)
- **Username**: `compliance.officer`
- **Role**: `COMPLIANCE_OFFICER`
- **Certification**: `CHC`
- **Permissions**: Audit log access, compliance reporting, system monitoring
- **Use Case**: Testing compliance and audit functionality

#### Data Analyst (`TEST-ANALYST-001`)
- **Username**: `data.analyst`
- **Role**: `DATA_ANALYST`
- **Clearance**: De-identified data only
- **Permissions**: Limited patient data read, bulk data access, analytics operations
- **Use Case**: Testing analytics workflows with de-identified data

### TEFCA Network Users

#### TEFCA Participant (`TEST-TEFCA-PARTICIPANT-001`)
- **Username**: `tefca.participant`
- **Role**: `TEFCA_PARTICIPANT`
- **TEFCA ID**: `TEFCA-PART-001`
- **Permissions**: TEFCA query/response, data transformation, patient data access
- **Use Case**: Testing TEFCA network participant operations

### Patient Access

#### Patient (`TEST-PATIENT-001`)
- **Username**: `patient.smith`
- **Role**: `PATIENT`
- **Patient ID**: `PAT-TEST-001`
- **DOB**: 1985-03-15
- **Permissions**: Self-consent management, limited self-data access
- **Use Case**: Testing patient portal and self-service features

#### Patient Proxy (`TEST-PATIENT-PROXY-001`)
- **Username**: `proxy.jones`
- **Role**: `PATIENT_PROXY`
- **Proxy For**: `PAT-TEST-002`
- **Relationship**: Legal Guardian
- **Authorization**: `POA-2024-001`
- **Permissions**: Proxy consent management for authorized patient
- **Use Case**: Testing patient proxy and guardian access

### System Integration

#### API Client (`TEST-API-CLIENT-001`)
- **Username**: `api.client.system`
- **Role**: `API_CLIENT`
- **Client ID**: `CLIENT-SYS-001`
- **System Type**: EHR Integration
- **Permissions**: System API access, data transformation, bulk operations
- **Use Case**: Testing system-to-system integration

### Special Test Cases

#### Multi-Role User (`TEST-MULTI-ROLE-001`)
- **Username**: `multi.role.user`
- **Roles**: `PHYSICIAN`, `TEFCA_PARTICIPANT`
- **Permissions**: Combined permissions from both roles
- **Use Case**: Testing users with multiple roles and combined permissions

#### Inactive User (`TEST-INACTIVE-001`)
- **Username**: `inactive.user`
- **Status**: Inactive (account disabled)
- **Deactivation Reason**: Employment terminated
- **Use Case**: Testing access control for disabled accounts

#### Limited Access User (`TEST-LIMITED-001`)
- **Username**: `limited.access`
- **Role**: `TECHNICIAN`
- **Permissions**: Minimal (API access only)
- **Restrictions**: Read-only access, limited to own organization
- **Use Case**: Testing authorization boundaries and minimal permissions

## Usage in Tests

### Using TestUserDataLoader Utility

```java
// Load a specific test user
TestUserDataLoader.TestUser physician = TestUserDataLoader.getUserById("TEST-PHYSICIAN-001").orElseThrow();

// Set security context for testing
TestUserDataLoader.setSecurityContext("TEST-PHYSICIAN-001");

// Create UserPrincipal for service testing
UserPrincipal userPrincipal = TestUserDataLoader.createUserPrincipal("TEST-PHYSICIAN-001");

// Get users by role
List<TestUserDataLoader.TestUser> physicians = TestUserDataLoader.getUsersByRole(HealthcareRole.PHYSICIAN);

// Get users by permission
List<TestUserDataLoader.TestUser> usersWithPatientAccess = TestUserDataLoader.getUsersByPermission(Permission.READ_PATIENT_DATA);

// Get JWT token for API testing
TestUserDataLoader.TestJwtToken token = TestUserDataLoader.getTokenByUserId("TEST-PHYSICIAN-001").orElseThrow();
String authHeader = token.getTokenType() + " " + token.getSampleToken();
```

### Integration Test Examples

```java
@Test
void testPhysicianCanAccessPatientData() {
    // Given
    TestUserDataLoader.setSecurityContext("TEST-PHYSICIAN-001");
    
    // When
    ResponseEntity<PatientData> response = restTemplate.exchange(
        "/api/v1/patients/PAT-001",
        HttpMethod.GET,
        createAuthenticatedRequest("TEST-PHYSICIAN-001"),
        PatientData.class
    );
    
    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
}

@Test
void testTechnicianCannotAccessAuditLogs() {
    // Given
    TestUserDataLoader.setSecurityContext("TEST-TECHNICIAN-001");
    
    // When & Then
    assertThrows(AccessDeniedException.class, () -> {
        auditService.getAuditLogs("2025-01-01", "2025-01-31");
    });
}
```

## Organization Structure

Test users are distributed across multiple test organizations:

- **TEST-ORG-001**: Primary healthcare organization (physicians, nurses, patients)
- **TEST-ORG-002**: Secondary healthcare organization (pharmacist, technician)
- **TEST-ORG-003**: Analytics organization (data analyst, limited user)
- **TEST-ORG-004**: TEFCA participant organization
- **TEST-ORG-005**: TEFCA administrative organization
- **TEST-ORG-006**: System integration organization (API clients)

## Security Considerations

### Token Expiration
- **Standard users**: 3600 seconds (1 hour)
- **Patient/Proxy users**: 1800 seconds (30 minutes) - shorter for security
- **System clients**: 7200 seconds (2 hours) - longer for system operations

### Permission Mapping
Each role has specific permissions mapped according to healthcare industry standards and TEFCA requirements. The permission assignments follow the principle of least privilege.

### Testing Scenarios

1. **Positive Authorization Tests**: Verify users can access resources they should have access to
2. **Negative Authorization Tests**: Verify users cannot access resources they shouldn't have access to
3. **Role Boundary Tests**: Test the boundaries between different role permissions
4. **Multi-Role Tests**: Test users with multiple roles have combined permissions
5. **Inactive User Tests**: Verify inactive users are properly denied access
6. **Organization Boundary Tests**: Test cross-organization access controls

## Maintenance

When adding new roles or permissions:

1. Update the test user data files with appropriate role assignments
2. Add corresponding JWT tokens with proper claims
3. Update the TestUserDataLoader utility if needed
4. Add test cases for the new roles/permissions
5. Update this documentation

## Best Practices

1. **Use Specific Test Users**: Choose test users that match your specific test scenario
2. **Clean Up Security Context**: Always clear the security context after tests
3. **Test Both Positive and Negative Cases**: Test both allowed and denied access scenarios
4. **Use Realistic Data**: Test users include realistic healthcare role metadata
5. **Test Cross-Organization Scenarios**: Use users from different organizations to test boundaries