package com.bridge.controller;

import com.bridge.model.*;
import com.bridge.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FhirBridgeController
 */
@ExtendWith(MockitoExtension.class)
class FhirBridgeControllerTest {

    @InjectMocks
    private FhirBridgeController controller;

    @Mock
    private Hl7V2Validator hl7Validator;

    @Mock
    private Hl7MessageParser messageParser;

    @Mock
    private Hl7ToFhirTransformer transformer;

    @Mock
    private FhirValidator fhirValidator;

    @Mock
    private AuditService auditService;

    private ValidationResult validValidationResult;
    private ValidationResult invalidValidationResult;
    private ParsedHl7Data mockParsedData;
    private List<FhirResource> mockFhirResources;

    @BeforeEach
    void setUp() {
        // Setup valid validation result
        validValidationResult = new ValidationResult(true);
        validValidationResult.setMessageType("ADT");
        validValidationResult.setMessageVersion("2.5");

        // Setup invalid validation result
        invalidValidationResult = new ValidationResult(false);
        ValidationError error = new ValidationError();
        error.setField("PID.3");
        error.setMessage("Patient identifier is required");
        error.setSeverity(ValidationSeverity.ERROR);
        invalidValidationResult.addError(error);

        // Setup mock parsed data
        mockParsedData = new ParsedHl7Data();
        PatientData patientData = new PatientData();
        patientData.setPatientId("12345");
        patientData.setFirstName("John");
        patientData.setLastName("Doe");
        mockParsedData.setPatientData(patientData);

        // Setup mock FHIR resources
        FhirResource patientResource = new FhirResource();
        patientResource.setResourceId("patient-12345");
        patientResource.setResourceType("Patient");
        patientResource.setJsonContent("{\"resourceType\":\"Patient\",\"id\":\"patient-12345\"}");
        patientResource.setCreatedAt(LocalDateTime.now());

        mockFhirResources = Arrays.asList(patientResource);
    }

