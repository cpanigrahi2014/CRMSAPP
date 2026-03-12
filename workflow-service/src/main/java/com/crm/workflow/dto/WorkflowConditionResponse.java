package com.crm.workflow.dto;

import com.crm.workflow.entity.WorkflowCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowConditionResponse {
    private UUID id;
    private String fieldName;
    private WorkflowCondition.ConditionOperator operator;
    private String value;
    private WorkflowCondition.LogicalOperator logicalOperator;
}
