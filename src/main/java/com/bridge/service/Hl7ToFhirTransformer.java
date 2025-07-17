package com.bridge.service;

import com.bridge.model.FhirResource;
import java.util.List;

/**
 * Service interface for transforming HL7 v2 messages to FHIR R4 resources
 */
public interface Hl7ToFhirTransformer {
    
    /**
     * Transform parsed HL7 data to FHIR R4 resources
     * 
     * @param parsedData The parsed HL7 message data
     * @return List of FHIR resources created from the HL7 data
     */
    List<FhirResource> transformToFhir(ParsedHl7Data parsedData);
    
    /**
     * Transform parsed HL7 data to a specific FHIR resource type
     * 
     * @param parsedData The parsed HL7 message data
     * @param resourceType The specific FHIR resource type to create
     * @return The FHIR resource or null if transformation is not applicable
     */
    FhirResource transformToFhirResource(ParsedHl7Data parsedData, String resourceType);
}