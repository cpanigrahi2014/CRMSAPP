package com.crm.workflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "workflow_actions", indexes = {
        @Index(name = "idx_workflow_action_rule", columnList = "rule_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    @JsonIgnore
    private WorkflowRule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "target_field")
    private String targetField;

    @Column(name = "target_value", columnDefinition = "TEXT")
    private String targetValue;

    @Column(name = "action_order")
    @Builder.Default
    private Integer actionOrder = 0;

    public enum ActionType {
        SEND_EMAIL,
        CREATE_TASK,
        UPDATE_FIELD,
        SEND_NOTIFICATION,
        ASSIGN_TO
    }
}
