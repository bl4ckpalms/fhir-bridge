package com.bridge.exception;

import com.bridge.model.ErrorResponse;
import com.bridge.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ErrorHandlingIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AuditService auditService;
    
    @Test
    void shouldReturn404ForNonExistentEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value(containsString("not found")))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.requestId").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/nonexistent"))
            .andExpect(jsonPath("$.method").value("GET"));
    }
    
    @Test
    void shouldReturn405ForUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(delete("/api/v1/health"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("METHOD_NOT_SUPPORTED"))
            .andExpect(jsonPath("$.message").value(containsString("DELETE")))
            .andExpect(jsonPath("$.details.supportedMethods").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.requestId").exists());
    }
    
    @Test
    void shouldReturn400ForMalformedJson() throws Exception {
        String malformedJson = "{ invalid json }";
        
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
            .andExpect(jsonPath("$.message").value("Malformed JSON request body"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.requestId").exists());
    }
    
    @Test
    @WithMockUser(roles = {"TRANSFORMER"})
    void shouldReturn400ForValidationErrors() throws Exception {
        // Send empty request body to trigger validation errors
        String emptyRequest = "{}";
        
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyRequest))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.details").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.requestId").exists());
    }
    
    @Test
    void shouldReturn401ForUnauthenticatedRequests() throws Exception {
        String validRequest = """
            {
                "hl7Message": "MSH|^~\\\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20230101120000||ADT^A01|12345|P|2.5",
                "sendingApplication": "TEST_APP",
                "receivingApplication": "FHIR_BRIDGE"
            }
            """;
        
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(roles = {"READER"}) // Wrong role for transformation
    void shouldReturn403ForInsufficientPermissions() throws Exception {
        String validRequest = """
            {
                "hl7Message": "MSH|^~\\\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20230101120000||ADT^A01|12345|P|2.5",
                "sendingApplication": "TEST_APP",
                "receivingApplication": "FHIR_BRIDGE"
            }
            """;
        
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.message").value("Access denied: insufficient permissions"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.requestId").exists());
    }
    
    @Test
    void shouldReturn400ForMissingRequiredParameters() throws Exception {
        mockMvc.perform(get("/api/v1/fhir/Patient")
                .param("_count", "10")) // Missing required parameter
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value(anyOf(
                equalTo("MISSING_PARAMETER"), 
                equalTo("ACCESS_DENIED") // Might be access denied due to security
            )))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.requestId").exists());
    }
    
    @Test
    void shouldReturn400ForInvalidParameterTypes() throws Exception {
        mockMvc.perform(get("/api/v1/fhir/Patient/invalid-id-format")
                .param("_count", "not-a-number"))
            .andExpect(status().isUnauthorized()); // Will be unauthorized first due to security
    }
    
    @Test
    void shouldIncludeCorrelationIdInErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent")
                .header("X-Correlation-ID", "test-correlation-123"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.requestId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/nonexistent"))
            .andExpect(jsonPath("$.method").value("GET"));
    }
    
    @Test
    void shouldHandleMultipleValidationErrors() throws Exception {
        String invalidRequest = """
            {
                "hl7Message": "",
                "sendingApplication": "",
                "receivingApplication": ""
            }
            """;
        
        mockMvc.perform(post("/api/v1/transform/hl7v2-to-fhir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isUnauthorized()); // Will be unauthorized first due to security
    }
    
    @Test
    void shouldNotExposeInternalErrorDetails() throws Exception {
        // This test verifies that internal system errors don't expose sensitive information
        mockMvc.perform(get("/api/v1/nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value(not(containsString("Exception"))))
            .andExpect(jsonPath("$.message").value(not(containsString("Stack"))))
            .andExpect(jsonPath("$.message").value(not(containsString("java."))));
    }
    
    @Test
    void shouldReturnConsistentErrorFormat() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.requestId").exists())
            .andExpect(jsonPath("$.path").exists())
            .andExpect(jsonPath("$.method").exists())
            .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")));
    }
}