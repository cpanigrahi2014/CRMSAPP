package com.crm.lead.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.lead.dto.*;
import com.crm.lead.entity.LeadAttachment;
import com.crm.lead.entity.ScoringRule;
import com.crm.lead.entity.WebForm;
import com.crm.lead.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Lead management APIs")
public class LeadController {

    private final LeadService leadService;

    // ── Core CRUD ──────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Create a new lead")
    public ResponseEntity<ApiResponse<LeadResponse>> createLead(
            @Valid @RequestBody CreateLeadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LeadResponse response = leadService.createLead(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Lead created successfully"));
    }

    @PutMapping("/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Update an existing lead")
    public ResponseEntity<ApiResponse<LeadResponse>> updateLead(
            @PathVariable UUID leadId,
            @Valid @RequestBody UpdateLeadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LeadResponse response = leadService.updateLead(leadId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Lead updated successfully"));
    }

    @GetMapping("/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get lead by ID")
    public ResponseEntity<ApiResponse<LeadResponse>> getLeadById(@PathVariable UUID leadId) {
        LeadResponse response = leadService.getLeadById(leadId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all leads with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<LeadResponse>>> getAllLeads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<LeadResponse> response = leadService.getAllLeads(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Search leads")
    public ResponseEntity<ApiResponse<PagedResponse<LeadResponse>>> searchLeads(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<LeadResponse> response = leadService.searchLeads(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{leadId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Assign lead to a user")
    public ResponseEntity<ApiResponse<LeadResponse>> assignLead(
            @PathVariable UUID leadId,
            @RequestParam UUID assigneeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        LeadResponse response = leadService.assignLead(leadId, assigneeId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Lead assigned successfully"));
    }

    @PostMapping("/{leadId}/convert")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Convert lead to opportunity")
    public ResponseEntity<ApiResponse<LeadResponse>> convertLead(
            @PathVariable UUID leadId,
            @Valid @RequestBody ConvertLeadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LeadResponse response = leadService.convertLead(leadId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Lead converted successfully"));
    }

    @DeleteMapping("/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a lead (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteLead(
            @PathVariable UUID leadId,
            @AuthenticationPrincipal UserPrincipal principal) {
        leadService.deleteLead(leadId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Lead deleted successfully"));
    }

    // ── Notes ──────────────────────────────────────────────

    @PostMapping("/{leadId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add a note to a lead")
    public ResponseEntity<ApiResponse<LeadNoteResponse>> addNote(
            @PathVariable UUID leadId,
            @Valid @RequestBody LeadNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leadService.addNote(leadId, request, principal.getUserId()), "Note added"));
    }

    @GetMapping("/{leadId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get notes for a lead")
    public ResponseEntity<ApiResponse<PagedResponse<LeadNoteResponse>>> getNotes(
            @PathVariable UUID leadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(leadService.getNotes(leadId, page, size)));
    }

    @DeleteMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a note")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable UUID noteId,
            @AuthenticationPrincipal UserPrincipal principal) {
        leadService.deleteNote(noteId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Note deleted"));
    }

    // ── Tags ───────────────────────────────────────────────

    @GetMapping("/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all tags")
    public ResponseEntity<ApiResponse<List<LeadTagResponse>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.success(leadService.getAllTags()));
    }

    @PostMapping("/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a tag")
    public ResponseEntity<ApiResponse<LeadTagResponse>> createTag(@Valid @RequestBody LeadTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leadService.createTag(request), "Tag created"));
    }

    @GetMapping("/{leadId}/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get tags for a lead")
    public ResponseEntity<ApiResponse<List<LeadTagResponse>>> getLeadTags(@PathVariable UUID leadId) {
        return ResponseEntity.ok(ApiResponse.success(leadService.getLeadTags(leadId)));
    }

    @PostMapping("/{leadId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add tag to a lead")
    public ResponseEntity<ApiResponse<Void>> addTagToLead(
            @PathVariable UUID leadId, @PathVariable UUID tagId,
            @AuthenticationPrincipal UserPrincipal principal) {
        leadService.addTagToLead(leadId, tagId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Tag added to lead"));
    }

    @DeleteMapping("/{leadId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Remove tag from a lead")
    public ResponseEntity<ApiResponse<Void>> removeTagFromLead(
            @PathVariable UUID leadId, @PathVariable UUID tagId,
            @AuthenticationPrincipal UserPrincipal principal) {
        leadService.removeTagFromLead(leadId, tagId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Tag removed from lead"));
    }

    // ── Attachments ────────────────────────────────────────

    @PostMapping("/{leadId}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Upload attachment to a lead")
    public ResponseEntity<ApiResponse<LeadAttachmentResponse>> addAttachment(
            @PathVariable UUID leadId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leadService.addAttachment(leadId, file, principal.getUserId()), "Attachment uploaded"));
    }

    @GetMapping("/{leadId}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get attachments for a lead")
    public ResponseEntity<ApiResponse<List<LeadAttachmentResponse>>> getAttachments(@PathVariable UUID leadId) {
        return ResponseEntity.ok(ApiResponse.success(leadService.getAttachments(leadId)));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable UUID attachmentId) {
        LeadAttachment att = leadService.getAttachmentWithData(attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(att.getFileType() != null ? att.getFileType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + att.getFileName() + "\"")
                .body(att.getFileData());
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete an attachment")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        leadService.deleteAttachment(attachmentId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Attachment deleted"));
    }

    // ── Activities / Timeline ──────────────────────────────

    @GetMapping("/{leadId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activities/timeline for a lead")
    public ResponseEntity<ApiResponse<PagedResponse<LeadActivityResponse>>> getActivities(
            @PathVariable UUID leadId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(leadService.getActivities(leadId, type, page, size)));
    }

    // ── Duplicate Detection ────────────────────────────────

    @GetMapping("/duplicates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Find duplicate leads")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> findDuplicates(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return ResponseEntity.ok(ApiResponse.success(leadService.findDuplicates(email, phone)));
    }

    // ── Bulk Operations ────────────────────────────────────

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Bulk update/delete leads")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkUpdate(
            @Valid @RequestBody BulkUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                leadService.bulkUpdate(request, principal.getUserId()), "Bulk operation completed"));
    }

    // ── Import / Export ────────────────────────────────────

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Import leads from CSV")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importLeads(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                leadService.importLeads(file, principal.getUserId()), "Import completed"));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Export leads as CSV")
    public ResponseEntity<byte[]> exportLeads() {
        byte[] csv = leadService.exportLeads();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leads_export.csv\"")
                .body(csv);
    }

    // ── Assignment Rules ───────────────────────────────────

    @GetMapping("/assignment-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all assignment rules")
    public ResponseEntity<ApiResponse<List<AssignmentRuleResponse>>> getAssignmentRules() {
        return ResponseEntity.ok(ApiResponse.success(leadService.getAssignmentRules()));
    }

    @PostMapping("/assignment-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create an assignment rule")
    public ResponseEntity<ApiResponse<AssignmentRuleResponse>> createAssignmentRule(
            @Valid @RequestBody AssignmentRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leadService.createAssignmentRule(request), "Rule created"));
    }

    @DeleteMapping("/assignment-rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete an assignment rule")
    public ResponseEntity<ApiResponse<Void>> deleteAssignmentRule(@PathVariable UUID ruleId) {
        leadService.deleteAssignmentRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null, "Rule deleted"));
    }

    // ── Scoring Rules ──────────────────────────────────────

    @GetMapping("/scoring-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all scoring rules")
    public ResponseEntity<ApiResponse<List<ScoringRule>>> getScoringRules() {
        return ResponseEntity.ok(ApiResponse.success(leadService.getScoringRules()));
    }

    @PostMapping("/scoring-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a scoring rule")
    public ResponseEntity<ApiResponse<ScoringRule>> createScoringRule(@RequestBody ScoringRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leadService.createScoringRule(rule), "Scoring rule created"));
    }

    @DeleteMapping("/scoring-rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete a scoring rule")
    public ResponseEntity<ApiResponse<Void>> deleteScoringRule(@PathVariable UUID ruleId) {
        leadService.deleteScoringRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null, "Scoring rule deleted"));
    }

    @PostMapping("/{leadId}/recalculate-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Recalculate lead score")
    public ResponseEntity<ApiResponse<LeadResponse>> recalculateScore(
            @PathVariable UUID leadId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                leadService.recalculateScore(leadId, principal.getUserId()), "Score recalculated"));
    }

    // ── Analytics ──────────────────────────────────────────

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get lead analytics")
    public ResponseEntity<ApiResponse<LeadAnalyticsResponse>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(leadService.getAnalytics()));
    }

    // ── SLA Tracking ───────────────────────────────────────

    @GetMapping("/sla-breached")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get leads past SLA")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getLeadsPastSla() {
        return ResponseEntity.ok(ApiResponse.success(leadService.getLeadsPastSla()));
    }

    // ── Campaign Tracking ──────────────────────────────────

    @GetMapping("/campaign/{campaignId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get leads by campaign")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getLeadsByCampaign(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(ApiResponse.success(leadService.getLeadsByCampaign(campaignId)));
    }

    // ── Web Forms ──────────────────────────────────────────

    @GetMapping("/web-forms")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all web forms")
    public ResponseEntity<ApiResponse<List<WebForm>>> getWebForms() {
        return ResponseEntity.ok(ApiResponse.success(leadService.getWebForms()));
    }

    @PostMapping("/web-forms")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a web form")
    public ResponseEntity<ApiResponse<WebForm>> createWebForm(@RequestBody WebForm form) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leadService.createWebForm(form), "Web form created"));
    }

    @PostMapping("/web-forms/{formId}/submit")
    @Operation(summary = "Submit a web form (public)")
    public ResponseEntity<ApiResponse<LeadResponse>> submitWebForm(
            @PathVariable UUID formId,
            @Valid @RequestBody CreateLeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leadService.submitWebForm(formId, request), "Form submitted"));
    }

    // ── Email Capture ──────────────────────────────────────

    @PostMapping("/capture-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Capture lead from email")
    public ResponseEntity<ApiResponse<LeadResponse>> captureEmail(
            @RequestParam String email,
            @RequestParam(required = false) String source) {
        return ResponseEntity.ok(ApiResponse.success(leadService.captureEmail(email, source), "Email captured"));
    }

    @PostMapping("/capture-phone")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Capture lead from phone (WhatsApp/SMS)")
    public ResponseEntity<ApiResponse<LeadResponse>> capturePhone(
            @RequestParam String phone,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leadService.capturePhone(phone, source, firstName, lastName), "Phone captured"));
    }

    // ── Territory ──────────────────────────────────────────

    @GetMapping("/territory/{territory}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get leads by territory")
    public ResponseEntity<ApiResponse<PagedResponse<LeadResponse>>> getLeadsByTerritory(
            @PathVariable String territory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(leadService.getLeadsByTerritory(territory, page, size)));
    }
}
