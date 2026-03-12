package com.crm.workflow.repository;

import com.crm.workflow.entity.WorkflowSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowSuggestionRepository extends JpaRepository<WorkflowSuggestion, UUID> {

    Page<WorkflowSuggestion> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);

    Page<WorkflowSuggestion> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<WorkflowSuggestion> findByTenantIdAndStatusOrderByConfidenceDesc(String tenantId, String status);

    long countByTenantIdAndStatus(String tenantId, String status);
}
