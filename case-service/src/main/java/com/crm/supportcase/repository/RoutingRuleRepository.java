package com.crm.supportcase.repository;

import com.crm.supportcase.entity.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {

    Optional<RoutingRule> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    List<RoutingRule> findByTenantIdAndDeletedFalse(String tenantId);

    List<RoutingRule> findByTenantIdAndActiveAndDeletedFalseOrderByRulePriorityAsc(
            String tenantId, boolean active);

    List<RoutingRule> findByTenantIdAndQueueIdAndDeletedFalse(String tenantId, UUID queueId);
}
