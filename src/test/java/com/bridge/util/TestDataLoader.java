package com.bridge.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class for loading synthetic HIPAA-compliant test data.
 * All data is completely synthetic and safe for development/testing environments.
 */
public class TestDataLoader {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    // Patient Data Loading Methods
    
    /**
     * Load all standard synthetic patients
     */
    public static List<Map<String, Object>> loadStandardPatients() {
        return loadJsonData("test-data/patients/synthetic-patients.json");
    }
    
    /**
     * Load pediatric patients (ages 0-18)
     */
    public static List<Map<String, Object>> loadPediatricPatients() {
        return loadJsonData("test-data/patients/pediatric-patients.json");
    }
    
    /**
     * Load geriatric patients (ages 65+)
     */
    public static List<Map<String, Object>> loadGeriatricPatients() {
        return loadJsonData("test-data/patients/geriatric-patients.json");
    }
    
    /**
     * Load patients with complex medical conditions
     */
    public static List<Map<String, Object>> loadComplexMedicalPatients() {
        return loadJsonData("test-data/patients/complex-medical-patients.json");
    }
    
    /**
     * Load patients with diverse demographic backgrounds
     */
    public static List<Map<String, Object>> loadDiverseDemographicPatients() {
        return loadJsonData("test-data/patients/diverse-demographics.json");
    }
    
    /**
     * Load all patients from all categories
     */
    public static List<Map<String, Object>> loadAllPatients() {
        List<Map<String, Object>> allPatients = loadStandardPatients();
        allPatients.addAll(loadPediatricPatients());
        allPatients.addAll(loadGeriatricPatients());
        allPatients.addAll(loadComplexMedicalPatients());
        allPatients.addAll(loadDiverseDemographicPatients());
        return allPatients;
    }
    
    /**
     * Get a specific patient by ID from all patient datasets
     */
    public static Optional<Map<String, Object>> getPatientById(String patientId) {
        return loadAllPatients().stream()
                .filter(patient -> patientId.equals(patient.get("patientId")))
                .findFirst();
    }
    
    /**
     * Get patients by gender
     */
    public static List<Map<String, Object>> getPatientsByGender(String gender) {
        return loadAllPatients().stream()
                .filter(patient -> gender.equals(patient.get("gender")))
                .collect(Collectors.toList());
    }
    
    /**
     * Get patients by race
     */
    public static List<Map<String, Object>> getPatientsByRace(String race) {
        return loadAllPatients().stream()
                .filter(patient -> race.equals(patient.get("race")))
                .collect(Collectors.toList());
    }
    
    /**
     * Get patients by state
     */
    public static List<Map<String, Object>> getPatientsByState(String state) {
        return loadAllPatients().stream()
                .filter(patient -> state.equals(patient.get("state")))
                .collect(Collectors.toList());
    }
    
    /**
     * Get patients by marital status
     */
    public static List<Map<String, Object>> getPatientsByMaritalStatus(String maritalStatus) {
        return loadAllPatients().stream()
                .filter(patient -> maritalStatus.equals(patient.get("maritalStatus")))
                .collect(Collectors.toList());
    }
    
    // Other Test Data Loading Methods
    
    /**
     * Load synthetic consent records
     */
    public static List<Map<String, Object>> loadConsentRecords() {
        return loadJsonData("test-data/consent/synthetic-consents.json");
    }
    
    /**
     * Load synthetic observation data
     */
    public static List<Map<String, Object>> loadObservations() {
        return loadJsonData("test-data/observations/synthetic-observations.json");
    }
    
    /**
     * Load synthetic order data
     */
    public static List<Map<String, Object>> loadOrders() {
        return loadJsonData("test-data/orders/synthetic-orders.json");
    }
    
    /**
     * Load synthetic visit data
     */
    public static List<Map<String, Object>> loadVisits() {
        return loadJsonData("test-data/visits/synthetic-visits.json");
    }
    
    /**
     * Load synthetic user data
     */
    public static List<Map<String, Object>> loadUsers() {
        return loadJsonData("test-data/users/synthetic-users.json");
    }
    
    /**
     * Get consent records for a specific patient
     */
    public static List<Map<String, Object>> getConsentRecordsForPatient(String patientId) {
        return loadConsentRecords().stream()
                .filter(consent -> patientId.equals(consent.get("patientId")))
                .collect(Collectors.toList());
    }
    
    /**
     * Get observations for a specific patient
     */
    public static List<Map<String, Object>> getObservationsForPatient(String patientId) {
        return loadObservations().stream()
                .filter(observation -> patientId.equals(observation.get("patientId")))
                .collect(Collectors.toList());
    }
    
