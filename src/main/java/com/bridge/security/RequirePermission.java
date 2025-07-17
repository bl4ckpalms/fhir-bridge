package com.bridge.security;

import com.bridge.model.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level security annotation to require specific permissions
 * Used for fine-grained authorization control on service methods
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * Required permission for accessing the method
     */
    Permission value();
    
    /**
     * Optional message for access denied scenarios
     */
    String message() default "Access denied: insufficient permissions";
}