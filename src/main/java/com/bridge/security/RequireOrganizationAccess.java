package com.bridge.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level security annotation to require organization-level access
 * Ensures user can only access data within their organization or has cross-org privileges
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireOrganizationAccess {
    
    /**
     * Parameter name that contains the organization ID to check
     * Default is "organizationId"
     */
    String organizationParam() default "organizationId";
    
    /**
     * Whether to allow cross-organization access for TEFCA participants
     * Default is true
     */
    boolean allowTefcaAccess() default true;
    
    /**
     * Optional message for access denied scenarios
     */
    String message() default "Access denied: insufficient organization privileges";
}