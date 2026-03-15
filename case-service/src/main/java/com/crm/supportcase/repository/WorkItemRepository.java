package com.crm.supportcase.repository;

import com.crm.supportcase.entity.WorkItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, UUID> {

    Optional<WorkItem> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<WorkItem> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Page<WorkItem> findByTenantIdAndStatusAndDeletedFalse(
            String tenantId, WorkItem.WorkItemStatus status, Pageable pageable);

    List<WorkItem> findByTenantIdAndQueueIdAndStatusAndDeletedFalseOrderByPriorityDescCreatedAtAsc(
            String tenantId, UUID queueId, WorkItem.WorkItemStatus status);

    List<WorkItem> findByTenantIdAndAssignedAgentIdAndStatusInAndDeletedFalse(
            String tenantId, UUID agentId, List<WorkItem.WorkItemStatus> statuses);

    Optional<WorkItem> findByEntityTypeAndEntityIdAndTenantIdAndDeletedFalseAndStatusIn(
            String entityType, UUID entityId, String tenantId, List<WorkItem.WorkItemStatus> statuses);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.tenantId = :tenantId AND w.deleted = false")
    long countByTenant(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.tenantId = :tenantId AND w.deleted = false AND w.status = :status")
    long countByTenantAndStatus(@Param("tenantId") String tenantId, @Param("status") WorkItem.WorkItemStatus status);

    @Query("SELECT AVG(w.waitTimeSeconds) FROM WorkItem w WHERE w.tenantId = :tenantId AND w.deleted = false AND w.waitTimeSeconds IS NOT NULL")
    Double avgWaitTime(@Param("tenantId") String tenantId);

    @Query("SELECT AVG(w.handleTimeSeconds) FROM WorkItem w WHERE w.tenantId = :tenantId AND w.deleted = false AND w.handleTimeSeconds IS NOT NULL")
    Double avgHandleTime(@Param("tenantId") String tenantId);

    @Query("SELECT w.channel, COUNT(w) FROM WorkItem w WHERE w.tenantId = :tenantId AND w.deleted = false GROUP BY w.channel")
    List<Object[]> countByChannel(@Param("tenantId") String tenantId);

    @Query("SELECT CAST(w.status AS string), COUNT(w) FROM WorkItem w WHERE w.tenantId = :tenantId AND w.deleted = false GROUP BY w.status")
    List<Object[]> countByStatus(@Param("tenantId") String tenantId);
}
