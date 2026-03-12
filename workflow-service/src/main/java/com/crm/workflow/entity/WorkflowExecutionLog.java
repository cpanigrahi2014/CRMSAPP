package com.crm.workflow.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_execution_logs", indexes = {
        @Index(name = "idx_exec_log_tenant", columnList = "tenant_id"),
        @Index(name = "idx_exec_log_rule", columnList = "rule_id"),
        @Index(name = "idx_exec_log_status", columnList = "status"),
        @Index(name = "idx_exec_log_executed_at", columnList = "executed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecutionLog extends BaseEntity {

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "trigger_entity_id", nullable = false)
    private UUID triggerEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status;

    @Column(name = "execution_details", columnDefinition = "TEXT")
    private String executionDetails;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    public enum ExecutionStatus {
        SUCCESS,
        FAILED,
        SKIPPED
    }
}
