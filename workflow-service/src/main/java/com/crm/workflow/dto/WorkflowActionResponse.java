package com.crm.workflow.dto;

import com.crm.workflow.entity.WorkflowAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowActionResponse {
    private UUID id;
    private WorkflowAction.ActionType actionType;
    private String targetField;
    private String targetValue;
    private Integer actionOrder;
}
