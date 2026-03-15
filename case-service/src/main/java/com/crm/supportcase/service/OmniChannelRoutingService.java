package com.crm.supportcase.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.supportcase.dto.*;
import com.crm.supportcase.entity.*;
import com.crm.supportcase.entity.AgentPresence.PresenceStatus;
import com.crm.supportcase.entity.RoutingRule.MatchField;
import com.crm.supportcase.entity.RoutingRule.MatchOperator;
import com.crm.supportcase.entity.WorkItem.WorkItemStatus;
import com.crm.supportcase.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OmniChannelRoutingService {

    private final RoutingQueueRepository queueRepository;
    private final AgentPresenceRepository presenceRepository;
    private final AgentSkillRepository skillRepository;
    private final RoutingRuleRepository ruleRepository;
    private final WorkItemRepository workItemRepository;
    private final EventPublisher eventPublisher;

    // ═══════════════════════════════════════════════════════════════
    // ROUTING QUEUES
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public QueueResponse createQueue(CreateQueueRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating routing queue '{}' for tenant: {}", request.getName(), tenantId);

        RoutingQueue queue = RoutingQueue.builder()
                .name(request.getName())
                .description(request.getDescription())
                .channel(request.getChannel() != null ? request.getChannel() : RoutingQueue.Channel.CASE)
                .routingModel(request.getRoutingModel() != null ? request.getRoutingModel() : RoutingQueue.RoutingModel.LEAST_ACTIVE)
                .priorityWeight(request.getPriorityWeight() != null ? request.getPriorityWeight() : 1)
                .maxWaitSeconds(request.getMaxWaitSeconds() != null ? request.getMaxWaitSeconds() : 300)
                .overflowQueueId(request.getOverflowQueueId())
                .build();
        queue.setTenantId(tenantId);

        RoutingQueue saved = queueRepository.save(queue);
        eventPublisher.publish("routing-events", tenantId, userId, "RoutingQueue",
                saved.getId().toString(), "QUEUE_CREATED", toQueueResponse(saved));
        return toQueueResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<QueueResponse> listQueues(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<RoutingQueue> queuesPage = queueRepository.findByTenantIdAndDeletedFalse(
                tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        List<QueueResponse> content = queuesPage.getContent().stream()
                .map(this::toQueueResponse).collect(Collectors.toList());
        return PagedResponse.<QueueResponse>builder()
                .content(content).pageNumber(page).pageSize(size)
                .totalElements(queuesPage.getTotalElements())
                .totalPages(queuesPage.getTotalPages())
                .first(queuesPage.isFirst()).last(queuesPage.isLast()).build();
    }

    @Transactional(readOnly = true)
    public QueueResponse getQueue(UUID queueId) {
        String tenantId = TenantContext.getTenantId();
        RoutingQueue queue = queueRepository.findByIdAndTenantIdAndDeletedFalse(queueId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("RoutingQueue", "id", queueId));
        return toQueueResponse(queue);
    }

    @Transactional
    public QueueResponse updateQueue(UUID queueId, CreateQueueRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        RoutingQueue queue = queueRepository.findByIdAndTenantIdAndDeletedFalse(queueId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("RoutingQueue", "id", queueId));

        if (request.getName() != null) queue.setName(request.getName());
        if (request.getDescription() != null) queue.setDescription(request.getDescription());
        if (request.getChannel() != null) queue.setChannel(request.getChannel());
        if (request.getRoutingModel() != null) queue.setRoutingModel(request.getRoutingModel());
        if (request.getPriorityWeight() != null) queue.setPriorityWeight(request.getPriorityWeight());
        if (request.getMaxWaitSeconds() != null) queue.setMaxWaitSeconds(request.getMaxWaitSeconds());
        if (request.getOverflowQueueId() != null) queue.setOverflowQueueId(request.getOverflowQueueId());

        return toQueueResponse(queueRepository.save(queue));
    }

    @Transactional
    public void deleteQueue(UUID queueId, String userId) {
        String tenantId = TenantContext.getTenantId();
        RoutingQueue queue = queueRepository.findByIdAndTenantIdAndDeletedFalse(queueId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("RoutingQueue", "id", queueId));
        queue.setDeleted(true);
        queueRepository.save(queue);
    }

    // ═══════════════════════════════════════════════════════════════
    // AGENT PRESENCE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public AgentPresenceResponse setPresence(AgentPresenceRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        UUID agentUserId = request.getUserId() != null ? request.getUserId() : UUID.fromString(userId);

        AgentPresence presence = presenceRepository.findByUserIdAndTenantIdAndDeletedFalse(agentUserId, tenantId)
                .orElseGet(() -> {
                    AgentPresence p = AgentPresence.builder()
                            .userId(agentUserId)
                            .build();
                    p.setTenantId(tenantId);
                    return p;
                });

        if (request.getStatus() != null) {
            presence.setStatus(request.getStatus());
            presence.setStatusChangedAt(LocalDateTime.now());
        }
        if (request.getAgentName() != null) presence.setAgentName(request.getAgentName());
        if (request.getAgentEmail() != null) presence.setAgentEmail(request.getAgentEmail());
        if (request.getQueueId() != null) presence.setQueueId(request.getQueueId());
        if (request.getCapacity() != null) presence.setCapacity(request.getCapacity());
        if (request.getAutoAccept() != null) presence.setAutoAccept(request.getAutoAccept());

        AgentPresence saved = presenceRepository.save(presence);
        log.info("Agent {} presence set to {} (queue={}, capacity={})",
                saved.getUserId(), saved.getStatus(), saved.getQueueId(), saved.getCapacity());

        eventPublisher.publish("routing-events", tenantId, userId, "AgentPresence",
                saved.getId().toString(), "PRESENCE_CHANGED", toPresenceResponse(saved));
        return toPresenceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AgentPresenceResponse> listAgents(String tenantIdOverride) {
        String tenantId = tenantIdOverride != null ? tenantIdOverride : TenantContext.getTenantId();
        return presenceRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .map(this::toPresenceResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AgentPresenceResponse> listAgentsByQueue(UUID queueId) {
        String tenantId = TenantContext.getTenantId();
        return presenceRepository.findByTenantIdAndQueueIdAndDeletedFalse(tenantId, queueId).stream()
                .map(this::toPresenceResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgentPresenceResponse getMyPresence(String userId) {
        String tenantId = TenantContext.getTenantId();
        AgentPresence presence = presenceRepository.findByUserIdAndTenantIdAndDeletedFalse(
                UUID.fromString(userId), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentPresence", "userId", userId));
        return toPresenceResponse(presence);
    }

    // ═══════════════════════════════════════════════════════════════
    // AGENT SKILLS
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public AgentSkill addSkill(AgentSkillRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        UUID agentUserId = request.getUserId() != null ? request.getUserId() : UUID.fromString(userId);
        AgentSkill skill = AgentSkill.builder()
                .userId(agentUserId)
                .skillName(request.getSkillName())
                .proficiency(request.getProficiency() != null ? request.getProficiency() : 3)
                .category(request.getCategory())
                .build();
        skill.setTenantId(tenantId);
        return skillRepository.save(skill);
    }

    @Transactional(readOnly = true)
    public List<AgentSkill> getAgentSkills(UUID agentUserId) {
        String tenantId = TenantContext.getTenantId();
        return skillRepository.findByUserIdAndTenantIdAndDeletedFalse(agentUserId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<AgentSkill> listAllSkills() {
        String tenantId = TenantContext.getTenantId();
        return skillRepository.findByTenantIdAndDeletedFalse(tenantId);
    }

    @Transactional
    public void removeSkill(UUID skillId, String userId) {
        String tenantId = TenantContext.getTenantId();
        AgentSkill skill = skillRepository.findByIdAndTenantIdAndDeletedFalse(skillId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentSkill", "id", skillId));
        skill.setDeleted(true);
        skillRepository.save(skill);
    }

    // ═══════════════════════════════════════════════════════════════
    // ROUTING RULES
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public RoutingRuleResponse createRule(CreateRoutingRuleRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        RoutingRule rule = RoutingRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .queueId(request.getQueueId())
                .matchField(request.getMatchField())
                .matchOperator(request.getMatchOperator() != null ? request.getMatchOperator() : MatchOperator.EQUALS)
                .matchValue(request.getMatchValue())
                .requiredSkill(request.getRequiredSkill())
                .minProficiency(request.getMinProficiency() != null ? request.getMinProficiency() : 1)
                .rulePriority(request.getRulePriority() != null ? request.getRulePriority() : 10)
                .build();
        rule.setTenantId(tenantId);
        RoutingRule saved = ruleRepository.save(rule);
        return toRuleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoutingRuleResponse> listRules() {
        String tenantId = TenantContext.getTenantId();
        return ruleRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .map(this::toRuleResponse).collect(Collectors.toList());
    }

    @Transactional
    public RoutingRuleResponse updateRule(UUID ruleId, CreateRoutingRuleRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        RoutingRule rule = ruleRepository.findByIdAndTenantIdAndDeletedFalse(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("RoutingRule", "id", ruleId));

        if (request.getName() != null) rule.setName(request.getName());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        if (request.getQueueId() != null) rule.setQueueId(request.getQueueId());
        if (request.getMatchField() != null) rule.setMatchField(request.getMatchField());
        if (request.getMatchOperator() != null) rule.setMatchOperator(request.getMatchOperator());
        if (request.getMatchValue() != null) rule.setMatchValue(request.getMatchValue());
        if (request.getRequiredSkill() != null) rule.setRequiredSkill(request.getRequiredSkill());
        if (request.getMinProficiency() != null) rule.setMinProficiency(request.getMinProficiency());
        if (request.getRulePriority() != null) rule.setRulePriority(request.getRulePriority());

        return toRuleResponse(ruleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(UUID ruleId, String userId) {
        String tenantId = TenantContext.getTenantId();
        RoutingRule rule = ruleRepository.findByIdAndTenantIdAndDeletedFalse(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("RoutingRule", "id", ruleId));
        rule.setDeleted(true);
        ruleRepository.save(rule);
    }

    // ═══════════════════════════════════════════════════════════════
    // CORE ROUTING ENGINE — Route a work item to best agent
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public WorkItemResponse routeWorkItem(SupportCase supportCase, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Routing case {} (priority={}, origin={})", supportCase.getCaseNumber(),
                supportCase.getPriority(), supportCase.getOrigin());

        // 1 — Evaluate routing rules to find the target queue
        UUID targetQueueId = evaluateRules(supportCase, tenantId);
        String requiredSkill = findRequiredSkill(supportCase, tenantId);

        // 2 — Create work item
        WorkItem workItem = WorkItem.builder()
                .entityType("Case")
                .entityId(supportCase.getId())
                .queueId(targetQueueId)
                .priority(supportCase.getPriority())
                .channel(supportCase.getOrigin() != null ? supportCase.getOrigin().name() : "CASE")
                .subject(supportCase.getSubject())
                .queuedAt(LocalDateTime.now())
                .build();
        workItem.setTenantId(tenantId);

        // 3 — Find best available agent
        AgentPresence bestAgent = findBestAgent(targetQueueId, requiredSkill, tenantId);

        if (bestAgent != null) {
            assignToAgent(workItem, bestAgent, tenantId, userId);
        } else {
            workItem.setStatus(WorkItemStatus.QUEUED);
            log.info("No available agent for case {}, queued in {}", supportCase.getCaseNumber(), targetQueueId);
        }

        WorkItem saved = workItemRepository.save(workItem);

        eventPublisher.publish("routing-events", tenantId, userId, "WorkItem",
                saved.getId().toString(), "WORK_ITEM_ROUTED", toWorkItemResponse(saved));

        return toWorkItemResponse(saved);
    }

    @Transactional
    public WorkItemResponse routeCaseById(UUID caseId, String userId) {
        String tenantId = TenantContext.getTenantId();
        // Load the case via repository (injected later or passed)
        // For now, create a minimal work item for manual routing
        WorkItem workItem = WorkItem.builder()
                .entityType("Case")
                .entityId(caseId)
                .queuedAt(LocalDateTime.now())
                .build();
        workItem.setTenantId(tenantId);

        // Evaluate rules with just the entity ID
        List<RoutingRule> rules = ruleRepository.findByTenantIdAndActiveAndDeletedFalseOrderByRulePriorityAsc(tenantId, true);
        UUID targetQueueId = !rules.isEmpty() ? rules.get(0).getQueueId() : null;

        if (targetQueueId == null) {
            List<RoutingQueue> queues = queueRepository.findByTenantIdAndActiveAndDeletedFalse(tenantId, true);
            if (!queues.isEmpty()) targetQueueId = queues.get(0).getId();
        }

        workItem.setQueueId(targetQueueId);

        AgentPresence bestAgent = targetQueueId != null
                ? findBestAgent(targetQueueId, null, tenantId) : null;

        if (bestAgent != null) {
            assignToAgent(workItem, bestAgent, tenantId, userId);
        } else {
            workItem.setStatus(WorkItemStatus.QUEUED);
        }

        WorkItem saved = workItemRepository.save(workItem);
        eventPublisher.publish("routing-events", tenantId, userId, "WorkItem",
                saved.getId().toString(), "WORK_ITEM_ROUTED", toWorkItemResponse(saved));
        return toWorkItemResponse(saved);
    }

    @Transactional
    public WorkItemResponse acceptWorkItem(UUID workItemId, String userId) {
        String tenantId = TenantContext.getTenantId();
        WorkItem item = workItemRepository.findByIdAndTenantIdAndDeletedFalse(workItemId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", workItemId));

        item.setStatus(WorkItemStatus.ACCEPTED);
        item.setAcceptedAt(LocalDateTime.now());
        if (item.getQueuedAt() != null) {
            item.setWaitTimeSeconds(Duration.between(item.getQueuedAt(), LocalDateTime.now()).getSeconds());
        }

        WorkItem saved = workItemRepository.save(item);
        eventPublisher.publish("routing-events", tenantId, userId, "WorkItem",
                saved.getId().toString(), "WORK_ITEM_ACCEPTED", toWorkItemResponse(saved));
        return toWorkItemResponse(saved);
    }

    @Transactional
    public WorkItemResponse declineWorkItem(UUID workItemId, String userId) {
        String tenantId = TenantContext.getTenantId();
        WorkItem item = workItemRepository.findByIdAndTenantIdAndDeletedFalse(workItemId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", workItemId));

        item.setDeclinedCount(item.getDeclinedCount() + 1);

        // Decrement previous agent's count
        if (item.getAssignedAgentId() != null) {
            presenceRepository.findByUserIdAndTenantIdAndDeletedFalse(item.getAssignedAgentId(), tenantId)
                    .ifPresent(agent -> {
                        agent.setActiveWorkCount(Math.max(0, agent.getActiveWorkCount() - 1));
                        presenceRepository.save(agent);
                    });
        }

        // Re-route to next available agent
        AgentPresence nextAgent = item.getQueueId() != null
                ? findBestAgent(item.getQueueId(), null, tenantId) : null;

        if (nextAgent != null && !nextAgent.getUserId().equals(item.getAssignedAgentId())) {
            assignToAgent(item, nextAgent, tenantId, userId);
        } else {
            item.setStatus(WorkItemStatus.QUEUED);
            item.setAssignedAgentId(null);
            item.setAssignedAt(null);
        }

        WorkItem saved = workItemRepository.save(item);
        eventPublisher.publish("routing-events", tenantId, userId, "WorkItem",
                saved.getId().toString(), "WORK_ITEM_DECLINED", toWorkItemResponse(saved));
        return toWorkItemResponse(saved);
    }

    @Transactional
    public WorkItemResponse completeWorkItem(UUID workItemId, String userId) {
        String tenantId = TenantContext.getTenantId();
        WorkItem item = workItemRepository.findByIdAndTenantIdAndDeletedFalse(workItemId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", workItemId));

        item.setStatus(WorkItemStatus.COMPLETED);
        item.setCompletedAt(LocalDateTime.now());
        if (item.getAcceptedAt() != null) {
            item.setHandleTimeSeconds(Duration.between(item.getAcceptedAt(), LocalDateTime.now()).getSeconds());
        }

        // Free up agent capacity
        if (item.getAssignedAgentId() != null) {
            presenceRepository.findByUserIdAndTenantIdAndDeletedFalse(item.getAssignedAgentId(), tenantId)
                    .ifPresent(agent -> {
                        agent.setActiveWorkCount(Math.max(0, agent.getActiveWorkCount() - 1));
                        presenceRepository.save(agent);
                    });
        }

        WorkItem saved = workItemRepository.save(item);
        eventPublisher.publish("routing-events", tenantId, userId, "WorkItem",
                saved.getId().toString(), "WORK_ITEM_COMPLETED", toWorkItemResponse(saved));
        return toWorkItemResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // WORK ITEM QUERIES
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PagedResponse<WorkItemResponse> listWorkItems(int page, int size, String status) {
        String tenantId = TenantContext.getTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<WorkItem> items;
        if (status != null && !status.isBlank()) {
            items = workItemRepository.findByTenantIdAndStatusAndDeletedFalse(
                    tenantId, WorkItemStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            items = workItemRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        }
        List<WorkItemResponse> content = items.getContent().stream()
                .map(this::toWorkItemResponse).collect(Collectors.toList());
        return PagedResponse.<WorkItemResponse>builder()
                .content(content).pageNumber(page).pageSize(size)
                .totalElements(items.getTotalElements())
                .totalPages(items.getTotalPages())
                .first(items.isFirst()).last(items.isLast()).build();
    }

    @Transactional(readOnly = true)
    public List<WorkItemResponse> getAgentWorkItems(UUID agentId) {
        String tenantId = TenantContext.getTenantId();
        List<WorkItemStatus> activeStatuses = List.of(
                WorkItemStatus.ASSIGNED, WorkItemStatus.ACCEPTED, WorkItemStatus.IN_PROGRESS);
        return workItemRepository.findByTenantIdAndAssignedAgentIdAndStatusInAndDeletedFalse(
                        tenantId, agentId, activeStatuses).stream()
                .map(this::toWorkItemResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkItemResponse getWorkItem(UUID workItemId) {
        String tenantId = TenantContext.getTenantId();
        WorkItem item = workItemRepository.findByIdAndTenantIdAndDeletedFalse(workItemId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", workItemId));
        return toWorkItemResponse(item);
    }

    // ═══════════════════════════════════════════════════════════════
    // ANALYTICS
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public RoutingAnalytics getAnalytics() {
        String tenantId = TenantContext.getTenantId();

        Map<String, Long> byChannel = new LinkedHashMap<>();
        workItemRepository.countByChannel(tenantId).forEach(row ->
                byChannel.put(String.valueOf(row[0]), (Long) row[1]));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        workItemRepository.countByStatus(tenantId).forEach(row ->
                byStatus.put(String.valueOf(row[0]), (Long) row[1]));

        return RoutingAnalytics.builder()
                .totalWorkItems(workItemRepository.countByTenant(tenantId))
                .queuedItems(workItemRepository.countByTenantAndStatus(tenantId, WorkItemStatus.QUEUED))
                .assignedItems(workItemRepository.countByTenantAndStatus(tenantId, WorkItemStatus.ASSIGNED))
                .completedItems(workItemRepository.countByTenantAndStatus(tenantId, WorkItemStatus.COMPLETED))
                .timedOutItems(workItemRepository.countByTenantAndStatus(tenantId, WorkItemStatus.TIMED_OUT))
                .avgWaitTimeSeconds(workItemRepository.avgWaitTime(tenantId))
                .avgHandleTimeSeconds(workItemRepository.avgHandleTime(tenantId))
                .onlineAgents(presenceRepository.countOnlineAgents(tenantId))
                .busyAgents(presenceRepository.countBusyAgents(tenantId))
                .totalAgents(presenceRepository.countActiveAgents(tenantId))
                .avgAgentUtilization(presenceRepository.avgUtilization(tenantId) != null
                        ? presenceRepository.avgUtilization(tenantId) : 0.0)
                .itemsByChannel(byChannel)
                .itemsByStatus(byStatus)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // SCHEDULED: Process queued items when agents come online
    // ═══════════════════════════════════════════════════════════════

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processQueuedItems() {
        List<RoutingQueue> allQueues = queueRepository.findAll().stream()
                .filter(q -> !q.isDeleted() && q.isActive()).toList();

        for (RoutingQueue queue : allQueues) {
            String tenantId = queue.getTenantId();
            List<WorkItem> queued = workItemRepository
                    .findByTenantIdAndQueueIdAndStatusAndDeletedFalseOrderByPriorityDescCreatedAtAsc(
                            tenantId, queue.getId(), WorkItemStatus.QUEUED);

            for (WorkItem item : queued) {
                AgentPresence agent = findBestAgent(queue.getId(), null, tenantId);
                if (agent != null) {
                    assignToAgent(item, agent, tenantId, "system");
                    workItemRepository.save(item);
                    log.info("Auto-assigned queued item {} to agent {}", item.getId(), agent.getUserId());
                } else {
                    break; // no more agents available
                }
            }

            // Handle overflow — items waiting > maxWaitSeconds
            if (queue.getOverflowQueueId() != null) {
                LocalDateTime threshold = LocalDateTime.now().minusSeconds(queue.getMaxWaitSeconds());
                for (WorkItem item : queued) {
                    if (item.getQueuedAt() != null && item.getQueuedAt().isBefore(threshold)
                            && item.getStatus() == WorkItemStatus.QUEUED) {
                        item.setQueueId(queue.getOverflowQueueId());
                        workItemRepository.save(item);
                        log.info("Overflow: moved item {} to queue {}", item.getId(), queue.getOverflowQueueId());
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE — Routing Logic
    // ═══════════════════════════════════════════════════════════════

    private UUID evaluateRules(SupportCase supportCase, String tenantId) {
        List<RoutingRule> rules = ruleRepository
                .findByTenantIdAndActiveAndDeletedFalseOrderByRulePriorityAsc(tenantId, true);

        for (RoutingRule rule : rules) {
            if (matchesRule(rule, supportCase)) {
                log.debug("Case {} matched rule '{}' → queue {}", supportCase.getCaseNumber(),
                        rule.getName(), rule.getQueueId());
                return rule.getQueueId();
            }
        }

        // Fallback: first active queue or null
        List<RoutingQueue> queues = queueRepository.findByTenantIdAndActiveAndDeletedFalse(tenantId, true);
        return queues.isEmpty() ? null : queues.get(0).getId();
    }

    private boolean matchesRule(RoutingRule rule, SupportCase supportCase) {
        String fieldValue = extractFieldValue(rule.getMatchField(), supportCase);
        if (fieldValue == null) return false;

        String matchVal = rule.getMatchValue();
        return switch (rule.getMatchOperator()) {
            case EQUALS -> fieldValue.equalsIgnoreCase(matchVal);
            case NOT_EQUALS -> !fieldValue.equalsIgnoreCase(matchVal);
            case CONTAINS -> fieldValue.toLowerCase().contains(matchVal.toLowerCase());
            case STARTS_WITH -> fieldValue.toLowerCase().startsWith(matchVal.toLowerCase());
            case REGEX -> Pattern.compile(matchVal, Pattern.CASE_INSENSITIVE).matcher(fieldValue).find();
        };
    }

    private String extractFieldValue(MatchField field, SupportCase supportCase) {
        return switch (field) {
            case PRIORITY -> supportCase.getPriority() != null ? supportCase.getPriority().name() : null;
            case ORIGIN -> supportCase.getOrigin() != null ? supportCase.getOrigin().name() : null;
            case SUBJECT -> supportCase.getSubject();
            case ACCOUNT_NAME -> supportCase.getAccountName();
            case CONTACT_EMAIL -> supportCase.getContactEmail();
            case STATUS -> supportCase.getStatus() != null ? supportCase.getStatus().name() : null;
        };
    }

    private String findRequiredSkill(SupportCase supportCase, String tenantId) {
        List<RoutingRule> rules = ruleRepository
                .findByTenantIdAndActiveAndDeletedFalseOrderByRulePriorityAsc(tenantId, true);
        for (RoutingRule rule : rules) {
            if (matchesRule(rule, supportCase) && rule.getRequiredSkill() != null) {
                return rule.getRequiredSkill();
            }
        }
        return null;
    }

    private AgentPresence findBestAgent(UUID queueId, String requiredSkill, String tenantId) {
        if (queueId == null) return null;

        List<AgentPresence> available = presenceRepository.findAvailableAgentsInQueue(tenantId, queueId);
        if (available.isEmpty()) return null;

        // If skill required, filter by skill proficiency
        if (requiredSkill != null && !requiredSkill.isBlank()) {
            List<AgentSkill> skilledAgents = skillRepository.findByTenantIdAndSkillNameAndDeletedFalse(
                    tenantId, requiredSkill);
            Set<UUID> skilledUserIds = skilledAgents.stream()
                    .map(AgentSkill::getUserId).collect(Collectors.toSet());

            List<AgentPresence> filtered = available.stream()
                    .filter(a -> skilledUserIds.contains(a.getUserId()))
                    .toList();

            if (!filtered.isEmpty()) return filtered.get(0); // already sorted by least active
        }

        // Default: least active agent (already sorted by query)
        return available.get(0);
    }

    private void assignToAgent(WorkItem item, AgentPresence agent, String tenantId, String userId) {
        item.setAssignedAgentId(agent.getUserId());
        item.setAssignedAt(LocalDateTime.now());
        item.setStatus(agent.isAutoAccept() ? WorkItemStatus.ACCEPTED : WorkItemStatus.ASSIGNED);
        if (agent.isAutoAccept()) {
            item.setAcceptedAt(LocalDateTime.now());
            if (item.getQueuedAt() != null) {
                item.setWaitTimeSeconds(Duration.between(item.getQueuedAt(), LocalDateTime.now()).getSeconds());
            }
        }

        // Update agent workload
        agent.setActiveWorkCount(agent.getActiveWorkCount() + 1);
        agent.setLastRoutedAt(LocalDateTime.now());
        if (agent.getActiveWorkCount() >= agent.getCapacity()) {
            agent.setStatus(PresenceStatus.BUSY);
            agent.setStatusChangedAt(LocalDateTime.now());
        }
        presenceRepository.save(agent);

        log.info("Assigned work item to agent {} (workload: {}/{})",
                agent.getUserId(), agent.getActiveWorkCount(), agent.getCapacity());
    }

    // ═══════════════════════════════════════════════════════════════
    // MAPPERS
    // ═══════════════════════════════════════════════════════════════

    private QueueResponse toQueueResponse(RoutingQueue q) {
        return QueueResponse.builder()
                .id(q.getId()).name(q.getName()).description(q.getDescription())
                .channel(q.getChannel()).routingModel(q.getRoutingModel())
                .priorityWeight(q.getPriorityWeight()).maxWaitSeconds(q.getMaxWaitSeconds())
                .overflowQueueId(q.getOverflowQueueId()).active(q.isActive())
                .createdAt(q.getCreatedAt()).updatedAt(q.getUpdatedAt()).build();
    }

    private AgentPresenceResponse toPresenceResponse(AgentPresence a) {
        return AgentPresenceResponse.builder()
                .id(a.getId()).userId(a.getUserId()).agentName(a.getAgentName())
                .agentEmail(a.getAgentEmail()).status(a.getStatus()).queueId(a.getQueueId())
                .capacity(a.getCapacity()).activeWorkCount(a.getActiveWorkCount())
                .utilization(a.getUtilization()).available(a.isAvailable())
                .lastRoutedAt(a.getLastRoutedAt()).statusChangedAt(a.getStatusChangedAt())
                .autoAccept(a.isAutoAccept()).createdAt(a.getCreatedAt()).build();
    }

    private RoutingRuleResponse toRuleResponse(RoutingRule r) {
        return RoutingRuleResponse.builder()
                .id(r.getId()).name(r.getName()).description(r.getDescription())
                .queueId(r.getQueueId()).matchField(r.getMatchField())
                .matchOperator(r.getMatchOperator()).matchValue(r.getMatchValue())
                .requiredSkill(r.getRequiredSkill()).minProficiency(r.getMinProficiency())
                .rulePriority(r.getRulePriority()).active(r.isActive())
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt()).build();
    }

    private WorkItemResponse toWorkItemResponse(WorkItem w) {
        return WorkItemResponse.builder()
                .id(w.getId()).entityType(w.getEntityType()).entityId(w.getEntityId())
                .queueId(w.getQueueId()).assignedAgentId(w.getAssignedAgentId())
                .status(w.getStatus()).priority(w.getPriority()).channel(w.getChannel())
                .subject(w.getSubject()).queuedAt(w.getQueuedAt()).assignedAt(w.getAssignedAt())
                .acceptedAt(w.getAcceptedAt()).completedAt(w.getCompletedAt())
                .declinedCount(w.getDeclinedCount()).waitTimeSeconds(w.getWaitTimeSeconds())
                .handleTimeSeconds(w.getHandleTimeSeconds()).createdAt(w.getCreatedAt()).build();
    }
}
