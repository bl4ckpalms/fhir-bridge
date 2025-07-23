package com.bridge.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application logging
 * Sets up structured logging with correlation IDs and performance monitoring
 */
@Configuration
public class LoggingConfig {
    
    @Value("${logging.level.com.bridge:INFO}")
    private String bridgeLogLevel;
    
    @Value("${logging.level.org.springframework.security:WARN}")
    private String securityLogLevel;
    
    @Value("${logging.pattern.console:%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{requestId:-}] %logger{36} - %msg%n}")
    private String consolePattern;
    
    @Value("${logging.pattern.file:%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{requestId:-}] [%X{userId:-}] [%X{operation:-}] %logger{36} - %msg%n}")
    private String filePattern;
    
    /**
     * Configure logging context for the application
     */
    @Bean
    public LoggerContext loggerContext() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Print logback configuration status for debugging
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        
        return context;
    }
    
    /**
     * Configure root logger settings
     */
    @Bean
    public Logger rootLogger(LoggerContext context) {
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        
        // Set appropriate log levels for different packages
        context.getLogger("com.bridge").setLevel(ch.qos.logback.classic.Level.valueOf(bridgeLogLevel));
        context.getLogger("org.springframework.security").setLevel(ch.qos.logback.classic.Level.valueOf(securityLogLevel));
        
        // Reduce noise from common libraries
        context.getLogger("org.springframework.web.servlet.DispatcherServlet").setLevel(ch.qos.logback.classic.Level.WARN);
        context.getLogger("org.hibernate.SQL").setLevel(ch.qos.logback.classic.Level.WARN);
        context.getLogger("org.hibernate.type.descriptor.sql.BasicBinder").setLevel(ch.qos.logback.classic.Level.WARN);
        
        return rootLogger;
    }
}