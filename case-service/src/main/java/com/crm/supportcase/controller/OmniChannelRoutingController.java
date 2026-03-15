package com.crm.supportcase.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.supportcase.dto.*;
import com.crm.supportcase.entity.AgentSkill;
import com.crm.supportcase.service.OmniChannelRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/routing")
@RequiredArgsConstructor
@Tag(name = "Omnichannel Routing", description = "Intelligent work-item routing, agent presence, queues & skills")
public class OmniChannelRoutingController {

    private final OmniChannelRoutingService routingService;

    // ═══════════════════════════════════════════════════════════════
    // ROUTING QUEUES
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/queues")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a routing queue")
    public ResponseEntity<ApiResponse<QueueResponse>> createQueue(
            @Valid @RequestBody CreateQueueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        QueueResponse response = routingService.createQueue(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Queue created successfully"));
    }

    @GetMapping("/queues")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "List routing queues")
    public ResponseEntity<ApiResponse<PagedResponse<QueueResponse>>> listQueues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<QueueResponse> response = routingService.listQueues(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/queues/{queueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get a routing queue by ID")
    public ResponseEntity<ApiResponse<QueueResponse>> getQueue(@PathVariable UUID queueId) {
        QueueResponse response = routingService.getQueue(queueId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/queues/{queueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a routing queue")
    public ResponseEntity<ApiResponse<QueueResponse>> updateQueue(
            @PathVariable UUID queueId,
            @Valid @RequestBody CreateQueueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        QueueResponse response = routingService.updateQueue(queueId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Queue updated successfully"));
    }

    @DeleteMapping("/queues/{queueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a routing queue")
    public ResponseEntity<ApiResponse<Void>> deleteQueue(
            @PathVariable UUID queueId,
            @AuthenticationPrincipal UserPrincipal principal) {
        routingService.deleteQueue(queueId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Queue deleted successfully"));
    }

    // ═══════════════════════════════════════════════════════════════
    // AGENT PRESENCE
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/agents/presence")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Set agent presence status (online/busy/away/offline)")
    public ResponseEntity<ApiResponse<AgentPresenceResponse>> setPresence(
            @RequestBody AgentPresenceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AgentPresenceResponse response = routingService.setPresence(request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Presence updated"));
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List all agents and their presence")
    public ResponseEntity<ApiResponse<List<AgentPresenceResponse>>> listAgents() {
        List<AgentPresenceResponse> response = routingService.listAgents(null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/agents/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get my presence status")
    public ResponseEntity<ApiResponse<AgentPresenceResponse>> getMyPresence(
            @AuthenticationPrincipal UserPrincipal principal) {
        AgentPresenceResponse response = routingService.getMyPresence(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/agents/queue/{queueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List agents in a specific queue")
    public ResponseEntity<ApiResponse<List<AgentPresenceResponse>>> listAgentsByQueue(
            @PathVariable UUID queueId) {
        List<AgentPresenceResponse> response = routingService.listAgentsByQueue(queueId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ═══════════════════════════════════════════════════════════════
    // AGENT SKILLS
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/skills")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Add a skill to an agent")
    public ResponseEntity<ApiResponse<AgentSkill>> addSkill(
            @RequestBody AgentSkillRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AgentSkill skill = routingService.addSkill(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(skill, "Skill added"));
    }

    @GetMapping("/skills/agent/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get skills for an agent")
    public ResponseEntity<ApiResponse<List<AgentSkill>>> getAgentSkills(@PathVariable UUID userId) {
        List<AgentSkill> skills = routingService.getAgentSkills(userId);
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @GetMapping("/skills")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List all skills")
    public ResponseEntity<ApiResponse<List<AgentSkill>>> listAllSkills() {
        List<AgentSkill> skills = routingService.listAllSkills();
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @DeleteMapping("/skills/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove a skill")
    public ResponseEntity<ApiResponse<Void>> removeSkill(
            @PathVariable UUID skillId,
            @AuthenticationPrincipal UserPrincipal principal) {
        routingService.removeSkill(skillId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Skill removed"));
    }

    // ═══════════════════════════════════════════════════════════════
    // ROUTING RULES
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a routing rule")
    public ResponseEntity<ApiResponse<RoutingRuleResponse>> createRule(
            @Valid @RequestBody CreateRoutingRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RoutingRuleResponse response = routingService.createRule(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Rule created"));
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List all routing rules")
    public ResponseEntity<ApiResponse<List<RoutingRuleResponse>>> listRules() {
        List<RoutingRuleResponse> response = routingService.listRules();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a routing rule")
    public ResponseEntity<ApiResponse<RoutingRuleResponse>> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody CreateRoutingRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RoutingRuleResponse response = routingService.updateRule(ruleId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Rule updated"));
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a routing rule")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable UUID ruleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        routingService.deleteRule(ruleId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Rule deleted"));
    }

    // ═══════════════════════════════════════════════════════════════
    // WORK ITEMS — Route, Accept, Decline, Complete
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/route/case/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Route a case to the best available agent")
    public ResponseEntity<ApiResponse<WorkItemResponse>> routeCase(
            @PathVariable UUID caseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        WorkItemResponse response = routingService.routeCaseById(caseId, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Case routed"));
    }

    @PatchMapping("/work-items/{workItemId}/accept")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Accept a work item assignment")
    public ResponseEntity<ApiResponse<WorkItemResponse>> acceptWorkItem(
            @PathVariable UUID workItemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        WorkItemResponse response = routingService.acceptWorkItem(workItemId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Work item accepted"));
    }

    @PatchMapping("/work-items/{workItemId}/decline")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Decline a work item and re-route")
    public ResponseEntity<ApiResponse<WorkItemResponse>> declineWorkItem(
            @PathVariable UUID workItemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        WorkItemResponse response = routingService.declineWorkItem(workItemId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Work item declined, re-routing"));
    }

    @PatchMapping("/work-items/{workItemId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Complete a work item")
    public ResponseEntity<ApiResponse<WorkItemResponse>> completeWorkItem(
            @PathVariable UUID workItemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        WorkItemResponse response = routingService.completeWorkItem(workItemId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Work item completed"));
    }

    @GetMapping("/work-items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List work items with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<WorkItemResponse>>> listWorkItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        PagedResponse<WorkItemResponse> response = routingService.listWorkItems(page, size, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/work-items/{workItemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get a work item by ID")
    public ResponseEntity<ApiResponse<WorkItemResponse>> getWorkItem(@PathVariable UUID workItemId) {
        WorkItemResponse response = routingService.getWorkItem(workItemId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/work-items/agent/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get active work items for an agent")
    public ResponseEntity<ApiResponse<List<WorkItemResponse>>> getAgentWorkItems(@PathVariable UUID agentId) {
        List<WorkItemResponse> response = routingService.getAgentWorkItems(agentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ═══════════════════════════════════════════════════════════════
    // ANALYTICS
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get omnichannel routing analytics")
    public ResponseEntity<ApiResponse<RoutingAnalytics>> getAnalytics() {
        RoutingAnalytics analytics = routingService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
