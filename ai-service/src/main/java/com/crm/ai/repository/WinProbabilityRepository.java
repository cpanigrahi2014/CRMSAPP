package com.crm.ai.repository;

import com.crm.ai.entity.WinProbabilityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WinProbabilityRepository extends JpaRepository<WinProbabilityRecord, UUID> {
    List<WinProbabilityRecord> findByTenantIdOrderByWinProbabilityDesc(String tenantId);
    Optional<WinProbabilityRecord> findByOpportunityIdAndTenantId(String opportunityId, String tenantId);
    Optional<WinProbabilityRecord> findByIdAndTenantId(UUID id, String tenantId);
}
