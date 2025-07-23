package com.bridge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for FHIR Bridge API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.application.name:fhir-bridge}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FHIR Bridge API")
                        .version("1.0.0")
                        .description("""
                                # FHIR Bridge API
                                
                                A cloud-hosted, zero-trust gateway that facilitates secure healthcare data interoperability 
                                by transforming HL7 v2 messages to FHIR R4 format while managing consent for TEFCA compliance.
                                
                                ## Features
                                - **Data Transformation**: Converts HL7 v2 messages to FHIR R4 resources
                                - **Security Gateway**: Implements zero-trust architecture with OAuth2/JWT authentication
                                - **Consent Management**: Handles patient consent records and data access authorization
                                - **Audit Logging**: Comprehensive audit trail for all data access and transformations
                                - **Monitoring**: Health checks and performance metrics
                                
                                ## Authentication
                                This API uses OAuth 2.0 with JWT tokens for authentication. Include the JWT token in the 
                                Authorization header as `Bearer <token>`.
                                
                                ## Rate Limiting
                                API requests are rate limited to ensure system stability. Current limits:
                                - 1000 requests per hour for authenticated users
                                - 100 requests per hour for unauthenticated endpoints
                                
                                ## Error Handling
                                All errors follow a consistent format with detailed error codes and descriptions.
                                """)
                        .contact(new Contact()
                                .name("FHIR Bridge Support")
                                .email("support@fhirbridge.com")
                                .url("https://fhirbridge.com/support"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development server"),
                        new Server()
                                .url("https://api.fhirbridge.com")
                                .description("Production server"),
                        new Server()
                                .url("https://staging-api.fhirbridge.com")
                                .description("Staging server")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from authentication endpoints")))
                .tags(List.of(
                        new Tag()
                                .name("Transformation")
                                .description("HL7 v2 to FHIR transformation operations"),
                        new Tag()
                                .name("FHIR Resources")
                                .description("FHIR resource retrieval and search operations"),
                        new Tag()
                                .name("Consent Management")
                                .description("Patient consent verification and management"),
                        new Tag()
                                .name("Authentication")
                                .description("OAuth2 authentication and token management"),
                        new Tag()
                                .name("Monitoring")
                                .description("System health checks and monitoring endpoints"),
                        new Tag()
                                .name("System")
                                .description("System utilities and health checks")));
    }
}