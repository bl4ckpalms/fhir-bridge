package com.bridge.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validating synthetic HIPAA-compliant test data.
 * Ensures all test data is properly formatted and accessible.
 */
@SpringBootTest
class TestDataLoaderTest {

    @Test
    @DisplayName("Should load standard synthetic patients successfully")
    void testLoadStandardPatients() {
        List<Map<String, Object>> patients = TestDataLoader.loadStandardPatients();
        
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        assertEquals(20, patients.size(), "Should have 20 standard patients");
        
        // Validate first patient structure
        Map<String, Object> firstPatient = patients.get(0);
        assertNotNull(firstPatient.get("patientId"));
        assertNotNull(firstPatient.get("medicalRecordNumber"));
        assertNotNull(firstPatient.get("firstName"));
        assertNotNull(firstPatient.get("lastName"));
        assertNotNull(firstPatient.get("dateOfBirth"));
        assertNotNull(firstPatient.get("gender"));
        assertNotNull(firstPatient.get("address"));
        assertNotNull(firstPatient.get("city"));
        assertNotNull(firstPatient.get("state"));
        assertNotNull(firstPatient.get("zipCode"));
        assertNotNull(firstPatient.get("phoneNumber"));
        assertNotNull(firstPatient.get("email"));
        assertNotNull(firstPatient.get("maritalStatus"));
        assertNotNull(firstPatient.get("race"));
        assertNotNull(firstPatient.get("ethnicity"));
        assertNotNull(firstPatient.get("language"));
        assertNotNull(firstPatient.get("emergencyContact"));
        assertNotNull(firstPatient.get("insurance"));
    }

    @Test
    @DisplayName("Should load pediatric patients successfully")
    void testLoadPediatricPatients() {
        List<Map<String, Object>> patients = TestDataLoader.loadPediatricPatients();
        
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        assertEquals(5, patients.size(), "Should have 5 pediatric patients");
        
        // Validate pediatric-specific fields
        Map<String, Object> pediatricPatient = patients.get(0);
        assertNotNull(pediatricPatient.get("guardian"));
        assertTrue(pediatricPatient.containsKey("birthWeight") || 
                  pediatricPatient.containsKey("allergies") || 
                  pediatricPatient.containsKey("schoolInfo"));
    }

    @Test
    @DisplayName("Should load geriatric patients successfully")
    void testLoadGeriatricPatients() {
        List<Map<String, Object>> patients = TestDataLoader.loadGeriatricPatients();
        
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        assertEquals(5, patients.size(), "Should have 5 geriatric patients");
        
        // Validate geriatric-specific fields
        Map<String, Object> geriatricPatient = patients.get(0);
        assertNotNull(geriatricPatient.get("chronicConditions"));
        assertNotNull(geriatricPatient.get("medications"));
        assertNotNull(geriatricPatient.get("cognitiveStatus"));
        
        // Verify Medicare insurance
        Map<String, Object> insurance = (Map<String, Object>) geriatricPatient.get("insurance");
        assertEquals("Medicare", insurance.get("provider"));
    }

    @Test
    @DisplayName("Should load complex medical patients successfully")
    void testLoadComplexMedicalPatients() {
        List<Map<String, Object>> patients = TestDataLoader.loadComplexMedicalPatients();
        
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        assertEquals(5, patients.size(), "Should have 5 complex medical patients");
        
        // Validate complex medical fields
        Map<String, Object> complexPatient = patients.get(0);
        assertNotNull(complexPatient.get("chronicConditions"));
        assertNotNull(complexPatient.get("medications"));
        assertNotNull(complexPatient.get("specialists"));
        
        List<String> chronicConditions = (List<String>) complexPatient.get("chronicConditions");
        assertTrue(chronicConditions.size() >= 3, "Complex patients should have multiple chronic conditions");
    }

    @Test
    @DisplayName("Should load diverse demographic patients successfully")
    void testLoadDiverseDemographicPatients() {
        List<Map<String, Object>> patients = TestDataLoader.loadDiverseDemographicPatients();
        
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        assertEquals(10, patients.size(), "Should have 10 diverse demographic patients");
        
        // Validate diversity in languages
        long uniqueLanguages = patients.stream()
                .map(patient -> (String) patient.get("language"))
                .distinct()
                .count();
        assertTrue(uniqueLanguages >= 5, "Should have diverse language representation");
        
        // Validate cultural considerations
        Map<String, Object> diversePatient = patients.get(0);
        assertTrue(diversePatient.containsKey("culturalConsiderations") ||
                  diversePatient.containsKey("religion") ||
                  diversePatient.containsKey("tribe") ||
                  diversePatient.containsKey("countryOfOrigin"));
    }

