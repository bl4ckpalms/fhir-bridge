package com.bridge.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FhirResourceTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidFhirResource() {
        FhirResource resource = new FhirResource("patient-123", "Patient", 
                "{\"resourceType\":\"Patient\",\"id\":\"patient-123\"}");
        
        Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
        assertTrue(violations.isEmpty(), "Valid resource should have no validation errors");
    }

    @Test
    void testMissingResourceId() {
        FhirResource resource = new FhirResource();
        resource.setResourceType("Patient");
        resource.setJsonContent("{\"resourceType\":\"Patient\"}");
        
        Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Resource ID is required")));
    }

    @Test
    void testBlankResourceId() {
        FhirResource resource = new FhirResource();
        resource.setResourceId("");
        resource.setResourceType("Patient");
        resource.setJsonContent("{\"resourceType\":\"Patient\"}");
        
        Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Resource ID is required")));
    }

    @Test
    void testResourceIdTooLong() {
        FhirResource resource = new FhirResource();
        resource.setResourceId("A".repeat(101)); // 101 characters
        resource.setResourceType("Patient");
        resource.setJsonContent("{\"resourceType\":\"Patient\"}");
        
        Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 100 characters")));
    }

    @Test
    void testMissingResourceType() {
        FhirResource resource = new FhirResource();
        resource.setResourceId("patient-123");
        resource.setJsonContent("{\"resourceType\":\"Patient\"}");
        
        Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Resource type is required")));
    }

    @Test
    void testMissingJsonContent() {
        FhirResource resource = new FhirResource();
        resource.setResourceId("patient-123");
        resource.setResourceType("Patient");
        
        Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("JSON content is required")));
    }

    @Test
    void testInvalidFhirVersion() {
        FhirResource resource = new FhirResource();
        resource.setResourceId("patient-123");
        resource.setResourceType("Patient");
        resource.setJsonContent("{\"resourceType\":\"Patient\"}");
        resource.setFhirVersion("3.0.1"); // Invalid version format
        
        Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("FHIR version must be in format 4.x.x")));
    }

    @Test
    void testValidFhirVersions() {
        FhirResource resource = new FhirResource("patient-123", "Patient", 
                "{\"resourceType\":\"Patient\",\"id\":\"patient-123\"}");
        
        // Test valid FHIR R4 versions
        String[] validVersions = {"4.0.0", "4.0.1", "4.3.0", "4.10.15"};
        
        for (String version : validVersions) {
            resource.setFhirVersion(version);
            Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
            assertTrue(violations.isEmpty(), "Version " + version + " should be valid");
        }
    }

    @Test
    void testJsonContentTooLong() {
        FhirResource resource = new FhirResource();
        resource.setResourceId("patient-123");
        resource.setResourceType("Patient");
        resource.setJsonContent("A".repeat(50001)); // 50001 characters
        
        Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 50000 characters")));
    }

    @Test
    void testSourceMessageIdTooLong() {
        FhirResource resource = new FhirResource();
        resource.setResourceId("patient-123");
        resource.setResourceType("Patient");
        resource.setJsonContent("{\"resourceType\":\"Patient\"}");
        resource.setSourceMessageId("A".repeat(101)); // 101 characters
        
        Set<ConstraintViolation<FhirResource>> violations = validator.validate(resource);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 100 characters")));
    }

    @Test
    void testDefaultConstructor() {
        FhirResource resource = new FhirResource();
        
        assertNotNull(resource.getCreatedAt());
        assertEquals("4.0.1", resource.getFhirVersion());
        assertTrue(resource.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testConstructorWithRequiredFields() {
        String resourceId = "patient-123";
        String resourceType = "Patient";
        String jsonContent = "{\"resourceType\":\"Patient\",\"id\":\"patient-123\"}";
        
        FhirResource resource = new FhirResource(resourceId, resourceType, jsonContent);
        
        assertEquals(resourceId, resource.getResourceId());
        assertEquals(resourceType, resource.getResourceType());
        assertEquals(jsonContent, resource.getJsonContent());
        assertEquals("4.0.1", resource.getFhirVersion());
        assertNotNull(resource.getCreatedAt());
        assertNull(resource.getSourceMessageId());
    }

    @Test
    void testConstructorWithSourceMessageId() {
        String resourceId = "patient-123";
        String resourceType = "Patient";
        String jsonContent = "{\"resourceType\":\"Patient\",\"id\":\"patient-123\"}";
        String sourceMessageId = "MSG001";
        
        FhirResource resource = new FhirResource(resourceId, resourceType, jsonContent, sourceMessageId);
        
        assertEquals(resourceId, resource.getResourceId());
        assertEquals(resourceType, resource.getResourceType());
        assertEquals(jsonContent, resource.getJsonContent());
        assertEquals(sourceMessageId, resource.getSourceMessageId());
        assertEquals("4.0.1", resource.getFhirVersion());
        assertNotNull(resource.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        FhirResource resource1 = new FhirResource("patient-123", "Patient", "{\"resourceType\":\"Patient\"}");
        FhirResource resource2 = new FhirResource("patient-123", "Patient", "{\"different\":\"content\"}");
        FhirResource resource3 = new FhirResource("patient-456", "Patient", "{\"resourceType\":\"Patient\"}");
        FhirResource resource4 = new FhirResource("patient-123", "Observation", "{\"resourceType\":\"Patient\"}");
        
        assertEquals(resource1, resource2); // Same ID and type
        assertNotEquals(resource1, resource3); // Different ID
        assertNotEquals(resource1, resource4); // Different type
        assertEquals(resource1.hashCode(), resource2.hashCode());
        assertNotEquals(resource1.hashCode(), resource3.hashCode());
    }

    @Test
    void testToString() {
        FhirResource resource = new FhirResource("patient-123", "Patient", 
                "{\"resourceType\":\"Patient\"}", "MSG001");
        String toString = resource.toString();
        
        assertTrue(toString.contains("patient-123"));
        assertTrue(toString.contains("Patient"));
        assertTrue(toString.contains("4.0.1"));
        assertTrue(toString.contains("MSG001"));
    }
}