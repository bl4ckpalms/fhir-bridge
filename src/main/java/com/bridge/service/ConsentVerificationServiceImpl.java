package com.bridge.service;

import com.bridge.entity.ConsentEntity;
import com.bridge.model.*;
import com.bridge.repository.ConsentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of consent verification service for patient consent management
 */
@Service
@Transactional
public class ConsentVerificationServiceImpl implements ConsentVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(ConsentVerificationServiceImpl.class);

    private final ConsentRepository consentRepository;

    @Autowired
    public ConsentVerificationServiceImpl(ConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "consent-records", key = "'patient:' + #patientId + ':org:' + #organizationId")
    public ConsentVerificationResult verifyConsent(String patientId, String organizationId) {
        logger.debug("Verifying consent for patient: {} and organization: {}", patientId, organizationId);

        if (!StringUtils.hasText(patientId) || !StringUtils.hasText(organizationId)) {
            logger.warn("Invalid parameters: patientId={}, organizationId={}", patientId, organizationId);
            return ConsentVerificationResult.invalid(patientId, organizationId, 
                "Patient ID and Organization ID are required");
        }

        try {
            // First check if consent exists
            Optional<ConsentEntity> consentEntityOpt = consentRepository
                .findByPatientIdAndOrganizationId(patientId, organizationId);

            if (consentEntityOpt.isEmpty()) {
                logger.info("No consent record found for patient: {} and organization: {}", 
                    patientId, organizationId);
                return ConsentVerificationResult.notFound(patientId, organizationId);
            }

            ConsentEntity consentEntity = consentEntityOpt.get();
            ConsentRecord consentRecord = convertToConsentRecord(consentEntity);

            // Check if consent is expired
            if (consentRecord.isExpired()) {
                logger.info("Consent is expired for patient: {} and organization: {}", 
                    patientId, organizationId);
                // Update status to expired if not already
                if (consentEntity.getStatus() != ConsentStatus.EXPIRED) {
                    consentEntity.setStatus(ConsentStatus.EXPIRED);
                    consentRepository.save(consentEntity);
                }
                return ConsentVerificationResult.expired(patientId, organizationId, 
                    consentRecord.getExpirationDate());
            }

            // Check consent status
            switch (consentRecord.getStatus()) {
                case ACTIVE:
                    if (consentRecord.isActive()) {
                        logger.debug("Valid active consent found for patient: {} and organization: {}", 
                            patientId, organizationId);
                        return ConsentVerificationResult.valid(patientId, organizationId, consentRecord);
                    } else {
                        logger.info("Consent is not yet effective for patient: {} and organization: {}", 
                            patientId, organizationId);
                        return ConsentVerificationResult.invalid(patientId, organizationId, 
                            "Consent is not yet effective");
                    }
                case REVOKED:
                    logger.info("Consent has been revoked for patient: {} and organization: {}", 
                        patientId, organizationId);
                    return ConsentVerificationResult.revoked(patientId, organizationId);
                case SUSPENDED:
                    logger.info("Consent is suspended for patient: {} and organization: {}", 
                        patientId, organizationId);
                    return ConsentVerificationResult.invalid(patientId, organizationId, 
                        "Consent is currently suspended");
                case DENIED:
                    logger.info("Consent has been denied for patient: {} and organization: {}", 
                        patientId, organizationId);
                    return ConsentVerificationResult.invalid(patientId, organizationId, 
                        "Consent has been denied");
                case PENDING:
                    logger.info("Consent is pending for patient: {} and organization: {}", 
                        patientId, organizationId);
                    return ConsentVerificationResult.invalid(patientId, organizationId, 
                        "Consent is pending approval");
                default:
                    logger.warn("Unknown consent status: {} for patient: {} and organization: {}", 
                        consentRecord.getStatus(), patientId, organizationId);
                    return ConsentVerificationResult.invalid(patientId, organizationId, 
                        "Unknown consent status");
            }

        } catch (Exception e) {
            logger.error("Error verifying consent for patient: {} and organization: {}", 
                patientId, organizationId, e);
            return ConsentVerificationResult.invalid(patientId, organizationId, 
                "Error occurred during consent verification: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentVerificationResult verifyConsentForCategories(String patientId, String organizationId, 
                                                               List<DataCategory> dataCategories) {
        logger.debug("Verifying consent for categories: {} for patient: {} and organization: {}", 
            dataCategories, patientId, organizationId);

        if (dataCategories == null || dataCategories.isEmpty()) {
            logger.warn("No data categories provided for verification");
            return ConsentVerificationResult.invalid(patientId, organizationId, 
                "Data categories are required for verification");
        }

        // First verify basic consent
        ConsentVerificationResult basicResult = verifyConsent(patientId, organizationId);
        if (!basicResult.isValid()) {
            return basicResult;
        }

        // Check category-specific permissions
        List<DataCategory> allowedCategories = basicResult.getAllowedCategories();
        List<DataCategory> deniedCategories = new ArrayList<>();

        // If ALL category is allowed, all requested categories are allowed
        if (allowedCategories.contains(DataCategory.ALL)) {
            logger.debug("ALL data categories are allowed for patient: {} and organization: {}", 
                patientId, organizationId);
            basicResult.setAllowedCategories(new ArrayList<>(dataCategories));
            return basicResult;
        }

        // Check each requested category
        for (DataCategory category : dataCategories) {
            if (!allowedCategories.contains(category)) {
                deniedCategories.add(category);
            }
        }

        if (!deniedCategories.isEmpty()) {
            logger.info("Some data categories are not allowed: {} for patient: {} and organization: {}", 
                deniedCategories, patientId, organizationId);
            basicResult.setDeniedCategories(deniedCategories);
            basicResult.setReason("Access denied for categories: " + deniedCategories);
            
            // Filter allowed categories to only include requested ones
            List<DataCategory> filteredAllowed = allowedCategories.stream()
                .filter(dataCategories::contains)
                .collect(Collectors.toList());
            basicResult.setAllowedCategories(filteredAllowed);
        }

        return basicResult;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConsentRecord> getActiveConsent(String patientId, String organizationId) {
        logger.debug("Getting active consent for patient: {} and organization: {}", 
            patientId, organizationId);

        if (!StringUtils.hasText(patientId) || !StringUtils.hasText(organizationId)) {
            logger.warn("Invalid parameters: patientId={}, organizationId={}", patientId, organizationId);
            return Optional.empty();
        }

        try {
            Optional<ConsentEntity> consentEntityOpt = consentRepository
                .findActiveConsentByPatientIdAndOrganizationId(patientId, organizationId, LocalDateTime.now());

            return consentEntityOpt.map(this::convertToConsentRecord);

        } catch (Exception e) {
            logger.error("Error getting active consent for patient: {} and organization: {}", 
                patientId, organizationId, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "consent-records", key = "'patient:' + #patientId + ':org:' + #organizationId")
    public boolean checkAndUpdateExpiredConsent(String patientId, String organizationId) {
        logger.debug("Checking and updating expired consent for patient: {} and organization: {}", 
            patientId, organizationId);

        if (!StringUtils.hasText(patientId) || !StringUtils.hasText(organizationId)) {
            logger.warn("Invalid parameters: patientId={}, organizationId={}", patientId, organizationId);
            return false;
        }

        try {
            Optional<ConsentEntity> consentEntityOpt = consentRepository
                .findByPatientIdAndOrganizationId(patientId, organizationId);

            if (consentEntityOpt.isEmpty()) {
                return false;
            }

            ConsentEntity consentEntity = consentEntityOpt.get();
            if (consentEntity.isExpired() && consentEntity.getStatus() != ConsentStatus.EXPIRED) {
                logger.info("Updating expired consent status for patient: {} and organization: {}", 
                    patientId, organizationId);
                consentEntity.setStatus(ConsentStatus.EXPIRED);
                consentRepository.save(consentEntity);
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("Error checking and updating expired consent for patient: {} and organization: {}", 
                patientId, organizationId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public int processExpiredConsents() {
        logger.info("Processing expired consents");

        try {
            List<ConsentEntity> expiredConsents = consentRepository.findExpiredConsents(LocalDateTime.now());
            
            if (expiredConsents.isEmpty()) {
                logger.debug("No expired consents found");
                return 0;
            }

            logger.info("Found {} expired consents to update", expiredConsents.size());

            for (ConsentEntity consent : expiredConsents) {
                consent.setStatus(ConsentStatus.EXPIRED);
                logger.debug("Updated consent to expired status for patient: {} and organization: {}", 
                    consent.getPatientId(), consent.getOrganizationId());
            }

            consentRepository.saveAll(expiredConsents);
            logger.info("Successfully updated {} expired consents", expiredConsents.size());
            
            return expiredConsents.size();

        } catch (Exception e) {
            logger.error("Error processing expired consents", e);
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsentRecord> getActiveConsentsForPatient(String patientId) {
        logger.debug("Getting active consents for patient: {}", patientId);

        if (!StringUtils.hasText(patientId)) {
            logger.warn("Invalid patient ID: {}", patientId);
            return new ArrayList<>();
        }

        try {
            List<ConsentEntity> activeConsents = consentRepository
                .findActiveConsentsByPatientId(patientId, LocalDateTime.now());

            return activeConsents.stream()
                .map(this::convertToConsentRecord)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting active consents for patient: {}", patientId, e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateConsentPolicy(ConsentRecord consentRecord) {
        if (consentRecord == null) {
            logger.warn("Consent record is null");
            return false;
        }

        logger.debug("Validating consent policy for patient: {} and organization: {}", 
            consentRecord.getPatientId(), consentRecord.getOrganizationId());

        try {
            // Basic validation rules
            if (!StringUtils.hasText(consentRecord.getPatientId()) || 
                !StringUtils.hasText(consentRecord.getOrganizationId())) {
                logger.warn("Missing required fields in consent record");
                return false;
            }

            if (consentRecord.getStatus() == null) {
                logger.warn("Consent status is null");
                return false;
            }

            if (consentRecord.getEffectiveDate() == null) {
                logger.warn("Effective date is null");
                return false;
            }

            // Check if effective date is not in the future beyond reasonable limits (e.g., 1 year)
            LocalDateTime maxFutureDate = LocalDateTime.now().plusYears(1);
            if (consentRecord.getEffectiveDate().isAfter(maxFutureDate)) {
                logger.warn("Effective date is too far in the future: {}", consentRecord.getEffectiveDate());
                return false;
            }

            // Check if expiration date is after effective date
            if (consentRecord.getExpirationDate() != null && 
                consentRecord.getExpirationDate().isBefore(consentRecord.getEffectiveDate())) {
                logger.warn("Expiration date is before effective date");
                return false;
            }

            // Check if allowed categories are specified for active consents
            if (consentRecord.getStatus() == ConsentStatus.ACTIVE && 
                (consentRecord.getAllowedCategories() == null || consentRecord.getAllowedCategories().isEmpty())) {
                logger.warn("Active consent must have allowed categories specified");
                return false;
            }

            logger.debug("Consent policy validation passed for patient: {} and organization: {}", 
                consentRecord.getPatientId(), consentRecord.getOrganizationId());
            return true;

        } catch (Exception e) {
            logger.error("Error validating consent policy", e);
            return false;
        }
    }

    /**
     * Convert ConsentEntity to ConsentRecord
     */
    private ConsentRecord convertToConsentRecord(ConsentEntity entity) {
        ConsentRecord record = new ConsentRecord();
        record.setPatientId(entity.getPatientId());
        record.setOrganizationId(entity.getOrganizationId());
        record.setStatus(entity.getStatus());
        record.setAllowedCategories(new ArrayList<>(entity.getAllowedCategories()));
        record.setEffectiveDate(entity.getEffectiveDate());
        record.setExpirationDate(entity.getExpirationDate());
        record.setPolicyReference(entity.getPolicyReference());
        return record;
    }
}