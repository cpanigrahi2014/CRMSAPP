package com.crm.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    @NotBlank(message = "Trigger event is required")
    private String triggerEvent;

    @Valid
    private List<WorkflowConditionRequest> conditions;

    @NotEmpty(message = "At least one action is required")
    @Valid
    private List<WorkflowActionRequest> actions;
}
