package com.crm.workflow.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowRuleRequest {

    private String name;
    private String description;
    private String entityType;
    private String triggerEvent;

    @Valid
    private List<WorkflowConditionRequest> conditions;

    @Valid
    private List<WorkflowActionRequest> actions;
}
