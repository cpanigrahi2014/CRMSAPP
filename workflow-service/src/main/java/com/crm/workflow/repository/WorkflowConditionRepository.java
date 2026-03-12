package com.crm.workflow.repository;

import com.crm.workflow.entity.WorkflowCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowConditionRepository extends JpaRepository<WorkflowCondition, UUID> {

    List<WorkflowCondition> findByRuleIdOrderByLogicalOperatorAsc(UUID ruleId);

    void deleteByRuleId(UUID ruleId);
}
