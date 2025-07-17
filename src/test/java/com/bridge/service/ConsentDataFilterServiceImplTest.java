package com.bridge.service;

import com.bridge.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConsentDataFilterServiceImplTest {

    private ConsentDataFilterServiceImpl consentDataFilterService;
    private ConsentRecord activeConsentRecord;
    private ConsentRecord restrictedConsentRecord;
    private ConsentRecord inactiveConsentRecord;
    private FhirResource patientResource;
    private FhirResource observationResource;
    private String patientId;
    private String organizationId;

    @BeforeEach
    void setUp() {
        consentDataFilterService = new ConsentDataFilterServiceImpl();
        patientId = "patient-123";
        organizationId = "org-456";

        // Active consent with all categories
        activeConsentRecord = new ConsentRecord();
        activeConsentRecord.setPatientId(patientId);
        activeConsentRecord.setOrganizationId(organizationId);
        activeConsentRecord.setStatus(ConsentStatus.ACTIVE);
        activeConsentRecord.setEffectiveDate(LocalDateTime.now().minusDays(1));
        activeConsentRecord.setExpirationDate(LocalDateTime.now().plusDays(30));
        activeConsentRecord.setAllowedCategories(Arrays.asList(DataCategory.ALL));

        // Restricted consent with limited categories
        restrictedConsentRecord = new ConsentRecord();
        restrictedConsentRecord.setPatientId(patientId);
        restrictedConsentRecord.setOrganizationId(organizationId);
        restrictedConsentRecord.setStatus(ConsentStatus.ACTIVE);
        restrictedConsentRecord.setEffectiveDate(LocalDateTime.now().minusDays(1));
        restrictedConsentRecord.setExpirationDate(LocalDateTime.now().plusDays(30));
        restrictedConsentRecord.setAllowedCategories(Arrays.asList(
            DataCategory.DEMOGRAPHICS, DataCategory.LAB_RESULTS));

        // Inactive consent
        inactiveConsentRecord = new ConsentRecord();
        inactiveConsentRecord.setPatientId(patientId);
        inactiveConsentRecord.setOrganizationId(organizationId);
        inactiveConsentRecord.setStatus(ConsentStatus.REVOKED);
        inactiveConsentRecord.setEffectiveDate(LocalDateTime.now().minusDays(1));

        // Sample Patient FHIR resource
        patientResource = new FhirResource();
        patientResource.setResourceId("patient-123");
        patientResource.setResourceType("Patient");
        patientResource.setFhirVersion("4.0.1");
        patientResource.setJsonContent(createPatientJson());
        patientResource.setCreatedAt(LocalDateTime.now());

        // Sample Observation FHIR resource
        observationResource = new FhirResource();
        observationResource.setResourceId("obs-456");
        observationResource.setResourceType("Observation");
        observationResource.setFhirVersion("4.0.1");
        observationResource.setJsonContent(createObservationJson());
        observationResource.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void filterFhirResource_AllCategoriesAllowed_ReturnsOriginalResource() {
        // Act
        FhirResource result = consentDataFilterService.filterFhirResource(patientResource, activeConsentRecord);

        // Assert
        assertNotNull(result);
        assertEquals(patientResource.getResourceId(), result.getResourceId());
        assertEquals(patientResource.getResourceType(), result.getResourceType());
        assertEquals(patientResource.getJsonContent(), result.getJsonContent());
    }

    @Test
    void filterFhirResource_RestrictedCategories_ReturnsFilteredResource() {
        // Act
        FhirResource result = consentDataFilterService.filterFhirResource(patientResource, restrictedConsentRecord);

        // Assert
        assertNotNull(result);
        assertEquals(patientResource.getResourceId(), result.getResourceId());
        assertEquals(patientResource.getResourceType(), result.getResourceType());
        // JSON content should be different (filtered)
        assertNotEquals(patientResource.getJsonContent(), result.getJsonContent());
        // Should still contain allowed fields
        assertTrue(result.getJsonContent().contains("name"));
        assertTrue(result.getJsonContent().contains("birthDate"));
    }

    @Test
    void filterFhirResource_InactiveConsent_ReturnsNull() {
        // Act
        FhirResource result = consentDataFilterService.filterFhirResource(patientResource, inactiveConsentRecord);

        // Assert
        assertNull(result);
    }

    @Test
    void filterFhirResource_NullInputs_HandlesGracefully() {
        // Test null resource
        FhirResource result1 = consentDataFilterService.filterFhirResource(null, activeConsentRecord);
        assertNull(result1);

        // Test null consent
        FhirResource result2 = consentDataFilterService.filterFhirResource(patientResource, null);
        assertEquals(patientResource, result2);

        // Test both null
        FhirResource result3 = consentDataFilterService.filterFhirResource(null, null);
        assertNull(result3);
    }

    @Test
    void filterFhirResources_MultipleResources_FiltersCorrectly() {
        // Arrange
        List<FhirResource> resources = Arrays.asList(patientResource, observationResource);

        // Act
        List<FhirResource> result = consentDataFilterService.filterFhirResources(resources, activeConsentRecord);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(patientResource.getResourceId(), result.get(0).getResourceId());
        assertEquals(observationResource.getResourceId(), result.get(1).getResourceId());
    }

    @Test
    void filterFhirResources_InactiveConsent_ReturnsEmptyList() {
        // Arrange
        List<FhirResource> resources = Arrays.asList(patientResource, observationResource);

        // Act
        List<FhirResource> result = consentDataFilterService.filterFhirResources(resources, inactiveConsentRecord);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void filterFhirResources_EmptyList_ReturnsEmptyList() {
        // Act
        List<FhirResource> result = consentDataFilterService.filterFhirResources(Arrays.asList(), activeConsentRecord);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void isCategoryAllowed_AllCategoriesAllowed_ReturnsTrue() {
        // Act & Assert
        assertTrue(consentDataFilterService.isCategoryAllowed(DataCategory.DEMOGRAPHICS, activeConsentRecord));
        assertTrue(consentDataFilterService.isCategoryAllowed(DataCategory.MEDICATIONS, activeConsentRecord));
        assertTrue(consentDataFilterService.isCategoryAllowed(DataCategory.GENETIC_DATA, activeConsentRecord));
    }

    @Test
    void isCategoryAllowed_RestrictedCategories_ReturnsCorrectly() {
        // Act & Assert
        assertTrue(consentDataFilterService.isCategoryAllowed(DataCategory.DEMOGRAPHICS, restrictedConsentRecord));
        assertTrue(consentDataFilterService.isCategoryAllowed(DataCategory.LAB_RESULTS, restrictedConsentRecord));
        assertFalse(consentDataFilterService.isCategoryAllowed(DataCategory.MEDICATIONS, restrictedConsentRecord));
        assertFalse(consentDataFilterService.isCategoryAllowed(DataCategory.GENETIC_DATA, restrictedConsentRecord));
    }

    @Test
    void isCategoryAllowed_NullInputs_ReturnsFalse() {
        // Act & Assert
        assertFalse(consentDataFilterService.isCategoryAllowed(null, activeConsentRecord));
        assertFalse(consentDataFilterService.isCategoryAllowed(DataCategory.DEMOGRAPHICS, null));
        assertFalse(consentDataFilterService.isCategoryAllowed(null, null));
    }

    @Test
    void getAllowedCategories_ValidConsent_ReturnsCategories() {
        // Act
        List<DataCategory> result = consentDataFilterService.getAllowedCategories(restrictedConsentRecord);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(DataCategory.DEMOGRAPHICS));
        assertTrue(result.contains(DataCategory.LAB_RESULTS));
    }

    @Test
    void getAllowedCategories_NullConsent_ReturnsEmptyList() {
        // Act
        List<DataCategory> result = consentDataFilterService.getAllowedCategories(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void filterJsonContent_AllCategoriesAllowed_ReturnsOriginal() {
        // Arrange
        String originalJson = patientResource.getJsonContent();
        List<DataCategory> allowedCategories = Arrays.asList(DataCategory.ALL);

        // Act
        String result = consentDataFilterService.filterJsonContent(originalJson, allowedCategories, "Patient");

        // Assert
        assertEquals(originalJson, result);
    }

    @Test
    void filterJsonContent_RestrictedCategories_FiltersContent() {
        // Arrange
        String originalJson = patientResource.getJsonContent();
        List<DataCategory> allowedCategories = Arrays.asList(DataCategory.DEMOGRAPHICS);

        // Act
        String result = consentDataFilterService.filterJsonContent(originalJson, allowedCategories, "Patient");

        // Assert
        assertNotNull(result);
        assertNotEquals(originalJson, result);
        // Should still contain demographic fields
        assertTrue(result.contains("name"));
        assertTrue(result.contains("birthDate"));
    }

    @Test
    void filterJsonContent_InvalidInput_ReturnsOriginal() {
        // Test null JSON
        String result1 = consentDataFilterService.filterJsonContent(null, Arrays.asList(DataCategory.ALL), "Patient");
        assertNull(result1);

        // Test empty JSON
        String result2 = consentDataFilterService.filterJsonContent("", Arrays.asList(DataCategory.ALL), "Patient");
        assertEquals("", result2);

        // Test null categories
        String originalJson = patientResource.getJsonContent();
        String result3 = consentDataFilterService.filterJsonContent(originalJson, null, "Patient");
        assertEquals(originalJson, result3);
    }

    @Test
    void createFilteredResponse_UnfilteredResource_ReturnsUnfilteredResponse() {
        // Act
        FilteredResourceResponse result = consentDataFilterService.createFilteredResponse(
            patientResource, patientResource, activeConsentRecord);

        // Assert
        assertNotNull(result);
        assertEquals(patientResource, result.getFilteredResource());
        // When ALL categories are allowed, there might still be filtered categories detected
        // but the resource itself is unfiltered
        assertTrue(result.getAllowedCategories().contains(DataCategory.ALL));
    }

    @Test
    void createFilteredResponse_FilteredResource_ReturnsFilteredResponse() {
        // Arrange
        FhirResource filteredResource = consentDataFilterService.filterFhirResource(patientResource, restrictedConsentRecord);

        // Act
        FilteredResourceResponse result = consentDataFilterService.createFilteredResponse(
            patientResource, filteredResource, restrictedConsentRecord);

        // Assert
        assertNotNull(result);
        assertEquals(filteredResource, result.getFilteredResource());
        assertEquals(2, result.getAllowedCategories().size());
        assertTrue(result.getAllowedCategories().contains(DataCategory.DEMOGRAPHICS));
        assertTrue(result.getAllowedCategories().contains(DataCategory.LAB_RESULTS));
    }

    @Test
    void createFilteredResponse_BlockedResource_ReturnsBlockedResponse() {
        // Act
        FilteredResourceResponse result = consentDataFilterService.createFilteredResponse(
            patientResource, null, inactiveConsentRecord);

        // Assert
        assertNotNull(result);
        assertNull(result.getFilteredResource());
        assertTrue(result.getFilterReason().contains("blocked") || result.getFilterReason().contains("consent"));
    }

    @Test
    void createFilteredResponse_NullOriginalResource_ReturnsBlockedResponse() {
        // Act
        FilteredResourceResponse result = consentDataFilterService.createFilteredResponse(
            null, patientResource, activeConsentRecord);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFilterReason().contains("null"));
    }

    @Test
    void getFieldCategoryMapping_PatientResource_ReturnsCorrectMappings() {
        // Act
        Map<String, DataCategory> result = consentDataFilterService.getFieldCategoryMapping("Patient");

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(DataCategory.DEMOGRAPHICS, result.get("name"));
        assertEquals(DataCategory.DEMOGRAPHICS, result.get("birthDate"));
        assertEquals(DataCategory.EMERGENCY_CONTACTS, result.get("contact"));
    }

    @Test
    void getFieldCategoryMapping_ObservationResource_ReturnsCorrectMappings() {
        // Act
        Map<String, DataCategory> result = consentDataFilterService.getFieldCategoryMapping("Observation");

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(DataCategory.LAB_RESULTS, result.get("code"));
        assertEquals(DataCategory.LAB_RESULTS, result.get("value"));
        assertEquals(DataCategory.CLINICAL_NOTES, result.get("note"));
    }

    @Test
    void getFieldCategoryMapping_UnknownResource_ReturnsEmptyMap() {
        // Act
        Map<String, DataCategory> result = consentDataFilterService.getFieldCategoryMapping("UnknownResource");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Helper methods to create sample JSON content
    private String createPatientJson() {
        return """
            {
              "resourceType": "Patient",
              "id": "patient-123",
              "name": [
                {
                  "family": "Doe",
                  "given": ["John"]
                }
              ],
              "telecom": [
                {
                  "system": "phone",
                  "value": "555-1234"
                }
              ],
              "birthDate": "1980-01-01",
              "gender": "male",
              "address": [
                {
                  "line": ["123 Main St"],
                  "city": "Anytown",
                  "state": "CA",
                  "postalCode": "12345"
                }
              ],
              "contact": [
                {
                  "name": {
                    "family": "Doe",
                    "given": ["Jane"]
                  },
                  "telecom": [
                    {
                      "system": "phone",
                      "value": "555-5678"
                    }
                  ]
                }
              ]
            }
            """;
    }

    private String createObservationJson() {
        return """
            {
              "resourceType": "Observation",
              "id": "obs-456",
              "status": "final",
              "code": {
                "coding": [
                  {
                    "system": "http://loinc.org",
                    "code": "33747-0",
                    "display": "General appearance of patient"
                  }
                ]
              },
              "subject": {
                "reference": "Patient/patient-123"
              },
              "valueString": "Patient appears well",
              "note": [
                {
                  "text": "Patient is in good health"
                }
              ]
            }
            """;
    }
}