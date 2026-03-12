package com.crm.workflow.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.security.TenantContext;
import com.crm.workflow.dto.WorkflowSuggestionResponse;
import com.crm.workflow.entity.WorkflowSuggestion;
import com.crm.workflow.entity.WorkflowExecutionLog;
import com.crm.workflow.repository.WorkflowSuggestionRepository;
import com.crm.workflow.repository.WorkflowExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Generates AI workflow suggestions based on execution log patterns
 * and best-practice automation templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowSuggestionService {

    private final WorkflowSuggestionRepository suggestionRepository;
    private final WorkflowExecutionLogRepository executionLogRepository;

    /* ── Read ────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public PagedResponse<WorkflowSuggestionResponse> getSuggestions(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<WorkflowSuggestion> p = suggestionRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size));
        return PagedResponse.<WorkflowSuggestionResponse>builder()
                .content(p.getContent().stream().map(this::toResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<WorkflowSuggestionResponse> getPendingSuggestions(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<WorkflowSuggestion> p = suggestionRepository
                .findByTenantIdAndStatus(tenantId, "PENDING", PageRequest.of(page, size));
        return PagedResponse.<WorkflowSuggestionResponse>builder()
                .content(p.getContent().stream().map(this::toResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return suggestionRepository.countByTenantIdAndStatus(TenantContext.getTenantId(), "PENDING");
    }

    /* ── Accept / Dismiss ────────────────────────────────── */

    @Transactional
    public WorkflowSuggestionResponse acceptSuggestion(UUID suggestionId) {
        String tenantId = TenantContext.getTenantId();
        WorkflowSuggestion s = suggestionRepository.findById(suggestionId)
                .filter(sg -> sg.getTenantId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        s.setStatus("ACCEPTED");
        return toResponse(suggestionRepository.save(s));
    }

    @Transactional
    public WorkflowSuggestionResponse dismissSuggestion(UUID suggestionId) {
        String tenantId = TenantContext.getTenantId();
        WorkflowSuggestion s = suggestionRepository.findById(suggestionId)
                .filter(sg -> sg.getTenantId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        s.setStatus("DISMISSED");
        return toResponse(suggestionRepository.save(s));
    }

    /* ── Pattern-based Suggestion Generator (scheduled) ── */

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void generatePatternSuggestions() {
        log.debug("Running pattern-based suggestion generator...");
        // Analyze recent execution logs for patterns
        List<WorkflowExecutionLog> recentLogs = executionLogRepository
                .findAll(PageRequest.of(0, 100, Sort.by("executedAt").descending()))
                .getContent();

        if (recentLogs.isEmpty()) return;

        // Count failed/success executions per rule
        Map<UUID, Integer> failuresByRule = new HashMap<>();
        Map<UUID, Integer> successByRule = new HashMap<>();
        for (WorkflowExecutionLog elog : recentLogs) {
            UUID ruleId = elog.getRuleId();
            if (ruleId == null) continue;
            if ("FAILED".equals(elog.getStatus().name())) {
                failuresByRule.merge(ruleId, 1, Integer::sum);
            } else if ("SUCCESS".equals(elog.getStatus().name())) {
                successByRule.merge(ruleId, 1, Integer::sum);
            }
        }

        // Suggest optimisations for rules with high success rates
        for (Map.Entry<UUID, Integer> entry : successByRule.entrySet()) {
            if (entry.getValue() >= 5 && !failuresByRule.containsKey(entry.getKey())) {
                checkAndCreateSuggestion(
                        "Optimize Rule " + entry.getKey().toString().substring(0, 8),
                        "This workflow rule has a high success rate (" + entry.getValue() + " recent successes). " +
                                "Consider adding more automation rules with similar patterns.",
                        "OPTIMIZATION",
                        "GENERAL",
                        0.7,
                        recentLogs.get(0).getTenantId()
                );
            }
        }

        // Suggest for rules with frequent failures
        for (Map.Entry<UUID, Integer> entry : failuresByRule.entrySet()) {
            if (entry.getValue() >= 3) {
                checkAndCreateSuggestion(
                        "Fix Failing Rule " + entry.getKey().toString().substring(0, 8),
                        entry.getValue() + " workflow executions failed for this rule. " +
                                "Review conditions and action configurations.",
                        "PATTERN_DETECTED",
                        "GENERAL",
                        0.85,
                        recentLogs.get(0).getTenantId()
                );
            }
        }
        log.debug("Pattern suggestion generation complete.");
    }

    /* ── Generate Best Practice Suggestions ───────────── */

    @Transactional
    public void generateBestPracticeSuggestions(String tenantId) {
        String[][] bestPractices = {
                {"Auto-assign new leads", "Automatically route new leads to sales reps based on territory or source.",
                        "BEST_PRACTICE", "LEAD", "CREATED",
                        "[{\"fieldName\":\"source\",\"operator\":\"IS_NOT_NULL\",\"logicalOperator\":\"AND\"}]",
                        "[{\"actionType\":\"ASSIGN_TO\",\"targetField\":\"assignedTo\",\"targetValue\":\"round-robin\",\"actionOrder\":0}]"},
                {"Send welcome email on lead creation", "Automatically send a welcome email when a new lead is created.",
                        "BEST_PRACTICE", "LEAD", "CREATED",
                        "[]",
                        "[{\"actionType\":\"SEND_EMAIL\",\"targetField\":\"email\",\"targetValue\":\"Welcome to our CRM!\",\"actionOrder\":0}]"},
                {"Notify manager on deal stage change", "Alert the sales manager when a deal moves to negotiation or proposal stage.",
                        "BEST_PRACTICE", "OPPORTUNITY", "STAGE_CHANGED",
                        "[{\"fieldName\":\"stage\",\"operator\":\"IN\",\"value\":\"PROPOSAL,NEGOTIATION\",\"logicalOperator\":\"AND\"}]",
                        "[{\"actionType\":\"SEND_NOTIFICATION\",\"targetField\":\"manager\",\"targetValue\":\"Deal moved to advanced stage\",\"actionOrder\":0}]"},
                {"Create follow-up task on opportunity update", "Automatically create a follow-up task when an opportunity is updated.",
                        "BEST_PRACTICE", "OPPORTUNITY", "UPDATED",
                        "[]",
                        "[{\"actionType\":\"CREATE_TASK\",\"targetField\":\"Follow up on opportunity\",\"targetValue\":\"Review updated opportunity details\",\"actionOrder\":0}]"},
                {"Auto-notify on contact creation", "Send a notification when a new contact is added to the CRM.",
                        "BEST_PRACTICE", "CONTACT", "CREATED",
                        "[]",
                        "[{\"actionType\":\"SEND_NOTIFICATION\",\"targetField\":\"team\",\"targetValue\":\"New contact added\",\"actionOrder\":0}]"},
                {"Escalate stale leads", "Notify management when leads haven't been contacted within 48 hours.",
                        "BEST_PRACTICE", "LEAD", "UPDATED",
                        "[{\"fieldName\":\"status\",\"operator\":\"EQUALS\",\"value\":\"NEW\",\"logicalOperator\":\"AND\"}]",
                        "[{\"actionType\":\"SEND_NOTIFICATION\",\"targetField\":\"manager\",\"targetValue\":\"Lead needs attention - not yet contacted\",\"actionOrder\":0},{\"actionType\":\"CREATE_TASK\",\"targetField\":\"Contact stale lead\",\"targetValue\":\"This lead requires immediate follow-up\",\"actionOrder\":1}]"},
        };

        for (String[] bp : bestPractices) {
            checkAndCreateSuggestion(bp[0], bp[1], bp[2], bp[3], 0.9, tenantId);
        }
    }

    /* ── Helpers ──────────────────────────────────────────── */

    private void checkAndCreateSuggestion(String title, String description,
                                           String type, String entityType,
                                           double confidence, String tenantId) {
        // Avoid duplicates — check if same title already exists for tenant
        List<WorkflowSuggestion> existing = suggestionRepository
                .findByTenantIdAndStatusOrderByConfidenceDesc(tenantId, "PENDING");
        boolean alreadyExists = existing.stream().anyMatch(s -> s.getTitle().equals(title));
        if (alreadyExists) return;

        WorkflowSuggestion suggestion = WorkflowSuggestion.builder()
                .title(title)
                .description(description)
                .suggestionType(type)
                .entityType(entityType)
                .confidence(confidence)
                .reason("AI analysis of workflow patterns and CRM best practices.")
                .status("PENDING")
                .tenantId(tenantId)
                .build();
        suggestionRepository.save(suggestion);
        log.info("Created AI suggestion: '{}' for tenant: {}", title, tenantId);
    }

    private WorkflowSuggestionResponse toResponse(WorkflowSuggestion s) {
        return WorkflowSuggestionResponse.builder()
                .id(s.getId()).title(s.getTitle()).description(s.getDescription())
                .suggestionType(s.getSuggestionType()).entityType(s.getEntityType())
                .triggerEvent(s.getTriggerEvent())
                .conditionsJson(s.getConditionsJson()).actionsJson(s.getActionsJson())
                .confidence(s.getConfidence()).reason(s.getReason())
                .status(s.getStatus()).acceptedRuleId(s.getAcceptedRuleId())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
