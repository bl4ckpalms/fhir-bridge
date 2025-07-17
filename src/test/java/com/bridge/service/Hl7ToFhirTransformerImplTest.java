package com.bridge.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.bridge.model.FhirResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Hl7ToFhirTransformerImplTest {

    private Hl7ToFhirTransformerImpl transformer;
    private FhirContext fhirContext;
    private IParser jsonParser;

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4();
        jsonParser = fhirContext.newJsonParser();
        transformer = new Hl7ToFhirTransformerImpl(fhirContext);
    }

    @Test
    void testTransformToFhir_WithCompleteData() {
        // Given
        ParsedHl7Data parsedData = createCompleteHl7Data();

        // When
        List<FhirResource> resources = transformer.transformToFhir(parsedData);

        // Then
        assertNotNull(resources);
        assertEquals(4, resources.size()); // Patient, Encounter, Observation, ServiceRequest

        // Verify Patient resource
        FhirResource patientResource = resources.stream()
            .filter(r -> "Patient".equals(r.getResourceType()))
            .findFirst()
            .orElse(null);
        assertNotNull(patientResource);
        assertEquals("PAT123", patientResource.getResourceId());
        assertEquals("MSG001", patientResource.getSourceMessageId());

        // Parse and validate Patient content
        Patient patient = (Patient) jsonParser.parseResource(patientResource.getJsonContent());
        assertEquals("John", patient.getName().get(0).getGivenAsSingleString());
        assertEquals("Doe", patient.getName().get(0).getFamily());
        assertEquals(Enumerations.AdministrativeGender.MALE, patient.getGender());

        // Verify Encounter resource
        FhirResource encounterResource = resources.stream()
            .filter(r -> "Encounter".equals(r.getResourceType()))
            .findFirst()
            .orElse(null);
        assertNotNull(encounterResource);
        assertEquals("VISIT123", encounterResource.getResourceId());

        // Verify Observation resource
        FhirResource observationResource = resources.stream()
            .filter(r -> "Observation".equals(r.getResourceType()))
            .findFirst()
            .orElse(null);
        assertNotNull(observationResource);

        // Verify ServiceRequest resource
        FhirResource serviceRequestResource = resources.stream()
            .filter(r -> "ServiceRequest".equals(r.getResourceType()))
            .findFirst()
            .orElse(null);
        assertNotNull(serviceRequestResource);
    }

    @Test
    void testTransformToFhir_PatientOnly() {
        // Given
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setMessageControlId("MSG002");
        parsedData.setPatientData(createPatientData());

        // When
        List<FhirResource> resources = transformer.transformToFhir(parsedData);

        // Then
        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals("Patient", resources.get(0).getResourceType());
    }

    @Test
    void testTransformToFhirResource_Patient() {
        // Given
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setPatientData(createPatientData());

        // When
        FhirResource resource = transformer.transformToFhirResource(parsedData, "Patient");

        // Then
        assertNotNull(resource);
        assertEquals("Patient", resource.getResourceType());
        assertEquals("PAT123", resource.getResourceId());

        // Parse and validate content
        Patient patient = (Patient) jsonParser.parseResource(resource.getJsonContent());
        assertEquals("John", patient.getName().get(0).getGivenAsSingleString());
        assertEquals("Doe", patient.getName().get(0).getFamily());
        assertEquals(Enumerations.AdministrativeGender.MALE, patient.getGender());
        assertEquals(1, patient.getIdentifier().size());
        assertEquals("PAT123", patient.getIdentifier().get(0).getValue());
    }

    @Test
    void testTransformToFhirResource_Encounter() {
        // Given
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setPatientData(createPatientData());
        parsedData.setVisitData(createVisitData());

        // When
        FhirResource resource = transformer.transformToFhirResource(parsedData, "Encounter");

        // Then
        assertNotNull(resource);
        assertEquals("Encounter", resource.getResourceType());
        assertEquals("VISIT123", resource.getResourceId());

        // Parse and validate content
        Encounter encounter = (Encounter) jsonParser.parseResource(resource.getJsonContent());
        assertEquals(Encounter.EncounterStatus.FINISHED, encounter.getStatus());
        assertEquals("Patient/PAT123", encounter.getSubject().getReference());
        assertTrue(encounter.hasLocation());
    }

    @Test
    void testTransformToFhirResource_Observation() {
        // Given
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setPatientData(createPatientData());
        parsedData.addObservation(createObservationData());

        // When
        FhirResource resource = transformer.transformToFhirResource(parsedData, "Observation");

        // Then
        assertNotNull(resource);
        assertEquals("Observation", resource.getResourceType());

        // Parse and validate content
        Observation observation = (Observation) jsonParser.parseResource(resource.getJsonContent());
        assertEquals(Observation.ObservationStatus.FINAL, observation.getStatus());
        assertEquals("Patient/PAT123", observation.getSubject().getReference());
        assertEquals("33747-0", observation.getCode().getCoding().get(0).getCode());
        assertTrue(observation.getValue() instanceof Quantity);
        assertEquals(98.6, ((Quantity) observation.getValue()).getValue().doubleValue(), 0.01);
    }

    @Test
    void testTransformToFhirResource_ServiceRequest() {
        // Given
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setPatientData(createPatientData());
        parsedData.addOrder(createOrderData());

        // When
        FhirResource resource = transformer.transformToFhirResource(parsedData, "ServiceRequest");

        // Then
        assertNotNull(resource);
        assertEquals("ServiceRequest", resource.getResourceType());

        // Parse and validate content
        ServiceRequest serviceRequest = (ServiceRequest) jsonParser.parseResource(resource.getJsonContent());
        assertEquals(ServiceRequest.ServiceRequestStatus.ACTIVE, serviceRequest.getStatus());
        assertEquals(ServiceRequest.ServiceRequestIntent.ORDER, serviceRequest.getIntent());
        assertEquals("Patient/PAT123", serviceRequest.getSubject().getReference());
        assertEquals("CBC", serviceRequest.getCode().getCoding().get(0).getCode());
    }

    @Test
    void testTransformToFhirResource_InvalidResourceType() {
        // Given
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setPatientData(createPatientData());

        // When
        FhirResource resource = transformer.transformToFhirResource(parsedData, "InvalidType");

        // Then
        assertNull(resource);
    }

    @Test
    void testTransformPatientData_WithAllFields() {
        // Given
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setMessageControlId("MSG003");
        
        PatientData patientData = new PatientData();
        patientData.setPatientId("PAT456");
        patientData.setMedicalRecordNumber("MRN789");
        patientData.setFirstName("Jane");
        patientData.setMiddleName("Marie");
        patientData.setLastName("Smith");
        patientData.setGender("F");
        patientData.setDateOfBirth(LocalDate.of(1985, 5, 15));
        patientData.setAddress("123 Main St");
        patientData.setCity("Anytown");
        patientData.setState("CA");
        patientData.setZipCode("12345");
        patientData.setPhoneNumber("555-1234");
        patientData.setMaritalStatus("M");
        
        parsedData.setPatientData(patientData);

        // When
        FhirResource resource = transformer.transformToFhirResource(parsedData, "Patient");

        // Then
        assertNotNull(resource);
        Patient patient = (Patient) jsonParser.parseResource(resource.getJsonContent());
        
        // Verify identifiers
        assertEquals(2, patient.getIdentifier().size());
        
        // Verify name
        HumanName name = patient.getName().get(0);
        assertEquals("Jane", name.getGiven().get(0).getValue());
        assertEquals("Marie", name.getGiven().get(1).getValue());
        assertEquals("Smith", name.getFamily());
        
        // Verify gender
        assertEquals(Enumerations.AdministrativeGender.FEMALE, patient.getGender());
        
        // Verify address
        Address address = patient.getAddress().get(0);
        assertEquals("123 Main St", address.getLine().get(0).getValue());
        assertEquals("Anytown", address.getCity());
        assertEquals("CA", address.getState());
        assertEquals("12345", address.getPostalCode());
        
        // Verify telecom
        assertEquals(1, patient.getTelecom().size());
        assertEquals("555-1234", patient.getTelecom().get(0).getValue());
    }

    @Test
    void testTransformObservationData_StringValue() {
        // Given
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setPatientData(createPatientData());
        
        ObservationData obsData = new ObservationData();
        obsData.setObservationId("OBS123");
        obsData.setObservationCode("8302-2");
        obsData.setObservationName("Body height");
        obsData.setValue("Tall");
        obsData.setObservationStatus("F");
        obsData.setObservationDateTime(LocalDateTime.of(2024, 1, 15, 10, 30));
        
        parsedData.addObservation(obsData);

        // When
        FhirResource resource = transformer.transformToFhirResource(parsedData, "Observation");

        // Then
        assertNotNull(resource);
        Observation observation = (Observation) jsonParser.parseResource(resource.getJsonContent());
        
        assertTrue(observation.getId().contains("OBS123"));
        assertEquals(Observation.ObservationStatus.FINAL, observation.getStatus());
        assertTrue(observation.getValue() instanceof StringType);
        assertEquals("Tall", ((StringType) observation.getValue()).getValue());
    }

    @Test
    void testTransformWithEmptyData() {
        // Given
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setMessageControlId("MSG004");

        // When
        List<FhirResource> resources = transformer.transformToFhir(parsedData);

        // Then
        assertNotNull(resources);
        assertTrue(resources.isEmpty());
    }

    @Test
    void testGenderMapping() {
        // Test various gender mappings
        ParsedHl7Data parsedData = new ParsedHl7Data();
        
        // Test male
        PatientData malePatient = createPatientData();
        malePatient.setGender("M");
        parsedData.setPatientData(malePatient);
        FhirResource resource = transformer.transformToFhirResource(parsedData, "Patient");
        Patient patient = (Patient) jsonParser.parseResource(resource.getJsonContent());
        assertEquals(Enumerations.AdministrativeGender.MALE, patient.getGender());
        
        // Test female
        PatientData femalePatient = createPatientData();
        femalePatient.setGender("F");
        parsedData.setPatientData(femalePatient);
        resource = transformer.transformToFhirResource(parsedData, "Patient");
        patient = (Patient) jsonParser.parseResource(resource.getJsonContent());
        assertEquals(Enumerations.AdministrativeGender.FEMALE, patient.getGender());
        
        // Test unknown
        PatientData unknownPatient = createPatientData();
        unknownPatient.setGender("U");
        parsedData.setPatientData(unknownPatient);
        resource = transformer.transformToFhirResource(parsedData, "Patient");
        patient = (Patient) jsonParser.parseResource(resource.getJsonContent());
        assertEquals(Enumerations.AdministrativeGender.UNKNOWN, patient.getGender());
    }

    // Helper methods to create test data
    private ParsedHl7Data createCompleteHl7Data() {
        ParsedHl7Data parsedData = new ParsedHl7Data();
        parsedData.setMessageType("ORU^R01");
        parsedData.setMessageControlId("MSG001");
        parsedData.setMessageTimestamp(LocalDateTime.now());
        parsedData.setSendingApplication("LAB");
        parsedData.setReceivingApplication("EMR");
        
        parsedData.setPatientData(createPatientData());
        parsedData.setVisitData(createVisitData());
        parsedData.addObservation(createObservationData());
        parsedData.addOrder(createOrderData());
        
        return parsedData;
    }

    private PatientData createPatientData() {
        PatientData patientData = new PatientData();
        patientData.setPatientId("PAT123");
        patientData.setFirstName("John");
        patientData.setLastName("Doe");
        patientData.setGender("M");
        patientData.setDateOfBirth(LocalDate.of(1980, 1, 1));
        return patientData;
    }

    private VisitData createVisitData() {
        VisitData visitData = new VisitData();
        visitData.setVisitNumber("VISIT123");
        visitData.setPatientClass("I");
        visitData.setAssignedPatientLocation("ICU");
        visitData.setRoom("101");
        visitData.setBed("A");
        visitData.setAdmitDateTime(LocalDateTime.of(2024, 1, 15, 8, 0));
        visitData.setDischargeDateTime(LocalDateTime.of(2024, 1, 17, 10, 0));
        return visitData;
    }

    private ObservationData createObservationData() {
        ObservationData obsData = new ObservationData();
        obsData.setObservationId("OBS001");
        obsData.setObservationCode("33747-0");
        obsData.setObservationName("Body temperature");
        obsData.setValue("98.6");
        obsData.setUnits("degF");
        obsData.setObservationStatus("F");
        obsData.setObservationDateTime(LocalDateTime.of(2024, 1, 15, 10, 30));
        return obsData;
    }

    private OrderData createOrderData() {
        OrderData orderData = new OrderData();
        orderData.setOrderNumber("ORD001");
        orderData.setPlacerOrderNumber("PLC001");
        orderData.setFillerOrderNumber("FIL001");
        orderData.setOrderCode("CBC");
        orderData.setOrderName("Complete Blood Count");
        orderData.setOrderStatus("A");
        orderData.setOrderDateTime(LocalDateTime.of(2024, 1, 15, 9, 0));
        orderData.setPriority("R");
        return orderData;
    }
}