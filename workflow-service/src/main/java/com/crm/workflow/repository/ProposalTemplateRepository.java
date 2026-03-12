package com.crm.workflow.repository;

import com.crm.workflow.entity.ProposalTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProposalTemplateRepository extends JpaRepository<ProposalTemplate, UUID> {

    Page<ProposalTemplate> findByTenantId(String tenantId, Pageable pageable);

    List<ProposalTemplate> findByTenantIdAndCategory(String tenantId, String category);

    Optional<ProposalTemplate> findByTenantIdAndIsDefaultTrue(String tenantId);
}
