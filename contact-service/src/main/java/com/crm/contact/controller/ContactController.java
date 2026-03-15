package com.crm.contact.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.contact.dto.*;
import com.crm.contact.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Contact management APIs")
public class ContactController {

    private final ContactService contactService;

    // ── Feature 1: CRUD ──────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Create a new contact")
    public ResponseEntity<ApiResponse<ContactResponse>> createContact(
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContactResponse response = contactService.createContact(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Contact created successfully"));
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Update an existing contact")
    public ResponseEntity<ApiResponse<ContactResponse>> updateContact(
            @PathVariable UUID contactId,
            @Valid @RequestBody UpdateContactRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContactResponse response = contactService.updateContact(contactId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Contact updated successfully"));
    }

    @GetMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get contact by ID")
    public ResponseEntity<ApiResponse<ContactResponse>> getContactById(@PathVariable UUID contactId) {
        ContactResponse response = contactService.getContactById(contactId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all contacts with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<ContactResponse>>> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<ContactResponse> response = contactService.getAllContacts(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a contact (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteContact(
            @PathVariable UUID contactId,
            @AuthenticationPrincipal UserPrincipal principal) {
        contactService.deleteContact(contactId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Contact deleted successfully"));
    }

    // ── Feature 2: Account linking ───────────────────────────
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get contacts by account ID")
    public ResponseEntity<ApiResponse<PagedResponse<ContactResponse>>> getContactsByAccount(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ContactResponse> response = contactService.getContactsByAccount(accountId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Search contacts")
    public ResponseEntity<ApiResponse<PagedResponse<ContactResponse>>> searchContacts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ContactResponse> response = contactService.searchContacts(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Feature 3: Communication history ─────────────────────
    @PostMapping("/{contactId}/communications")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Log a communication for a contact")
    public ResponseEntity<ApiResponse<CommunicationResponse>> addCommunication(
            @PathVariable UUID contactId,
            @Valid @RequestBody CreateCommunicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CommunicationResponse response = contactService.addCommunication(contactId, request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Communication logged"));
    }

    @GetMapping("/{contactId}/communications")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get communication history for a contact")
    public ResponseEntity<ApiResponse<PagedResponse<CommunicationResponse>>> getCommunications(
            @PathVariable UUID contactId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CommunicationResponse> response = contactService.getCommunications(contactId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/communications/{commId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a communication record")
    public ResponseEntity<ApiResponse<Void>> deleteCommunication(@PathVariable UUID commId) {
        contactService.deleteCommunication(commId);
        return ResponseEntity.ok(ApiResponse.success(null, "Communication deleted"));
    }

    // ── Feature 5: Segmentation ──────────────────────────────
    @GetMapping("/segment/{segment}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get contacts by segment")
    public ResponseEntity<ApiResponse<PagedResponse<ContactResponse>>> getBySegment(
            @PathVariable String segment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(contactService.getContactsBySegment(segment, page, size)));
    }

    @GetMapping("/lifecycle/{stage}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get contacts by lifecycle stage")
    public ResponseEntity<ApiResponse<PagedResponse<ContactResponse>>> getByLifecycle(
            @PathVariable String stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(contactService.getContactsByLifecycleStage(stage, page, size)));
    }

    // ── Feature 6: Marketing consent ─────────────────────────
    @PutMapping("/{contactId}/consent")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Update marketing consent for a contact")
    public ResponseEntity<ApiResponse<ContactResponse>> updateConsent(
            @PathVariable UUID contactId,
            @Valid @RequestBody UpdateConsentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContactResponse response = contactService.updateConsent(contactId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Consent updated"));
    }

    // ── Feature 7: Activity timeline ─────────────────────────
    @GetMapping("/{contactId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activity timeline for a contact")
    public ResponseEntity<ApiResponse<PagedResponse<ContactActivityResponse>>> getActivityTimeline(
            @PathVariable UUID contactId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(contactService.getActivityTimeline(contactId, page, size)));
    }

    // ── Feature 8: Tagging ───────────────────────────────────
    @PostMapping("/{contactId}/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Add a tag to a contact")
    public ResponseEntity<ApiResponse<ContactTagResponse>> addTag(
            @PathVariable UUID contactId,
            @RequestParam String tagName,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContactTagResponse response = contactService.addTag(contactId, tagName, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Tag added"));
    }

    @GetMapping("/{contactId}/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all tags for a contact")
    public ResponseEntity<ApiResponse<List<ContactTagResponse>>> getTags(@PathVariable UUID contactId) {
        return ResponseEntity.ok(ApiResponse.success(contactService.getTags(contactId)));
    }

    @DeleteMapping("/{contactId}/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove a tag from a contact")
    public ResponseEntity<ApiResponse<Void>> removeTag(
            @PathVariable UUID contactId,
            @RequestParam String tagName,
            @AuthenticationPrincipal UserPrincipal principal) {
        contactService.removeTag(contactId, tagName, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Tag removed"));
    }

    // ── Feature 9: Duplicate detection ───────────────────────
    @GetMapping("/duplicates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Detect duplicate contacts")
    public ResponseEntity<ApiResponse<List<DuplicateContactGroup>>> detectDuplicates() {
        return ResponseEntity.ok(ApiResponse.success(contactService.detectDuplicates()));
    }

    @PostMapping("/merge")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Merge duplicate contacts")
    public ResponseEntity<ApiResponse<ContactResponse>> mergeContacts(
            @RequestParam UUID primaryId,
            @RequestParam UUID duplicateId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContactResponse response = contactService.mergeContacts(primaryId, duplicateId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Contacts merged successfully"));
    }

    // ── Feature 10: Analytics ────────────────────────────────
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get contact analytics")
    public ResponseEntity<ApiResponse<ContactAnalyticsResponse>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(contactService.getAnalytics()));
    }

    // ── Import / Export ──────────────────────────────────────
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Import contacts from CSV file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importContacts(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                contactService.importContactsFromFile(file, principal.getUserId()), "Import completed"));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Export contacts to CSV")
    public ResponseEntity<byte[]> exportContacts() {
        String csv = contactService.exportContactsToCsv();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=contacts.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
