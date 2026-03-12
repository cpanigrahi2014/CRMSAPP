package com.crm.workflow.service;

import com.crm.common.event.EventPublisher;
import com.crm.workflow.entity.*;
import com.crm.workflow.repository.WorkflowExecutionLogRepository;
import com.crm.workflow.repository.WorkflowRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final WorkflowRuleRepository ruleRepository;
    private final WorkflowExecutionLogRepository executionLogRepository;
    private final EventPublisher eventPublisher;

    /**
     * Evaluates all active workflow rules matching the given entity type and trigger event.
     * For each matching rule, conditions are evaluated against the entity data.
     * If conditions pass, actions are executed in order.
     */
    @Transactional
    public void evaluateRules(String tenantId, String entityType, String triggerEvent,
                              String entityId, Map<String, Object> entityData) {
        log.info("Evaluating workflow rules for tenant: {}, entity: {}, event: {}",
                tenantId, entityType, triggerEvent);

        List<WorkflowRule> activeRules = ruleRepository.findActiveRules(tenantId, entityType, triggerEvent);

        if (activeRules.isEmpty()) {
            log.debug("No active workflow rules found for entity: {}, event: {}", entityType, triggerEvent);
            return;
        }

        log.info("Found {} active rules to evaluate", activeRules.size());

        for (WorkflowRule rule : activeRules) {
            evaluateAndExecuteRule(rule, tenantId, entityId, entityData);
        }
    }

    private void evaluateAndExecuteRule(WorkflowRule rule, String tenantId,
                                         String entityId, Map<String, Object> entityData) {
        UUID entityUuid;
        try {
            entityUuid = UUID.fromString(entityId);
        } catch (IllegalArgumentException e) {
            entityUuid = UUID.nameUUIDFromBytes(entityId.getBytes());
        }

        try {
            boolean conditionsMet = evaluateConditions(rule.getConditions(), entityData);

            if (!conditionsMet) {
                log.debug("Conditions not met for rule: {} ({})", rule.getName(), rule.getId());
                logExecution(rule.getId(), entityUuid, tenantId,
                        WorkflowExecutionLog.ExecutionStatus.SKIPPED,
                        "Conditions not met for rule: " + rule.getName());
                return;
            }

            log.info("Conditions met for rule: {} ({}). Executing actions.", rule.getName(), rule.getId());
            List<String> actionResults = executeActions(rule.getActions(), tenantId, entityId, entityData);

            String details = String.format("Rule '%s' executed successfully. Actions: [%s]",
                    rule.getName(), String.join(", ", actionResults));

            logExecution(rule.getId(), entityUuid, tenantId,
                    WorkflowExecutionLog.ExecutionStatus.SUCCESS, details);

        } catch (Exception e) {
            log.error("Error executing workflow rule: {} ({})", rule.getName(), rule.getId(), e);
            logExecution(rule.getId(), entityUuid, tenantId,
                    WorkflowExecutionLog.ExecutionStatus.FAILED,
                    "Error: " + e.getMessage());
        }
    }

    /**
     * Evaluates a list of conditions against entity data using logical operators.
     * Conditions are evaluated in order. The first condition's logicalOperator is ignored
     * (it starts the evaluation). Subsequent conditions use AND/OR to combine with the running result.
     */
    boolean evaluateConditions(List<WorkflowCondition> conditions, Map<String, Object> entityData) {
        if (conditions == null || conditions.isEmpty()) {
            return true; // No conditions means the rule always matches
        }

        boolean result = evaluateSingleCondition(conditions.get(0), entityData);

        for (int i = 1; i < conditions.size(); i++) {
            WorkflowCondition condition = conditions.get(i);
            boolean conditionResult = evaluateSingleCondition(condition, entityData);

            if (condition.getLogicalOperator() == WorkflowCondition.LogicalOperator.OR) {
                result = result || conditionResult;
            } else {
                result = result && conditionResult;
            }
        }

        return result;
    }

    /**
     * Evaluates a single condition against entity data.
     * Supports nested field access using dot notation (e.g., "address.city").
     */
    private boolean evaluateSingleCondition(WorkflowCondition condition, Map<String, Object> entityData) {
        String fieldName = condition.getFieldName();
        Object actualValue = resolveFieldValue(fieldName, entityData);
        String expectedValue = condition.getValue();

        log.debug("Evaluating condition: field={}, operator={}, expected={}, actual={}",
                fieldName, condition.getOperator(), expectedValue, actualValue);

        return switch (condition.getOperator()) {
            case EQUALS -> evaluateEquals(actualValue, expectedValue);
            case NOT_EQUALS -> !evaluateEquals(actualValue, expectedValue);
            case GREATER_THAN -> evaluateGreaterThan(actualValue, expectedValue);
            case LESS_THAN -> evaluateLessThan(actualValue, expectedValue);
            case CONTAINS -> evaluateContains(actualValue, expectedValue);
            case IN -> evaluateIn(actualValue, expectedValue);
            case IS_NULL -> actualValue == null;
            case IS_NOT_NULL -> actualValue != null;
        };
    }

    /**
     * Resolves field value from entity data, supporting dot notation for nested access.
     */
    @SuppressWarnings("unchecked")
    private Object resolveFieldValue(String fieldName, Map<String, Object> entityData) {
        if (entityData == null) return null;

        // Support dot notation for nested fields (e.g., "address.city")
        String[] parts = fieldName.split("\\.");
        Object current = entityData;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
            if (current == null) return null;
        }

        return current;
    }

    private boolean evaluateEquals(Object actual, String expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        String actualStr = String.valueOf(actual);
        return actualStr.equalsIgnoreCase(expected);
    }

    private boolean evaluateGreaterThan(Object actual, String expected) {
        if (actual == null || expected == null) return false;
        try {
            double actualNum = toDouble(actual);
            double expectedNum = Double.parseDouble(expected);
            return actualNum > expectedNum;
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return String.valueOf(actual).compareTo(expected) > 0;
        }
    }

    private boolean evaluateLessThan(Object actual, String expected) {
        if (actual == null || expected == null) return false;
        try {
            double actualNum = toDouble(actual);
            double expectedNum = Double.parseDouble(expected);
            return actualNum < expectedNum;
        } catch (NumberFormatException e) {
            return String.valueOf(actual).compareTo(expected) < 0;
        }
    }

    private boolean evaluateContains(Object actual, String expected) {
        if (actual == null || expected == null) return false;
        String actualStr = String.valueOf(actual).toLowerCase();
        return actualStr.contains(expected.toLowerCase());
    }

    private boolean evaluateIn(Object actual, String expected) {
        if (actual == null || expected == null) return false;
        String actualStr = String.valueOf(actual).trim();
        // expected is a comma-separated list of values
        String[] values = expected.split(",");
        for (String value : values) {
            if (actualStr.equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    /**
     * Executes the list of actions for a matched rule, in order.
     * Each action type produces a Kafka event that downstream services can consume.
     */
    List<String> executeActions(List<WorkflowAction> actions, String tenantId,
                                String entityId, Map<String, Object> entityData) {
        List<String> results = new ArrayList<>();

        for (WorkflowAction action : actions) {
            String result = executeSingleAction(action, tenantId, entityId, entityData);
            results.add(result);
        }

        return results;
    }

    private String executeSingleAction(WorkflowAction action, String tenantId,
                                        String entityId, Map<String, Object> entityData) {
        log.info("Executing action: type={}, targetField={}, targetValue={}",
                action.getActionType(), action.getTargetField(), action.getTargetValue());

        return switch (action.getActionType()) {
            case SEND_EMAIL -> executeSendEmail(action, tenantId, entityId, entityData);
            case CREATE_TASK -> executeCreateTask(action, tenantId, entityId, entityData);
            case UPDATE_FIELD -> executeUpdateField(action, tenantId, entityId, entityData);
            case SEND_NOTIFICATION -> executeSendNotification(action, tenantId, entityId, entityData);
            case ASSIGN_TO -> executeAssignTo(action, tenantId, entityId, entityData);
        };
    }

    private String executeSendEmail(WorkflowAction action, String tenantId,
                                     String entityId, Map<String, Object> entityData) {
        Map<String, Object> emailPayload = new HashMap<>();
        emailPayload.put("actionType", "SEND_EMAIL");
        emailPayload.put("entityId", entityId);
        emailPayload.put("recipient", action.getTargetField());
        emailPayload.put("templateOrBody", action.getTargetValue());
        emailPayload.put("entityData", entityData);

        eventPublisher.publish("workflow-actions", tenantId, "workflow-engine",
                "WorkflowAction", entityId, "SEND_EMAIL", emailPayload);

        return "SEND_EMAIL to " + action.getTargetField();
    }

    private String executeCreateTask(WorkflowAction action, String tenantId,
                                      String entityId, Map<String, Object> entityData) {
        Map<String, Object> taskPayload = new HashMap<>();
        taskPayload.put("actionType", "CREATE_TASK");
        taskPayload.put("entityId", entityId);
        taskPayload.put("taskSubject", action.getTargetField());
        taskPayload.put("taskDescription", action.getTargetValue());
        taskPayload.put("entityData", entityData);

        eventPublisher.publish("workflow-actions", tenantId, "workflow-engine",
                "WorkflowAction", entityId, "CREATE_TASK", taskPayload);

        return "CREATE_TASK: " + action.getTargetField();
    }

    private String executeUpdateField(WorkflowAction action, String tenantId,
                                       String entityId, Map<String, Object> entityData) {
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("actionType", "UPDATE_FIELD");
        updatePayload.put("entityId", entityId);
        updatePayload.put("fieldName", action.getTargetField());
        updatePayload.put("fieldValue", action.getTargetValue());
        updatePayload.put("entityData", entityData);

        eventPublisher.publish("workflow-actions", tenantId, "workflow-engine",
                "WorkflowAction", entityId, "UPDATE_FIELD", updatePayload);

        return "UPDATE_FIELD: " + action.getTargetField() + " = " + action.getTargetValue();
    }

    private String executeSendNotification(WorkflowAction action, String tenantId,
                                            String entityId, Map<String, Object> entityData) {
        Map<String, Object> notifPayload = new HashMap<>();
        notifPayload.put("actionType", "SEND_NOTIFICATION");
        notifPayload.put("entityId", entityId);
        notifPayload.put("recipient", action.getTargetField());
        notifPayload.put("message", action.getTargetValue());
        notifPayload.put("entityData", entityData);

        eventPublisher.publish("notification-events", tenantId, "workflow-engine",
                "WorkflowAction", entityId, "SEND_NOTIFICATION", notifPayload);

        return "SEND_NOTIFICATION to " + action.getTargetField();
    }

    private String executeAssignTo(WorkflowAction action, String tenantId,
                                    String entityId, Map<String, Object> entityData) {
        Map<String, Object> assignPayload = new HashMap<>();
        assignPayload.put("actionType", "ASSIGN_TO");
        assignPayload.put("entityId", entityId);
        assignPayload.put("assigneeId", action.getTargetValue());
        assignPayload.put("entityData", entityData);

        eventPublisher.publish("workflow-actions", tenantId, "workflow-engine",
                "WorkflowAction", entityId, "ASSIGN_TO", assignPayload);

        return "ASSIGN_TO: " + action.getTargetValue();
    }

    private void logExecution(UUID ruleId, UUID entityId, String tenantId,
                               WorkflowExecutionLog.ExecutionStatus status, String details) {
        WorkflowExecutionLog logEntry = WorkflowExecutionLog.builder()
                .ruleId(ruleId)
                .triggerEntityId(entityId)
                .status(status)
                .executionDetails(details)
                .executedAt(LocalDateTime.now())
                .build();
        logEntry.setTenantId(tenantId);

        executionLogRepository.save(logEntry);
        log.debug("Execution log saved: ruleId={}, status={}", ruleId, status);
    }
}
