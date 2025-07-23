package com.bridge.aspect;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceLoggingAspectTest {
    
    @InjectMocks
    private PerformanceLoggingAspect performanceLoggingAspect;
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private Signature signature;
    
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    
    @BeforeEach
    void setUp() {
        // Set up test logger
        logger = (Logger) LoggerFactory.getLogger(PerformanceLoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        // Clear MDC
        MDC.clear();
        
        // Mock signature
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestService.testMethod()");
        when(signature.getName()).thenReturn("testMethod");
    }
    
    @Test
    void logServicePerformance_ShouldLogFastOperation() throws Throwable {
        // Given
        Object mockTarget = new TestService();
        when(joinPoint.getTarget()).thenReturn(mockTarget);
        when(joinPoint.proceed()).thenReturn("result");
        
        // When
        Object result = performanceLoggingAspect.logServicePerformance(joinPoint);
        
        // Then
        assertEquals("result", result);
        
        List<ILoggingEvent> logEvents = listAppender.list;
        assertTrue(logEvents.size() >= 2); // Start and completion logs
        
        // Check start log
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Starting operation")));
        
        // Check completion log (should be DEBUG for fast operations)
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Operation completed") && 
            event.getLevel() == ch.qos.logback.classic.Level.DEBUG));
        
        verify(joinPoint).proceed();
    }
    
    @Test
    void logServicePerformance_ShouldLogSlowOperation() throws Throwable {
        // Given
        Object mockTarget = new TestService();
        when(joinPoint.getTarget()).thenReturn(mockTarget);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(150); // Simulate slow operation
            return "result";
        });
        
        // When
        Object result = performanceLoggingAspect.logServicePerformance(joinPoint);
        
        // Then
        assertEquals("result", result);
        
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Should log as INFO for moderately slow operations (> 100ms)
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Operation completed") && 
            event.getLevel() == ch.qos.logback.classic.Level.INFO));
        
        verify(joinPoint).proceed();
    }
    
    @Test
    void logServicePerformance_ShouldLogVerySlowOperation() throws Throwable {
        // Given
        Object mockTarget = new TestService();
        when(joinPoint.getTarget()).thenReturn(mockTarget);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(1100); // Simulate very slow operation
            return "result";
        });
        
        // When
        Object result = performanceLoggingAspect.logServicePerformance(joinPoint);
        
        // Then
        assertEquals("result", result);
        
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Should log as WARN for very slow operations (> 1000ms)
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Slow operation detected") && 
            event.getLevel() == ch.qos.logback.classic.Level.WARN));
        
        verify(joinPoint).proceed();
    }
    
    @Test
    void logServicePerformance_ShouldLogFailedOperation() throws Throwable {
        // Given
        Object mockTarget = new TestService();
        when(joinPoint.getTarget()).thenReturn(mockTarget);
        RuntimeException testException = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(testException);
        
        // When/Then
        assertThrows(RuntimeException.class, () -> 
            performanceLoggingAspect.logServicePerformance(joinPoint));
        
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Should log error
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Operation failed") && 
            event.getLevel() == ch.qos.logback.classic.Level.ERROR));
        
        verify(joinPoint).proceed();
    }
    
    @Test
    void logServicePerformance_ShouldSetAndCleanUpMDC() throws Throwable {
        // Given
        Object mockTarget = new TestService();
        when(joinPoint.getTarget()).thenReturn(mockTarget);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            // Verify MDC is set during execution
            assertEquals("TestService.testMethod", MDC.get("operation"));
            return "result";
        });
        
        // When
        performanceLoggingAspect.logServicePerformance(joinPoint);
        
        // Then - MDC should be cleaned up
        assertNull(MDC.get("operation"));
        
        verify(joinPoint).proceed();
    }
    
    @Test
    void logTransformationPerformance_ShouldAlwaysLogTransformations() throws Throwable {
        // Given
        when(joinPoint.proceed()).thenReturn("transformed-result");
        
        // When
        Object result = performanceLoggingAspect.logTransformationPerformance(joinPoint);
        
        // Then
        assertEquals("transformed-result", result);
        
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Should always log transformation start and completion as INFO
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Starting transformation") && 
            event.getLevel() == ch.qos.logback.classic.Level.INFO));
        
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Transformation completed") && 
            event.getLevel() == ch.qos.logback.classic.Level.INFO));
        
        verify(joinPoint).proceed();
    }
    
    @Test
    void logTransformationPerformance_ShouldLogSlowTransformation() throws Throwable {
        // Given
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(2100); // Simulate slow transformation
            return "result";
        });
        
        // When
        performanceLoggingAspect.logTransformationPerformance(joinPoint);
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Should log slow transformation warning
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Slow transformation detected") && 
            event.getLevel() == ch.qos.logback.classic.Level.WARN));
        
        verify(joinPoint).proceed();
    }
    
    @Test
    void logTransformationPerformance_ShouldLogVerySlowTransformation() throws Throwable {
        // Given
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(5100); // Simulate very slow transformation
            return "result";
        });
        
        // When
        performanceLoggingAspect.logTransformationPerformance(joinPoint);
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Should log very slow transformation error
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Very slow transformation detected") && 
            event.getLevel() == ch.qos.logback.classic.Level.ERROR));
        
        verify(joinPoint).proceed();
    }
    
    @Test
    void logRepositoryPerformance_ShouldLogDatabaseOperations() throws Throwable {
        // Given
        when(joinPoint.proceed()).thenReturn("db-result");
        
        // When
        Object result = performanceLoggingAspect.logRepositoryPerformance(joinPoint);
        
        // Then
        assertEquals("db-result", result);
        
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Should log database operation start and completion
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Starting database operation")));
        
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Database operation completed")));
        
        verify(joinPoint).proceed();
    }
    
    @Test
    void logRepositoryPerformance_ShouldLogSlowDatabaseOperation() throws Throwable {
        // Given
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(600); // Simulate slow database operation
            return "result";
        });
        
        // When
        performanceLoggingAspect.logRepositoryPerformance(joinPoint);
        
        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Should log slow database operation warning
        assertTrue(logEvents.stream().anyMatch(event -> 
            event.getMessage().contains("Slow database operation") && 
            event.getLevel() == ch.qos.logback.classic.Level.WARN));
        
        verify(joinPoint).proceed();
    }
    
    // Test service class
    private static class TestService {
        // Empty test service
    }
}