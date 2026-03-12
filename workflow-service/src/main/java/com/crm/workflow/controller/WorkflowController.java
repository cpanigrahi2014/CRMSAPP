package com.crm.workflow.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.workflow.dto.*;
import com.crm.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflows", description = "Workflow rule management APIs")
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new workflow rule")
    public ResponseEntity<ApiResponse<WorkflowRuleResponse>> createRule(
            @Valid @RequestBody CreateWorkflowRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        WorkflowRuleResponse response = workflowService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Workflow rule created successfully"));
    }

    @PutMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update an existing workflow rule")
    public ResponseEntity<ApiResponse<WorkflowRuleResponse>> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateWorkflowRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        WorkflowRuleResponse response = workflowService.updateRule(ruleId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Workflow rule updated successfully"));
    }

    @GetMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get workflow rule by ID")
    public ResponseEntity<ApiResponse<WorkflowRuleResponse>> getRuleById(@PathVariable UUID ruleId) {
        WorkflowRuleResponse response = workflowService.getRuleById(ruleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all workflow rules with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<WorkflowRuleResponse>>> getAllRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<WorkflowRuleResponse> response = workflowService.getAllRules(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/entity/{entityType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get workflow rules by entity type")
    public ResponseEntity<ApiResponse<PagedResponse<WorkflowRuleResponse>>> getRulesByEntityType(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<WorkflowRuleResponse> response = workflowService.getRulesByEntityType(entityType, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{ruleId}/enable")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Enable a workflow rule")
    public ResponseEntity<ApiResponse<WorkflowRuleResponse>> enableRule(@PathVariable UUID ruleId) {
        WorkflowRuleResponse response = workflowService.enableRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(response, "Workflow rule enabled"));
    }

    @PatchMapping("/{ruleId}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Disable a workflow rule")
    public ResponseEntity<ApiResponse<WorkflowRuleResponse>> disableRule(@PathVariable UUID ruleId) {
        WorkflowRuleResponse response = workflowService.disableRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(response, "Workflow rule disabled"));
    }

    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a workflow rule (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable UUID ruleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        workflowService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null, "Workflow rule deleted successfully"));
    }

    @GetMapping("/executions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get workflow execution logs")
    public ResponseEntity<ApiResponse<PagedResponse<WorkflowExecutionLogResponse>>> getExecutionLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<WorkflowExecutionLogResponse> response = workflowService.getExecutionLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{ruleId}/executions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get execution logs for a specific rule")
    public ResponseEntity<ApiResponse<PagedResponse<WorkflowExecutionLogResponse>>> getExecutionLogsByRule(
            @PathVariable UUID ruleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<WorkflowExecutionLogResponse> response = workflowService.getExecutionLogsByRule(ruleId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
