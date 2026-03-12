package com.crm.workflow.dto;

import com.crm.workflow.entity.WorkflowCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowConditionRequest {

    @NotBlank(message = "Field name is required")
    private String fieldName;

    @NotNull(message = "Operator is required")
    private WorkflowCondition.ConditionOperator operator;

    private String value;

    @Builder.Default
    private WorkflowCondition.LogicalOperator logicalOperator = WorkflowCondition.LogicalOperator.AND;
}
