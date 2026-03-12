package com.crm.opportunity.repository;

import com.crm.opportunity.entity.OpportunityCompetitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityCompetitorRepository extends JpaRepository<OpportunityCompetitor, UUID> {

    Page<OpportunityCompetitor> findByOpportunityIdAndTenantId(UUID opportunityId, String tenantId, Pageable pageable);

    Optional<OpportunityCompetitor> findByIdAndTenantId(UUID id, String tenantId);

    long countByOpportunityIdAndTenantId(UUID opportunityId, String tenantId);

    void deleteByIdAndTenantId(UUID id, String tenantId);
}