    /**
     * Get orders for a specific patient
     */
    public static List<Map<String, Object>> getOrdersForPatient(String patientId) {
        return loadOrders().stream()
                .filter(order -> patientId.equals(order.get("patientId")))
                .collect(Collectors.toList());
    }
    
    /**
     * Get visits for a specific patient
     */
    public static List<Map<String, Object>> getVisitsForPatient(String patientId) {
        return loadVisits().stream()
                .filter(visit -> patientId.equals(visit.get("patientId")))
                .collect(Collectors.toList());
    }
    
    // HL7 and FHIR Resource Loading Methods
    
    /**
     * Load HL7 v2 message from file
     */
    public static String loadHl7Message(String filename) {
        return loadTextResource("test-data/hl7-messages/" + filename);
    }
    
    /**
     * Load FHIR resource from file
     */
    public static String loadFhirResource(String filename) {
        return loadTextResource("test-data/fhir-resources/" + filename);
    }
    
    // Utility Methods for Test Scenarios
    
    /**
     * Get a random patient from all datasets
     */
    public static Map<String, Object> getRandomPatient() {
        List<Map<String, Object>> allPatients = loadAllPatients();
        int randomIndex = (int) (Math.random() * allPatients.size());
        return allPatients.get(randomIndex);
    }
    
    /**
     * Get patients suitable for transformation testing (have complete data)
     */
    public static List<Map<String, Object>> getPatientsForTransformationTesting() {
        return loadAllPatients().stream()
                .filter(patient -> 
                    patient.containsKey("firstName") &&
                    patient.containsKey("lastName") &&
                    patient.containsKey("dateOfBirth") &&
                    patient.containsKey("gender") &&
                    patient.containsKey("address")
                )
                .collect(Collectors.toList());
    }
    
    /**
     * Get patients suitable for consent testing
     */
    public static List<Map<String, Object>> getPatientsForConsentTesting() {
        List<String> patientIdsWithConsent = loadConsentRecords().stream()
                .map(consent -> (String) consent.get("patientId"))
                .distinct()
                .collect(Collectors.toList());
        
        return loadAllPatients().stream()
                .filter(patient -> patientIdsWithConsent.contains(patient.get("patientId")))
                .collect(Collectors.toList());
    }
    
    /**
     * Get patients suitable for security testing (various roles and permissions)
     */
    public static List<Map<String, Object>> getPatientsForSecurityTesting() {
        return loadAllPatients().stream()
                .limit(10) // Limit for focused security testing
                .collect(Collectors.toList());
    }
    
    // Data Statistics and Validation Methods
    
    /**
     * Get statistics about the test patient data
     */
    public static Map<String, Object> getPatientDataStatistics() {
        List<Map<String, Object>> allPatients = loadAllPatients();
        
        Map<String, Long> genderCounts = allPatients.stream()
                .collect(Collectors.groupingBy(
                    patient -> (String) patient.get("gender"),
                    Collectors.counting()
                ));
        
        Map<String, Long> raceCounts = allPatients.stream()
                .collect(Collectors.groupingBy(
                    patient -> (String) patient.get("race"),
                    Collectors.counting()
                ));
        
        Map<String, Long> stateCounts = allPatients.stream()
                .collect(Collectors.groupingBy(
                    patient -> (String) patient.get("state"),
                    Collectors.counting()
                ));
        
        return Map.of(
            "totalPatients", allPatients.size(),
            "genderDistribution", genderCounts,
            "raceDistribution", raceCounts,
            "stateDistribution", stateCounts
        );
    }
    
    /**
     * Validate that all test data is properly formatted and complete
     */
    public static boolean validateTestData() {
        try {
            // Validate all patient datasets load successfully
            loadStandardPatients();
            loadPediatricPatients();
            loadGeriatricPatients();
            loadComplexMedicalPatients();
            loadDiverseDemographicPatients();
            
            // Validate other test data
            loadConsentRecords();
            loadObservations();
            loadOrders();
            loadVisits();
            loadUsers();
            
            // Validate that all patients have required fields
            List<Map<String, Object>> allPatients = loadAllPatients();
            for (Map<String, Object> patient : allPatients) {
                if (!patient.containsKey("patientId") ||
                    !patient.containsKey("firstName") ||
                    !patient.containsKey("lastName") ||
                    !patient.containsKey("dateOfBirth") ||
                    !patient.containsKey("gender")) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Private Helper Methods
    
    private static List<Map<String, Object>> loadJsonData(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            InputStream inputStream = resource.getInputStream();
            String jsonContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            
            TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<List<Map<String, Object>>>() {};
            return objectMapper.readValue(jsonContent, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test data from: " + resourcePath, e);
        }
    }
    
    private static String loadTextResource(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            InputStream inputStream = resource.getInputStream();
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load text resource from: " + resourcePath, e);
        }
    }
}