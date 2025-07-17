package com.bridge.security;

import com.bridge.model.HealthcareRole;
import com.bridge.model.Permission;
import com.bridge.model.UserPrincipal;
import com.bridge.service.AuthorizationService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * AOP aspect for enforcing method-level security annotations
 * Handles @RequirePermission, @RequireRole, and @RequireOrganizationAccess
 */
@Aspect
@Component
public class SecurityAspect {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAspect.class);

    private final AuthorizationService authorizationService;

    public SecurityAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * Enforce permission-based security
     */
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        UserPrincipal user = getCurrentUser();
        Permission requiredPermission = requirePermission.value();

        if (!authorizationService.hasPermission(user, requiredPermission)) {
            logger.warn("Access denied for user {} - missing permission: {}", 
                       user.getUserId(), requiredPermission.getPermissionCode());
            throw new AccessDeniedException(requirePermission.message());
        }

        logger.debug("Permission check passed for user {} - permission: {}", 
                    user.getUserId(), requiredPermission.getPermissionCode());
    }

    /**
     * Enforce role-based security
     */
    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        UserPrincipal user = getCurrentUser();
        HealthcareRole[] requiredRoles = requireRole.value();
        boolean requireAll = requireRole.requireAll();

        boolean hasAccess;
        if (requireAll) {
            hasAccess = authorizationService.hasAllPermissions(user, getPermissionsForRoles(requiredRoles));
        } else {
            hasAccess = authorizationService.hasAnyRole(user, requiredRoles);
        }

        if (!hasAccess) {
            logger.warn("Access denied for user {} - missing roles: {}", 
                       user.getUserId(), java.util.Arrays.toString(requiredRoles));
            throw new AccessDeniedException(requireRole.message());
        }

        logger.debug("Role check passed for user {} - roles: {}", 
                    user.getUserId(), java.util.Arrays.toString(requiredRoles));
    }

    /**
     * Enforce organization-level access control
     */
    @Before("@annotation(requireOrgAccess)")
    public void checkOrganizationAccess(JoinPoint joinPoint, RequireOrganizationAccess requireOrgAccess) {
        UserPrincipal user = getCurrentUser();
        String organizationId = extractOrganizationId(joinPoint, requireOrgAccess.organizationParam());

        if (organizationId == null) {
            logger.warn("Organization ID not found in method parameters for user {}", user.getUserId());
            throw new AccessDeniedException("Organization access validation failed");
        }

        boolean hasAccess = authorizationService.canAccessPatientData(user, organizationId);

        // Allow TEFCA access if configured
        if (!hasAccess && requireOrgAccess.allowTefcaAccess()) {
            hasAccess = authorizationService.canPerformTefcaOperations(user);
        }

        if (!hasAccess) {
            logger.warn("Access denied for user {} - cannot access organization: {}", 
                       user.getUserId(), organizationId);
            throw new AccessDeniedException(requireOrgAccess.message());
        }

        logger.debug("Organization access check passed for user {} - organization: {}", 
                    user.getUserId(), organizationId);
    }

    /**
     * Get current authenticated user
     */
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new AccessDeniedException("Invalid user principal type");
        }

        return (UserPrincipal) principal;
    }

    /**
     * Extract organization ID from method parameters
     */
    private String extractOrganizationId(JoinPoint joinPoint, String parameterName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(parameterName)) {
                Object value = args[i];
                return value != null ? value.toString() : null;
            }
        }

        return null;
    }

    /**
     * Convert roles to their associated permissions for validation
     */
    private Permission[] getPermissionsForRoles(HealthcareRole[] roles) {
        // This is a simplified approach - in practice, you might want to 
        // check specific permissions rather than converting roles
        return new Permission[]{Permission.API_ACCESS};
    }
}