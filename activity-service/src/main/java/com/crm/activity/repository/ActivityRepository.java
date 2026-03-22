package com.crm.activity.repository;

import com.crm.activity.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    Page<Activity> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Optional<Activity> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<Activity> findByTenantIdAndTypeAndDeletedFalse(String tenantId, Activity.ActivityType type, Pageable pageable);

    Page<Activity> findByTenantIdAndStatusAndDeletedFalse(String tenantId, Activity.ActivityStatus status, Pageable pageable);

    Page<Activity> findByTenantIdAndTypeAndStatusAndDeletedFalse(String tenantId, Activity.ActivityType type,
                                                                  Activity.ActivityStatus status, Pageable pageable);

    Page<Activity> findByTenantIdAndRelatedEntityTypeAndRelatedEntityIdAndDeletedFalse(
            String tenantId, String relatedEntityType, UUID relatedEntityId, Pageable pageable);

    Page<Activity> findByTenantIdAndRelatedEntityIdAndDeletedFalse(
            String tenantId, UUID relatedEntityId, Pageable pageable);

    Page<Activity> findByTenantIdAndAssignedToAndDeletedFalse(String tenantId, UUID assignedTo, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND a.deleted = false " +
            "AND a.status NOT IN ('COMPLETED', 'CANCELLED') " +
            "AND a.dueDate IS NOT NULL AND a.dueDate < :now")
    Page<Activity> findOverdueActivities(@Param("tenantId") String tenantId,
                                         @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND a.deleted = false " +
            "AND (LOWER(a.subject) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Activity> searchActivities(@Param("tenantId") String tenantId,
                                    @Param("search") String search, Pageable pageable);

    long countByTenantIdAndStatusAndDeletedFalse(String tenantId, Activity.ActivityStatus status);

    /* ---- Analytics queries ---- */
    long countByTenantIdAndTypeAndDeletedFalse(String tenantId, Activity.ActivityType type);

    long countByTenantIdAndDeletedFalse(String tenantId);

    @Query("SELECT a.type, COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId AND a.deleted = false GROUP BY a.type")
    List<Object[]> countByType(@Param("tenantId") String tenantId);

    @Query("SELECT a.status, COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId AND a.deleted = false GROUP BY a.status")
    List<Object[]> countByStatus(@Param("tenantId") String tenantId);

    @Query("SELECT a.priority, COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId AND a.deleted = false GROUP BY a.priority")
    List<Object[]> countByPriority(@Param("tenantId") String tenantId);

    @Query("SELECT a.assignedTo, COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId AND a.deleted = false " +
            "AND a.assignedTo IS NOT NULL GROUP BY a.assignedTo")
    List<Object[]> countByAssignee(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId AND a.deleted = false " +
            "AND a.status NOT IN ('COMPLETED', 'CANCELLED') AND a.dueDate IS NOT NULL AND a.dueDate < :now")
    long countOverdue(@Param("tenantId") String tenantId, @Param("now") LocalDateTime now);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - created_at)) / 86400.0) " +
            "FROM activities WHERE tenant_id = :tenantId AND deleted = false AND status = 'COMPLETED' AND completed_at IS NOT NULL",
            nativeQuery = true)
    Double avgCompletionDays(@Param("tenantId") String tenantId);

    /* ---- Reminder queries ---- */
    @Query("SELECT a FROM Activity a WHERE a.deleted = false AND a.reminderSent = false " +
            "AND a.reminderAt IS NOT NULL AND a.reminderAt <= :now " +
            "AND a.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<Activity> findDueReminders(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Activity a SET a.reminderSent = true WHERE a.id = :id")
    void markReminderSent(@Param("id") UUID id);

    /* ---- Recurring task queries ---- */
    @Query("SELECT a FROM Activity a WHERE a.deleted = false " +
            "AND a.recurrenceRule IS NOT NULL AND a.parentActivityId IS NULL " +
            "AND a.status = 'COMPLETED' " +
            "AND (a.recurrenceEnd IS NULL OR a.recurrenceEnd >= CURRENT_DATE)")
    List<Activity> findCompletedRecurringTasks();

    /* ---- Timeline: all activities by entity, sorted desc ---- */
    List<Activity> findByTenantIdAndRelatedEntityIdAndDeletedFalseOrderByCreatedAtDesc(
            String tenantId, UUID relatedEntityId);

    List<Activity> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(String tenantId);

    /* ---- Upcoming activities for dashboard/calendar ---- */
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND a.deleted = false " +
            "AND a.status NOT IN ('COMPLETED', 'CANCELLED') " +
            "AND a.dueDate IS NOT NULL AND a.dueDate >= :from AND a.dueDate <= :to ORDER BY a.dueDate ASC")
    List<Activity> findUpcoming(@Param("tenantId") String tenantId,
                                @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /* ---- Calendar feed: all non-deleted activities with a date (for iCal export) ---- */
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND a.deleted = false " +
            "AND (a.dueDate IS NOT NULL OR a.startTime IS NOT NULL) " +
            "AND a.dueDate >= :from ORDER BY a.dueDate ASC")
    List<Activity> findForCalendarFeed(@Param("tenantId") String tenantId,
                                       @Param("from") LocalDateTime from);
}
