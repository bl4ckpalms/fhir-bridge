package com.bridge.util;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import com.bridge.model.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class for loading test user data and JWT tokens for integration testing
 */
public class TestUserDataLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static List<TestUser> testUsers;
    private static List<TestJwtToken> testTokens;

    static {
        loadTestData();
    }

    /**
     * Load test user data from JSON files
     */
    private static void loadTestData() {
        try {
            // Load test users
            ClassPathResource usersResource = new ClassPathResource("test-data/users/test-users-with-roles.json");
            testUsers = objectMapper.readValue(usersResource.getInputStream(), new TypeReference<List<TestUser>>() {});

            // Load test JWT tokens
            ClassPathResource tokensResource = new ClassPathResource("test-data/users/test-jwt-tokens.json");
            testTokens = objectMapper.readValue(tokensResource.getInputStream(), new TypeReference<List<TestJwtToken>>() {});

        } catch (IOException e) {
            throw new RuntimeException("Failed to load test user data", e);
        }
    }

    /**
     * Get test user by user ID
     */
    public static Optional<TestUser> getUserById(String userId) {
        return testUsers.stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst();
    }

    /**
     * Get test user by username
     */
    public static Optional<TestUser> getUserByUsername(String username) {
        return testUsers.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    /**
     * Get all test users with a specific role
     */
    public static List<TestUser> getUsersByRole(HealthcareRole role) {
        return testUsers.stream()
                .filter(user -> user.getRoles().contains(role.getRoleName()))
                .collect(Collectors.toList());
    }

    /**
     * Get all test users with a specific permission
     */
    public static List<TestUser> getUsersByPermission(Permission permission) {
        return testUsers.stream()
                .filter(user -> user.getPermissions().contains(permission.getPermissionCode()))
                .collect(Collectors.toList());
    }

    /**
     * Get all active test users
     */
    public static List<TestUser> getActiveUsers() {
        return testUsers.stream()
                .filter(TestUser::isActive)
                .collect(Collectors.toList());
    }

    /**
     * Get all inactive test users
     */
    public static List<TestUser> getInactiveUsers() {
        return testUsers.stream()
                .filter(user -> !user.isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get test JWT token by user ID
     */
    public static Optional<TestJwtToken> getTokenByUserId(String userId) {
        return testTokens.stream()
                .filter(token -> token.getUserId().equals(userId))
                .findFirst();
    }

    /**
     * Get test JWT token by username
     */
    public static Optional<TestJwtToken> getTokenByUsername(String username) {
        return testTokens.stream()
                .filter(token -> token.getUsername().equals(username))
                .findFirst();
    }

    /**
     * Create UserPrincipal from test user data
     */
    public static UserPrincipal createUserPrincipal(String userId) {
        TestUser testUser = getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Test user not found: " + userId));

        return new UserPrincipal(
                testUser.getUserId(),
                testUser.getUsername(),
                testUser.getOrganizationId(),
                testUser.getRoles(),
                testUser.isActive()
        );
    }

    /**
     * Set security context with test user
     */
    public static void setSecurityContext(String userId) {
        UserPrincipal userPrincipal = createUserPrincipal(userId);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Clear security context
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Get all test users
     */
    public static List<TestUser> getAllUsers() {
        return testUsers;
    }

    /**
     * Get all test tokens
     */
    public static List<TestJwtToken> getAllTokens() {
        return testTokens;
    }

    /**
     * Test user data structure
     */
    public static class TestUser {
        private String userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private List<String> roles;
        private String organizationId;
        private List<String> permissions;
        private boolean active;
        private String lastLogin;
        private String createdDate;
        private String description;
        
        // Optional fields for specific roles
        private String licenseNumber;
        private String specialty;
        private String department;
        private String certification;
        private String tefcaId;
        private String networkRole;
        private String patientId;
        private String dateOfBirth;
        private String proxyForPatientId;
        private String relationship;
        private String authorizationDocument;
        private String clientId;
        private String systemType;
        private String deactivationReason;
        private String deactivationDate;
        private List<String> accessRestrictions;
        private String clearanceLevel;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }

        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public String getLastLogin() { return lastLogin; }
        public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

        public String getCreatedDate() { return createdDate; }
        public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

        public String getSpecialty() { return specialty; }
        public void setSpecialty(String specialty) { this.specialty = specialty; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getCertification() { return certification; }
        public void setCertification(String certification) { this.certification = certification; }

        public String getTefcaId() { return tefcaId; }
        public void setTefcaId(String tefcaId) { this.tefcaId = tefcaId; }

        public String getNetworkRole() { return networkRole; }
        public void setNetworkRole(String networkRole) { this.networkRole = networkRole; }

        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }

        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public String getProxyForPatientId() { return proxyForPatientId; }
        public void setProxyForPatientId(String proxyForPatientId) { this.proxyForPatientId = proxyForPatientId; }

        public String getRelationship() { return relationship; }
        public void setRelationship(String relationship) { this.relationship = relationship; }

        public String getAuthorizationDocument() { return authorizationDocument; }
        public void setAuthorizationDocument(String authorizationDocument) { this.authorizationDocument = authorizationDocument; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getSystemType() { return systemType; }
        public void setSystemType(String systemType) { this.systemType = systemType; }

        public String getDeactivationReason() { return deactivationReason; }
        public void setDeactivationReason(String deactivationReason) { this.deactivationReason = deactivationReason; }

        public String getDeactivationDate() { return deactivationDate; }
        public void setDeactivationDate(String deactivationDate) { this.deactivationDate = deactivationDate; }

        public List<String> getAccessRestrictions() { return accessRestrictions; }
        public void setAccessRestrictions(List<String> accessRestrictions) { this.accessRestrictions = accessRestrictions; }

        public String getClearanceLevel() { return clearanceLevel; }
        public void setClearanceLevel(String clearanceLevel) { this.clearanceLevel = clearanceLevel; }
    }

    /**
     * Test JWT token data structure
     */
    public static class TestJwtToken {
        private String userId;
        private String username;
        private List<String> roles;
        private String organizationId;
        private String tokenType;
        private int expiresIn;
        private String scope;
        private String sampleToken;
        private String description;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }

        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public int getExpiresIn() { return expiresIn; }
        public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        public String getSampleToken() { return sampleToken; }
        public void setSampleToken(String sampleToken) { this.sampleToken = sampleToken; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}