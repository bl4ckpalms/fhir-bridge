package com.bridge.filter;

import com.bridge.service.StructuredLoggingService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter to log HTTP requests and responses with structured logging
 */
@Component
@Order(2)
public class RequestLoggingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    @Autowired
    private StructuredLoggingService structuredLoggingService;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Skip logging for health check and actuator endpoints to reduce noise
        String requestURI = httpRequest.getRequestURI();
        if (shouldSkipLogging(requestURI)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Wrap request and response to capture content
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Log incoming request
            logIncomingRequest(wrappedRequest);
            
            // Process the request
            chain.doFilter(wrappedRequest, wrappedResponse);
            
            // Log outgoing response
            long responseTime = System.currentTimeMillis() - startTime;
            logOutgoingResponse(wrappedRequest, wrappedResponse, responseTime);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logRequestError(wrappedRequest, e, responseTime);
            throw e;
        } finally {
            // Copy response content back to original response
            wrappedResponse.copyBodyToResponse();
        }
    }
    
    private boolean shouldSkipLogging(String requestURI) {
        return requestURI.startsWith("/actuator/") ||
               requestURI.equals("/api/v1/health") ||
               requestURI.startsWith("/swagger-") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.endsWith(".css") ||
               requestURI.endsWith(".js") ||
               requestURI.endsWith(".ico");
    }
    
    private void logIncomingRequest(ContentCachingRequestWrapper request) {
        try {
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("method", request.getMethod());
            requestData.put("path", request.getRequestURI());
            requestData.put("queryString", request.getQueryString());
            requestData.put("remoteAddr", getClientIpAddress(request));
            requestData.put("userAgent", request.getHeader("User-Agent"));
            requestData.put("contentType", request.getContentType());
            requestData.put("contentLength", request.getContentLength());
            
            // Add user information if available
            Principal principal = request.getUserPrincipal();
            if (principal != null) {
                requestData.put("userId", principal.getName());
                MDC.put("userId", principal.getName());
            }
            
            // Add request headers (selective)
            Map<String, String> headers = new HashMap<>();
            if (request.getHeader("Authorization") != null) {
                headers.put("Authorization", "[REDACTED]");
            }
            if (request.getHeader("X-Forwarded-For") != null) {
                headers.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
            }
            if (request.getHeader("X-Real-IP") != null) {
                headers.put("X-Real-IP", request.getHeader("X-Real-IP"));
            }
            if (!headers.isEmpty()) {
                requestData.put("headers", headers);
            }
            
            // Log request body for POST/PUT requests (with size limit)
            if (shouldLogRequestBody(request)) {
                String requestBody = getRequestBody(request);
                if (requestBody != null && !requestBody.isEmpty()) {
                    if (requestBody.length() > 1000) {
                        requestData.put("requestBody", requestBody.substring(0, 1000) + "... [TRUNCATED]");
                    } else {
                        requestData.put("requestBody", requestBody);
                    }
                }
            }
            
            structuredLoggingService.logEvent("HTTP_REQUEST", "INCOMING", "INFO", requestData);
            
        } catch (Exception e) {
            logger.warn("Failed to log incoming request: {}", e.getMessage());
        }
    }
    
    private void logOutgoingResponse(ContentCachingRequestWrapper request, 
                                   ContentCachingResponseWrapper response, 
                                   long responseTimeMs) {
        try {
            String userId = request.getUserPrincipal() != null ? 
                request.getUserPrincipal().getName() : null;
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("statusCode", response.getStatus());
            responseData.put("contentType", response.getContentType());
            responseData.put("contentLength", response.getContentSize());
            
            // Log response body for errors or if specifically configured
            if (shouldLogResponseBody(response)) {
                String responseBody = getResponseBody(response);
                if (responseBody != null && !responseBody.isEmpty()) {
                    if (responseBody.length() > 1000) {
                        responseData.put("responseBody", responseBody.substring(0, 1000) + "... [TRUNCATED]");
                    } else {
                        responseData.put("responseBody", responseBody);
                    }
                }
            }
            
            structuredLoggingService.logApiEvent(
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                responseTimeMs,
                userId,
                responseData
            );
            
        } catch (Exception e) {
            logger.warn("Failed to log outgoing response: {}", e.getMessage());
        }
    }
    
    private void logRequestError(ContentCachingRequestWrapper request, Exception error, long responseTimeMs) {
        try {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", error.getClass().getSimpleName());
            errorData.put("errorMessage", error.getMessage());
            errorData.put("responseTimeMs", responseTimeMs);
            
            structuredLoggingService.logEvent("HTTP_REQUEST", "ERROR", "ERROR", errorData);
            
        } catch (Exception e) {
            logger.warn("Failed to log request error: {}", e.getMessage());
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    private boolean shouldLogRequestBody(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String contentType = request.getContentType();
        
        return ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) &&
               contentType != null &&
               (contentType.contains("application/json") || contentType.contains("application/xml"));
    }
    
    private boolean shouldLogResponseBody(ContentCachingResponseWrapper response) {
        int status = response.getStatus();
        String contentType = response.getContentType();
        
        // Log response body for errors or JSON responses
        return (status >= 400) ||
               (contentType != null && contentType.contains("application/json"));
    }
    
    private String getRequestBody(ContentCachingRequestWrapper request) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.debug("Failed to read request body: {}", e.getMessage());
        }
        return null;
    }
    
    private String getResponseBody(ContentCachingResponseWrapper response) {
        try {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.debug("Failed to read response body: {}", e.getMessage());
        }
        return null;
    }
}