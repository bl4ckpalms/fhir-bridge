package com.bridge.service;

import com.bridge.model.FhirResource;
import com.bridge.model.ValidationResult;
import org.hl7.fhir.r4.model.Resource;

/**
 * Service interface for validating FHIR R4 resources
 */
public interface FhirValidator {
    
    /**
     * Validate a FHIR resource against R4 specification and profiles
     * 
     * @param resource The FHIR resource to validate
     * @return ValidationResult containing validation outcome and any issues
     */
    ValidationResult validateResource(Resource resource);
    
    /**
     * Validate a FHIR resource from JSON content
     * 
     * @param fhirResource The FhirResource containing JSON content to validate
     * @return ValidationResult containing validation outcome and any issues
     */
    ValidationResult validateFhirResource(FhirResource fhirResource);
    
    /**
     * Validate a FHIR resource against specific profiles (US Core, TEFCA)
     * 
     * @param resource The FHIR resource to validate
     * @param profileUrls Array of profile URLs to validate against
     * @return ValidationResult containing validation outcome and any issues
     */
    ValidationResult validateAgainstProfiles(Resource resource, String... profileUrls);
    
    /**
     * Validate FHIR resource JSON content directly
     * 
     * @param jsonContent The JSON content of the FHIR resource
     * @param resourceType The expected resource type
     * @return ValidationResult containing validation outcome and any issues
     */
    ValidationResult validateJsonContent(String jsonContent, String resourceType);
}