package com.crm.workflow.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflow_rules", indexes = {
        @Index(name = "idx_workflow_rule_tenant", columnList = "tenant_id"),
        @Index(name = "idx_workflow_rule_entity_type", columnList = "entity_type"),
        @Index(name = "idx_workflow_rule_trigger_event", columnList = "trigger_event"),
        @Index(name = "idx_workflow_rule_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowRule extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "trigger_event", nullable = false)
    private String triggerEvent;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("logicalOperator ASC")
    @Builder.Default
    private List<WorkflowCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("actionOrder ASC")
    @Builder.Default
    private List<WorkflowAction> actions = new ArrayList<>();

    public void addCondition(WorkflowCondition condition) {
        conditions.add(condition);
        condition.setRule(this);
    }

    public void addAction(WorkflowAction action) {
        actions.add(action);
        action.setRule(this);
    }
}
