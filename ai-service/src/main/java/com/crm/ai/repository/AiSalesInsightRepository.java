package com.crm.ai.repository;

import com.crm.ai.entity.AiSalesInsightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiSalesInsightRepository extends JpaRepository<AiSalesInsightRecord, UUID> {
    List<AiSalesInsightRecord> findByTenantIdOrderByGeneratedAtDesc(String tenantId);
    List<AiSalesInsightRecord> findByInsightTypeAndTenantId(String insightType, String tenantId);
    Optional<AiSalesInsightRecord> findByIdAndTenantId(UUID id, String tenantId);
}
