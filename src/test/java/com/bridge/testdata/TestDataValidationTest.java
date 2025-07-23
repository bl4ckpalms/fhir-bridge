package com.bridge.testdata;

import com.bridge.model.ConsentRecord;
import com.bridge.model.ConsentStatus;
import com.bridge.model.DataCategory;
import com.bridge.service.PatientData;
import com.bridge.service.ObservationData;
import com.bridge.service.OrderData;
import com.bridge.service.VisitData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to validate synthetic test data integrity and HIPAA compliance
 */
class TestDataValidationTest {
    
    @Test
    @DisplayName("Validate synthetic patient data is properly structured and HIPAA-compliant")
    void testPatientDataValidation() throws IOException {
        List<PatientData> patients = TestDataLoader.loadPatients();
        
        assertNotNull(patients, "Patient data should not be null");
        assertFalse(patients.isEmpty(), "Patient data should not be empty");
        
        for (PatientData patient : patients) {
            // Validate required fields
            assertNotNull(patient.getPatientId(), "Patient ID should not be null");
            assertNotNull(patient.getMedicalRecordNumber(), "MRN should not be null");
            assertNotNull(patient.getFirstName(), "First name should not be null");
            assertNotNull(patient.getLastName(), "Last name should not be null");
            assertNotNull(patient.getDateOfBirth(), "Date of birth should not be null");
            assertNotNull(patient.getGender(), "Gender should not be null");
            
            // Validate ID format (synthetic pattern)
            assertTrue(patient.getPatientId().startsWith("PAT-"), 
                "Patient ID should follow synthetic pattern");
            assertTrue(patient.getMedicalRecordNumber().startsWith("MRN-"), 
                "MRN should follow synthetic pattern");
            
            // Validate date of birth is reasonable
            LocalDate dob = patient.getDateOfBirth();
            assertTrue(dob.isAfter(LocalDate.of(1900, 1, 1)), 
                "Date of birth should be after 1900");
            assertTrue(dob.isBefore(LocalDate.now()), 
                "Date of birth should be in the past");
            
            // Validate gender values
            assertTrue(Set.of("M", "F", "O", "U").contains(patient.getGender()), 
                "Gender should be valid HL7 value");
            
            // Validate phone number format (if present)
            if (patient.getPhoneNumber() != null) {
                assertTrue(patient.getPhoneNumber().matches("\\(\\d{3}\\) \\d{3}-\\d{4}"), 
                    "Phone number should follow expected format");
            }
            
            // Validate zip code format (if present)
            if (patient.getZipCode() != null) {
                assertTrue(patient.getZipCode().matches("\\d{5}"), 
                    "Zip code should be 5 digits");
            }
        }
        
        // Validate uniqueness of patient IDs
        Set<String> patientIds = patients.stream()
                .map(PatientData::getPatientId)
                .collect(Collectors.toSet());
        assertEquals(patients.size(), patientIds.size(), 
            "All patient IDs should be unique");
    }
    
    @Test
    @DisplayName("Validate synthetic consent records are properly structured")
    void testConsentRecordsValidation() throws IOException {
        List<ConsentRecord> consents = TestDataLoader.loadConsentRecords();
        
        assertNotNull(consents, "Consent records should not be null");
        assertFalse(consents.isEmpty(), "Consent records should not be empty");
        
        for (ConsentRecord consent : consents) {
            // Validate required fields
            assertNotNull(consent.getPatientId(), "Patient ID should not be null");
            assertNotNull(consent.getOrganizationId(), "Organization ID should not be null");
            assertNotNull(consent.getStatus(), "Consent status should not be null");
            assertNotNull(consent.getAllowedCategories(), "Allowed categories should not be null");
            assertNotNull(consent.getEffectiveDate(), "Effective date should not be null");
            
            // Validate ID formats
            assertTrue(consent.getPatientId().startsWith("PAT-"), 
                "Patient ID should follow synthetic pattern");
            assertTrue(consent.getOrganizationId().startsWith("ORG-"), 
                "Organization ID should follow synthetic pattern");
            
            // Validate consent status is valid enum value
            assertNotNull(ConsentStatus.valueOf(consent.getStatus().name()), 
                "Consent status should be valid enum value");
            
            // Validate data categories
            assertFalse(consent.getAllowedCategories().isEmpty(), 
                "Allowed categories should not be empty");
            
            for (DataCategory category : consent.getAllowedCategories()) {
                assertNotNull(DataCategory.valueOf(category.name()), 
                    "Data category should be valid enum value");
            }
            
            // Validate date logic
            if (consent.getExpirationDate() != null) {
                assertTrue(consent.getExpirationDate().isAfter(consent.getEffectiveDate()), 
                    "Expiration date should be after effective date");
            }
            
            // Validate policy reference format
            if (consent.getPolicyReference() != null) {
                assertTrue(consent.getPolicyReference().startsWith("POLICY-"), 
                    "Policy reference should follow synthetic pattern");
            }
        }
    }
    
