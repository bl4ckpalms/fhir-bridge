package com.bridge.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain model representing patient consent information
 */
public class ConsentRecord {
    private String patientId;
    private String organizationId;
    private ConsentStatus status;
    private List<DataCategory> allowedCategories;
    private LocalDateTime effectiveDate;
    private LocalDateTime expirationDate;
    private String policyReference;

    // Constructors, getters, and setters will be added in later tasks
}