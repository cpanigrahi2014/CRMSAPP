package com.crm.account.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.account.dto.*;
import com.crm.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management APIs")
public class AccountController {

    private final AccountService accountService;

    // ── CRUD ───────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Create a new account")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccountResponse response = accountService.createAccount(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Account created successfully"));
    }

    @PutMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Update an existing account")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateAccountRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccountResponse response = accountService.updateAccount(accountId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Account updated successfully"));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable UUID accountId) {
        AccountResponse response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all accounts with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<AccountResponse> response = accountService.getAllAccounts(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Search accounts")
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> searchAccounts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AccountResponse> response = accountService.searchAccounts(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete an account (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        accountService.deleteAccount(accountId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Account deleted successfully"));
    }

    // ── Hierarchy ──────────────────────────────────────────────
    @GetMapping("/{accountId}/children")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get child accounts of a parent account")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getChildAccounts(@PathVariable UUID accountId) {
        List<AccountResponse> response = accountService.getChildAccounts(accountId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Owner Assignment ───────────────────────────────────────
    @PutMapping("/{accountId}/owner")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Assign account owner")
    public ResponseEntity<ApiResponse<AccountResponse>> assignOwner(
            @PathVariable UUID accountId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccountResponse response = accountService.assignOwner(accountId, body.get("ownerId"), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Owner assigned"));
    }

    // ── Territory Assignment ───────────────────────────────────
    @PutMapping("/{accountId}/territory")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Assign account territory")
    public ResponseEntity<ApiResponse<AccountResponse>> assignTerritory(
            @PathVariable UUID accountId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccountResponse response = accountService.assignTerritory(accountId, body.get("territory"), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Territory assigned"));
    }

    // ── Filter endpoints ───────────────────────────────────────
    @GetMapping("/by-type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get accounts by type")
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getByType(
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountsByType(type, page, size)));
    }

    @GetMapping("/by-territory")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get accounts by territory")
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getByTerritory(
            @RequestParam String territory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountsByTerritory(territory, page, size)));
    }

    @GetMapping("/by-segment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get accounts by segment")
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getBySegment(
            @RequestParam String segment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountsBySegment(segment, page, size)));
    }

    @GetMapping("/by-lifecycle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get accounts by lifecycle stage")
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getByLifecycleStage(
            @RequestParam String stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountsByLifecycleStage(stage, page, size)));
    }

    @GetMapping("/by-owner")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get accounts by owner")
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getByOwner(
            @RequestParam String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountsByOwner(ownerId, page, size)));
    }

    // ── Notes ──────────────────────────────────────────────────
    @PostMapping("/{accountId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add a note to an account")
    public ResponseEntity<ApiResponse<AccountNoteResponse>> addNote(
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccountNoteResponse response = accountService.addNote(accountId, request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Note added"));
    }

    @GetMapping("/{accountId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all notes for an account")
    public ResponseEntity<ApiResponse<List<AccountNoteResponse>>> getNotes(@PathVariable UUID accountId) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getNotes(accountId)));
    }

    @DeleteMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a note")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable UUID noteId) {
        accountService.deleteNote(noteId);
        return ResponseEntity.ok(ApiResponse.success(null, "Note deleted"));
    }

    // ── Attachments ────────────────────────────────────────────
    @PostMapping("/{accountId}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add an attachment to an account")
    public ResponseEntity<ApiResponse<AccountAttachmentResponse>> addAttachment(
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountAttachmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccountAttachmentResponse response = accountService.addAttachment(accountId, request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Attachment added"));
    }

    @GetMapping("/{accountId}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all attachments for an account")
    public ResponseEntity<ApiResponse<List<AccountAttachmentResponse>>> getAttachments(@PathVariable UUID accountId) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAttachments(accountId)));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete an attachment")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable UUID attachmentId) {
        accountService.deleteAttachment(attachmentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Attachment deleted"));
    }

    // ── Activities / Timeline ──────────────────────────────────
    @GetMapping("/{accountId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activity timeline for an account")
    public ResponseEntity<ApiResponse<List<AccountActivityResponse>>> getActivities(@PathVariable UUID accountId) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getActivities(accountId)));
    }

    // ── Tags ───────────────────────────────────────────────────
    @PostMapping("/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a tag")
    public ResponseEntity<ApiResponse<AccountTagResponse>> createTag(@RequestBody Map<String, String> body) {
        AccountTagResponse response = accountService.createTag(body.get("name"), body.get("color"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Tag created"));
    }

    @GetMapping("/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all tags")
    public ResponseEntity<ApiResponse<List<AccountTagResponse>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAllTags()));
    }

    @GetMapping("/{accountId}/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get tags for an account")
    public ResponseEntity<ApiResponse<List<AccountTagResponse>>> getAccountTags(@PathVariable UUID accountId) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountTags(accountId)));
    }

    @PostMapping("/{accountId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add tag to account")
    public ResponseEntity<ApiResponse<Void>> addTagToAccount(
            @PathVariable UUID accountId,
            @PathVariable UUID tagId,
            @AuthenticationPrincipal UserPrincipal principal) {
        accountService.addTagToAccount(accountId, tagId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Tag added to account"));
    }

    @DeleteMapping("/{accountId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove tag from account")
    public ResponseEntity<ApiResponse<Void>> removeTagFromAccount(
            @PathVariable UUID accountId,
            @PathVariable UUID tagId) {
        accountService.removeTagFromAccount(accountId, tagId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tag removed from account"));
    }

    // ── Duplicate Detection ────────────────────────────────────
    @GetMapping("/duplicates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Detect potential duplicate accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> detectDuplicates(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String website) {
        return ResponseEntity.ok(ApiResponse.success(accountService.detectDuplicates(name, phone, website)));
    }

    // ── Health Score ───────────────────────────────────────────
    @PutMapping("/{accountId}/health-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update account health score")
    public ResponseEntity<ApiResponse<AccountResponse>> updateHealthScore(
            @PathVariable UUID accountId,
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccountResponse response = accountService.updateHealthScore(accountId, body.get("healthScore"), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Health score updated"));
    }

    // ── Engagement Score ───────────────────────────────────────
    @PutMapping("/{accountId}/engagement-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update account engagement score")
    public ResponseEntity<ApiResponse<AccountResponse>> updateEngagementScore(
            @PathVariable UUID accountId,
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccountResponse response = accountService.updateEngagementScore(accountId, body.get("engagementScore"), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Engagement score updated"));
    }

    // ── Import / Export ────────────────────────────────────────
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Import accounts from CSV")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importAccounts(
            @RequestBody String csvContent,
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = accountService.importAccountsFromCsv(csvContent, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("imported", count), count + " accounts imported"));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Export accounts to CSV")
    public ResponseEntity<String> exportAccounts() {
        String csv = accountService.exportAccountsToCsv();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=accounts.csv")
                .body(csv);
    }

    // ── Bulk Operations ────────────────────────────────────────
    @PutMapping("/bulk-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Bulk update accounts")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkUpdate(
            @RequestBody BulkAccountUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = accountService.bulkUpdate(request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", count), count + " accounts updated"));
    }

    @PostMapping("/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Bulk delete accounts")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkDelete(
            @RequestBody List<String> accountIds,
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = accountService.bulkDelete(accountIds, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", count), count + " accounts deleted"));
    }

    // ── Analytics / Reporting / Dashboard ──────────────────────
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get account analytics and reporting data")
    public ResponseEntity<ApiResponse<AccountAnalyticsResponse>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAnalytics()));
    }
}