    @Test
    @DisplayName("Validate synthetic observation data is properly structured")
    void testObservationDataValidation() throws IOException {
        List<ObservationData> observations = TestDataLoader.loadObservations();
        
        assertNotNull(observations, "Observation data should not be null");
        assertFalse(observations.isEmpty(), "Observation data should not be empty");
        
        for (ObservationData obs : observations) {
            // Validate required fields
            assertNotNull(obs.getObservationId(), "Observation ID should not be null");
            assertNotNull(obs.getObservationCode(), "Observation code should not be null");
            assertNotNull(obs.getObservationName(), "Observation name should not be null");
            assertNotNull(obs.getValue(), "Observation value should not be null");
            assertNotNull(obs.getObservationDateTime(), "Observation date should not be null");
            
            // Validate ID format
            assertTrue(obs.getObservationId().startsWith("OBS-"), 
                "Observation ID should follow synthetic pattern");
            
            // Validate LOINC codes (basic format check)
            assertTrue(obs.getObservationCode().matches("\\d+-\\d"), 
                "Observation code should follow LOINC pattern");
            
            // Validate status values
            if (obs.getObservationStatus() != null) {
                assertTrue(Set.of("F", "P", "C", "X").contains(obs.getObservationStatus()), 
                    "Observation status should be valid HL7 value");
            }
            
            // Validate abnormal flags (if present)
            if (obs.getAbnormalFlags() != null) {
                assertTrue(Set.of("H", "L", "N", "A").contains(obs.getAbnormalFlags()), 
                    "Abnormal flags should be valid HL7 values");
            }
        }
    }
    
    @Test
    @DisplayName("Validate synthetic order data is properly structured")
    void testOrderDataValidation() throws IOException {
        List<OrderData> orders = TestDataLoader.loadOrders();
        
        assertNotNull(orders, "Order data should not be null");
        assertFalse(orders.isEmpty(), "Order data should not be empty");
        
        for (OrderData order : orders) {
            // Validate required fields
            assertNotNull(order.getOrderNumber(), "Order number should not be null");
            assertNotNull(order.getOrderCode(), "Order code should not be null");
            assertNotNull(order.getOrderName(), "Order name should not be null");
            assertNotNull(order.getOrderDateTime(), "Order date should not be null");
            
            // Validate ID formats
            assertTrue(order.getOrderNumber().startsWith("ORD-"), 
                "Order number should follow synthetic pattern");
            
            if (order.getPlacerOrderNumber() != null) {
                assertTrue(order.getPlacerOrderNumber().startsWith("PLC-"), 
                    "Placer order number should follow synthetic pattern");
            }
            
            if (order.getFillerOrderNumber() != null) {
                assertTrue(order.getFillerOrderNumber().startsWith("FIL-"), 
                    "Filler order number should follow synthetic pattern");
            }
            
            // Validate status values
            if (order.getOrderStatus() != null) {
                assertTrue(Set.of("A", "C", "P", "X").contains(order.getOrderStatus()), 
                    "Order status should be valid HL7 value");
            }
            
            // Validate priority values
            if (order.getPriority() != null) {
                assertTrue(Set.of("R", "S", "U").contains(order.getPriority()), 
                    "Priority should be valid HL7 value");
            }
        }
    }
    
