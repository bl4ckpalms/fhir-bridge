package com.bridge.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {
    
    private CorrelationIdFilter correlationIdFilter;
    
    @Mock
    private FilterChain filterChain;
    
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    
    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        MDC.clear();
    }
    
    @Test
    void doFilter_ShouldGenerateCorrelationIdWhenNotProvided() throws ServletException, IOException {
        // When
        correlationIdFilter.doFilter(request, response, filterChain);
        
        // Then
        String correlationId = response.getHeader("X-Correlation-ID");
        String requestId = response.getHeader("X-Request-ID");
        
        assertNotNull(correlationId);
        assertNotNull(requestId);
        assertFalse(correlationId.isEmpty());
        assertFalse(requestId.isEmpty());
        
        // Verify UUIDs are valid format
        assertDoesNotThrow(() -> UUID.fromString(correlationId));
        assertDoesNotThrow(() -> UUID.fromString(requestId));
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilter_ShouldUseProvidedCorrelationId() throws ServletException, IOException {
        // Given
        String providedCorrelationId = "test-correlation-123";
        String providedRequestId = "test-request-456";
        request.addHeader("X-Correlation-ID", providedCorrelationId);
        request.addHeader("X-Request-ID", providedRequestId);
        
        // When
        correlationIdFilter.doFilter(request, response, filterChain);
        
        // Then
        assertEquals(providedCorrelationId, response.getHeader("X-Correlation-ID"));
        assertEquals(providedRequestId, response.getHeader("X-Request-ID"));
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilter_ShouldAddIdsToMDC() throws ServletException, IOException {
        // Given
        String providedCorrelationId = "test-correlation-123";
        request.addHeader("X-Correlation-ID", providedCorrelationId);
        
        // When
        correlationIdFilter.doFilter(request, response, (req, res) -> {
            // Verify MDC contains the IDs during request processing
            assertEquals(providedCorrelationId, MDC.get("correlationId"));
            assertNotNull(MDC.get("requestId"));
        });
        
        // Then - MDC should be cleaned up after request
        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }
    
    @Test
    void doFilter_ShouldCleanUpMDCEvenWhenExceptionThrown() throws ServletException, IOException {
        // Given
        RuntimeException testException = new RuntimeException("Test exception");
        
        // When/Then
        assertThrows(RuntimeException.class, () -> {
            correlationIdFilter.doFilter(request, response, (req, res) -> {
                // Verify MDC is set during processing
                assertNotNull(MDC.get("correlationId"));
                assertNotNull(MDC.get("requestId"));
                throw testException;
            });
        });
        
        // MDC should still be cleaned up
        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }
    
    @Test
    void doFilter_ShouldHandleEmptyHeaders() throws ServletException, IOException {
        // Given
        request.addHeader("X-Correlation-ID", "");
        request.addHeader("X-Request-ID", "   ");
        
        // When
        correlationIdFilter.doFilter(request, response, filterChain);
        
        // Then - Should generate new IDs for empty headers
        String correlationId = response.getHeader("X-Correlation-ID");
        String requestId = response.getHeader("X-Request-ID");
        
        assertNotNull(correlationId);
        assertNotNull(requestId);
        assertFalse(correlationId.isEmpty());
        assertFalse(requestId.trim().isEmpty());
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilter_ShouldGenerateRequestIdWhenOnlyCorrelationIdProvided() throws ServletException, IOException {
        // Given
        String providedCorrelationId = "test-correlation-123";
        request.addHeader("X-Correlation-ID", providedCorrelationId);
        
        // When
        correlationIdFilter.doFilter(request, response, filterChain);
        
        // Then
        assertEquals(providedCorrelationId, response.getHeader("X-Correlation-ID"));
        
        String requestId = response.getHeader("X-Request-ID");
        assertNotNull(requestId);
        assertFalse(requestId.isEmpty());
        assertDoesNotThrow(() -> UUID.fromString(requestId));
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilter_ShouldGenerateCorrelationIdWhenOnlyRequestIdProvided() throws ServletException, IOException {
        // Given
        String providedRequestId = "test-request-456";
        request.addHeader("X-Request-ID", providedRequestId);
        
        // When
        correlationIdFilter.doFilter(request, response, filterChain);
        
        // Then
        assertEquals(providedRequestId, response.getHeader("X-Request-ID"));
        
        String correlationId = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationId);
        assertFalse(correlationId.isEmpty());
        assertDoesNotThrow(() -> UUID.fromString(correlationId));
        
        verify(filterChain).doFilter(request, response);
    }
}