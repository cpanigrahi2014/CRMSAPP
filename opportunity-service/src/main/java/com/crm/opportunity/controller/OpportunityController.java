package com.crm.opportunity.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.opportunity.dto.*;
import com.crm.opportunity.entity.Opportunity;
import com.crm.opportunity.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/opportunities")
@RequiredArgsConstructor
@Tag(name = "Opportunities", description = "Opportunity management APIs")
public class OpportunityController {

    private final OpportunityService opportunityService;

    // ─── CRUD ────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Create a new opportunity")
    public ResponseEntity<ApiResponse<OpportunityResponse>> createOpportunity(
            @Valid @RequestBody CreateOpportunityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        OpportunityResponse response = opportunityService.createOpportunity(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Opportunity created successfully"));
    }

    @PutMapping("/{opportunityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Update an existing opportunity")
    public ResponseEntity<ApiResponse<OpportunityResponse>> updateOpportunity(
            @PathVariable UUID opportunityId,
            @Valid @RequestBody UpdateOpportunityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        OpportunityResponse response = opportunityService.updateOpportunity(opportunityId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Opportunity updated successfully"));
    }

    @GetMapping("/{opportunityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get opportunity by ID")
    public ResponseEntity<ApiResponse<OpportunityResponse>> getOpportunityById(@PathVariable UUID opportunityId) {
        OpportunityResponse response = opportunityService.getOpportunityById(opportunityId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all opportunities with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<OpportunityResponse>>> getAllOpportunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<OpportunityResponse> response = opportunityService.getAllOpportunities(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{opportunityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete an opportunity (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteOpportunity(
            @PathVariable UUID opportunityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        opportunityService.deleteOpportunity(opportunityId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Opportunity deleted successfully"));
    }

    // ─── Filters ─────────────────────────────────────────────────────

    @GetMapping("/stage/{stage}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get opportunities by stage")
    public ResponseEntity<ApiResponse<PagedResponse<OpportunityResponse>>> getOpportunitiesByStage(
            @PathVariable Opportunity.OpportunityStage stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getOpportunitiesByStage(stage, page, size)));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get opportunities by account")
    public ResponseEntity<ApiResponse<PagedResponse<OpportunityResponse>>> getOpportunitiesByAccount(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getOpportunitiesByAccount(accountId, page, size)));
    }

    @GetMapping("/assignee/{assignedTo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get opportunities by assignee")
    public ResponseEntity<ApiResponse<PagedResponse<OpportunityResponse>>> getOpportunitiesByAssignee(
            @PathVariable UUID assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getOpportunitiesByAssignee(assignedTo, page, size)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Search opportunities")
    public ResponseEntity<ApiResponse<PagedResponse<OpportunityResponse>>> searchOpportunities(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.searchOpportunities(query, page, size)));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get opportunities by close date range")
    public ResponseEntity<ApiResponse<List<OpportunityResponse>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getOpportunitiesByDateRange(startDate, endDate)));
    }

    // ─── Stage Management ────────────────────────────────────────────

    @PatchMapping("/{opportunityId}/stage")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update opportunity stage with automation")
    public ResponseEntity<ApiResponse<OpportunityResponse>> updateStage(
            @PathVariable UUID opportunityId,
            @RequestParam Opportunity.OpportunityStage stage,
            @RequestParam(required = false) String lostReason,
            @RequestParam(required = false) String competitor,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.updateStage(opportunityId, stage, lostReason, competitor, principal.getUserId()), "Stage updated"));
    }

    // ─── Close Date Prediction ───────────────────────────────────────