    @Test
    @DisplayName("Should load all patients from all categories")
    void testLoadAllPatients() {
        List<Map<String, Object>> allPatients = TestDataLoader.loadAllPatients();
        
        assertNotNull(allPatients);
        assertFalse(allPatients.isEmpty());
        assertEquals(45, allPatients.size(), "Should have 45 total patients (20+5+5+5+10)");
        
        // Verify no duplicate patient IDs
        long uniquePatientIds = allPatients.stream()
                .map(patient -> (String) patient.get("patientId"))
                .distinct()
                .count();
        assertEquals(45, uniquePatientIds, "All patient IDs should be unique");
    }

    @Test
    @DisplayName("Should find specific patient by ID")
    void testGetPatientById() {
        Optional<Map<String, Object>> patient = TestDataLoader.getPatientById("PAT-000001");
        
        assertTrue(patient.isPresent());
        assertEquals("PAT-000001", patient.get().get("patientId"));
        assertEquals("Alex", patient.get().get("firstName"));
        assertEquals("Smith", patient.get().get("lastName"));
    }

    @Test
    @DisplayName("Should filter patients by gender")
    void testGetPatientsByGender() {
        List<Map<String, Object>> malePatients = TestDataLoader.getPatientsByGender("M");
        List<Map<String, Object>> femalePatients = TestDataLoader.getPatientsByGender("F");
        List<Map<String, Object>> otherPatients = TestDataLoader.getPatientsByGender("O");
        
        assertFalse(malePatients.isEmpty());
        assertFalse(femalePatients.isEmpty());
        assertFalse(otherPatients.isEmpty());
        
        // Verify all returned patients have correct gender
        assertTrue(malePatients.stream().allMatch(p -> "M".equals(p.get("gender"))));
        assertTrue(femalePatients.stream().allMatch(p -> "F".equals(p.get("gender"))));
        assertTrue(otherPatients.stream().allMatch(p -> "O".equals(p.get("gender"))));
    }

    @Test
    @DisplayName("Should filter patients by race")
    void testGetPatientsByRace() {
        List<Map<String, Object>> whitePatients = TestDataLoader.getPatientsByRace("White");
        List<Map<String, Object>> hispanicPatients = TestDataLoader.getPatientsByRace("Hispanic");
        List<Map<String, Object>> blackPatients = TestDataLoader.getPatientsByRace("Black");
        List<Map<String, Object>> asianPatients = TestDataLoader.getPatientsByRace("Asian");
        
        assertFalse(whitePatients.isEmpty());
        assertFalse(hispanicPatients.isEmpty());
        assertFalse(blackPatients.isEmpty());
        assertFalse(asianPatients.isEmpty());
        
        // Verify racial diversity
        assertTrue(whitePatients.stream().allMatch(p -> "White".equals(p.get("race"))));
        assertTrue(hispanicPatients.stream().allMatch(p -> "Hispanic".equals(p.get("race"))));
    }

    @Test
    @DisplayName("Should filter patients by state")
    void testGetPatientsByState() {
        List<Map<String, Object>> californiaPatients = TestDataLoader.getPatientsByState("CA");
        List<Map<String, Object>> texasPatients = TestDataLoader.getPatientsByState("TX");
        List<Map<String, Object>> floridaPatients = TestDataLoader.getPatientsByState("FL");
        
        assertFalse(californiaPatients.isEmpty());
        assertFalse(texasPatients.isEmpty());
        assertFalse(floridaPatients.isEmpty());
        
        // Verify geographic distribution
        assertTrue(californiaPatients.stream().allMatch(p -> "CA".equals(p.get("state"))));
        assertTrue(texasPatients.stream().allMatch(p -> "TX".equals(p.get("state"))));
    }

    @Test
    @DisplayName("Should load consent records successfully")
    void testLoadConsentRecords() {
        List<Map<String, Object>> consents = TestDataLoader.loadConsentRecords();
        
        assertNotNull(consents);
        assertFalse(consents.isEmpty());
        
        // Validate consent record structure
        Map<String, Object> consent = consents.get(0);
        assertNotNull(consent.get("patientId"));
        assertNotNull(consent.get("organizationId"));
        assertNotNull(consent.get("status"));
        assertNotNull(consent.get("effectiveDate"));
    }

    @Test
    @DisplayName("Should load observations successfully")
    void testLoadObservations() {
        List<Map<String, Object>> observations = TestDataLoader.loadObservations();
        
        assertNotNull(observations);
        assertFalse(observations.isEmpty());
        
        // Validate observation structure
        Map<String, Object> observation = observations.get(0);
        assertNotNull(observation.get("patientId"));
        assertNotNull(observation.get("observationId"));
        assertNotNull(observation.get("loincCode"));
        assertNotNull(observation.get("testName"));
        assertNotNull(observation.get("value"));
        assertNotNull(observation.get("unit"));
    }

    @Test
    @DisplayName("Should get consent records for specific patient")
    void testGetConsentRecordsForPatient() {
        List<Map<String, Object>> consents = TestDataLoader.getConsentRecordsForPatient("PAT-000001");
        
        assertNotNull(consents);
        // Verify all returned consents are for the correct patient
        assertTrue(consents.stream().allMatch(c -> "PAT-000001".equals(c.get("patientId"))));
    }

