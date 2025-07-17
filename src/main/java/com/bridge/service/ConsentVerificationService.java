package com.bridge.service;

import com.bridge.model.ConsentRecord;
import com.bridge.model.ConsentVerificationResult;
import com.bridge.model.DataCategory;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for patient consent verification and management
 */
public interface ConsentVerificationService {

    /**
     * Verify if a patient has valid consent for data sharing with an organization
     *
     * @param patientId the patient identifier
     * @param organizationId the organization identifier
     * @return verification result with consent details
     */
    ConsentVerificationResult verifyConsent(String patientId, String organizationId);

    /**
     * Verify if a patient has consent for specific data categories
     *
     * @param patientId the patient identifier
     * @param organizationId the organization identifier
     * @param dataCategories the data categories to check
     * @return verification result with allowed categories
     */
    ConsentVerificationResult verifyConsentForCategories(String patientId, String organizationId, 
                                                        List<DataCategory> dataCategories);

    /**
     * Get active consent record for a patient and organization
     *
     * @param patientId the patient identifier
     * @param organizationId the organization identifier
     * @return optional consent record if found and active
     */
    Optional<ConsentRecord> getActiveConsent(String patientId, String organizationId);

    /**
     * Check if consent is expired and update status if needed
     *
     * @param patientId the patient identifier
     * @param organizationId the organization identifier
     * @return true if consent was expired and updated
     */
    boolean checkAndUpdateExpiredConsent(String patientId, String organizationId);

    /**
     * Process expired consents in batch
     *
     * @return number of consents updated to expired status
     */
    int processExpiredConsents();

    /**
     * Get all active consents for a patient
     *
     * @param patientId the patient identifier
     * @return list of active consent records
     */
    List<ConsentRecord> getActiveConsentsForPatient(String patientId);

    /**
     * Validate consent policy rules
     *
     * @param consentRecord the consent record to validate
     * @return true if consent meets policy requirements
     */
    boolean validateConsentPolicy(ConsentRecord consentRecord);
}