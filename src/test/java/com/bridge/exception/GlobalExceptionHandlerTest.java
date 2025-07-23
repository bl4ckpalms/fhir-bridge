package com.bridge.exception;

import com.bridge.model.ErrorResponse;
import com.bridge.model.ValidationError;
import com.bridge.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;
    
    private MockHttpServletRequest request;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/test");
        objectMapper = new ObjectMapper();
        MDC.clear();
    }
    
    @Test
    void handleValidationException_ShouldReturnBadRequest() {
        // Given
        ValidationError validationError = new ValidationError("PID.3", "Patient ID is required");
        ValidationException exception = new ValidationException(
            "HL7 validation failed", 
            Arrays.asList(validationError)
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBridgeException(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getCode());
        assertEquals("HL7 validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getDetails());
        assertEquals("/api/v1/test", response.getBody().getPath());
        assertEquals("POST", response.getBody().getMethod());
        
        verify(auditService).logSystemEvent(anyString(), anyString(), anyString(), any());
    }
    
    @Test
    void handleTransformationException_ShouldReturnUnprocessableEntity() {
        // Given
        TransformationException exception = new TransformationException(
            "Failed to transform HL7 to FHIR", 
            "Invalid segment structure"
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBridgeException(exception, request);
        
        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TRANSFORMATION_ERROR", response.getBody().getCode());
        assertEquals("Failed to transform HL7 to FHIR", response.getBody().getMessage());
        assertEquals("Invalid segment structure", response.getBody().getDetails());
    }
    
    @Test
    void handleConsentException_ShouldReturnForbidden() {
        // Given
        ConsentException exception = new ConsentException(
            "Patient consent not found", 
            "Patient ID: 12345"
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBridgeException(exception, request);
        
        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CONSENT_ERROR", response.getBody().getCode());
        assertEquals("Patient consent not found", response.getBody().getMessage());
        assertEquals("Patient ID: 12345", response.getBody().getDetails());
    }
    
    @Test
    void handleAuthenticationException_ShouldReturnUnauthorized() {
        // Given
        AuthenticationException exception = new AuthenticationException(
            "Invalid JWT token"
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBridgeException(exception, request);
        
        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTHENTICATION_ERROR", response.getBody().getCode());
        assertEquals("Invalid JWT token", response.getBody().getMessage());
    }
    
    @Test
    void handleAuthorizationException_ShouldReturnForbidden() {
        // Given
        AuthorizationException exception = new AuthorizationException(
            "Insufficient permissions for resource access"
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBridgeException(exception, request);
        
        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTHORIZATION_ERROR", response.getBody().getCode());
        assertEquals("Insufficient permissions for resource access", response.getBody().getMessage());
    }
    
    @Test
    void handleResourceNotFoundException_ShouldReturnNotFound() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException(
            "FHIR resource not found", 
            "Resource ID: patient-123"
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBridgeException(exception, request);
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().getCode());
        assertEquals("FHIR resource not found", response.getBody().getMessage());
        assertEquals("Resource ID: patient-123", response.getBody().getDetails());
    }
    
    @Test
    void handleSystemException_ShouldReturnInternalServerError() {
        // Given
        SystemException exception = new SystemException(
            "Database connection failed"
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBridgeException(exception, request);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SYSTEM_ERROR", response.getBody().getCode());
        assertEquals("Database connection failed", response.getBody().getMessage());
    }
    
    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequest() {
        // Given
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "field1", "Field is required"));
        bindingResult.addError(new FieldError("testObject", "field2", "Invalid format"));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            null, bindingResult
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getCode());
        assertEquals("Request validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getDetails());
    }
    
    @Test
    void handleConstraintViolationException_ShouldReturnBadRequest() {
        // Given
        Set<ConstraintViolation<Object>> violations = new HashSet<>();
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Value must not be null");
        violations.add(violation);
        
        ConstraintViolationException exception = new ConstraintViolationException(violations);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolationException(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CONSTRAINT_VIOLATION", response.getBody().getCode());
        assertEquals("Constraint validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getDetails());
    }
    
    @Test
    void handleAccessDeniedException_ShouldReturnForbidden() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access is denied");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDeniedException(exception, request);
        
        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCESS_DENIED", response.getBody().getCode());
        assertEquals("Access denied: insufficient permissions", response.getBody().getMessage());
        
        verify(auditService).logSecurityEvent(anyString(), anyString(), anyString(), anyString(), any());
    }
    
    @Test
    void handleHttpRequestMethodNotSupportedException_ShouldReturnMethodNotAllowed() {
        // Given
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException(
            "DELETE", Arrays.asList("GET", "POST")
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupportedException(exception, request);
        
        // Then
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("METHOD_NOT_SUPPORTED", response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("DELETE"));
        assertNotNull(response.getBody().getDetails());
    }
    
    @Test
    void handleMissingServletRequestParameterException_ShouldReturnBadRequest() {
        // Given
        MissingServletRequestParameterException exception = new MissingServletRequestParameterException(
            "patientId", "String"
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingParameterException(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MISSING_PARAMETER", response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("patientId"));
        assertNotNull(response.getBody().getDetails());
    }
    
    @Test
    void handleMethodArgumentTypeMismatchException_ShouldReturnBadRequest() {
        // Given
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
            "invalid", Integer.class, "id", null, null
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTypeMismatchException(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TYPE_MISMATCH", response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("id"));
        assertNotNull(response.getBody().getDetails());
    }
    
    @Test
    void handleHttpMessageNotReadableException_ShouldReturnBadRequest() {
        // Given
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
            "Malformed JSON", (Throwable) null
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadableException(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MALFORMED_REQUEST", response.getBody().getCode());
        assertEquals("Malformed JSON request body", response.getBody().getMessage());
    }
    
    @Test
    void handleNoHandlerFoundException_ShouldReturnNotFound() {
        // Given
        NoHandlerFoundException exception = new NoHandlerFoundException(
            "GET", "/api/v1/nonexistent", null
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFoundException(exception, request);
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("/api/v1/nonexistent"));
    }
    
    @Test
    void handleDataAccessException_ShouldReturnInternalServerError() {
        // Given
        DataAccessException exception = new DataAccessException("Database connection failed") {};
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataAccessException(exception, request);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DATABASE_ERROR", response.getBody().getCode());
        assertEquals("Database operation failed", response.getBody().getMessage());
        assertNull(response.getBody().getDetails()); // Should not expose internal details
        
        verify(auditService).logSystemEvent(anyString(), anyString(), anyString(), any());
    }
    
    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error occurred");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, request);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertNull(response.getBody().getDetails()); // Should not expose internal details
        
        verify(auditService).logSystemEvent(anyString(), anyString(), anyString(), any());
    }
    
    @Test
    void shouldGenerateRequestIdWhenNotPresent() {
        // Given
        ValidationException exception = new ValidationException(
            "Test validation error", 
            Arrays.asList(new ValidationError("field", "error"))
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBridgeException(exception, request);
        
        // Then
        assertNotNull(response.getBody().getRequestId());
        assertFalse(response.getBody().getRequestId().isEmpty());
    }
    
    @Test
    void shouldUseExistingRequestIdFromMDC() {
        // Given
        String existingRequestId = "existing-request-id";
        MDC.put("requestId", existingRequestId);
        
        ValidationException exception = new ValidationException(
            "Test validation error", 
            Arrays.asList(new ValidationError("field", "error"))
        );
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBridgeException(exception, request);
        
        // Then
        assertEquals(existingRequestId, response.getBody().getRequestId());
    }
}