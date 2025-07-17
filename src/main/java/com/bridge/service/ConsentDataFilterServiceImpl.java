package com.bridge.service;

import com.bridge.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of consent-based data filtering service
 */
@Service
public class ConsentDataFilterServiceImpl implements ConsentDataFilterService {

    private static final Logger logger = LoggerFactory.getLogger(ConsentDataFilterServiceImpl.class);
    
    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, DataCategory>> resourceFieldMappings;

    public ConsentDataFilterServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.resourceFieldMappings = initializeFieldMappings();
    }

    @Override
    public FhirResource filterFhirResource(FhirResource fhirResource, ConsentRecord consentRecord) {
        if (fhirResource == null || consentRecord == null) {
            logger.warn("Cannot filter: fhirResource or consentRecord is null");
            return fhirResource;
        }

        logger.debug("Filtering FHIR resource: {} based on consent for patient: {}", 
            fhirResource.getResourceType(), consentRecord.getPatientId());

        if (!consentRecord.isActive()) {
            logger.warn("Consent is not active for patient: {}", consentRecord.getPatientId());
            return null; // Block all data if consent is not active
        }

        List<DataCategory> allowedCategories = getAllowedCategories(consentRecord);
        
        // If ALL category is allowed, return original resource
        if (allowedCategories.contains(DataCategory.ALL)) {
            logger.debug("ALL data categories allowed, returning original resource");
            return fhirResource;
        }

        try {
            String filteredJson = filterJsonContent(fhirResource.getJsonContent(), 
                allowedCategories, fhirResource.getResourceType());
            
            // Create filtered resource
            FhirResource filteredResource = new FhirResource();
            filteredResource.setResourceId(fhirResource.getResourceId());
            filteredResource.setResourceType(fhirResource.getResourceType());
            filteredResource.setFhirVersion(fhirResource.getFhirVersion());
            filteredResource.setJsonContent(filteredJson);
            filteredResource.setSourceMessageId(fhirResource.getSourceMessageId());
            filteredResource.setCreatedAt(LocalDateTime.now());

            logger.debug("Successfully filtered FHIR resource: {}", fhirResource.getResourceType());
            return filteredResource;

        } catch (Exception e) {
            logger.error("Error filtering FHIR resource: {}", fhirResource.getResourceType(), e);
            return null; // Block resource if filtering fails
        }
    }

    @Override
    public List<FhirResource> filterFhirResources(List<FhirResource> fhirResources, ConsentRecord consentRecord) {
        logger.debug("Filtering {} FHIR resources based on consent for patient: {}", 
            fhirResources != null ? fhirResources.size() : 0, 
            consentRecord != null ? consentRecord.getPatientId() : "unknown");

        if (fhirResources == null || fhirResources.isEmpty()) {
            return new ArrayList<>();
        }

        if (consentRecord == null || !consentRecord.isActive()) {
            logger.warn("Consent is null or not active, blocking all resources");
            return new ArrayList<>();
        }

        return fhirResources.stream()
            .map(resource -> filterFhirResource(resource, consentRecord))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isCategoryAllowed(DataCategory dataCategory, ConsentRecord consentRecord) {
        if (consentRecord == null || dataCategory == null) {
            return false;
        }

        List<DataCategory> allowedCategories = getAllowedCategories(consentRecord);
        return allowedCategories.contains(DataCategory.ALL) || allowedCategories.contains(dataCategory);
    }

    @Override
    public List<DataCategory> getAllowedCategories(ConsentRecord consentRecord) {
        if (consentRecord == null || consentRecord.getAllowedCategories() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(consentRecord.getAllowedCategories());
    }

    @Override
    public String filterJsonContent(String jsonContent, List<DataCategory> allowedCategories, String resourceType) {
        if (!StringUtils.hasText(jsonContent) || allowedCategories == null) {
            logger.warn("Invalid input for JSON filtering");
            return jsonContent;
        }

        if (allowedCategories.contains(DataCategory.ALL)) {
            return jsonContent;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            if (!rootNode.isObject()) {
                return jsonContent;
            }

            ObjectNode filteredNode = rootNode.deepCopy();
            Map<String, DataCategory> fieldMappings = getFieldCategoryMapping(resourceType);

            // Filter fields based on category mappings
            filterJsonFields(filteredNode, fieldMappings, allowedCategories, "");

            return objectMapper.writeValueAsString(filteredNode);

        } catch (Exception e) {
            logger.error("Error filtering JSON content for resource type: {}", resourceType, e);
            return jsonContent; // Return original if filtering fails
        }
    }

    @Override
    public FilteredResourceResponse createFilteredResponse(FhirResource originalResource, 
                                                          FhirResource filteredResource, 
                                                          ConsentRecord consentRecord) {
        logger.debug("Creating filtered response for resource: {}", 
            originalResource != null ? originalResource.getResourceType() : "unknown");

        if (originalResource == null) {
            return FilteredResourceResponse.blocked("Original resource is null");
        }

        if (consentRecord == null || !consentRecord.isActive()) {
            return FilteredResourceResponse.blocked("No active consent found");
        }

        List<DataCategory> allowedCategories = getAllowedCategories(consentRecord);
        
        if (filteredResource == null) {
            return FilteredResourceResponse.blocked("Resource blocked by consent restrictions");
        }

        // Determine what was filtered
        List<DataCategory> filteredCategories = determineFilteredCategories(
            originalResource, filteredResource, allowedCategories);
        List<String> filteredFields = determineFilteredFields(
            originalResource, filteredResource);

        FilteredResourceResponse response;
        if (filteredCategories.isEmpty() && filteredFields.isEmpty()) {
            response = FilteredResourceResponse.unfiltered(filteredResource, allowedCategories);
        } else {
            response = FilteredResourceResponse.filtered(originalResource, filteredResource, 
                allowedCategories, filteredCategories, filteredFields);
        }

        response.setConsentReference(consentRecord.getPolicyReference());
        return response;
    }

    @Override
    public Map<String, DataCategory> getFieldCategoryMapping(String resourceType) {
        return resourceFieldMappings.getOrDefault(resourceType, new HashMap<>());
    }

    /**
     * Initialize field mappings for different FHIR resource types
     */
    private Map<String, Map<String, DataCategory>> initializeFieldMappings() {
        Map<String, Map<String, DataCategory>> mappings = new HashMap<>();

        // Patient resource mappings
        Map<String, DataCategory> patientMappings = new HashMap<>();
        patientMappings.put("name", DataCategory.DEMOGRAPHICS);
        patientMappings.put("telecom", DataCategory.DEMOGRAPHICS);
        patientMappings.put("address", DataCategory.DEMOGRAPHICS);
        patientMappings.put("birthDate", DataCategory.DEMOGRAPHICS);
        patientMappings.put("gender", DataCategory.DEMOGRAPHICS);
        patientMappings.put("contact", DataCategory.EMERGENCY_CONTACTS);
        patientMappings.put("communication", DataCategory.DEMOGRAPHICS);
        mappings.put("Patient", patientMappings);

        // Observation resource mappings
        Map<String, DataCategory> observationMappings = new HashMap<>();
        observationMappings.put("code", DataCategory.LAB_RESULTS);
        observationMappings.put("value", DataCategory.LAB_RESULTS);
        observationMappings.put("component", DataCategory.LAB_RESULTS);
        observationMappings.put("interpretation", DataCategory.LAB_RESULTS);
        observationMappings.put("note", DataCategory.CLINICAL_NOTES);
        mappings.put("Observation", observationMappings);

        // Medication resource mappings
        Map<String, DataCategory> medicationMappings = new HashMap<>();
        medicationMappings.put("medication", DataCategory.MEDICATIONS);
        medicationMappings.put("dosage", DataCategory.MEDICATIONS);
        medicationMappings.put("dispenseRequest", DataCategory.MEDICATIONS);
        medicationMappings.put("substitution", DataCategory.MEDICATIONS);
        mappings.put("MedicationRequest", medicationMappings);

        // AllergyIntolerance resource mappings
        Map<String, DataCategory> allergyMappings = new HashMap<>();
        allergyMappings.put("code", DataCategory.ALLERGIES);
        allergyMappings.put("reaction", DataCategory.ALLERGIES);
        allergyMappings.put("severity", DataCategory.ALLERGIES);
        allergyMappings.put("note", DataCategory.CLINICAL_NOTES);
        mappings.put("AllergyIntolerance", allergyMappings);

        // Immunization resource mappings
        Map<String, DataCategory> immunizationMappings = new HashMap<>();
        immunizationMappings.put("vaccineCode", DataCategory.IMMUNIZATIONS);
        immunizationMappings.put("occurrence", DataCategory.IMMUNIZATIONS);
        immunizationMappings.put("site", DataCategory.IMMUNIZATIONS);
        immunizationMappings.put("route", DataCategory.IMMUNIZATIONS);
        immunizationMappings.put("note", DataCategory.CLINICAL_NOTES);
        mappings.put("Immunization", immunizationMappings);

        // DiagnosticReport resource mappings
        Map<String, DataCategory> diagnosticMappings = new HashMap<>();
        diagnosticMappings.put("code", DataCategory.LAB_RESULTS);
        diagnosticMappings.put("result", DataCategory.LAB_RESULTS);
        diagnosticMappings.put("conclusion", DataCategory.CLINICAL_NOTES);
        diagnosticMappings.put("media", DataCategory.IMAGING);
        mappings.put("DiagnosticReport", diagnosticMappings);

        return mappings;
    }

    /**
     * Recursively filter JSON fields based on category mappings
     */
    private void filterJsonFields(ObjectNode node, Map<String, DataCategory> fieldMappings, 
                                 List<DataCategory> allowedCategories, String currentPath) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        List<String> fieldsToRemove = new ArrayList<>();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            String fieldPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
            
            // Check if this field should be filtered
            DataCategory fieldCategory = fieldMappings.get(fieldName);
            if (fieldCategory != null && !allowedCategories.contains(fieldCategory)) {
                fieldsToRemove.add(fieldName);
                logger.debug("Filtering field: {} (category: {})", fieldPath, fieldCategory);
            } else if (field.getValue().isObject()) {
                // Recursively filter nested objects
                filterJsonFields((ObjectNode) field.getValue(), fieldMappings, allowedCategories, fieldPath);
            }
        }

        // Remove filtered fields
        fieldsToRemove.forEach(node::remove);
    }

    /**
     * Determine which categories were filtered by comparing original and filtered resources
     */
    private List<DataCategory> determineFilteredCategories(FhirResource originalResource, 
                                                          FhirResource filteredResource,
                                                          List<DataCategory> allowedCategories) {
        List<DataCategory> allCategories = Arrays.asList(DataCategory.values());
        return allCategories.stream()
            .filter(category -> category != DataCategory.ALL)
            .filter(category -> !allowedCategories.contains(category))
            .collect(Collectors.toList());
    }

    /**
     * Determine which fields were filtered by comparing JSON content
     */
    private List<String> determineFilteredFields(FhirResource originalResource, FhirResource filteredResource) {
        List<String> filteredFields = new ArrayList<>();
        
        try {
            JsonNode originalNode = objectMapper.readTree(originalResource.getJsonContent());
            JsonNode filteredNode = objectMapper.readTree(filteredResource.getJsonContent());
            
            compareJsonNodes(originalNode, filteredNode, "", filteredFields);
            
        } catch (Exception e) {
            logger.error("Error determining filtered fields", e);
        }
        
        return filteredFields;
    }

    /**
     * Compare JSON nodes to find removed fields
     */
    private void compareJsonNodes(JsonNode original, JsonNode filtered, String path, List<String> filteredFields) {
        if (original.isObject() && filtered.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> originalFields = original.fields();
            
            while (originalFields.hasNext()) {
                Map.Entry<String, JsonNode> field = originalFields.next();
                String fieldName = field.getKey();
                String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
                
                if (!filtered.has(fieldName)) {
                    filteredFields.add(fieldPath);
                } else {
                    compareJsonNodes(field.getValue(), filtered.get(fieldName), fieldPath, filteredFields);
                }
            }
        }
    }
}