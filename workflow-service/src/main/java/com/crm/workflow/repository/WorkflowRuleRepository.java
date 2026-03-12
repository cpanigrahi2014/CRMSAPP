package com.crm.workflow.repository;

import com.crm.workflow.entity.WorkflowRule;
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
public interface WorkflowRuleRepository extends JpaRepository<WorkflowRule, UUID> {

    Page<WorkflowRule> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Optional<WorkflowRule> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    @Query("SELECT r FROM WorkflowRule r WHERE r.tenantId = :tenantId AND r.entityType = :entityType " +
            "AND r.triggerEvent = :triggerEvent AND r.active = true AND r.deleted = false")
    List<WorkflowRule> findActiveRules(@Param("tenantId") String tenantId,
                                       @Param("entityType") String entityType,
                                       @Param("triggerEvent") String triggerEvent);

    Page<WorkflowRule> findByTenantIdAndEntityTypeAndDeletedFalse(String tenantId, String entityType, Pageable pageable);

    long countByTenantIdAndActiveAndDeletedFalse(String tenantId, boolean active);
}
