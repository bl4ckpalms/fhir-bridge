package com.bridge.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging performance metrics of service methods
 */
@Aspect
@Component
public class PerformanceLoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceLoggingAspect.class);
    
    /**
     * Log performance metrics for all service methods
     */
    @Around("execution(* com.bridge.service..*(..))")
    public Object logServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Add operation context to MDC
        String operation = className + "." + joinPoint.getSignature().getName();
        MDC.put("operation", operation);
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Starting operation: {}", methodName);
            
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > 1000) { // Log slow operations (> 1 second)
                logger.warn("Slow operation detected: {} took {}ms", methodName, executionTime);
            } else if (executionTime > 100) { // Log moderately slow operations (> 100ms)
                logger.info("Operation completed: {} took {}ms", methodName, executionTime);
            } else {
                logger.debug("Operation completed: {} took {}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Operation failed: {} after {}ms - {}", methodName, executionTime, e.getMessage());
            throw e;
        } finally {
            MDC.remove("operation");
        }
    }
    
    /**
     * Log performance metrics for transformation operations specifically
     */
    @Around("execution(* com.bridge.service..*Transformer.*(..))")
    public Object logTransformationPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Starting transformation: {}", methodName);
            
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Always log transformation performance as it's critical
            logger.info("Transformation completed: {} took {}ms", methodName, executionTime);
            
            // Log additional metrics for transformations
            if (executionTime > 5000) { // > 5 seconds is very slow for transformation
                logger.error("Very slow transformation detected: {} took {}ms", methodName, executionTime);
            } else if (executionTime > 2000) { // > 2 seconds is slow
                logger.warn("Slow transformation detected: {} took {}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Transformation failed: {} after {}ms - {}", methodName, executionTime, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Log performance metrics for database operations
     */
    @Around("execution(* com.bridge.repository..*(..))")
    public Object logRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Starting database operation: {}", methodName);
            
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > 500) { // Log slow database operations (> 500ms)
                logger.warn("Slow database operation: {} took {}ms", methodName, executionTime);
            } else {
                logger.debug("Database operation completed: {} took {}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Database operation failed: {} after {}ms - {}", methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}