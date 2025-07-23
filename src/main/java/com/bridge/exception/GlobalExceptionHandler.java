package com.bridge.exception;

import com.bridge.model.ErrorResponse;
import com.bridge.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for all API endpoints
 * Provides centralized error handling with standardized error responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Handle custom bridge exceptions
     */
    @ExceptionHandler(BridgeException.class)
    public ResponseEntity<ErrorResponse> handleBridgeException(
            BridgeException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        HttpStatus status = mapErrorCodeToHttpStatus(ex.getErrorCode());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getDetails(),
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logError(ex, requestId, request);
        auditError(ex, requestId, request);
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle validation exceptions from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            validationErrors,
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.warn("Validation error for request {}: {}", requestId, validationErrors);
        auditValidationError(validationErrors, requestId, request);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        List<String> violations = ex.getConstraintViolations()
            .stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Constraint validation failed",
            violations,
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.warn("Constraint violation for request {}: {}", requestId, violations);
        auditValidationError(violations, requestId, request);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle Spring Security access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        ErrorResponse errorResponse = new ErrorResponse(
            "ACCESS_DENIED",
            "Access denied: insufficient permissions",
            null,
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.warn("Access denied for request {}: {}", requestId, ex.getMessage());
        auditSecurityError("ACCESS_DENIED", ex.getMessage(), requestId, request);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle HTTP method not supported
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        ErrorResponse errorResponse = new ErrorResponse(
            "METHOD_NOT_SUPPORTED",
            "HTTP method not supported: " + ex.getMethod(),
            Map.of("supportedMethods", ex.getSupportedMethods()),
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.warn("Method not supported for request {}: {}", requestId, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }
    
    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        ErrorResponse errorResponse = new ErrorResponse(
            "MISSING_PARAMETER",
            "Required parameter is missing: " + ex.getParameterName(),
            Map.of("parameterName", ex.getParameterName(), "parameterType", ex.getParameterType()),
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.warn("Missing parameter for request {}: {}", requestId, ex.getMessage());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        ErrorResponse errorResponse = new ErrorResponse(
            "TYPE_MISMATCH",
            "Parameter type mismatch: " + ex.getName(),
            Map.of(
                "parameterName", ex.getName(),
                "expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                "providedValue", ex.getValue()
            ),
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.warn("Type mismatch for request {}: {}", requestId, ex.getMessage());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle malformed JSON requests
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        ErrorResponse errorResponse = new ErrorResponse(
            "MALFORMED_REQUEST",
            "Malformed JSON request body",
            null,
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.warn("Malformed request for request {}: {}", requestId, ex.getMessage());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle 404 not found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        ErrorResponse errorResponse = new ErrorResponse(
            "NOT_FOUND",
            "Endpoint not found: " + ex.getRequestURL(),
            null,
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.warn("Endpoint not found for request {}: {}", requestId, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle database access exceptions
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        ErrorResponse errorResponse = new ErrorResponse(
            "DATABASE_ERROR",
            "Database operation failed",
            null, // Don't expose internal database details
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.error("Database error for request {}: {}", requestId, ex.getMessage(), ex);
        auditError(ex, requestId, request);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String requestId = getOrGenerateRequestId();
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            null, // Don't expose internal error details
            requestId
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        
        logger.error("Unexpected error for request {}: {}", requestId, ex.getMessage(), ex);
        auditError(ex, requestId, request);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    // Helper methods
    
    private String getOrGenerateRequestId() {
        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            MDC.put("requestId", requestId);
        }
        return requestId;
    }
    
    private HttpStatus mapErrorCodeToHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "AUTHENTICATION_ERROR" -> HttpStatus.UNAUTHORIZED;
            case "AUTHORIZATION_ERROR", "ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
            case "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CONSENT_ERROR" -> HttpStatus.FORBIDDEN;
            case "TRANSFORMATION_ERROR" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "SYSTEM_ERROR", "DATABASE_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    private void logError(Exception ex, String requestId, HttpServletRequest request) {
        logger.error("Error processing request {} {} {}: {}", 
            request.getMethod(), request.getRequestURI(), requestId, ex.getMessage(), ex);
    }
    
    private void auditError(Exception ex, String requestId, HttpServletRequest request) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("requestId", requestId);
            details.put("method", request.getMethod());
            details.put("path", request.getRequestURI());
            details.put("errorType", ex.getClass().getSimpleName());
            details.put("errorMessage", ex.getMessage());
            
            auditService.logSystemEvent("API_ERROR", "ERROR_OCCURRED", "ERROR", details);
        } catch (Exception auditEx) {
            logger.error("Failed to audit error event: {}", auditEx.getMessage());
        }
    }
    
    private void auditValidationError(Object validationDetails, String requestId, HttpServletRequest request) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("requestId", requestId);
            details.put("method", request.getMethod());
            details.put("path", request.getRequestURI());
            details.put("validationErrors", validationDetails);
            
            auditService.logSystemEvent("VALIDATION_ERROR", "VALIDATION_FAILED", "ERROR", details);
        } catch (Exception auditEx) {
            logger.error("Failed to audit validation error event: {}", auditEx.getMessage());
        }
    }
    
    private void auditSecurityError(String errorType, String message, String requestId, HttpServletRequest request) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("requestId", requestId);
            details.put("method", request.getMethod());
            details.put("path", request.getRequestURI());
            details.put("errorMessage", message);
            
            // Get user ID from request if available
            String userId = request.getUserPrincipal() != null ? 
                request.getUserPrincipal().getName() : "anonymous";
            
            auditService.logSecurityEvent(userId, errorType, "ERROR", "HIGH", details);
        } catch (Exception auditEx) {
            logger.error("Failed to audit security error event: {}", auditEx.getMessage());
        }
    }
}