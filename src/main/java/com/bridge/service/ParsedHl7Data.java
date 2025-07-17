package com.bridge.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Container class for parsed HL7 v2 message data
 */
public class ParsedHl7Data {
    
    private String messageType;
    private String messageVersion;
    private String messageControlId;
    private LocalDateTime messageTimestamp;
    private String sendingApplication;
    private String receivingApplication;
    
    // Patient information
    private PatientData patientData;
    
    // Clinical data
    private List<ObservationData> observations;
    private List<OrderData> orders;
    private VisitData visitData;
    
    public ParsedHl7Data() {
        this.observations = new ArrayList<>();
        this.orders = new ArrayList<>();
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
    
    public String getMessageControlId() {
        return messageControlId;
    }
    
    public void setMessageControlId(String messageControlId) {
        this.messageControlId = messageControlId;
    }
    
    public LocalDateTime getMessageTimestamp() {
        return messageTimestamp;
    }
    
    public void setMessageTimestamp(LocalDateTime messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
    }
    
    public String getSendingApplication() {
        return sendingApplication;
    }
    
    public void setSendingApplication(String sendingApplication) {
        this.sendingApplication = sendingApplication;
    }
    
    public String getReceivingApplication() {
        return receivingApplication;
    }
    
    public void setReceivingApplication(String receivingApplication) {
        this.receivingApplication = receivingApplication;
    }
    
    public PatientData getPatientData() {
        return patientData;
    }
    
    public void setPatientData(PatientData patientData) {
        this.patientData = patientData;
    }
    
    public List<ObservationData> getObservations() {
        return observations;
    }
    
    public void setObservations(List<ObservationData> observations) {
        this.observations = observations;
    }
    
    public void addObservation(ObservationData observation) {
        this.observations.add(observation);
    }
    
    public List<OrderData> getOrders() {
        return orders;
    }
    
    public void setOrders(List<OrderData> orders) {
        this.orders = orders;
    }
    
    public void addOrder(OrderData order) {
        this.orders.add(order);
    }
    
    public VisitData getVisitData() {
        return visitData;
    }
    
    public void setVisitData(VisitData visitData) {
        this.visitData = visitData;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedHl7Data that = (ParsedHl7Data) o;
        return Objects.equals(messageControlId, that.messageControlId) &&
                Objects.equals(messageType, that.messageType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(messageControlId, messageType);
    }
    
    @Override
    public String toString() {
        return "ParsedHl7Data{" +
                "messageType='" + messageType + '\'' +
                ", messageControlId='" + messageControlId + '\'' +
                ", patientData=" + patientData +
                ", observationCount=" + observations.size() +
                ", orderCount=" + orders.size() +
                '}';
    }
}