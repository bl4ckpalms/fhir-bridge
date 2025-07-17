package com.bridge.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of HL7 v2 message validation
 */
public class ValidationResult {
    
    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;
    private String messageType;
    private String messageVersion;
    
    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.valid = true;
    }
    
    public ValidationResult(boolean valid) {
        this();
        this.valid = valid;
    }
    
    public void addError(ValidationError error) {
        this.errors.add(error);
        this.valid = false;
    }
    
    public void addWarning(ValidationWarning warning) {
        this.warnings.add(warning);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
        this.valid = errors.isEmpty();
    }
    
    public List<ValidationWarning> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<ValidationWarning> warnings) {
        this.warnings = warnings;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public String getMessageVersion() {
        return messageVersion;
    }
    
    public void setMessageVersion(String messageVersion) {
        this.messageVersion = messageVersion;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    public int getWarningCount() {
        return warnings.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid &&
                Objects.equals(errors, that.errors) &&
                Objects.equals(warnings, that.warnings) &&
                Objects.equals(messageType, that.messageType) &&
                Objects.equals(messageVersion, that.messageVersion);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, errors, warnings, messageType, messageVersion);
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", errorCount=" + errors.size() +
                ", warningCount=" + warnings.size() +
                ", messageType='" + messageType + '\'' +
                ", messageVersion='" + messageVersion + '\'' +
                '}';
    }
}