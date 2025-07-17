package com.bridge.security;

import com.bridge.model.HealthcareRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level security annotation to require specific healthcare roles
 * Used for role-based authorization control on service methods
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    
    /**
     * Required roles for accessing the method
     * User must have at least one of the specified roles
     */
    HealthcareRole[] value();
    
    /**
     * Whether user must have ALL specified roles (true) or ANY role (false)
     * Default is false (ANY role)
     */
    boolean requireAll() default false;
    
    /**
     * Optional message for access denied scenarios
     */
    String message() default "Access denied: insufficient role privileges";
}