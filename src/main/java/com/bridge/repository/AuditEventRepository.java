package com.bridge.repository;

import com.bridge.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for audit event data access operations
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    /**
     * Find audit event by event ID
     */
    Optional<AuditEventEntity> findByEventId(String eventId);

    /**
     * Find audit events by user ID
     */
    List<AuditEventEntity> findByUserIdOrderByTimestampDesc(String userId);

    /**
     * Find audit events by user ID with pagination
     */
    Page<AuditEventEntity> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    /**
     * Find audit events by action
     */
    List<AuditEventEntity> findByActionOrderByTimestampDesc(String action);

    /**
     * Find audit events by action with pagination
     */
    Page<AuditEventEntity> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    /**
     * Find audit events by resource type and ID
     */
    List<AuditEventEntity> findByResourceTypeAndResourceIdOrderByTimestampDesc(String resourceType, String resourceId);

    /**
     * Find audit events by resource type
     */
    List<AuditEventEntity> findByResourceTypeOrderByTimestampDesc(String resourceType);

    /**
     * Find audit events by outcome
     */
    List<AuditEventEntity> findByOutcomeOrderByTimestampDesc(String outcome);

    /**
     * Find audit events within a time range
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findByTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * Find audit events within a time range with pagination
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    Page<AuditEventEntity> findByTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                                  @Param("endTime") LocalDateTime endTime, 
                                                  Pageable pageable);

    /**
     * Find audit events by user and action within time range
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.userId = :userId " +
           "AND a.action = :action " +
           "AND a.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findByUserIdAndActionAndTimestampBetween(
            @Param("userId") String userId, 
            @Param("action") String action, 
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find failed audit events (FAILURE or ERROR outcome)
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.outcome IN ('FAILURE', 'ERROR') ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findFailedEvents();

    /**
     * Find failed audit events with pagination
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.outcome IN ('FAILURE', 'ERROR') ORDER BY a.timestamp DESC")
    Page<AuditEventEntity> findFailedEvents(Pageable pageable);

    /**
     * Find successful audit events
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.outcome = 'SUCCESS' ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findSuccessfulEvents();

    /**
     * Count audit events by action within time range
     */
    @Query("SELECT COUNT(a) FROM AuditEventEntity a WHERE a.action = :action " +
           "AND a.timestamp BETWEEN :startTime AND :endTime")
    long countByActionAndTimestampBetween(@Param("action") String action, 
                                         @Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * Count audit events by user within time range
     */
    @Query("SELECT COUNT(a) FROM AuditEventEntity a WHERE a.userId = :userId " +
           "AND a.timestamp BETWEEN :startTime AND :endTime")
    long countByUserIdAndTimestampBetween(@Param("userId") String userId, 
                                         @Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * Count failed events within time range
     */
    @Query("SELECT COUNT(a) FROM AuditEventEntity a WHERE a.outcome IN ('FAILURE', 'ERROR') " +
           "AND a.timestamp BETWEEN :startTime AND :endTime")
    long countFailedEventsByTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * Find recent audit events (last N hours)
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findRecentEvents(@Param("since") LocalDateTime since);

    /**
     * Find recent audit events with pagination
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    Page<AuditEventEntity> findRecentEvents(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Delete old audit events before a certain date
     */
    void deleteByTimestampBefore(LocalDateTime cutoffDate);
}