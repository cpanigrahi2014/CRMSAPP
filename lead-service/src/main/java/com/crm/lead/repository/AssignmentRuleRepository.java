package com.crm.lead.repository;

import com.crm.lead.entity.AssignmentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, UUID> {
    List<AssignmentRule> findByTenantIdAndActiveTrueOrderByPriorityDesc(String tenantId);
    List<AssignmentRule> findByTenantIdOrderByPriorityDesc(String tenantId);
}
