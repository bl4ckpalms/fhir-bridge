package com.bridge.service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Container class for visit/encounter data extracted from HL7 messages
 */
public class VisitData {
    
    private String visitNumber;
    private String patientClass;
    private String assignedPatientLocation;
    private String room;
    private String bed;
    private String facility;
    private String admissionType;
    private String hospitalService;
    private LocalDateTime admitDateTime;
    private LocalDateTime dischargeDateTime;
    private String attendingDoctor;
    private String referringDoctor;
    private String consultingDoctor;
    private String visitIndicator;
    
    public VisitData() {
    }
    
    public VisitData(String visitNumber, String patientClass) {
        this.visitNumber = visitNumber;
        this.patientClass = patientClass;
    }
    
    public String getVisitNumber() {
        return visitNumber;
    }
    
    public void setVisitNumber(String visitNumber) {
        this.visitNumber = visitNumber;
    }
    
    public String getPatientClass() {
        return patientClass;
    }
    
    public void setPatientClass(String patientClass) {
        this.patientClass = patientClass;
    }
    
    public String getAssignedPatientLocation() {
        return assignedPatientLocation;
    }
    
    public void setAssignedPatientLocation(String assignedPatientLocation) {
        this.assignedPatientLocation = assignedPatientLocation;
    }
    
    public String getRoom() {
        return room;
    }
    
    public void setRoom(String room) {
        this.room = room;
    }
    
    public String getBed() {
        return bed;
    }
    
    public void setBed(String bed) {
        this.bed = bed;
    }
    
    public String getFacility() {
        return facility;
    }
    
    public void setFacility(String facility) {
        this.facility = facility;
    }
    
    public String getAdmissionType() {
        return admissionType;
    }
    
    public void setAdmissionType(String admissionType) {
        this.admissionType = admissionType;
    }
    
    public String getHospitalService() {
        return hospitalService;
    }
    
    public void setHospitalService(String hospitalService) {
        this.hospitalService = hospitalService;
    }
    
    public LocalDateTime getAdmitDateTime() {
        return admitDateTime;
    }
    
    public void setAdmitDateTime(LocalDateTime admitDateTime) {
        this.admitDateTime = admitDateTime;
    }
    
    public LocalDateTime getDischargeDateTime() {
        return dischargeDateTime;
    }
    
    public void setDischargeDateTime(LocalDateTime dischargeDateTime) {
        this.dischargeDateTime = dischargeDateTime;
    }
    
    public String getAttendingDoctor() {
        return attendingDoctor;
    }
    
    public void setAttendingDoctor(String attendingDoctor) {
        this.attendingDoctor = attendingDoctor;
    }
    
    public String getReferringDoctor() {
        return referringDoctor;
    }
    
    public void setReferringDoctor(String referringDoctor) {
        this.referringDoctor = referringDoctor;
    }
    
    public String getConsultingDoctor() {
        return consultingDoctor;
    }
    
    public void setConsultingDoctor(String consultingDoctor) {
        this.consultingDoctor = consultingDoctor;
    }
    
    public String getVisitIndicator() {
        return visitIndicator;
    }
    
    public void setVisitIndicator(String visitIndicator) {
        this.visitIndicator = visitIndicator;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisitData visitData = (VisitData) o;
        return Objects.equals(visitNumber, visitData.visitNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(visitNumber);
    }
    
    @Override
    public String toString() {
        return "VisitData{" +
                "visitNumber='" + visitNumber + '\'' +
                ", patientClass='" + patientClass + '\'' +
                ", assignedPatientLocation='" + assignedPatientLocation + '\'' +
                ", admissionType='" + admissionType + '\'' +
                ", attendingDoctor='" + attendingDoctor + '\'' +
                '}';
    }
}