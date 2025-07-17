package com.bridge.security;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import com.bridge.model.UserPrincipal;
import com.bridge.service.AuthorizationService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for SecurityAspect
 * Tests method-level security enforcement through AOP
 */
@ExtendWith(MockitoExtension.class)
class SecurityAspectTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Method method;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private SecurityAspect securityAspect;
    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        securityAspect = new SecurityAspect(authorizationService);
        
        testUser = new UserPrincipal(
                "test-user", "testuser", "org-1", 
                Arrays.asList("PHYSICIAN"), true);

        // Setup security context with lenient stubbing to avoid unnecessary stubbing errors
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Test
    void testCheckPermission_WithValidPermission_PassesThrough() {
        // Arrange
        RequirePermission requirePermission = createRequirePermissionAnnotation(Permission.READ_PATIENT_DATA);
        when(authorizationService.hasPermission(testUser, Permission.READ_PATIENT_DATA))
                .thenReturn(true);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> securityAspect.checkPermission(joinPoint, requirePermission));
    }

    @Test
    void testCheckPermission_WithInvalidPermission_ThrowsAccessDeniedException() {
        // Arrange
        RequirePermission requirePermission = createRequirePermissionAnnotation(Permission.MANAGE_SYSTEM);
        when(authorizationService.hasPermission(testUser, Permission.MANAGE_SYSTEM))
                .thenReturn(false);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, 
                () -> securityAspect.checkPermission(joinPoint, requirePermission));
        
        assertEquals("Access denied: insufficient permissions", exception.getMessage());
    }

    @Test
    void testCheckRole_WithValidRole_PassesThrough() {
        // Arrange
        RequireRole requireRole = createRequireRoleAnnotation(false, HealthcareRole.PHYSICIAN);
        when(authorizationService.hasAnyRole(testUser, HealthcareRole.PHYSICIAN))
                .thenReturn(true);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> securityAspect.checkRole(joinPoint, requireRole));
    }

    @Test
    void testCheckRole_WithInvalidRole_ThrowsAccessDeniedException() {
        // Arrange
        RequireRole requireRole = createRequireRoleAnnotation(false, HealthcareRole.SYSTEM_ADMIN);
        when(authorizationService.hasAnyRole(testUser, HealthcareRole.SYSTEM_ADMIN))
                .thenReturn(false);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, 
                () -> securityAspect.checkRole(joinPoint, requireRole));
        
        assertEquals("Access denied: insufficient role privileges", exception.getMessage());
    }

    @Test
    void testCheckRole_WithRequireAllTrue_ChecksAllPermissions() {
        // Arrange
        RequireRole requireRole = createRequireRoleAnnotation(true, HealthcareRole.PHYSICIAN, HealthcareRole.NURSE);
        when(authorizationService.hasAllPermissions(eq(testUser), any()))
                .thenReturn(true);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> securityAspect.checkRole(joinPoint, requireRole));
        
        verify(authorizationService).hasAllPermissions(eq(testUser), any());
    }

    @Test
    void testCheckOrganizationAccess_WithValidOrganization_PassesThrough() {
        // Arrange
        RequireOrganizationAccess requireOrgAccess = createRequireOrganizationAccessAnnotation("organizationId");
        setupMethodSignatureForOrgAccess("org-1");
        when(authorizationService.canAccessPatientData(testUser, "org-1"))
                .thenReturn(true);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> securityAspect.checkOrganizationAccess(joinPoint, requireOrgAccess));
    }

    @Test
    void testCheckOrganizationAccess_WithInvalidOrganization_ThrowsAccessDeniedException() {
        // Arrange
        RequireOrganizationAccess requireOrgAccess = createRequireOrganizationAccessAnnotation("organizationId");
        setupMethodSignatureForOrgAccess("org-2");
        when(authorizationService.canAccessPatientData(testUser, "org-2"))
                .thenReturn(false);
        when(authorizationService.canPerformTefcaOperations(testUser))
                .thenReturn(false);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, 
                () -> securityAspect.checkOrganizationAccess(joinPoint, requireOrgAccess));
        
        assertEquals("Access denied: insufficient organization privileges", exception.getMessage());
    }

    @Test
    void testCheckOrganizationAccess_WithTefcaAccess_PassesThrough() {
        // Arrange
        RequireOrganizationAccess requireOrgAccess = createRequireOrganizationAccessAnnotation("organizationId");
        setupMethodSignatureForOrgAccess("org-2");
        when(authorizationService.canAccessPatientData(testUser, "org-2"))
                .thenReturn(false);
        when(authorizationService.canPerformTefcaOperations(testUser))
                .thenReturn(true);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> securityAspect.checkOrganizationAccess(joinPoint, requireOrgAccess));
    }

    @Test
    void testCheckPermission_WithUnauthenticatedUser_ThrowsAccessDeniedException() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);
        RequirePermission requirePermission = createRequirePermissionAnnotation(Permission.READ_PATIENT_DATA);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, 
                () -> securityAspect.checkPermission(joinPoint, requirePermission));
        
        assertEquals("User not authenticated", exception.getMessage());
    }

    @Test
    void testCheckPermission_WithNullAuthentication_ThrowsAccessDeniedException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);
        RequirePermission requirePermission = createRequirePermissionAnnotation(Permission.READ_PATIENT_DATA);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, 
                () -> securityAspect.checkPermission(joinPoint, requirePermission));
        
        assertEquals("User not authenticated", exception.getMessage());
    }

    @Test
    void testCheckPermission_WithInvalidPrincipalType_ThrowsAccessDeniedException() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn("invalid-principal");
        RequirePermission requirePermission = createRequirePermissionAnnotation(Permission.READ_PATIENT_DATA);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, 
                () -> securityAspect.checkPermission(joinPoint, requirePermission));
        
        assertEquals("Invalid user principal type", exception.getMessage());
    }

    @Test
    void testCheckOrganizationAccess_WithMissingOrganizationParam_ThrowsAccessDeniedException() {
        // Arrange
        RequireOrganizationAccess requireOrgAccess = createRequireOrganizationAccessAnnotation("missingParam");
        setupMethodSignatureForOrgAccess(null); // No matching parameter

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, 
                () -> securityAspect.checkOrganizationAccess(joinPoint, requireOrgAccess));
        
        assertEquals("Organization access validation failed", exception.getMessage());
    }

    // Helper methods to create mock annotations
    private RequirePermission createRequirePermissionAnnotation(Permission permission) {
        return new RequirePermission() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequirePermission.class;
            }

            @Override
            public Permission value() {
                return permission;
            }

            @Override
            public String message() {
                return "Access denied: insufficient permissions";
            }
        };
    }

    private RequireRole createRequireRoleAnnotation(boolean requireAll, HealthcareRole... roles) {
        return new RequireRole() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequireRole.class;
            }

            @Override
            public HealthcareRole[] value() {
                return roles;
            }

            @Override
            public boolean requireAll() {
                return requireAll;
            }

            @Override
            public String message() {
                return "Access denied: insufficient role privileges";
            }
        };
    }

    private RequireOrganizationAccess createRequireOrganizationAccessAnnotation(String organizationParam) {
        return new RequireOrganizationAccess() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequireOrganizationAccess.class;
            }

            @Override
            public String organizationParam() {
                return organizationParam;
            }

            @Override
            public boolean allowTefcaAccess() {
                return true;
            }

            @Override
            public String message() {
                return "Access denied: insufficient organization privileges";
            }
        };
    }

    private void setupMethodSignatureForOrgAccess(String organizationValue) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        
        Parameter mockParameter = mock(Parameter.class);
        when(mockParameter.getName()).thenReturn("organizationId");
        when(method.getParameters()).thenReturn(new Parameter[]{mockParameter});
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{organizationValue});
    }
}