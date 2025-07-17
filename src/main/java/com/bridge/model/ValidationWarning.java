package com.bridge.model;

import java.util.Objects;

/**
 * Represents a validation warning found in an HL7 v2 message
 */
public class ValidationWarning {
    
    private String field;
    private String segment;
    private String code;
    private String message;
    private String location;
    private String actualValue;
    private String recommendedValue;
    
    public ValidationWarning() {
    }
    
    public ValidationWarning(String field, String message) {
        this.field = field;
        this.message = message;
    }
    
    public ValidationWarning(String field, String segment, String message) {
        this.field = field;
        this.segment = segment;
        this.message = message;
    }
    
    public ValidationWarning(String field, String segment, String code, String message) {
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
    
    public String getRecommendedValue() {
        return recommendedValue;
    }
    
    public void setRecommendedValue(String recommendedValue) {
        this.recommendedValue = recommendedValue;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationWarning that = (ValidationWarning) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(segment, that.segment) &&
                Objects.equals(code, that.code) &&
                Objects.equals(message, that.message);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(field, segment, code, message);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationWarning{");
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
        if (recommendedValue != null) {
            sb.append(", recommendedValue='").append(recommendedValue).append("'");
        }
        sb.append('}');
        return sb.toString();
    }
}