    @Test
    void testTransformHl7ToFhir_Success() {
        // Arrange
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message("MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20230101120000||ADT^A01|12345|P|2.5");
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        when(hl7Validator.validate(any(Hl7Message.class))).thenReturn(validValidationResult);
        when(messageParser.parseMessage(any(Hl7Message.class))).thenReturn(mockParsedData);
        when(transformer.transformToFhir(any(ParsedHl7Data.class))).thenReturn(mockFhirResources);
        when(fhirValidator.validateFhirResource(any(FhirResource.class))).thenReturn(validValidationResult);

        // Act
        ResponseEntity<?> response = controller.transformHl7ToFhir(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        FhirBridgeController.TransformationResponse transformationResponse = 
            (FhirBridgeController.TransformationResponse) response.getBody();
        assertEquals("SUCCESS", transformationResponse.getStatus());
        assertEquals(1, transformationResponse.getResourceCount());
        assertNotNull(transformationResponse.getFhirResources());
        assertEquals(1, transformationResponse.getFhirResources().size());
        assertEquals("Patient", transformationResponse.getFhirResources().get(0).getResourceType());

        // Verify service interactions
        verify(hl7Validator).validate(any(Hl7Message.class));
        verify(messageParser).parseMessage(any(Hl7Message.class));
        verify(transformer).transformToFhir(any(ParsedHl7Data.class));
        verify(fhirValidator).validateFhirResource(any(FhirResource.class));
        verify(auditService).logTransformation(anyString(), eq("HL7"), anyString(), 
            eq("FHIR"), eq("TRANSFORMATION_SUCCESS"), eq("SUCCESS"), any(Map.class));
    }

    @Test
    void testTransformHl7ToFhir_ValidationFailure() {
        // Arrange
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message("INVALID_HL7_MESSAGE");
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        when(hl7Validator.validate(any(Hl7Message.class))).thenReturn(invalidValidationResult);

        // Act
        ResponseEntity<?> response = controller.transformHl7ToFhir(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();
        assertNotNull(errorResponse.get("error"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertEquals("VALIDATION_ERROR", error.get("code"));
        assertEquals("HL7 message validation failed", error.get("message"));

        // Verify service interactions
        verify(hl7Validator).validate(any(Hl7Message.class));
        verify(messageParser, never()).parseMessage(any(Hl7Message.class));
        verify(transformer, never()).transformToFhir(any(ParsedHl7Data.class));
        verify(auditService).logTransformation(anyString(), eq("HL7"), anyString(), 
            eq("FHIR"), eq("VALIDATION_FAILED"), eq("VALIDATION_ERROR"), any(Map.class));
    }

    @Test
    void testTransformHl7ToFhir_TransformationError() {
        // Arrange
        FhirBridgeController.TransformationRequest request = new FhirBridgeController.TransformationRequest();
        request.setHl7Message("MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20230101120000||ADT^A01|12345|P|2.5");
        request.setSendingApplication("SENDING_APP");
        request.setReceivingApplication("RECEIVING_APP");

        when(hl7Validator.validate(any(Hl7Message.class))).thenReturn(validValidationResult);
        when(messageParser.parseMessage(any(Hl7Message.class))).thenReturn(mockParsedData);
        when(transformer.transformToFhir(any(ParsedHl7Data.class))).thenThrow(new RuntimeException("Transformation failed"));

        // Act
        ResponseEntity<?> response = controller.transformHl7ToFhir(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();
        assertNotNull(errorResponse.get("error"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertEquals("TRANSFORMATION_ERROR", error.get("code"));
        assertEquals("Failed to transform HL7 message to FHIR", error.get("message"));
        assertEquals("Transformation failed", error.get("details"));

        // Verify service interactions
        verify(hl7Validator).validate(any(Hl7Message.class));
        verify(messageParser).parseMessage(any(Hl7Message.class));
        verify(transformer).transformToFhir(any(ParsedHl7Data.class));
        verify(auditService).logTransformation(anyString(), eq("HL7"), anyString(), 
            eq("FHIR"), eq("TRANSFORMATION_ERROR"), eq("ERROR"), any(Map.class));
    }

    @Test
    void testGetFhirResource_Success() {
        // Act
        ResponseEntity<?> response = controller.getFhirResource("Patient", "patient-12345");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Patient", responseBody.get("resourceType"));
        assertEquals("patient-12345", responseBody.get("resourceId"));
        assertNotNull(responseBody.get("requestId"));

        // Verify audit logging
        verify(auditService).logSystemEvent(eq("FHIR_RESOURCE_ACCESS"), 
            eq("GET_RESOURCE"), eq("SUCCESS"), any(Map.class));
    }

    @Test
    void testSearchFhirResources_Success() {
        // Arrange
        Map<String, String> params = Map.of("family", "Doe", "given", "John");

        // Act
        ResponseEntity<?> response = controller.searchFhirResources("Patient", params);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Patient", responseBody.get("resourceType"));
        assertEquals(params, responseBody.get("searchParams"));
        assertNotNull(responseBody.get("requestId"));

        // Verify audit logging
        verify(auditService).logSystemEvent(eq("FHIR_RESOURCE_SEARCH"), 
            eq("SEARCH_RESOURCES"), eq("SUCCESS"), any(Map.class));
    }

    @Test
    void testHealthCheck() {
        // Act
        ResponseEntity<Map<String, Object>> response = controller.healthCheck();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> healthResponse = response.getBody();
        assertEquals("UP", healthResponse.get("status"));
        assertEquals("FHIR Bridge Transformation API", healthResponse.get("service"));
        assertEquals("1.0.0", healthResponse.get("version"));
        assertNotNull(healthResponse.get("timestamp"));
    }

    @Test
    void testTransformHl7ToFhir_NullRequest() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            controller.transformHl7ToFhir(null);
        });
    }

    @Test
    void testGetFhirResource_ServiceError() {
        // Arrange
        doThrow(new RuntimeException("Service error")).when(auditService)
            .logSystemEvent(anyString(), anyString(), anyString(), any(Map.class));

        // Act
        ResponseEntity<?> response = controller.getFhirResource("Patient", "patient-12345");

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();
        assertNotNull(errorResponse.get("error"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertEquals("RESOURCE_RETRIEVAL_ERROR", error.get("code"));
        assertEquals("Failed to retrieve FHIR resource", error.get("message"));
    }

    @Test
    void testSearchFhirResources_ServiceError() {
        // Arrange
        Map<String, String> params = Map.of("family", "Doe");
        doThrow(new RuntimeException("Service error")).when(auditService)
            .logSystemEvent(anyString(), anyString(), anyString(), any(Map.class));

        // Act
        ResponseEntity<?> response = controller.searchFhirResources("Patient", params);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response.getBody();
        assertNotNull(errorResponse.get("error"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
        assertEquals("RESOURCE_SEARCH_ERROR", error.get("code"));
        assertEquals("Failed to search FHIR resources", error.get("message"));
    }
}