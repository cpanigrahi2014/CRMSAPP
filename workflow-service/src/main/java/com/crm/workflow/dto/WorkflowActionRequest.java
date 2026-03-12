package com.crm.workflow.dto;

import com.crm.workflow.entity.WorkflowAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowActionRequest {

    @NotNull(message = "Action type is required")
    private WorkflowAction.ActionType actionType;

    private String targetField;
    private String targetValue;

    @Builder.Default
    private Integer actionOrder = 0;
}
