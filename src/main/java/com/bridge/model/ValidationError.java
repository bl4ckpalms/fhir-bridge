package com.bridge.model;

import java.util.Objects;

/**
 * Represents a validation error found in an HL7 v2 message
 */
public class ValidationError {
    
    private String field;
    private String segment;
    private String code;
    private String message;
    private ValidationSeverity severity;
    private String location;
    private String actualValue;
    private String expectedValue;
    
    public ValidationError() {
        this.severity = ValidationSeverity.ERROR;
    }
    
    public ValidationError(String field, String message) {
        this();
        this.field = field;
        this.message = message;
    }
    
    public ValidationError(String field, String segment, String message) {
        this();
        this.field = field;
        this.segment = segment;
        this.message = message;
    }
    
    public ValidationError(String field, String segment, String code, String message) {
        this();
        this.field = field;
        this.segment = segment;
        this.code = code;
        this.message = message;
    }
    
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public String getSegment() {
        return segment;
    }
    
    public void setSegment(String segment) {
        this.segment = segment;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public ValidationSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(ValidationSeverity severity) {
        this.severity = severity;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getActualValue() {
        return actualValue;
    }
    
    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }
    
    public String getExpectedValue() {
        return expectedValue;
    }
    
    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationError that = (ValidationError) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(segment, that.segment) &&
                Objects.equals(code, that.code) &&
                Objects.equals(message, that.message) &&
                severity == that.severity;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(field, segment, code, message, severity);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationError{");
        if (segment != null) {
            sb.append("segment='").append(segment).append("', ");
        }
        if (field != null) {
            sb.append("field='").append(field).append("', ");
        }
        if (code != null) {
            sb.append("code='").append(code).append("', ");
        }
        sb.append("message='").append(message).append("'");
        if (actualValue != null) {
            sb.append(", actualValue='").append(actualValue).append("'");
        }
        if (expectedValue != null) {
            sb.append(", expectedValue='").append(expectedValue).append("'");
        }
        sb.append(", severity=").append(severity);
        sb.append('}');
        return sb.toString();
    }
}