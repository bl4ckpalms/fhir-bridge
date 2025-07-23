package com.bridge.testdata;

import com.bridge.model.ConsentRecord;
import com.bridge.service.PatientData;
import com.bridge.service.ObservationData;
import com.bridge.service.OrderData;
import com.bridge.service.VisitData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Utility class for loading synthetic test data from JSON files
 * All data is HIPAA-compliant synthetic data for testing purposes
 */
public class TestDataLoader {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    /**
     * Load synthetic patient data from JSON file
     */
    public static List<PatientData> loadPatients() throws IOException {
        return loadFromResource("test-data/patients/synthetic-patients.json", 
                new TypeReference<List<PatientData>>() {});
    }
    
    /**
     * Load synthetic consent records from JSON file
     */
    public static List<ConsentRecord> loadConsentRecords() throws IOException {
        return loadFromResource("test-data/consent/synthetic-consents.json", 
                new TypeReference<List<ConsentRecord>>() {});
    }
    
    /**
     * Load synthetic observation data from JSON file
     */
    public static List<ObservationData> loadObservations() throws IOException {
        return loadFromResource("test-data/observations/synthetic-observations.json", 
                new TypeReference<List<ObservationData>>() {});
    }
    
    /**
     * Load synthetic order data from JSON file
     */
    public static List<OrderData> loadOrders() throws IOException {
        return loadFromResource("test-data/orders/synthetic-orders.json", 
                new TypeReference<List<OrderData>>() {});
    }
    
    /**
     * Load synthetic visit data from JSON file
     */
    public static List<VisitData> loadVisits() throws IOException {
        return loadFromResource("test-data/visits/synthetic-visits.json", 
                new TypeReference<List<VisitData>>() {});
    }
    
    /**
     * Load HL7 v2 message content from file
     */
    public static String loadHl7Message(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource("test-data/hl7-messages/" + filename);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes());
        }
    }
    
    /**
     * Load FHIR resource JSON from file
     */
    public static String loadFhirResource(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource("test-data/fhir-resources/" + filename);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes());
        }
    }
    
    /**
     * Get a specific patient by ID for testing
     */
    public static PatientData getPatientById(String patientId) throws IOException {
        List<PatientData> patients = loadPatients();
        return patients.stream()
                .filter(p -> patientId.equals(p.getPatientId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get consent records for a specific patient
     */
    public static List<ConsentRecord> getConsentRecordsForPatient(String patientId) throws IOException {
        List<ConsentRecord> consents = loadConsentRecords();
        return consents.stream()
                .filter(c -> patientId.equals(c.getPatientId()))
                .toList();
    }
    
    /**
     * Get active consent records for a specific patient and organization
     */
    public static List<ConsentRecord> getActiveConsentRecords(String patientId, String organizationId) throws IOException {
        List<ConsentRecord> consents = loadConsentRecords();
        return consents.stream()
                .filter(c -> patientId.equals(c.getPatientId()) && 
                           organizationId.equals(c.getOrganizationId()) &&
                           c.isActive())
                .toList();
    }
    
    private static <T> T loadFromResource(String resourcePath, TypeReference<T> typeReference) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        }
    }
}