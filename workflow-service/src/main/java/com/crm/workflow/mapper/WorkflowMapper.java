package com.crm.workflow.mapper;

import com.crm.workflow.dto.*;
import com.crm.workflow.entity.WorkflowAction;
import com.crm.workflow.entity.WorkflowCondition;
import com.crm.workflow.entity.WorkflowExecutionLog;
import com.crm.workflow.entity.WorkflowRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

    @Mapping(target = "conditions", source = "conditions")
    @Mapping(target = "actions", source = "actions")
    WorkflowRuleResponse toRuleResponse(WorkflowRule rule);

    List<WorkflowRuleResponse> toRuleResponseList(List<WorkflowRule> rules);

    WorkflowConditionResponse toConditionResponse(WorkflowCondition condition);

    WorkflowActionResponse toActionResponse(WorkflowAction action);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rule", ignore = true)
    WorkflowCondition toConditionEntity(WorkflowConditionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rule", ignore = true)
    WorkflowAction toActionEntity(WorkflowActionRequest request);

    WorkflowExecutionLogResponse toExecutionLogResponse(WorkflowExecutionLog log);

    List<WorkflowExecutionLogResponse> toExecutionLogResponseList(List<WorkflowExecutionLog> logs);
}
