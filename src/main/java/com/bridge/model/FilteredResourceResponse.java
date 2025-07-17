package com.bridge.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Response containing filtered FHIR resource with consent metadata
 */
public class FilteredResourceResponse {
    
    private FhirResource filteredResource;
    private List<DataCategory> allowedCategories;
    private List<DataCategory> filteredCategories;
    private List<String> filteredFields;
    private String consentReference;
    private LocalDateTime filterTimestamp;
    private boolean isPartiallyFiltered;
    private String filterReason;

    // Default constructor
    public FilteredResourceResponse() {
        this.allowedCategories = new ArrayList<>();
        this.filteredCategories = new ArrayList<>();
        this.filteredFields = new ArrayList<>();
        this.filterTimestamp = LocalDateTime.now();
        this.isPartiallyFiltered = false;
    }

    // Constructor with filtered resource
    public FilteredResourceResponse(FhirResource filteredResource) {
        this();
        this.filteredResource = filteredResource;
    }

    // Constructor with all main fields
    public FilteredResourceResponse(FhirResource filteredResource, 
                                   List<DataCategory> allowedCategories,
                                   List<DataCategory> filteredCategories) {
        this(filteredResource);
        this.allowedCategories = allowedCategories != null ? allowedCategories : new ArrayList<>();
        this.filteredCategories = filteredCategories != null ? filteredCategories : new ArrayList<>();
        this.isPartiallyFiltered = !this.filteredCategories.isEmpty();
    }

    // Static factory methods
    public static FilteredResourceResponse unfiltered(FhirResource resource, List<DataCategory> allowedCategories) {
        FilteredResourceResponse response = new FilteredResourceResponse(resource);
        response.setAllowedCategories(allowedCategories);
        response.setFilterReason("All requested data categories are allowed");
        return response;
    }

    public static FilteredResourceResponse filtered(FhirResource originalResource, 
                                                   FhirResource filteredResource,
                                                   List<DataCategory> allowedCategories,
                                                   List<DataCategory> filteredCategories,
                                                   List<String> filteredFields) {
        FilteredResourceResponse response = new FilteredResourceResponse(filteredResource, 
            allowedCategories, filteredCategories);
        response.setFilteredFields(filteredFields);
        response.setFilterReason("Some data categories were filtered based on consent preferences");
        return response;
    }

    public static FilteredResourceResponse blocked(String reason) {
        FilteredResourceResponse response = new FilteredResourceResponse();
        response.setFilterReason(reason);
        response.setPartiallyFiltered(true);
        return response;
    }

    // Getters and setters
    public FhirResource getFilteredResource() {
        return filteredResource;
    }

    public void setFilteredResource(FhirResource filteredResource) {
        this.filteredResource = filteredResource;
    }

    public List<DataCategory> getAllowedCategories() {
        return allowedCategories;
    }

    public void setAllowedCategories(List<DataCategory> allowedCategories) {
        this.allowedCategories = allowedCategories != null ? allowedCategories : new ArrayList<>();
    }

    public List<DataCategory> getFilteredCategories() {
        return filteredCategories;
    }

    public void setFilteredCategories(List<DataCategory> filteredCategories) {
        this.filteredCategories = filteredCategories != null ? filteredCategories : new ArrayList<>();
        this.isPartiallyFiltered = !this.filteredCategories.isEmpty();
    }

    public List<String> getFilteredFields() {
        return filteredFields;
    }

    public void setFilteredFields(List<String> filteredFields) {
        this.filteredFields = filteredFields != null ? filteredFields : new ArrayList<>();
    }

    public String getConsentReference() {
        return consentReference;
    }

    public void setConsentReference(String consentReference) {
        this.consentReference = consentReference;
    }

    public LocalDateTime getFilterTimestamp() {
        return filterTimestamp;
    }

    public void setFilterTimestamp(LocalDateTime filterTimestamp) {
        this.filterTimestamp = filterTimestamp;
    }

    public boolean isPartiallyFiltered() {
        return isPartiallyFiltered;
    }

    public void setPartiallyFiltered(boolean partiallyFiltered) {
        isPartiallyFiltered = partiallyFiltered;
    }

    public String getFilterReason() {
        return filterReason;
    }

    public void setFilterReason(String filterReason) {
        this.filterReason = filterReason;
    }

    // Utility methods
    public void addFilteredCategory(DataCategory category) {
        if (filteredCategories == null) {
            filteredCategories = new ArrayList<>();
        }
        if (!filteredCategories.contains(category)) {
            filteredCategories.add(category);
            this.isPartiallyFiltered = true;
        }
    }

    public void addFilteredField(String fieldPath) {
        if (filteredFields == null) {
            filteredFields = new ArrayList<>();
        }
        if (!filteredFields.contains(fieldPath)) {
            filteredFields.add(fieldPath);
        }
    }

    public boolean hasFilteredData() {
        return isPartiallyFiltered || (filteredCategories != null && !filteredCategories.isEmpty()) ||
               (filteredFields != null && !filteredFields.isEmpty());
    }

    public int getFilteredCategoryCount() {
        return filteredCategories != null ? filteredCategories.size() : 0;
    }

    public int getFilteredFieldCount() {
        return filteredFields != null ? filteredFields.size() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilteredResourceResponse that = (FilteredResourceResponse) o;
        return isPartiallyFiltered == that.isPartiallyFiltered &&
               Objects.equals(filteredResource, that.filteredResource) &&
               Objects.equals(allowedCategories, that.allowedCategories) &&
               Objects.equals(filteredCategories, that.filteredCategories) &&
               Objects.equals(consentReference, that.consentReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filteredResource, allowedCategories, filteredCategories, 
                           consentReference, isPartiallyFiltered);
    }

    @Override
    public String toString() {
        return "FilteredResourceResponse{" +
                "filteredResource=" + filteredResource +
                ", allowedCategories=" + allowedCategories +
                ", filteredCategories=" + filteredCategories +
                ", filteredFields=" + filteredFields +
                ", consentReference='" + consentReference + '\'' +
                ", filterTimestamp=" + filterTimestamp +
                ", isPartiallyFiltered=" + isPartiallyFiltered +
                ", filterReason='" + filterReason + '\'' +
                '}';
    }
}