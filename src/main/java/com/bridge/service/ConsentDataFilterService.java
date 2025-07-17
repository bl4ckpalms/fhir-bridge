package com.bridge.service;

import com.bridge.model.ConsentRecord;
import com.bridge.model.DataCategory;
import com.bridge.model.FhirResource;
import com.bridge.model.FilteredResourceResponse;

import java.util.List;
import java.util.Map;

/**
 * Service interface for filtering data based on patient consent preferences
 */
public interface ConsentDataFilterService {

    /**
     * Filter FHIR resource based on consent preferences
     *
     * @param fhirResource the FHIR resource to filter
     * @param consentRecord the consent record containing allowed categories
     * @return filtered FHIR resource with restricted data removed
     */
    FhirResource filterFhirResource(FhirResource fhirResource, ConsentRecord consentRecord);

    /**
     * Filter multiple FHIR resources based on consent preferences
     *
     * @param fhirResources list of FHIR resources to filter
     * @param consentRecord the consent record containing allowed categories
     * @return list of filtered FHIR resources
     */
    List<FhirResource> filterFhirResources(List<FhirResource> fhirResources, ConsentRecord consentRecord);

    /**
     * Check if a specific data category is allowed by consent
     *
     * @param dataCategory the data category to check
     * @param consentRecord the consent record to check against
     * @return true if the category is allowed, false otherwise
     */
    boolean isCategoryAllowed(DataCategory dataCategory, ConsentRecord consentRecord);

    /**
     * Get allowed data categories from consent record
     *
     * @param consentRecord the consent record
     * @return list of allowed data categories
     */
    List<DataCategory> getAllowedCategories(ConsentRecord consentRecord);

    /**
     * Filter JSON content based on data category restrictions
     *
     * @param jsonContent the JSON content to filter
     * @param allowedCategories list of allowed data categories
     * @param resourceType the FHIR resource type
     * @return filtered JSON content
     */
    String filterJsonContent(String jsonContent, List<DataCategory> allowedCategories, String resourceType);

    /**
     * Create filtered response with consent metadata
     *
     * @param originalResource the original FHIR resource
     * @param filteredResource the filtered FHIR resource
     * @param consentRecord the consent record used for filtering
     * @return filtered response with metadata
     */
    FilteredResourceResponse createFilteredResponse(FhirResource originalResource, 
                                                   FhirResource filteredResource, 
                                                   ConsentRecord consentRecord);

    /**
     * Get data category mapping for FHIR resource fields
     *
     * @param resourceType the FHIR resource type
     * @return mapping of field paths to data categories
     */
    Map<String, DataCategory> getFieldCategoryMapping(String resourceType);
}