package com.crm.workflow.repository;

import com.crm.workflow.entity.WorkflowAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowActionRepository extends JpaRepository<WorkflowAction, UUID> {

    List<WorkflowAction> findByRuleIdOrderByActionOrderAsc(UUID ruleId);

    void deleteByRuleId(UUID ruleId);
}
