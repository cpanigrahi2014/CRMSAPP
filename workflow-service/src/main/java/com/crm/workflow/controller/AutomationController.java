package com.crm.workflow.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.workflow.dto.*;
import com.crm.workflow.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/automation")
@RequiredArgsConstructor
public class AutomationController {

    private final WorkflowTemplateService templateService;
    private final WorkflowSuggestionService suggestionService;
    private final ProposalService proposalService;
    private final ContractService contractService;

    // ═══════════════════ Workflow Templates ════════════════════

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PagedResponse<WorkflowTemplateResponse>>> getTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplates(page, size)));
    }

    @GetMapping("/templates/entity/{entityType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<WorkflowTemplateResponse>>> getTemplatesByEntity(
            @PathVariable String entityType) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getByEntityType(entityType)));
    }

    // ═══════════════════ AI Suggestions ═══════════════════════

    @GetMapping("/suggestions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PagedResponse<WorkflowSuggestionResponse>>> getSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(suggestionService.getSuggestions(page, size)));
    }

    @GetMapping("/suggestions/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PagedResponse<WorkflowSuggestionResponse>>> getPendingSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(suggestionService.getPendingSuggestions(page, size)));
    }

    @GetMapping("/suggestions/pending-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<Long>> getPendingSuggestionCount() {
        return ResponseEntity.ok(ApiResponse.success(suggestionService.getPendingCount()));
    }

    @PostMapping("/suggestions/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<WorkflowSuggestionResponse>> acceptSuggestion(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(suggestionService.acceptSuggestion(id), "Suggestion accepted"));
    }

    @PostMapping("/suggestions/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<WorkflowSuggestionResponse>> dismissSuggestion(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(suggestionService.dismissSuggestion(id), "Suggestion dismissed"));
    }

    @PostMapping("/suggestions/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> generateSuggestions(
            @AuthenticationPrincipal UserPrincipal principal) {
        suggestionService.generateBestPracticeSuggestions(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Suggestions generated", "Best practice suggestions created"));
    }

    // ═══════════════════ Proposals ════════════════════════════

    @PostMapping("/proposals")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> createProposal(
            @Valid @RequestBody CreateProposalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                proposalService.createProposal(request, principal.getUserId()), "Proposal created"));
    }

    @GetMapping("/proposals")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PagedResponse<ProposalResponse>>> getAllProposals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(proposalService.getAllProposals(page, size)));
    }

    @GetMapping("/proposals/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> getProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(proposalService.getById(id)));
    }

    @GetMapping("/proposals/opportunity/{opportunityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PagedResponse<ProposalResponse>>> getByOpportunity(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(proposalService.getByOpportunity(opportunityId, page, size)));
    }

    @PostMapping("/proposals/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> sendProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(proposalService.sendProposal(id), "Proposal sent"));
    }

    @PostMapping("/proposals/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> acceptProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(proposalService.acceptProposal(id), "Proposal accepted"));
    }

    @PostMapping("/proposals/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> rejectProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(proposalService.rejectProposal(id), "Proposal rejected"));
    }

    @DeleteMapping("/proposals/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteProposal(@PathVariable UUID id) {
        proposalService.deleteProposal(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Proposal deleted"));
    }

    // ═══════════════════ Contracts ════════════════════════════

    @PostMapping("/contracts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ContractResponse>> createContract(
            @Valid @RequestBody CreateContractRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.createContract(request, principal.getUserId()), "Contract created"));
    }

    @GetMapping("/contracts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PagedResponse<ContractResponse>>> getAllContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getAllContracts(page, size)));
    }

    @GetMapping("/contracts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<ContractResponse>> getContract(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getById(id)));
    }

    @GetMapping("/contracts/opportunity/{opportunityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PagedResponse<ContractResponse>>> getContractsByOpportunity(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getByOpportunity(opportunityId, page, size)));
    }

    @PostMapping("/contracts/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ContractResponse>> sendContract(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.sendContract(id), "Contract sent"));
    }

    @PostMapping("/contracts/{id}/sign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<ContractResponse>> signContract(
            @PathVariable UUID id,
            @RequestBody SignContractRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(ApiResponse.success(contractService.signContract(id, request, ip), "Contract signed"));
    }

    @PostMapping("/contracts/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ContractResponse>> executeContract(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.executeContract(id), "Contract executed"));
    }

    @PostMapping("/contracts/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ContractResponse>> cancelContract(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.cancelContract(id), "Contract cancelled"));
    }

    @DeleteMapping("/contracts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteContract(@PathVariable UUID id) {
        contractService.deleteContract(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Contract deleted"));
    }
}