    @Test
    @DisplayName("Validate synthetic visit data is properly structured")
    void testVisitDataValidation() throws IOException {
        List<VisitData> visits = TestDataLoader.loadVisits();
        
        assertNotNull(visits, "Visit data should not be null");
        assertFalse(visits.isEmpty(), "Visit data should not be empty");
        
        for (VisitData visit : visits) {
            // Validate required fields
            assertNotNull(visit.getVisitNumber(), "Visit number should not be null");
            assertNotNull(visit.getPatientClass(), "Patient class should not be null");
            
            // Validate ID format
            assertTrue(visit.getVisitNumber().startsWith("VIS-"), 
                "Visit number should follow synthetic pattern");
            
            // Validate patient class values
            assertTrue(Set.of("I", "O", "E", "N").contains(visit.getPatientClass()), 
                "Patient class should be valid HL7 value");
            
            // Validate admission type (if present)
            if (visit.getAdmissionType() != null) {
                assertTrue(Set.of("E", "R", "U", "N").contains(visit.getAdmissionType()), 
                    "Admission type should be valid HL7 value");
            }
            
            // Validate date logic
            if (visit.getAdmitDateTime() != null && visit.getDischargeDateTime() != null) {
                assertTrue(visit.getDischargeDateTime().isAfter(visit.getAdmitDateTime()), 
                    "Discharge date should be after admit date");
            }
        }
    }
    
    @Test
    @DisplayName("Validate HL7 message samples can be loaded")
    void testHl7MessageLoading() throws IOException {
        // Test loading different HL7 message types
        String adtMessage = TestDataLoader.loadHl7Message("sample-adt-a01.hl7");
        assertNotNull(adtMessage, "ADT message should not be null");
        assertTrue(adtMessage.contains("MSH|"), "ADT message should contain MSH segment");
        assertTrue(adtMessage.contains("PID|"), "ADT message should contain PID segment");
        
        String ormMessage = TestDataLoader.loadHl7Message("sample-orm-o01.hl7");
        assertNotNull(ormMessage, "ORM message should not be null");
        assertTrue(ormMessage.contains("ORC|"), "ORM message should contain ORC segment");
        assertTrue(ormMessage.contains("OBR|"), "ORM message should contain OBR segment");
        
        String oruMessage = TestDataLoader.loadHl7Message("sample-oru-r01.hl7");
        assertNotNull(oruMessage, "ORU message should not be null");
        assertTrue(oruMessage.contains("OBX|"), "ORU message should contain OBX segment");
    }
    
    @Test
    @DisplayName("Validate FHIR resource samples can be loaded")
    void testFhirResourceLoading() throws IOException {
        String patientResource = TestDataLoader.loadFhirResource("sample-patient-fhir.json");
        assertNotNull(patientResource, "Patient FHIR resource should not be null");
        assertTrue(patientResource.contains("\"resourceType\": \"Patient\""), 
            "Should be a Patient resource");
        
        String observationResource = TestDataLoader.loadFhirResource("sample-observation-fhir.json");
        assertNotNull(observationResource, "Observation FHIR resource should not be null");
        assertTrue(observationResource.contains("\"resourceType\": \"Observation\""), 
            "Should be an Observation resource");
    }
    
    @Test
    @DisplayName("Validate test data relationships and consistency")
    void testDataRelationships() throws IOException {
        List<PatientData> patients = TestDataLoader.loadPatients();
        List<ConsentRecord> consents = TestDataLoader.loadConsentRecords();
        
        // Verify that all consent records reference valid patients
        Set<String> patientIds = patients.stream()
                .map(PatientData::getPatientId)
                .collect(Collectors.toSet());
        
        for (ConsentRecord consent : consents) {
            assertTrue(patientIds.contains(consent.getPatientId()), 
                "Consent record should reference valid patient: " + consent.getPatientId());
        }
        
        // Test specific patient lookup
        PatientData patient = TestDataLoader.getPatientById("PAT-000001");
        assertNotNull(patient, "Should find patient by ID");
        assertEquals("PAT-000001", patient.getPatientId(), "Patient ID should match");
        
        // Test consent lookup for patient
        List<ConsentRecord> patientConsents = TestDataLoader.getConsentRecordsForPatient("PAT-000001");
        assertFalse(patientConsents.isEmpty(), "Patient should have consent records");
        
        for (ConsentRecord consent : patientConsents) {
            assertEquals("PAT-000001", consent.getPatientId(), 
                "All consent records should be for the correct patient");
        }
    }
}