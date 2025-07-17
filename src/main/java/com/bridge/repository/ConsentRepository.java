package com.bridge.repository;

import com.bridge.entity.ConsentEntity;
import com.bridge.model.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for consent record data access operations
 */
@Repository
public interface ConsentRepository extends JpaRepository<ConsentEntity, Long> {

    /**
     * Find consent record by patient ID and organization ID
     */
    Optional<ConsentEntity> findByPatientIdAndOrganizationId(String patientId, String organizationId);

    /**
     * Find all consent records for a patient
     */
    List<ConsentEntity> findByPatientIdOrderByCreatedAtDesc(String patientId);

    /**
     * Find all consent records for an organization
     */
    List<ConsentEntity> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);

    /**
     * Find consent records by status
     */
    List<ConsentEntity> findByStatusOrderByCreatedAtDesc(ConsentStatus status);

    /**
     * Find active consent records for a patient
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.patientId = :patientId " +
           "AND c.status = 'ACTIVE' " +
           "AND c.effectiveDate <= :now " +
           "AND (c.expirationDate IS NULL OR c.expirationDate > :now)")
    List<ConsentEntity> findActiveConsentsByPatientId(@Param("patientId") String patientId, 
                                                      @Param("now") LocalDateTime now);

    /**
     * Find active consent record for a patient and organization
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.patientId = :patientId " +
           "AND c.organizationId = :organizationId " +
           "AND c.status = 'ACTIVE' " +
           "AND c.effectiveDate <= :now " +
           "AND (c.expirationDate IS NULL OR c.expirationDate > :now)")
    Optional<ConsentEntity> findActiveConsentByPatientIdAndOrganizationId(
            @Param("patientId") String patientId, 
            @Param("organizationId") String organizationId, 
            @Param("now") LocalDateTime now);

    /**
     * Find expired consent records
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.expirationDate IS NOT NULL " +
           "AND c.expirationDate <= :now " +
           "AND c.status != 'EXPIRED'")
    List<ConsentEntity> findExpiredConsents(@Param("now") LocalDateTime now);

    /**
     * Find consent records expiring within a specified period
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.expirationDate IS NOT NULL " +
           "AND c.expirationDate BETWEEN :now AND :expirationThreshold " +
           "AND c.status = 'ACTIVE'")
    List<ConsentEntity> findConsentsExpiringBefore(@Param("now") LocalDateTime now, 
                                                   @Param("expirationThreshold") LocalDateTime expirationThreshold);

    /**
     * Count active consents for a patient
     */
    @Query("SELECT COUNT(c) FROM ConsentEntity c WHERE c.patientId = :patientId " +
           "AND c.status = 'ACTIVE' " +
           "AND c.effectiveDate <= :now " +
           "AND (c.expirationDate IS NULL OR c.expirationDate > :now)")
    long countActiveConsentsByPatientId(@Param("patientId") String patientId, 
                                       @Param("now") LocalDateTime now);

    /**
     * Find consent records by policy reference
     */
    List<ConsentEntity> findByPolicyReferenceOrderByCreatedAtDesc(String policyReference);

    /**
     * Check if consent exists for patient and organization
     */
    boolean existsByPatientIdAndOrganizationId(String patientId, String organizationId);
}