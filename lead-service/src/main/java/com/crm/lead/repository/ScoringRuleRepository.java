package com.crm.lead.repository;

import com.crm.lead.entity.ScoringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScoringRuleRepository extends JpaRepository<ScoringRule, UUID> {
    List<ScoringRule> findByTenantIdAndActiveTrueOrderByCreatedAtDesc(String tenantId);
    List<ScoringRule> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