    @Test
    @DisplayName("Should get random patient")
    void testGetRandomPatient() {
        Map<String, Object> randomPatient1 = TestDataLoader.getRandomPatient();
        Map<String, Object> randomPatient2 = TestDataLoader.getRandomPatient();
        
        assertNotNull(randomPatient1);
        assertNotNull(randomPatient2);
        assertNotNull(randomPatient1.get("patientId"));
        assertNotNull(randomPatient2.get("patientId"));
    }

    @Test
    @DisplayName("Should get patients for transformation testing")
    void testGetPatientsForTransformationTesting() {
        List<Map<String, Object>> patients = TestDataLoader.getPatientsForTransformationTesting();
        
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        
        // Verify all patients have required fields for transformation
        for (Map<String, Object> patient : patients) {
            assertNotNull(patient.get("firstName"));
            assertNotNull(patient.get("lastName"));
            assertNotNull(patient.get("dateOfBirth"));
            assertNotNull(patient.get("gender"));
            assertNotNull(patient.get("address"));
        }
    }

    @Test
    @DisplayName("Should get patients for consent testing")
    void testGetPatientsForConsentTesting() {
        List<Map<String, Object>> patients = TestDataLoader.getPatientsForConsentTesting();
        
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        
        // Verify these patients have corresponding consent records
        for (Map<String, Object> patient : patients) {
            String patientId = (String) patient.get("patientId");
            List<Map<String, Object>> consents = TestDataLoader.getConsentRecordsForPatient(patientId);
            assertFalse(consents.isEmpty(), "Patient " + patientId + " should have consent records");
        }
    }

    @Test
    @DisplayName("Should generate patient data statistics")
    void testGetPatientDataStatistics() {
        Map<String, Object> stats = TestDataLoader.getPatientDataStatistics();
        
        assertNotNull(stats);
        assertEquals(45, stats.get("totalPatients"));
        assertNotNull(stats.get("genderDistribution"));
        assertNotNull(stats.get("raceDistribution"));
        assertNotNull(stats.get("stateDistribution"));
        
        Map<String, Long> genderDist = (Map<String, Long>) stats.get("genderDistribution");
        assertTrue(genderDist.containsKey("M"));
        assertTrue(genderDist.containsKey("F"));
        assertTrue(genderDist.containsKey("O"));
    }

    @Test
    @DisplayName("Should validate all test data successfully")
    void testValidateTestData() {
        boolean isValid = TestDataLoader.validateTestData();
        assertTrue(isValid, "All test data should be valid and properly formatted");
    }

    @Test
    @DisplayName("Should demonstrate HIPAA compliance of test data")
    void testHipaaCompliance() {
        List<Map<String, Object>> allPatients = TestDataLoader.loadAllPatients();
        
        // Verify all data is synthetic (no real SSNs, real addresses, etc.)
        for (Map<String, Object> patient : allPatients) {
            String patientId = (String) patient.get("patientId");
            String mrn = (String) patient.get("medicalRecordNumber");
            String email = (String) patient.get("email");
            String phone = (String) patient.get("phoneNumber");
            
            // Verify synthetic patterns
            assertTrue(patientId.startsWith("PAT-"), "Patient ID should follow synthetic pattern");
            assertTrue(mrn.startsWith("MRN-"), "MRN should follow synthetic pattern");
            assertTrue(email.contains("@email.example"), "Email should use example domain");
            assertTrue(phone.startsWith("(555)"), "Phone should use synthetic area code");
        }
        
        // Verify no real PHI indicators
        String allDataAsString = allPatients.toString().toLowerCase();
        assertFalse(allDataAsString.contains("real"), "Should not contain 'real' indicators");
        assertFalse(allDataAsString.contains("actual"), "Should not contain 'actual' indicators");
    }

    @Test
    @DisplayName("Should demonstrate data diversity and representation")
    void testDataDiversityAndRepresentation() {
        List<Map<String, Object>> allPatients = TestDataLoader.loadAllPatients();
        
        // Verify age diversity (birth years from different decades)
        long uniqueBirthYears = allPatients.stream()
                .map(patient -> ((String) patient.get("dateOfBirth")).substring(0, 4))
                .distinct()
                .count();
        assertTrue(uniqueBirthYears >= 10, "Should have patients from multiple decades");
        
        // Verify geographic diversity
        long uniqueStates = allPatients.stream()
                .map(patient -> (String) patient.get("state"))
                .distinct()
                .count();
        assertTrue(uniqueStates >= 15, "Should have patients from multiple states");
        
        // Verify racial diversity
        long uniqueRaces = allPatients.stream()
                .map(patient -> (String) patient.get("race"))
                .distinct()
                .count();
        assertTrue(uniqueRaces >= 7, "Should have diverse racial representation");
        
        // Verify language diversity
        long uniqueLanguages = allPatients.stream()
                .map(patient -> (String) patient.get("language"))
                .distinct()
                .count();
        assertTrue(uniqueLanguages >= 5, "Should have diverse language representation");
    }
}