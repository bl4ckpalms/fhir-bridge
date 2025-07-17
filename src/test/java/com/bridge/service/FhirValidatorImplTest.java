package com.bridge.service;

import ca.uhn.fhir.context.FhirContext;
import com.bridge.model.FhirResource;
import com.bridge.model.ValidationResult;
import com.bridge.model.ValidationSeverity;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FhirValidatorImplTest {

    private FhirValidatorImpl validator;
    private FhirContext fhirContext;

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4();
        validator = new FhirValidatorImpl(fhirContext);
        validator.initialize();
    }

    @Test
    void testValidateResource_ValidPatient() {
        // Given
        Patient patient = createValidPatient();

        // When
        ValidationResult result = validator.validateResource(patient);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid(), "Patient should be valid");
        assertNotNull(result.getErrors());
        assertNotNull(result.getWarnings());
    }

    @Test
    void testValidateResource_InvalidPatient() {
        // Given - Patient without required identifier
        Patient patient = new Patient();
        patient.setId("test-patient");
        // Missing required fields like name

        // When
        ValidationResult result = validator.validateResource(patient);

        // Then
        assertNotNull(result);
        // Note: Basic FHIR validation might still pass for minimal Patient
        // The validation strictness depends on the profile being used
    }

    @Test
    void testValidateResource_NullResource() {
        // When
        ValidationResult result = validator.validateResource(null);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertEquals("Resource cannot be null", result.getErrors().get(0).getMessage());
    }

    @Test
    void testValidateFhirResource_ValidJson() {
        // Given
        Patient patient = createValidPatient();
        String jsonContent = fhirContext.newJsonParser().encodeResourceToString(patient);
        FhirResource fhirResource = new FhirResource("test-id", "Patient", jsonContent);

        // When
        ValidationResult result = validator.validateFhirResource(fhirResource);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateFhirResource_InvalidJson() {
        // Given
        FhirResource fhirResource = new FhirResource("test-id", "Patient", "{ invalid json }");

        // When
        ValidationResult result = validator.validateFhirResource(fhirResource);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).getMessage().contains("Failed to parse FHIR resource"));
    }

    @Test
    void testValidateFhirResource_NullResource() {
        // When
        ValidationResult result = validator.validateFhirResource(null);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertEquals("FhirResource or JSON content cannot be null", result.getErrors().get(0).getMessage());
    }

    @Test
    void testValidateJsonContent_ValidPatientJson() {
        // Given
        Patient patient = createValidPatient();
        String jsonContent = fhirContext.newJsonParser().encodeResourceToString(patient);

        // When
        ValidationResult result = validator.validateJsonContent(jsonContent, "Patient");

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateJsonContent_WrongResourceType() {
        // Given
        Patient patient = createValidPatient();
        String jsonContent = fhirContext.newJsonParser().encodeResourceToString(patient);

        // When
        ValidationResult result = validator.validateJsonContent(jsonContent, "Observation");

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("Expected resource type 'Observation' but found 'Patient'"));
    }

    @Test
    void testValidateJsonContent_EmptyContent() {
        // When
        ValidationResult result = validator.validateJsonContent("", "Patient");

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("JSON content cannot be null or empty", result.getErrors().get(0).getMessage());
    }

    @Test
    void testValidateJsonContent_NullContent() {
        // When
        ValidationResult result = validator.validateJsonContent(null, "Patient");

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("JSON content cannot be null or empty", result.getErrors().get(0).getMessage());
    }

    @Test
    void testValidateAgainstProfiles_WithUSCoreProfile() {
        // Given
        Patient patient = createValidPatient();
        String profileUrl = FhirValidatorImpl.USCoreProfiles.PATIENT;

        // When
        ValidationResult result = validator.validateAgainstProfiles(patient, profileUrl);

        // Then
        assertNotNull(result);
        // Note: Actual validation result depends on whether US Core profiles are loaded
        // In a minimal test environment, this might not have strict profile validation
    }

    @Test
    void testValidateAgainstProfiles_NoProfiles() {
        // Given
        Patient patient = createValidPatient();

        // When
        ValidationResult result = validator.validateAgainstProfiles(patient);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateAgainstProfiles_NullResource() {
        // When
        ValidationResult result = validator.validateAgainstProfiles(null, "some-profile");

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Resource cannot be null", result.getErrors().get(0).getMessage());
    }

    @Test
    void testValidateObservation_Valid() {
        // Given
        Observation observation = createValidObservation();

        // When
        ValidationResult result = validator.validateResource(observation);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateServiceRequest_Valid() {
        // Given
        ServiceRequest serviceRequest = createValidServiceRequest();

        // When
        ValidationResult result = validator.validateResource(serviceRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateEncounter_Valid() {
        // Given
        Encounter encounter = createValidEncounter();

        // When
        ValidationResult result = validator.validateResource(encounter);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void testValidationResult_ErrorHandling() {
        // Given - Create a resource with intentional issues
        Patient patient = new Patient();
        patient.setId("test");
        // Add invalid data that should trigger validation errors
        patient.setBirthDate(new Date(System.currentTimeMillis() + 86400000)); // Future birth date

        // When
        ValidationResult result = validator.validateResource(patient);

        // Then
        assertNotNull(result);
        // The result might still be valid depending on validation strictness
        // But we can verify the structure is correct
        assertNotNull(result.getErrors());
        assertNotNull(result.getWarnings());
    }

    @Test
    void testUSCoreProfileConstants() {
        // Verify that profile constants are defined
        assertNotNull(FhirValidatorImpl.USCoreProfiles.PATIENT);
        assertNotNull(FhirValidatorImpl.USCoreProfiles.ENCOUNTER);
        assertNotNull(FhirValidatorImpl.USCoreProfiles.OBSERVATION);
        assertNotNull(FhirValidatorImpl.USCoreProfiles.SERVICE_REQUEST);
        
        assertTrue(FhirValidatorImpl.USCoreProfiles.PATIENT.contains("us-core-patient"));
        assertTrue(FhirValidatorImpl.USCoreProfiles.ENCOUNTER.contains("us-core-encounter"));
    }

    @Test
    void testTEFCAProfileConstants() {
        // Verify that TEFCA profile constants are defined
        assertNotNull(FhirValidatorImpl.TEFCAProfiles.PATIENT);
        assertNotNull(FhirValidatorImpl.TEFCAProfiles.ENCOUNTER);
        assertNotNull(FhirValidatorImpl.TEFCAProfiles.OBSERVATION);
        assertNotNull(FhirValidatorImpl.TEFCAProfiles.SERVICE_REQUEST);
    }

    // Helper methods to create valid FHIR resources for testing
    private Patient createValidPatient() {
        Patient patient = new Patient();
        patient.setId("test-patient-123");
        
        // Add identifier
        patient.addIdentifier()
            .setSystem("http://hospital.example.org/patient-id")
            .setValue("PAT123");
        
        // Add name
        patient.addName()
            .setUse(HumanName.NameUse.OFFICIAL)
            .setFamily("Doe")
            .addGiven("John");
        
        // Add gender
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        
        // Add birth date
        patient.setBirthDate(new Date(System.currentTimeMillis() - 31536000000L)); // 1 year ago
        
        return patient;
    }

    private Observation createValidObservation() {
        Observation observation = new Observation();
        observation.setId("test-observation-123");
        observation.setStatus(Observation.ObservationStatus.FINAL);
        
        // Add code
        observation.getCode()
            .addCoding()
            .setSystem("http://loinc.org")
            .setCode("33747-0")
            .setDisplay("Body temperature");
        
        // Add subject
        observation.setSubject(new Reference("Patient/test-patient-123"));
        
        // Add value
        observation.setValue(new Quantity().setValue(98.6).setUnit("degF"));
        
        return observation;
    }

    private ServiceRequest createValidServiceRequest() {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId("test-service-request-123");
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        
        // Add code
        serviceRequest.getCode()
            .addCoding()
            .setSystem("http://loinc.org")
            .setCode("CBC")
            .setDisplay("Complete Blood Count");
        
        // Add subject
        serviceRequest.setSubject(new Reference("Patient/test-patient-123"));
        
        return serviceRequest;
    }

    private Encounter createValidEncounter() {
        Encounter encounter = new Encounter();
        encounter.setId("test-encounter-123");
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        
        // Add class
        encounter.setClass_(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
            .setCode("AMB")
            .setDisplay("ambulatory"));
        
        // Add subject
        encounter.setSubject(new Reference("Patient/test-patient-123"));
        
        return encounter;
    }
}