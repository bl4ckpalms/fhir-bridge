package com.bridge.repository;

import com.bridge.entity.ConsentEntity;
import com.bridge.model.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
     * Find active consent records for a patient (optimized with index hints)
     */
    @Query(value = "SELECT c.* FROM consent_records c " +
           "WHERE c.patient_id = :patientId " +
           "AND c.status = 'ACTIVE' " +
           "AND c.effective_date <= :now " +
           "AND (c.expiration_date IS NULL OR c.expiration_date > :now) " +
           "ORDER BY c.effective_date DESC", 
           nativeQuery = true)
    List<ConsentEntity> findActiveConsentsByPatientId(@Param("patientId") String patientId, 
                                                      @Param("now") LocalDateTime now);

    /**
     * Find active consent record for a patient and organization (optimized)
     */
    @Query(value = "SELECT c.* FROM consent_records c " +
           "WHERE c.patient_id = :patientId " +
           "AND c.organization_id = :organizationId " +
           "AND c.status = 'ACTIVE' " +
           "AND c.effective_date <= :now " +
           "AND (c.expiration_date IS NULL OR c.expiration_date > :now) " +
           "LIMIT 1", 
           nativeQuery = true)
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

    /**
     * Batch update expired consents (optimized for performance)
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE consent_records SET status = 'EXPIRED', updated_at = :now " +
           "WHERE expiration_date IS NOT NULL " +
           "AND expiration_date <= :now " +
           "AND status != 'EXPIRED'", 
           nativeQuery = true)
    int batchUpdateExpiredConsents(@Param("now") LocalDateTime now);

    /**
     * Find consents by multiple patient IDs (batch lookup)
     */
    @Query(value = "SELECT c.* FROM consent_records c " +
           "WHERE c.patient_id IN :patientIds " +
           "AND c.status = 'ACTIVE' " +
           "AND c.effective_date <= :now " +
           "AND (c.expiration_date IS NULL OR c.expiration_date > :now) " +
           "ORDER BY c.patient_id, c.effective_date DESC", 
           nativeQuery = true)
    List<ConsentEntity> findActiveConsentsByPatientIds(@Param("patientIds") List<String> patientIds, 
                                                       @Param("now") LocalDateTime now);

    /**
     * Get consent statistics for monitoring
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total_consents, " +
           "COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_consents, " +
           "COUNT(CASE WHEN status = 'EXPIRED' THEN 1 END) as expired_consents, " +
           "COUNT(CASE WHEN status = 'REVOKED' THEN 1 END) as revoked_consents " +
           "FROM consent_records", 
           nativeQuery = true)
    Object[] getConsentStatistics();

    /**
     * Find recently created consents for audit purposes
     */
    @Query(value = "SELECT c.* FROM consent_records c " +
           "WHERE c.created_at >= :since " +
           "ORDER BY c.created_at DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<ConsentEntity> findRecentConsents(@Param("since") LocalDateTime since, 
                                          @Param("limit") int limit);
}