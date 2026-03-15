package com.crm.supportcase.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.supportcase.dto.*;
import com.crm.supportcase.service.CaseService;
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
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@Tag(name = "Cases", description = "Support Case Management APIs")
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Create a new support case")
    public ResponseEntity<ApiResponse<CaseResponse>> createCase(
            @Valid @RequestBody CreateCaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CaseResponse response = caseService.createCase(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Case created successfully"));
    }

    @PutMapping("/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Update a case")
    public ResponseEntity<ApiResponse<CaseResponse>> updateCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody UpdateCaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CaseResponse response = caseService.updateCase(caseId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Case updated successfully"));
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get case by ID")
    public ResponseEntity<ApiResponse<CaseResponse>> getCaseById(@PathVariable UUID caseId) {
        CaseResponse response = caseService.getCaseById(caseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{caseNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get case by case number")
    public ResponseEntity<ApiResponse<CaseResponse>> getCaseByCaseNumber(@PathVariable String caseNumber) {
        CaseResponse response = caseService.getCaseByCaseNumber(caseNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "List all cases with pagination and filtering")
    public ResponseEntity<ApiResponse<PagedResponse<CaseResponse>>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<CaseResponse> response = caseService.getAllCases(page, size, status, priority, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{caseId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Resolve a case")
    public ResponseEntity<ApiResponse<CaseResponse>> resolveCase(
            @PathVariable UUID caseId,
            @RequestParam(required = false) String resolutionNotes,
            @AuthenticationPrincipal UserPrincipal principal) {
        CaseResponse response = caseService.resolveCase(caseId, resolutionNotes, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Case resolved successfully"));
    }

    @PatchMapping("/{caseId}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Close a case")
    public ResponseEntity<ApiResponse<CaseResponse>> closeCase(
            @PathVariable UUID caseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        CaseResponse response = caseService.closeCase(caseId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Case closed successfully"));
    }

    @PatchMapping("/{caseId}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Manually escalate a case")
    public ResponseEntity<ApiResponse<CaseResponse>> escalateCase(
            @PathVariable UUID caseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        CaseResponse response = caseService.escalateCase(caseId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Case escalated successfully"));
    }

    @PostMapping("/{caseId}/csat")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Submit CSAT survey for a case")
    public ResponseEntity<ApiResponse<CaseResponse>> submitCsat(
            @PathVariable UUID caseId,
            @Valid @RequestBody CsatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CaseResponse response = caseService.submitCsat(caseId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "CSAT submitted successfully"));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get case analytics")
    public ResponseEntity<ApiResponse<CaseAnalytics>> getAnalytics() {
        CaseAnalytics analytics = caseService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @DeleteMapping("/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Soft delete a case")
    public ResponseEntity<ApiResponse<Void>> deleteCase(
            @PathVariable UUID caseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        caseService.deleteCase(caseId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Case deleted successfully"));
    }
}
