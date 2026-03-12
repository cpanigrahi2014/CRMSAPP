package com.crm.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRuleResponse {
    private UUID id;
    private String name;
    private String description;
    private String entityType;
    private String triggerEvent;
    private boolean active;
    private List<WorkflowConditionResponse> conditions;
    private List<WorkflowActionResponse> actions;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
