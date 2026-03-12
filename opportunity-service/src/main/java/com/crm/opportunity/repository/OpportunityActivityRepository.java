package com.crm.opportunity.repository;

import com.crm.opportunity.entity.OpportunityActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OpportunityActivityRepository extends JpaRepository<OpportunityActivity, UUID> {

    Page<OpportunityActivity> findByOpportunityIdAndTenantIdOrderByCreatedAtDesc(UUID opportunityId, String tenantId, Pageable pageable);

    long countByOpportunityIdAndTenantId(UUID opportunityId, String tenantId);
}
