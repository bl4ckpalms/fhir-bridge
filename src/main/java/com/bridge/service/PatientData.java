package com.bridge.service;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Container class for patient data extracted from HL7 messages
 */
public class PatientData {
    
    private String patientId;
    private String medicalRecordNumber;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phoneNumber;
    private String maritalStatus;
    private String race;
    private String ethnicity;
    
    public PatientData() {
    }
    
    public PatientData(String patientId, String firstName, String lastName) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    public String getPatientId() {
        return patientId;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    public String getMedicalRecordNumber() {
        return medicalRecordNumber;
    }
    
    public void setMedicalRecordNumber(String medicalRecordNumber) {
        this.medicalRecordNumber = medicalRecordNumber;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getMiddleName() {
        return middleName;
    }
    
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getMaritalStatus() {
        return maritalStatus;
    }
    
    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }
    
    public String getRace() {
        return race;
    }
    
    public void setRace(String race) {
        this.race = race;
    }
    
    public String getEthnicity() {
        return ethnicity;
    }
    
    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }
    
    public String getFullName() {
        StringBuilder name = new StringBuilder();
        if (firstName != null) {
            name.append(firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(middleName);
        }
        if (lastName != null) {
            if (name.length() > 0) name.append(" ");
            name.append(lastName);
        }
        return name.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatientData that = (PatientData) o;
        return Objects.equals(patientId, that.patientId) &&
                Objects.equals(medicalRecordNumber, that.medicalRecordNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(patientId, medicalRecordNumber);
    }
    
    @Override
    public String toString() {
        return "PatientData{" +
                "patientId='" + patientId + '\'' +
                ", name='" + getFullName() + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", gender='" + gender + '\'' +
                '}';
    }
}