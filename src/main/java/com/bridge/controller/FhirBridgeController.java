package com.bridge.controller;

import com.bridge.model.FhirResource;
import com.bridge.model.Hl7Message;
import com.bridge.model.MessageStatus;
import com.bridge.model.ValidationResult;
import com.bridge.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main REST controller for FHIR Bridge API endpoints
 * Handles HL7 v2 to FHIR transformation requests and FHIR resource operations
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Transformation", description = "HL7 v2 to FHIR transformation operations")
@Tag(name = "FHIR Resources", description = "FHIR resource retrieval and search operations")
@Tag(name = "System", description = "System utilities and health checks")
public class FhirBridgeController {
    
    private static final Logger logger = LoggerFactory.getLogger(FhirBridgeController.class);
    
    @Autowired
    private Hl7V2Validator hl7Validator;
    
    @Autowired
    private Hl7MessageParser messageParser;
    
    @Autowired
    private Hl7ToFhirTransformer transformer;
    
    @Autowired
    private FhirValidator fhirValidator;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Transform HL7 v2 message to FHIR R4 resources
     * 
     * @param request TransformationRequest containing the HL7 v2 message
     * @return ResponseEntity with transformation result or error details
     */
    @Operation(
        summary = "Transform HL7 v2 message to FHIR R4",
        description = """
            Transforms an HL7 v2 message to FHIR R4 resources. The endpoint validates the HL7 message,
            parses the content, transforms it to FHIR format, and validates the resulting FHIR resources.
            All operations are audited for compliance tracking.
            """,
        tags = {"Transformation"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transformation completed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TransformationResponse.class),
                examples = @ExampleObject(
                    name = "Successful transformation",
                    value = """
                        {
                          "requestId": "123e4567-e89b-12d3-a456-426614174000",
                          "status": "SUCCESS",
                          "fhirResources": [
                            {
                              "resourceId": "patient-123",
                              "resourceType": "Patient",
                              "fhirVersion": "R4",
                              "jsonContent": "{\\"resourceType\\": \\"Patient\\", \\"id\\": \\"patient-123\\"}",
                              "sourceMessageId": "123e4567-e89b-12d3-a456-426614174000",
                              "createdAt": "2025-01-15T10:30:00"
                            }
                          ],
                          "transformationTimestamp": "2025-01-15T10:30:00",
                          "resourceCount": 1
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid HL7 message or validation failed",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Validation error",
                    value = """
                        {
                          "error": {
                            "code": "VALIDATION_ERROR",
                            "message": "HL7 message validation failed",
                            "details": [
                              {
                                "field": "MSH.9",
                                "issue": "Message type is required"
                              }
                            ],
                            "timestamp": "2025-01-15T10:30:00Z",
                            "requestId": "123e4567-e89b-12d3-a456-426614174000"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions - requires TRANSFORMER or ADMIN role"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during transformation"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/transform/hl7v2-to-fhir", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TRANSFORMER') or hasRole('ADMIN')")
    public ResponseEntity<?> transformHl7ToFhir(
            @Parameter(
                description = "HL7 v2 message transformation request",
                required = true,
                schema = @Schema(implementation = TransformationRequest.class)
            )
            @Valid @RequestBody TransformationRequest request) {
        String requestId = UUID.randomUUID().toString();
        logger.info("Starting HL7 to FHIR transformation, requestId: {}", requestId);
        
        try {
            // Create HL7 message object
            Hl7Message hl7Message = new Hl7Message();
            hl7Message.setMessageId(requestId);
            hl7Message.setRawMessage(request.getHl7Message());
            hl7Message.setSendingApplication(request.getSendingApplication());
            hl7Message.setReceivingApplication(request.getReceivingApplication());
            hl7Message.setTimestamp(LocalDateTime.now());
            hl7Message.setStatus(MessageStatus.RECEIVED);
            
            // Validate HL7 message
            hl7Message.setStatus(MessageStatus.VALIDATING);
            ValidationResult validationResult = hl7Validator.validate(hl7Message);
            
            if (!validationResult.isValid()) {
                hl7Message.setStatus(MessageStatus.INVALID);
                auditService.logTransformation(requestId, "HL7", hl7Message.getMessageId(), 
                    "FHIR", "VALIDATION_FAILED", "VALIDATION_ERROR", 
                    createAuditDetails("validation_errors", validationResult.getErrors()));
                
                return ResponseEntity.badRequest().body(createErrorResponse(
                    "VALIDATION_ERROR", 
                    "HL7 message validation failed", 
                    validationResult.getErrors(),
                    requestId
                ));
            }
            
            hl7Message.setStatus(MessageStatus.VALID);
            
            // Parse HL7 message
            ParsedHl7Data parsedData = messageParser.parseMessage(hl7Message);
            
            // Transform to FHIR
            hl7Message.setStatus(MessageStatus.TRANSFORMING);
            List<FhirResource> fhirResources = transformer.transformToFhir(parsedData);
            
            // Validate FHIR resources
            for (FhirResource resource : fhirResources) {
                ValidationResult fhirValidation = fhirValidator.validateFhirResource(resource);
                if (!fhirValidation.isValid()) {
                    logger.warn("FHIR validation warnings for resource {}: {}", 
                        resource.getResourceId(), fhirValidation.getWarnings());
                }
            }
            
            hl7Message.setStatus(MessageStatus.TRANSFORMED);
            
            // Audit successful transformation
            auditService.logTransformation(requestId, "HL7", hl7Message.getMessageId(), 
                "FHIR", "TRANSFORMATION_SUCCESS", "SUCCESS", 
                createAuditDetails("fhir_resources_count", fhirResources.size()));
            
            // Create response
            TransformationResponse response = new TransformationResponse();
            response.setRequestId(requestId);
            response.setStatus("SUCCESS");
            response.setFhirResources(fhirResources);
            response.setTransformationTimestamp(LocalDateTime.now());
            response.setResourceCount(fhirResources.size());
            
            logger.info("HL7 to FHIR transformation completed successfully, requestId: {}, resourceCount: {}", 
                requestId, fhirResources.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during HL7 to FHIR transformation, requestId: {}", requestId, e);
            
            auditService.logTransformation(requestId, "HL7", requestId, 
                "FHIR", "TRANSFORMATION_ERROR", "ERROR", 
                createAuditDetails("error_message", e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                    "TRANSFORMATION_ERROR", 
                    "Failed to transform HL7 message to FHIR", 
                    e.getMessage(),
                    requestId
                ));
        }
    }
    
    /**
     * Retrieve a specific FHIR resource by type and ID
     * 
     * @param resourceType The FHIR resource type (e.g., Patient, Observation)
     * @param resourceId The unique identifier for the resource
     * @return ResponseEntity with the FHIR resource or error details
     */
    @Operation(
        summary = "Retrieve FHIR resource by type and ID",
        description = """
            Retrieves a specific FHIR resource by its type and unique identifier.
            Supports all FHIR R4 resource types including Patient, Observation, Encounter, etc.
            """,
        tags = {"FHIR Resources"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "FHIR resource retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "FHIR Patient resource",
                    value = """
                        {
                          "resourceType": "Patient",
                          "id": "patient-123",
                          "name": [
                            {
                              "family": "Doe",
                              "given": ["John"]
                            }
                          ],
                          "gender": "male",
                          "birthDate": "1980-01-01"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "FHIR resource not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/fhir/{resourceType}/{resourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('READER') or hasRole('TRANSFORMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getFhirResource(
            @Parameter(description = "FHIR resource type (e.g., Patient, Observation)", required = true, example = "Patient")
            @PathVariable String resourceType,
            @Parameter(description = "Unique resource identifier", required = true, example = "patient-123")
            @PathVariable String resourceId) {
        
        String requestId = UUID.randomUUID().toString();
        logger.info("Retrieving FHIR resource, type: {}, id: {}, requestId: {}", 
            resourceType, resourceId, requestId);
        
        try {
            // For now, return a placeholder response since we don't have a repository implementation
            // This would typically query a FHIR repository or database
            
            auditService.logSystemEvent("FHIR_RESOURCE_ACCESS", 
                "GET_RESOURCE", "SUCCESS", 
                createAuditDetails("resource_type", resourceType));
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "FHIR resource retrieval endpoint - implementation pending");
            response.put("resourceType", resourceType);
            response.put("resourceId", resourceId);
            response.put("requestId", requestId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving FHIR resource, type: {}, id: {}, requestId: {}", 
                resourceType, resourceId, requestId, e);
            
            auditService.logSystemEvent("FHIR_RESOURCE_ACCESS", 
                "GET_RESOURCE_ERROR", "ERROR", 
                createAuditDetails("error_message", e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                    "RESOURCE_RETRIEVAL_ERROR", 
                    "Failed to retrieve FHIR resource", 
                    e.getMessage(),
                    requestId
                ));
        }
    }
    
    /**
     * Search for FHIR resources by type with optional query parameters
     * 
     * @param resourceType The FHIR resource type to search
     * @param params Query parameters for filtering
     * @return ResponseEntity with search results or error details
     */
    @Operation(
        summary = "Search FHIR resources by type",
        description = """
            Searches for FHIR resources of a specific type using optional query parameters.
            Supports standard FHIR search parameters like _id, _lastUpdated, and resource-specific parameters.
            """,
        tags = {"FHIR Resources"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Search results",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 2,
                          "entry": [
                            {
                              "resource": {
                                "resourceType": "Patient",
                                "id": "patient-123"
                              }
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/fhir/{resourceType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('READER') or hasRole('TRANSFORMER') or hasRole('ADMIN')")
    public ResponseEntity<?> searchFhirResources(
            @Parameter(description = "FHIR resource type to search", required = true, example = "Patient")
            @PathVariable String resourceType,
            @Parameter(description = "Search parameters (e.g., name=John, birthdate=1980-01-01)")
            @RequestParam Map<String, String> params) {
        
        String requestId = UUID.randomUUID().toString();
        logger.info("Searching FHIR resources, type: {}, params: {}, requestId: {}", 
            resourceType, params, requestId);
        
        try {
            // For now, return a placeholder response since we don't have a repository implementation
            // This would typically query a FHIR repository or database with search parameters
            
            auditService.logSystemEvent("FHIR_RESOURCE_SEARCH", 
                "SEARCH_RESOURCES", "SUCCESS", 
                createAuditDetails("search_params", params));
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "FHIR resource search endpoint - implementation pending");
            response.put("resourceType", resourceType);
            response.put("searchParams", params);
            response.put("requestId", requestId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching FHIR resources, type: {}, requestId: {}", 
                resourceType, requestId, e);
            
            auditService.logSystemEvent("FHIR_RESOURCE_SEARCH", 
                "SEARCH_RESOURCES_ERROR", "ERROR", 
                createAuditDetails("error_message", e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                    "RESOURCE_SEARCH_ERROR", 
                    "Failed to search FHIR resources", 
                    e.getMessage(),
                    requestId
                ));
        }
    }
    
    /**
     * Health check endpoint for the transformation service
     * 
     * @return ResponseEntity with health status
     */
    @Operation(
        summary = "Health check for transformation service",
        description = "Returns the health status of the FHIR Bridge transformation service",
        tags = {"System"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Healthy service",
                    value = """
                        {
                          "status": "UP",
                          "service": "FHIR Bridge Transformation API",
                          "timestamp": "2025-01-15T10:30:00",
                          "version": "1.0.0"
                        }
                        """
                )
            )
        )
    })
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "FHIR Bridge Transformation API");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }
    
    // Helper methods
    
    private Map<String, Object> createErrorResponse(String code, String message, Object details, String requestId) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of(
            "code", code,
            "message", message,
            "details", details,
            "timestamp", LocalDateTime.now(),
            "requestId", requestId
        ));
        return error;
    }
    
    private Map<String, Object> createAuditDetails(String key, Object value) {
        Map<String, Object> details = new HashMap<>();
        details.put(key, value);
        return details;
    }
    
    // Inner classes for request/response DTOs
    
    public static class TransformationRequest {
        private String hl7Message;
        private String sendingApplication;
        private String receivingApplication;
        
        // Getters and setters
        public String getHl7Message() { return hl7Message; }
        public void setHl7Message(String hl7Message) { this.hl7Message = hl7Message; }
        
        public String getSendingApplication() { return sendingApplication; }
        public void setSendingApplication(String sendingApplication) { this.sendingApplication = sendingApplication; }
        
        public String getReceivingApplication() { return receivingApplication; }
        public void setReceivingApplication(String receivingApplication) { this.receivingApplication = receivingApplication; }
    }
    
    public static class TransformationResponse {
        private String requestId;
        private String status;
        private List<FhirResource> fhirResources;
        private LocalDateTime transformationTimestamp;
        private int resourceCount;
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public List<FhirResource> getFhirResources() { return fhirResources; }
        public void setFhirResources(List<FhirResource> fhirResources) { this.fhirResources = fhirResources; }
        
        public LocalDateTime getTransformationTimestamp() { return transformationTimestamp; }
        public void setTransformationTimestamp(LocalDateTime transformationTimestamp) { this.transformationTimestamp = transformationTimestamp; }
        
        public int getResourceCount() { return resourceCount; }
        public void setResourceCount(int resourceCount) { this.resourceCount = resourceCount; }
    }
}