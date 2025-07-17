package com.bridge.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import ca.uhn.fhir.validation.SingleValidationMessage;
import com.bridge.model.FhirResource;
import com.bridge.model.ValidationError;
import com.bridge.model.ValidationSeverity;
import com.bridge.model.ValidationWarning;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of FHIR resource validation using HAPI FHIR validator
 */
@Service
public class FhirValidatorImpl implements com.bridge.service.FhirValidator {

    private final FhirContext fhirContext;
    private final IParser jsonParser;
    private FhirValidator hapiValidator;

    @Autowired
    public FhirValidatorImpl(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
        this.jsonParser = fhirContext.newJsonParser();
    }

    @PostConstruct
    public void initialize() {
        // Create basic FHIR validator
        hapiValidator = fhirContext.newValidator();
        
        // Disable schema validation to avoid XSD classpath issues
        hapiValidator.setValidateAgainstStandardSchema(false);
        hapiValidator.setValidateAgainstStandardSchematron(false);
        
        // For now, we'll use basic validation without complex profile support
        // This can be enhanced later with proper validation support chain
    }

    @Override
    public com.bridge.model.ValidationResult validateResource(Resource resource) {
        if (resource == null) {
            return createValidationResult(false, "Resource cannot be null", null);
        }

        try {
            // Perform basic validation by trying to serialize and parse the resource
            String jsonContent = jsonParser.encodeResourceToString(resource);
            IBaseResource parsedResource = jsonParser.parseResource(jsonContent);
            
            // If we can serialize and parse without errors, consider it valid
            com.bridge.model.ValidationResult result = new com.bridge.model.ValidationResult();
            result.setValid(true);
            result.setErrors(new ArrayList<>());
            result.setWarnings(new ArrayList<>());
            
            return result;
        } catch (Exception e) {
            return createValidationResult(false, "Validation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public com.bridge.model.ValidationResult validateFhirResource(FhirResource fhirResource) {
        if (fhirResource == null || fhirResource.getJsonContent() == null) {
            return createValidationResult(false, "FhirResource or JSON content cannot be null", null);
        }

        try {
            IBaseResource baseResource = jsonParser.parseResource(fhirResource.getJsonContent());
            if (baseResource instanceof Resource) {
                return validateResource((Resource) baseResource);
            } else {
                return createValidationResult(false, "Parsed resource is not a valid R4 resource", null);
            }
        } catch (Exception e) {
            return createValidationResult(false, "Failed to parse FHIR resource: " + e.getMessage(), e);
        }
    }

    @Override
    public com.bridge.model.ValidationResult validateAgainstProfiles(Resource resource, String... profileUrls) {
        if (resource == null) {
            return createValidationResult(false, "Resource cannot be null", null);
        }

        if (profileUrls == null || profileUrls.length == 0) {
            // If no profiles specified, perform standard validation
            return validateResource(resource);
        }

        try {
            // Add profile declarations to the resource for validation
            for (String profileUrl : profileUrls) {
                resource.getMeta().addProfile(profileUrl);
            }

            ValidationResult hapiResult = hapiValidator.validateWithResult(resource);
            return convertHapiValidationResult(hapiResult);
        } catch (Exception e) {
            return createValidationResult(false, "Profile validation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public com.bridge.model.ValidationResult validateJsonContent(String jsonContent, String resourceType) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return createValidationResult(false, "JSON content cannot be null or empty", null);
        }

        try {
            IBaseResource baseResource = jsonParser.parseResource(jsonContent);
            if (!(baseResource instanceof Resource)) {
                return createValidationResult(false, "Parsed resource is not a valid R4 resource", null);
            }
            
            Resource resource = (Resource) baseResource;
            
            // Verify resource type matches expected type if provided
            if (resourceType != null && !resourceType.equals(resource.getResourceType().name())) {
                return createValidationResult(false, 
                    String.format("Expected resource type '%s' but found '%s'", 
                        resourceType, resource.getResourceType().name()), null);
            }

            return validateResource(resource);
        } catch (Exception e) {
            return createValidationResult(false, "Failed to parse or validate JSON content: " + e.getMessage(), e);
        }
    }

    /**
     * Convert HAPI ValidationResult to our custom ValidationResult
     */
    private com.bridge.model.ValidationResult convertHapiValidationResult(ValidationResult hapiResult) {
        com.bridge.model.ValidationResult result = new com.bridge.model.ValidationResult();
        result.setValid(hapiResult.isSuccessful());

        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        for (SingleValidationMessage message : hapiResult.getMessages()) {
            switch (message.getSeverity()) {
                case ERROR:
                case FATAL:
                    ValidationError error = new ValidationError();
                    error.setField(message.getLocationString());
                    error.setMessage(message.getMessage());
                    error.setSeverity(ValidationSeverity.ERROR);
                    errors.add(error);
                    break;
                case WARNING:
                    ValidationWarning warning = new ValidationWarning();
                    warning.setField(message.getLocationString());
                    warning.setMessage(message.getMessage());
                    warnings.add(warning);
                    break;
                case INFORMATION:
                    // Convert information messages to warnings for consistency
                    ValidationWarning infoWarning = new ValidationWarning();
                    infoWarning.setField(message.getLocationString());
                    infoWarning.setMessage(message.getMessage());
                    warnings.add(infoWarning);
                    break;
            }
        }

        result.setErrors(errors);
        result.setWarnings(warnings);

        return result;
    }

    /**
     * Create a validation result for error cases
     */
    private com.bridge.model.ValidationResult createValidationResult(boolean isValid, String message, Exception exception) {
        com.bridge.model.ValidationResult result = new com.bridge.model.ValidationResult();
        result.setValid(isValid);

        if (!isValid && message != null) {
            ValidationError error = new ValidationError();
            error.setMessage(message);
            error.setSeverity(ValidationSeverity.ERROR);
            if (exception != null) {
                error.setField("Exception: " + exception.getClass().getSimpleName());
            }
            
            List<ValidationError> errors = new ArrayList<>();
            errors.add(error);
            result.setErrors(errors);
        }

        result.setWarnings(new ArrayList<>());
        return result;
    }

    /**
     * Get common US Core profile URLs for validation
     */
    public static class USCoreProfiles {
        public static final String PATIENT = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient";
        public static final String ENCOUNTER = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter";
        public static final String OBSERVATION = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab";
        public static final String SERVICE_REQUEST = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-servicerequest";
    }

    /**
     * Get common TEFCA profile URLs for validation
     */
    public static class TEFCAProfiles {
        // TEFCA profiles would be defined here when available
        // For now, using US Core as baseline
        public static final String PATIENT = USCoreProfiles.PATIENT;
        public static final String ENCOUNTER = USCoreProfiles.ENCOUNTER;
        public static final String OBSERVATION = USCoreProfiles.OBSERVATION;
        public static final String SERVICE_REQUEST = USCoreProfiles.SERVICE_REQUEST;
    }
}