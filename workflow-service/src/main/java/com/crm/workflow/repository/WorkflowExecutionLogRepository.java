package com.crm.workflow.repository;

import com.crm.workflow.entity.WorkflowExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkflowExecutionLogRepository extends JpaRepository<WorkflowExecutionLog, UUID> {

    Page<WorkflowExecutionLog> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Page<WorkflowExecutionLog> findByTenantIdAndRuleIdAndDeletedFalse(String tenantId, UUID ruleId, Pageable pageable);

    Page<WorkflowExecutionLog> findByTenantIdAndStatusAndDeletedFalse(String tenantId,
                                                                       WorkflowExecutionLog.ExecutionStatus status,
                                                                       Pageable pageable);

    long countByTenantIdAndRuleIdAndDeletedFalse(String tenantId, UUID ruleId);
}
