package com.bridge.service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Container class for observation data extracted from HL7 messages
 */
public class ObservationData {
    
    private String observationId;
    private String observationCode;
    private String observationName;
    private String value;
    private String units;
    private String referenceRange;
    private String abnormalFlags;
    private String observationStatus;
    private LocalDateTime observationDateTime;
    private String performingLab;
    private String resultStatus;
    
    public ObservationData() {
    }
    
    public ObservationData(String observationCode, String observationName, String value) {
        this.observationCode = observationCode;
        this.observationName = observationName;
        this.value = value;
    }
    
    public String getObservationId() {
        return observationId;
    }
    
    public void setObservationId(String observationId) {
        this.observationId = observationId;
    }
    
    public String getObservationCode() {
        return observationCode;
    }
    
    public void setObservationCode(String observationCode) {
        this.observationCode = observationCode;
    }
    
    public String getObservationName() {
        return observationName;
    }
    
    public void setObservationName(String observationName) {
        this.observationName = observationName;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getUnits() {
        return units;
    }
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    public String getReferenceRange() {
        return referenceRange;
    }
    
    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }
    
    public String getAbnormalFlags() {
        return abnormalFlags;
    }
    
    public void setAbnormalFlags(String abnormalFlags) {
        this.abnormalFlags = abnormalFlags;
    }
    
    public String getObservationStatus() {
        return observationStatus;
    }
    
    public void setObservationStatus(String observationStatus) {
        this.observationStatus = observationStatus;
    }
    
    public LocalDateTime getObservationDateTime() {
        return observationDateTime;
    }
    
    public void setObservationDateTime(LocalDateTime observationDateTime) {
        this.observationDateTime = observationDateTime;
    }
    
    public String getPerformingLab() {
        return performingLab;
    }
    
    public void setPerformingLab(String performingLab) {
        this.performingLab = performingLab;
    }
    
    public String getResultStatus() {
        return resultStatus;
    }
    
    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObservationData that = (ObservationData) o;
        return Objects.equals(observationId, that.observationId) &&
                Objects.equals(observationCode, that.observationCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(observationId, observationCode);
    }
    
    @Override
    public String toString() {
        return "ObservationData{" +
                "observationCode='" + observationCode + '\'' +
                ", observationName='" + observationName + '\'' +
                ", value='" + value + '\'' +
                ", units='" + units + '\'' +
                ", abnormalFlags='" + abnormalFlags + '\'' +
                '}';
    }
}