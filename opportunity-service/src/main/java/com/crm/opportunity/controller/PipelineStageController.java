package com.crm.opportunity.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.opportunity.dto.PipelineStageRequest;
import com.crm.opportunity.dto.PipelineStageResponse;
import com.crm.opportunity.service.PipelineStageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/opportunities/pipeline-stages")
@RequiredArgsConstructor
@Tag(name = "Pipeline Stages", description = "Customizable pipeline stage management")
public class PipelineStageController {

    private final PipelineStageService pipelineStageService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all active pipeline stages")
    public ResponseEntity<ApiResponse<List<PipelineStageResponse>>> getActiveStages() {
        return ResponseEntity.ok(ApiResponse.success(pipelineStageService.getActiveStages()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all pipeline stages including inactive (admin)")
    public ResponseEntity<ApiResponse<List<PipelineStageResponse>>> getAllStages() {
        return ResponseEntity.ok(ApiResponse.success(pipelineStageService.getAllStages()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new pipeline stage")
    public ResponseEntity<ApiResponse<PipelineStageResponse>> createStage(
            @Valid @RequestBody PipelineStageRequest request) {
        PipelineStageResponse response = pipelineStageService.createStage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Pipeline stage created successfully"));
    }

    @PutMapping("/{stageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a pipeline stage")
    public ResponseEntity<ApiResponse<PipelineStageResponse>> updateStage(
            @PathVariable UUID stageId,
            @Valid @RequestBody PipelineStageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                pipelineStageService.updateStage(stageId, request), "Pipeline stage updated successfully"));
    }

    @DeleteMapping("/{stageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a pipeline stage (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteStage(@PathVariable UUID stageId) {
        pipelineStageService.deleteStage(stageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Pipeline stage deleted successfully"));
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reorder pipeline stages")
    public ResponseEntity<ApiResponse<List<PipelineStageResponse>>> reorderStages(
            @RequestBody List<UUID> stageIds) {
        return ResponseEntity.ok(ApiResponse.success(
                pipelineStageService.reorderStages(stageIds), "Stages reordered successfully"));
    }
}