    @PostMapping("/{opportunityId}/predict-close-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Predict close date based on historical data")
    public ResponseEntity<ApiResponse<OpportunityResponse>> predictCloseDate(@PathVariable UUID opportunityId) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.predictCloseDate(opportunityId), "Close date predicted"));
    }

    // ─── Products ────────────────────────────────────────────────────

    @PostMapping("/{opportunityId}/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add a product to an opportunity")
    public ResponseEntity<ApiResponse<ProductResponse>> addProduct(
            @PathVariable UUID opportunityId,
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(opportunityService.addProduct(opportunityId, request, principal.getUserId()), "Product added"));
    }

    @GetMapping("/{opportunityId}/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get products for an opportunity")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProducts(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getProducts(opportunityId, page, size)));
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove a product from an opportunity")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        opportunityService.deleteProduct(productId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Product removed"));
    }

    // ─── Competitors ─────────────────────────────────────────────────

    @PostMapping("/{opportunityId}/competitors")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add a competitor to an opportunity")
    public ResponseEntity<ApiResponse<CompetitorResponse>> addCompetitor(
            @PathVariable UUID opportunityId,
            @Valid @RequestBody CreateCompetitorRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(opportunityService.addCompetitor(opportunityId, request, principal.getUserId()), "Competitor added"));
    }

    @GetMapping("/{opportunityId}/competitors")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get competitors for an opportunity")
    public ResponseEntity<ApiResponse<PagedResponse<CompetitorResponse>>> getCompetitors(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getCompetitors(opportunityId, page, size)));
    }

    @DeleteMapping("/competitors/{competitorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove a competitor")
    public ResponseEntity<ApiResponse<Void>> deleteCompetitor(
            @PathVariable UUID competitorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        opportunityService.deleteCompetitor(competitorId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Competitor removed"));
    }

    // ─── Activity Timeline ───────────────────────────────────────────

    @GetMapping("/{opportunityId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activity timeline for an opportunity")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityResponse>>> getActivities(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getActivities(opportunityId, page, size)));
    }

    // ─── Collaboration ───────────────────────────────────────────────

    @PostMapping("/{opportunityId}/collaborators")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Add a collaborator to an opportunity")
    public ResponseEntity<ApiResponse<CollaboratorResponse>> addCollaborator(
            @PathVariable UUID opportunityId,
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "MEMBER") String role,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(opportunityService.addCollaborator(opportunityId, userId, role, principal.getUserId()), "Collaborator added"));
    }

    @GetMapping("/{opportunityId}/collaborators")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get collaborators for an opportunity")
    public ResponseEntity<ApiResponse<List<CollaboratorResponse>>> getCollaborators(@PathVariable UUID opportunityId) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getCollaborators(opportunityId)));
    }

    @DeleteMapping("/{opportunityId}/collaborators/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove a collaborator")
    public ResponseEntity<ApiResponse<Void>> removeCollaborator(
            @PathVariable UUID opportunityId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        opportunityService.removeCollaborator(opportunityId, userId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Collaborator removed"));
    }

    // Notes
    @PostMapping("/{opportunityId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add a note to an opportunity")
    public ResponseEntity<ApiResponse<NoteResponse>> addNote(
            @PathVariable UUID opportunityId,
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(opportunityService.addNote(opportunityId, request, principal.getUserId()), "Note added"));
    }

    @GetMapping("/{opportunityId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get notes for an opportunity")
    public ResponseEntity<ApiResponse<PagedResponse<NoteResponse>>> getNotes(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getNotes(opportunityId, page, size)));
    }

    @DeleteMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a note")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable UUID noteId,
            @AuthenticationPrincipal UserPrincipal principal) {
        opportunityService.deleteNote(noteId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Note deleted"));
    }

    // ─── Forecasting ─────────────────────────────────────────────────

    @GetMapping("/forecast")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get revenue forecast summary")
    public ResponseEntity<ApiResponse<ForecastSummary>> getRevenueForecast() {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getRevenueForecast()));
    }

    // ─── Reminders ───────────────────────────────────────────────────

    @PostMapping("/{opportunityId}/reminders")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add a reminder to an opportunity")
    public ResponseEntity<ApiResponse<ReminderResponse>> addReminder(
            @PathVariable UUID opportunityId,
            @Valid @RequestBody CreateReminderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(opportunityService.addReminder(opportunityId, request, principal.getUserId()), "Reminder added"));
    }

    @GetMapping("/{opportunityId}/reminders")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get reminders for an opportunity")
    public ResponseEntity<ApiResponse<PagedResponse<ReminderResponse>>> getReminders(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getReminders(opportunityId, page, size)));
    }

    @PatchMapping("/reminders/{reminderId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Mark a reminder as completed")
    public ResponseEntity<ApiResponse<ReminderResponse>> completeReminder(
            @PathVariable UUID reminderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.completeReminder(reminderId, principal.getUserId()), "Reminder completed"));
    }

    @DeleteMapping("/reminders/{reminderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a reminder")
    public ResponseEntity<ApiResponse<Void>> deleteReminder(
            @PathVariable UUID reminderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        opportunityService.deleteReminder(reminderId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Reminder deleted"));
    }

    @GetMapping("/reminders/due")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all overdue reminders")
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getDueReminders() {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getDueReminders()));
    }

    // ─── Revenue Analytics ───────────────────────────────────────────

    @GetMapping("/analytics/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get revenue analytics")
    public ResponseEntity<ApiResponse<RevenueAnalytics>> getRevenueAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getRevenueAnalytics()));
    }

    // ─── Win/Loss Analysis ───────────────────────────────────────────

    @GetMapping("/analytics/win-loss")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get win/loss analysis")
    public ResponseEntity<ApiResponse<WinLossAnalysis>> getWinLossAnalysis() {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getWinLossAnalysis()));
    }

    // ─── Alerts ──────────────────────────────────────────────────────

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get opportunity alerts (overdue, stale, closing soon)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlerts() {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getAlerts()));
    }

    // ─── Stage Conversion Analytics ──────────────────────────────────

    @GetMapping("/analytics/conversion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get stage conversion analytics")
    public ResponseEntity<ApiResponse<StageConversionAnalytics>> getConversionAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getConversionAnalytics()));
    }

    // ─── Pipeline Dashboard ──────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get unified pipeline dashboard data")
    public ResponseEntity<ApiResponse<PipelineDashboard>> getPipelineDashboard() {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getPipelineDashboard()));
    }

    // ─── Pipeline Performance + Velocity ─────────────────────────────

    @GetMapping("/analytics/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get pipeline performance and velocity metrics")
    public ResponseEntity<ApiResponse<PipelinePerformance>> getPipelinePerformance() {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getPipelinePerformance()));
    }

    // ─── Pipeline View (grouped by stage) ────────────────────────────

    @GetMapping("/pipeline")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get pipeline view grouped by stage")
    public ResponseEntity<ApiResponse<Map<String, List<OpportunityResponse>>>> getPipelineView() {
        return ResponseEntity.ok(ApiResponse.success(opportunityService.getPipelineView()));
    }

    // ─── Import / Export ───────────────────────────────────────
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Import opportunities from CSV file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importOpportunities(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                opportunityService.importOpportunitiesFromFile(file, principal.getUserId()), "Import completed"));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Export opportunities to CSV")
    public ResponseEntity<byte[]> exportOpportunities() {
        String csv = opportunityService.exportOpportunitiesToCsv();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=opportunities.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
