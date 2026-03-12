package com.crm.ai.repository;

import com.crm.ai.entity.AiReportInsightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiReportInsightRepository extends JpaRepository<AiReportInsightRecord, UUID> {
    List<AiReportInsightRecord> findByTenantIdOrderByGeneratedAtDesc(String tenantId);
    List<AiReportInsightRecord> findByInsightTypeAndTenantId(String insightType, String tenantId);
}
