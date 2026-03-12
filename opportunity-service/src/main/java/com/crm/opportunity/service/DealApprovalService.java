package com.crm.opportunity.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.security.TenantContext;
import com.crm.opportunity.dto.ApprovalDecisionRequest;
import com.crm.opportunity.dto.CreateApprovalRequest;
import com.crm.opportunity.dto.DealApprovalResponse;
import com.crm.opportunity.entity.DealApproval;
import com.crm.opportunity.repository.DealApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DealApprovalService {

    private final DealApprovalRepository approvalRepo;
    private final EventPublisher eventPublisher;

    @Transactional
    public DealApprovalResponse createApproval(CreateApprovalRequest request, String userId, String userName) {
        String tenantId = TenantContext.getTenantId();

        DealApproval approval = DealApproval.builder()
                .opportunityId(request.getOpportunityId())
                .requestedById(userId)
                .requestedByName(userName)
                .approverId(request.getApproverId())
                .approverName(request.getApproverName())
                .approvalType(DealApproval.ApprovalType.valueOf(request.getApprovalType()))
                .title(request.getTitle())
                .description(request.getDescription())
                .currentValue(request.getCurrentValue())
                .requestedValue(request.getRequestedValue())
                .priority(request.getPriority() != null
                        ? DealApproval.ApprovalPriority.valueOf(request.getPriority())
                        : DealApproval.ApprovalPriority.NORMAL)
                .dueDate(request.getDueDate())
                .tenantId(tenantId)
                .build();

        approval = approvalRepo.save(approval);
        log.info("Approval request created for deal {} by {}", request.getOpportunityId(), userId);

        eventPublisher.publish("opportunity-events", tenantId, userId,
                "DealApproval", approval.getId().toString(), "APPROVAL_REQUESTED",
                mapToResponse(approval));

        return mapToResponse(approval);
    }

    @Transactional
    public DealApprovalResponse decide(UUID approvalId, ApprovalDecisionRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        DealApproval approval = approvalRepo.findByIdAndTenantIdAndDeletedFalse(approvalId, tenantId)
                .orElseThrow(() -> new RuntimeException("Approval not found"));

        if (!approval.getApproverId().equals(userId)) {
            throw new RuntimeException("Only the assigned approver can decide on this approval");
        }

        if (approval.getStatus() != DealApproval.ApprovalStatus.PENDING) {
            throw new RuntimeException("This approval has already been decided");
        }

        DealApproval.ApprovalStatus newStatus = DealApproval.ApprovalStatus.valueOf(request.getDecision());
        approval.setStatus(newStatus);
        approval.setApproverComment(request.getComment());
        approval.setDecidedAt(LocalDateTime.now());

        approval = approvalRepo.save(approval);
        log.info("Approval {} decided as {} by {}", approvalId, newStatus, userId);

        String eventType = newStatus == DealApproval.ApprovalStatus.APPROVED
                ? "APPROVAL_APPROVED" : "APPROVAL_REJECTED";
        eventPublisher.publish("opportunity-events", tenantId, userId,
                "DealApproval", approval.getId().toString(), eventType,
                mapToResponse(approval));

        return mapToResponse(approval);
    }

    @Transactional(readOnly = true)
    public PagedResponse<DealApprovalResponse> getByOpportunity(UUID opportunityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<DealApproval> p = approvalRepo
                .findByOpportunityIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
                        opportunityId, tenantId, PageRequest.of(page, size));

        return buildPagedResponse(p);
    }

    @Transactional(readOnly = true)
    public PagedResponse<DealApprovalResponse> getPendingForApprover(String approverId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<DealApproval> p = approvalRepo
                .findByApproverIdAndStatusAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
                        approverId, DealApproval.ApprovalStatus.PENDING, tenantId, PageRequest.of(page, size));

        return buildPagedResponse(p);
    }

    @Transactional(readOnly = true)
    public PagedResponse<DealApprovalResponse> getMyApprovals(String approverId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<DealApproval> p = approvalRepo
                .findByApproverIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
                        approverId, tenantId, PageRequest.of(page, size));

        return buildPagedResponse(p);
    }

    @Transactional(readOnly = true)
    public List<DealApprovalResponse> getMyRequests(String requestedById) {
        String tenantId = TenantContext.getTenantId();
        return approvalRepo.findByRequestedByIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
                requestedById, tenantId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getPendingCount(String approverId) {
        String tenantId = TenantContext.getTenantId();
        return approvalRepo.countByApproverIdAndStatusAndTenantIdAndDeletedFalse(
                approverId, DealApproval.ApprovalStatus.PENDING, tenantId);
    }

    private PagedResponse<DealApprovalResponse> buildPagedResponse(Page<DealApproval> p) {
        return PagedResponse.<DealApprovalResponse>builder()
                .content(p.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(p.getNumber())
                .pageSize(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .first(p.isFirst())
                .build();
    }

    private DealApprovalResponse mapToResponse(DealApproval a) {
        return DealApprovalResponse.builder()
                .id(a.getId())
                .opportunityId(a.getOpportunityId())
                .requestedById(a.getRequestedById())
                .requestedByName(a.getRequestedByName())
                .approverId(a.getApproverId())
                .approverName(a.getApproverName())
                .approvalType(a.getApprovalType().name())
                .status(a.getStatus().name())
                .title(a.getTitle())
                .description(a.getDescription())
                .currentValue(a.getCurrentValue())
                .requestedValue(a.getRequestedValue())
                .approverComment(a.getApproverComment())
                .priority(a.getPriority().name())
                .dueDate(a.getDueDate())
                .decidedAt(a.getDecidedAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
