package com.bridge.service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Container class for order data extracted from HL7 messages
 */
public class OrderData {
    
    private String orderNumber;
    private String placerOrderNumber;
    private String fillerOrderNumber;
    private String orderCode;
    private String orderName;
    private String orderStatus;
    private LocalDateTime orderDateTime;
    private String orderingProvider;
    private String priority;
    private String orderType;
    private String department;
    private String specimen;
    
    public OrderData() {
    }
    
    public OrderData(String orderNumber, String orderCode, String orderName) {
        this.orderNumber = orderNumber;
        this.orderCode = orderCode;
        this.orderName = orderName;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public String getPlacerOrderNumber() {
        return placerOrderNumber;
    }
    
    public void setPlacerOrderNumber(String placerOrderNumber) {
        this.placerOrderNumber = placerOrderNumber;
    }
    
    public String getFillerOrderNumber() {
        return fillerOrderNumber;
    }
    
    public void setFillerOrderNumber(String fillerOrderNumber) {
        this.fillerOrderNumber = fillerOrderNumber;
    }
    
    public String getOrderCode() {
        return orderCode;
    }
    
    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }
    
    public String getOrderName() {
        return orderName;
    }
    
    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }
    
    public String getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public LocalDateTime getOrderDateTime() {
        return orderDateTime;
    }
    
    public void setOrderDateTime(LocalDateTime orderDateTime) {
        this.orderDateTime = orderDateTime;
    }
    
    public String getOrderingProvider() {
        return orderingProvider;
    }
    
    public void setOrderingProvider(String orderingProvider) {
        this.orderingProvider = orderingProvider;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getOrderType() {
        return orderType;
    }
    
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getSpecimen() {
        return specimen;
    }
    
    public void setSpecimen(String specimen) {
        this.specimen = specimen;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderData orderData = (OrderData) o;
        return Objects.equals(orderNumber, orderData.orderNumber) &&
                Objects.equals(placerOrderNumber, orderData.placerOrderNumber) &&
                Objects.equals(fillerOrderNumber, orderData.fillerOrderNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderNumber, placerOrderNumber, fillerOrderNumber);
    }
    
    @Override
    public String toString() {
        return "OrderData{" +
                "orderNumber='" + orderNumber + '\'' +
                ", orderCode='" + orderCode + '\'' +
                ", orderName='" + orderName + '\'' +
                ", orderStatus='" + orderStatus + '\'' +
                ", orderingProvider='" + orderingProvider + '\'' +
                '}';
    }
}