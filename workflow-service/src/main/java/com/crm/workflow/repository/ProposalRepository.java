package com.crm.workflow.repository;

import com.crm.workflow.entity.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProposalRepository extends JpaRepository<Proposal, UUID> {

    Page<Proposal> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<Proposal> findByOpportunityIdAndDeletedFalseOrderByVersionDesc(UUID opportunityId, Pageable pageable);

    Optional<Proposal> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    List<Proposal> findByTenantIdAndStatusAndDeletedFalse(String tenantId, String status);

    long countByTenantIdAndDeletedFalse(String tenantId);
}
