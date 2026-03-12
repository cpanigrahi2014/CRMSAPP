package com.crm.workflow.repository;

import com.crm.workflow.entity.WorkflowTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

    @Query("SELECT t FROM WorkflowTemplate t WHERE (t.tenantId = :tenantId OR t.tenantId IS NULL) ORDER BY t.popularity DESC")
    Page<WorkflowTemplate> findAvailableTemplates(String tenantId, Pageable pageable);

    Page<WorkflowTemplate> findByCategoryAndTenantIdOrTenantIdIsNull(String category, String tenantId, Pageable pageable);

    List<WorkflowTemplate> findByEntityTypeAndTenantIdOrTenantIdIsNull(String entityType, String tenantId);
}
