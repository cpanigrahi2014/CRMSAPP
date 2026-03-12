package com.crm.workflow.dto;

import com.crm.workflow.entity.WorkflowExecutionLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionLogResponse {
    private UUID id;
    private UUID ruleId;
    private UUID triggerEntityId;
    private WorkflowExecutionLog.ExecutionStatus status;
    private String executionDetails;
    private LocalDateTime executedAt;
    private String tenantId;
    private LocalDateTime createdAt;
}
