package com.crm.ai.repository;

import com.crm.ai.entity.ChurnPredictionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChurnPredictionRepository extends JpaRepository<ChurnPredictionRecord, UUID> {
    List<ChurnPredictionRecord> findByTenantIdOrderByChurnProbabilityDesc(String tenantId);
    List<ChurnPredictionRecord> findByRiskLevelAndTenantId(String riskLevel, String tenantId);
    Optional<ChurnPredictionRecord> findByAccountIdAndTenantId(String accountId, String tenantId);
    Optional<ChurnPredictionRecord> findByIdAndTenantId(UUID id, String tenantId);
}
