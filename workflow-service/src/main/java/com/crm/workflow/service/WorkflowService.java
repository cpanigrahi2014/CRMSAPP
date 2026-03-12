package com.crm.workflow.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.workflow.dto.*;
import com.crm.workflow.entity.WorkflowAction;
import com.crm.workflow.entity.WorkflowCondition;
import com.crm.workflow.entity.WorkflowExecutionLog;
import com.crm.workflow.entity.WorkflowRule;
import com.crm.workflow.mapper.WorkflowMapper;
import com.crm.workflow.repository.WorkflowExecutionLogRepository;
import com.crm.workflow.repository.WorkflowRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRuleRepository ruleRepository;
    private final WorkflowExecutionLogRepository executionLogRepository;
    private final WorkflowMapper workflowMapper;

    @Transactional
    @CacheEvict(value = "workflow-rules", allEntries = true)
    public WorkflowRuleResponse createRule(CreateWorkflowRuleRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating workflow rule '{}' for tenant: {}", request.getName(), tenantId);

        WorkflowRule rule = WorkflowRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .entityType(request.getEntityType())
                .triggerEvent(request.getTriggerEvent())
                .active(true)
                .build();
        rule.setTenantId(tenantId);

        if (request.getConditions() != null) {
            for (WorkflowConditionRequest condReq : request.getConditions()) {
                WorkflowCondition condition = workflowMapper.toConditionEntity(condReq);
                rule.addCondition(condition);
            }
        }

        if (request.getActions() != null) {
            for (WorkflowActionRequest actReq : request.getActions()) {
                WorkflowAction action = workflowMapper.toActionEntity(actReq);
                rule.addAction(action);
            }
        }

        WorkflowRule savedRule = ruleRepository.save(rule);
        log.info("Workflow rule created: {} for tenant: {}", savedRule.getId(), tenantId);

        return workflowMapper.toRuleResponse(savedRule);
    }

    @Transactional
    @CacheEvict(value = "workflow-rules", allEntries = true)
    public WorkflowRuleResponse updateRule(UUID ruleId, UpdateWorkflowRuleRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Updating workflow rule: {} for tenant: {}", ruleId, tenantId);

        WorkflowRule rule = ruleRepository.findByIdAndTenantIdAndDeletedFalse(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRule", "id", ruleId));

        if (request.getName() != null) rule.setName(request.getName());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        if (request.getEntityType() != null) rule.setEntityType(request.getEntityType());
        if (request.getTriggerEvent() != null) rule.setTriggerEvent(request.getTriggerEvent());

        if (request.getConditions() != null) {
            rule.getConditions().clear();
            for (WorkflowConditionRequest condReq : request.getConditions()) {
                WorkflowCondition condition = workflowMapper.toConditionEntity(condReq);
                rule.addCondition(condition);
            }
        }

        if (request.getActions() != null) {
            rule.getActions().clear();
            for (WorkflowActionRequest actReq : request.getActions()) {
                WorkflowAction action = workflowMapper.toActionEntity(actReq);
                rule.addAction(action);
            }
        }

        WorkflowRule updatedRule = ruleRepository.save(rule);
        log.info("Workflow rule updated: {}", ruleId);

        return workflowMapper.toRuleResponse(updatedRule);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "workflow-rules", key = "#ruleId + '_' + T(com.crm.common.security.TenantContext).getTenantId()")
    public WorkflowRuleResponse getRuleById(UUID ruleId) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching workflow rule: {} for tenant: {}", ruleId, tenantId);

        WorkflowRule rule = ruleRepository.findByIdAndTenantIdAndDeletedFalse(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRule", "id", ruleId));

        return workflowMapper.toRuleResponse(rule);
    }

    @Transactional(readOnly = true)
    public PagedResponse<WorkflowRuleResponse> getAllRules(int page, int size, String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowRule> rulePage = ruleRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);

        return PagedResponse.<WorkflowRuleResponse>builder()
                .content(rulePage.getContent().stream().map(workflowMapper::toRuleResponse).toList())
                .pageNumber(rulePage.getNumber())
                .pageSize(rulePage.getSize())
                .totalElements(rulePage.getTotalElements())
                .totalPages(rulePage.getTotalPages())
                .last(rulePage.isLast())
                .first(rulePage.isFirst())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<WorkflowRuleResponse> getRulesByEntityType(String entityType, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<WorkflowRule> rulePage = ruleRepository.findByTenantIdAndEntityTypeAndDeletedFalse(tenantId, entityType, pageable);

        return PagedResponse.<WorkflowRuleResponse>builder()
                .content(rulePage.getContent().stream().map(workflowMapper::toRuleResponse).toList())
                .pageNumber(rulePage.getNumber())
                .pageSize(rulePage.getSize())
                .totalElements(rulePage.getTotalElements())
                .totalPages(rulePage.getTotalPages())
                .last(rulePage.isLast())
                .first(rulePage.isFirst())
                .build();
    }

    @Transactional
    @CacheEvict(value = "workflow-rules", allEntries = true)
    public WorkflowRuleResponse enableRule(UUID ruleId) {
        return setRuleActive(ruleId, true);
    }

    @Transactional
    @CacheEvict(value = "workflow-rules", allEntries = true)
    public WorkflowRuleResponse disableRule(UUID ruleId) {
        return setRuleActive(ruleId, false);
    }

    private WorkflowRuleResponse setRuleActive(UUID ruleId, boolean active) {
        String tenantId = TenantContext.getTenantId();
        log.info("{} workflow rule: {} for tenant: {}", active ? "Enabling" : "Disabling", ruleId, tenantId);

        WorkflowRule rule = ruleRepository.findByIdAndTenantIdAndDeletedFalse(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRule", "id", ruleId));

        rule.setActive(active);
        WorkflowRule updatedRule = ruleRepository.save(rule);

        return workflowMapper.toRuleResponse(updatedRule);
    }

    @Transactional
    @CacheEvict(value = "workflow-rules", allEntries = true)
    public void deleteRule(UUID ruleId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Soft deleting workflow rule: {} for tenant: {}", ruleId, tenantId);

        WorkflowRule rule = ruleRepository.findByIdAndTenantIdAndDeletedFalse(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRule", "id", ruleId));

        rule.setDeleted(true);
        ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public PagedResponse<WorkflowExecutionLogResponse> getExecutionLogs(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("executedAt").descending());

        Page<WorkflowExecutionLog> logPage = executionLogRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);

        return PagedResponse.<WorkflowExecutionLogResponse>builder()
                .content(logPage.getContent().stream().map(workflowMapper::toExecutionLogResponse).toList())
                .pageNumber(logPage.getNumber())
                .pageSize(logPage.getSize())
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .last(logPage.isLast())
                .first(logPage.isFirst())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<WorkflowExecutionLogResponse> getExecutionLogsByRule(UUID ruleId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("executedAt").descending());

        Page<WorkflowExecutionLog> logPage = executionLogRepository
                .findByTenantIdAndRuleIdAndDeletedFalse(tenantId, ruleId, pageable);

        return PagedResponse.<WorkflowExecutionLogResponse>builder()
                .content(logPage.getContent().stream().map(workflowMapper::toExecutionLogResponse).toList())
                .pageNumber(logPage.getNumber())
                .pageSize(logPage.getSize())
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .last(logPage.isLast())
                .first(logPage.isFirst())
                .build();
    }
}
