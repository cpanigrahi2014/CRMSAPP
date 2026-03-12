package com.crm.opportunity.repository;

import com.crm.opportunity.entity.OpportunityNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityNoteRepository extends JpaRepository<OpportunityNote, UUID> {

    Page<OpportunityNote> findByOpportunityIdAndTenantIdOrderByIsPinnedDescCreatedAtDesc(UUID opportunityId, String tenantId, Pageable pageable);

    Optional<OpportunityNote> findByIdAndTenantId(UUID id, String tenantId);

    long countByOpportunityIdAndTenantId(UUID opportunityId, String tenantId);

    void deleteByIdAndTenantId(UUID id, String tenantId);
}
