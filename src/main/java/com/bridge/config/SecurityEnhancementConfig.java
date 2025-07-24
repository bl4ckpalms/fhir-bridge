package com.bridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Enhanced security configuration for CORS and security headers
 */
@Configuration
public class SecurityEnhancementConfig {

    @Value("${fhir-bridge.security.cors.enabled:true}")
    private boolean corsEnabled;

    @Value("${fhir-bridge.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;

    @Value("${fhir-bridge.security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String[] allowedMethods;

    @Value("${fhir-bridge.security.cors.allowed-headers:*}")
    private String[] allowedHeaders;

    @Value("${fhir-bridge.security.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        if (corsEnabled) {
            configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
            configuration.setAllowedMethods(Arrays.asList(allowedMethods));
            configuration.setAllowedHeaders(Arrays.asList(allowedHeaders));
            configuration.setAllowCredentials(allowCredentials);
            configuration.setMaxAge(3600L);
            
            // Add exposed headers for API responses
            configuration.setExposedHeaders(List.of(
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Reset",
                "X-Request-ID",
                "X-Correlation-ID"
            ));
        }
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}