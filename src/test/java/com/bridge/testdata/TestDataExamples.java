package com.bridge.testdata;

import com.bridge.model.ConsentRecord;
import com.bridge.model.ConsentStatus;
import com.bridge.model.DataCategory;
import com.bridge.service.PatientData;
import com.bridge.service.ObservationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example test class demonstrating how to use the synthetic test data
 * This serves as both documentation and validation of test data usage patterns
 */
class TestDataExamples {
    
    @Test
    @DisplayName("Example: Loading and using patient data")
    void examplePatientDataUsage() throws IOException {
        // Load all patients
        List<PatientData> patients = TestDataLoader.loadPatients();
        assertFalse(patients.isEmpty(), "Should have patient data");
        
        // Get a specific patient for testing
        PatientData testPatient = TestDataLoader.getPatientById("PAT-000001");
        assertNotNull(testPatient, "Should find test patient");
        assertEquals("Alex", testPatient.getFirstName());
        assertEquals("Smith", testPatient.getLastName());
        
        // Use patient data for testing demographics
        assertEquals("M", testPatient.getGender());
        assertEquals("Springfield", testPatient.getCity());
        assertEquals("CA", testPatient.getState());
        
        System.out.println("Test Patient: " + testPatient.getFullName() + 
                          " (ID: " + testPatient.getPatientId() + ")");
    }
    
    @Test
    @DisplayName("Example: Working with consent records")
    void exampleConsentDataUsage() throws IOException {
        // Load consent records for a specific patient
        List<ConsentRecord> consents = TestDataLoader.getConsentRecordsForPatient("PAT-000001");
        assertFalse(consents.isEmpty(), "Patient should have consent records");
        
        // Find active consents
        List<ConsentRecord> activeConsents = consents.stream()
                .filter(ConsentRecord::isActive)
                .toList();
        
        // Test consent scenarios
        for (ConsentRecord consent : activeConsents) {
            assertTrue(consent.getStatus() == ConsentStatus.ACTIVE);
            assertFalse(consent.getAllowedCategories().isEmpty());
            
            // Check if patient has consented to specific data categories
            boolean allowsLabResults = consent.getAllowedCategories().contains(DataCategory.LAB_RESULTS) ||
                                     consent.getAllowedCategories().contains(DataCategory.ALL);
            
            System.out.println("Consent for " + consent.getOrganizationId() + 
                              " allows lab results: " + allowsLabResults);
        }
    }
    
    @Test
    @DisplayName("Example: Using observation data for testing")
    void exampleObservationDataUsage() throws IOException {
        List<ObservationData> observations = TestDataLoader.loadObservations();
        assertFalse(observations.isEmpty(), "Should have observation data");
        
        // Find abnormal results for testing alert scenarios
        List<ObservationData> abnormalResults = observations.stream()
                .filter(obs -> obs.getAbnormalFlags() != null)
                .toList();
        
        assertFalse(abnormalResults.isEmpty(), "Should have some abnormal results for testing");
        
        // Test specific observation types
        List<ObservationData> hemoglobinResults = observations.stream()
                .filter(obs -> "Hemoglobin".equals(obs.getObservationName()))
                .toList();
        
        assertFalse(hemoglobinResults.isEmpty(), "Should have hemoglobin results");
        
        for (ObservationData hgb : hemoglobinResults) {
            assertNotNull(hgb.getValue());
            assertEquals("g/dL", hgb.getUnits());
            assertEquals("12.0-15.5", hgb.getReferenceRange());
            
            System.out.println("Hemoglobin: " + hgb.getValue() + " " + hgb.getUnits() + 
                              (hgb.getAbnormalFlags() != null ? " (" + hgb.getAbnormalFlags() + ")" : ""));
        }
    }
    
    @Test
    @DisplayName("Example: Loading HL7 messages for transformation testing")
    void exampleHl7MessageUsage() throws IOException {
        // Load different types of HL7 messages
        String adtMessage = TestDataLoader.loadHl7Message("sample-adt-a01.hl7");
        String ormMessage = TestDataLoader.loadHl7Message("sample-orm-o01.hl7");
        String oruMessage = TestDataLoader.loadHl7Message("sample-oru-r01.hl7");
        
        // Validate message structure
        assertTrue(adtMessage.startsWith("MSH|"), "ADT message should start with MSH segment");
        assertTrue(adtMessage.contains("PID|"), "ADT message should contain patient information");
        assertTrue(adtMessage.contains("PAT-000001"), "ADT message should reference test patient");
        
        assertTrue(ormMessage.contains("ORC|"), "ORM message should contain order control");
        assertTrue(ormMessage.contains("OBR|"), "ORM message should contain order request");
        
        assertTrue(oruMessage.contains("OBX|"), "ORU message should contain observation results");
        
        System.out.println("Loaded HL7 messages:");
        System.out.println("- ADT message: " + adtMessage.split("\\r?\\n")[0]);
        System.out.println("- ORM message: " + ormMessage.split("\\r?\\n")[0]);
        System.out.println("- ORU message: " + oruMessage.split("\\r?\\n")[0]);
    }
    
    @Test
    @DisplayName("Example: Loading FHIR resources for validation")
    void exampleFhirResourceUsage() throws IOException {
        String patientResource = TestDataLoader.loadFhirResource("sample-patient-fhir.json");
        String observationResource = TestDataLoader.loadFhirResource("sample-observation-fhir.json");
        
        // Validate FHIR resource structure
        assertTrue(patientResource.contains("\"resourceType\": \"Patient\""));
        assertTrue(patientResource.contains("\"id\": \"PAT-000001\""));
        assertTrue(patientResource.contains("\"family\": \"Smith\""));
        
        assertTrue(observationResource.contains("\"resourceType\": \"Observation\""));
        assertTrue(observationResource.contains("\"code\": \"33747-0\""));
        assertTrue(observationResource.contains("\"display\": \"Hemoglobin\""));
        
        System.out.println("FHIR resources loaded successfully");
        System.out.println("- Patient resource contains demographics and identifiers");
        System.out.println("- Observation resource contains lab result with LOINC coding");
    }
    
    @Test
    @DisplayName("Example: Testing data relationships")
    void exampleDataRelationships() throws IOException {
        // Load related data
        List<PatientData> patients = TestDataLoader.loadPatients();
        List<ConsentRecord> consents = TestDataLoader.loadConsentRecords();
        
        // Verify relationships
        PatientData patient = patients.get(0);
        List<ConsentRecord> patientConsents = TestDataLoader.getConsentRecordsForPatient(patient.getPatientId());
        
        assertFalse(patientConsents.isEmpty(), "Patient should have consent records");
        
        // Test consent-based data access scenario
        ConsentRecord activeConsent = patientConsents.stream()
                .filter(ConsentRecord::isActive)
                .findFirst()
                .orElse(null);
        
        if (activeConsent != null) {
            boolean canAccessLabResults = activeConsent.getAllowedCategories().contains(DataCategory.LAB_RESULTS) ||
                                        activeConsent.getAllowedCategories().contains(DataCategory.ALL);
            
            System.out.println("Patient " + patient.getPatientId() + 
                              " consent allows lab results: " + canAccessLabResults);
            
            // This would be used in actual consent checking logic
            if (canAccessLabResults) {
                List<ObservationData> observations = TestDataLoader.loadObservations();
                System.out.println("Would return " + observations.size() + " lab results");
            } else {
                System.out.println("Would filter out lab results due to consent restrictions");
            }
        }
    }
}