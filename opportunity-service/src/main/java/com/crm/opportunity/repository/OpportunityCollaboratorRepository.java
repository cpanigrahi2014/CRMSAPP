package com.crm.opportunity.repository;

import com.crm.opportunity.entity.OpportunityCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityCollaboratorRepository extends JpaRepository<OpportunityCollaborator, UUID> {

    List<OpportunityCollaborator> findByOpportunityIdAndTenantId(UUID opportunityId, String tenantId);

    Optional<OpportunityCollaborator> findByOpportunityIdAndUserIdAndTenantId(UUID opportunityId, UUID userId, String tenantId);

    void deleteByOpportunityIdAndUserIdAndTenantId(UUID opportunityId, UUID userId, String tenantId);

    long countByOpportunityIdAndTenantId(UUID opportunityId, String tenantId);
}
