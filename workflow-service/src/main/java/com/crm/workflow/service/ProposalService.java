package com.crm.workflow.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.security.TenantContext;
import com.crm.workflow.dto.CreateProposalRequest;
import com.crm.workflow.dto.ProposalResponse;
import com.crm.workflow.entity.Proposal;
import com.crm.workflow.entity.ProposalLineItem;
import com.crm.workflow.entity.ProposalTemplate;
import com.crm.workflow.repository.ProposalRepository;
import com.crm.workflow.repository.ProposalTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProposalTemplateRepository templateRepository;
    private final EventPublisher eventPublisher;

    /* ── Create ──────────────────────────────────────────── */

    @Transactional
    public ProposalResponse createProposal(CreateProposalRequest req, String userId) {
        String tenantId = TenantContext.getTenantId();

        Proposal proposal = Proposal.builder()
                .opportunityId(req.getOpportunityId())
                .templateId(req.getTemplateId())
                .title(req.getTitle())
                .amount(req.getAmount())
                .validUntil(req.getValidUntil())
                .recipientEmail(req.getRecipientEmail())
                .recipientName(req.getRecipientName())
                .notes(req.getNotes())
                .status("DRAFT")
                .tenantId(tenantId)
                .createdBy(userId)
                .build();

        // Generate content from template if provided
        if (req.getTemplateId() != null) {
            templateRepository.findById(req.getTemplateId()).ifPresent(template ->
                    proposal.setContent(generateContentFromTemplate(template, req)));
        }
        if (proposal.getContent() == null) {
            proposal.setContent(generateDefaultContent(req));
        }

        // Add line items
        if (req.getLineItems() != null) {
            BigDecimal total = BigDecimal.ZERO;
            int order = 0;
            for (CreateProposalRequest.LineItemRequest li : req.getLineItems()) {
                BigDecimal qty = BigDecimal.valueOf(li.getQuantity() != null ? li.getQuantity() : 1);
                BigDecimal discount = li.getDiscount() != null ? li.getDiscount() : BigDecimal.ZERO;
                BigDecimal lineTotal = li.getUnitPrice().multiply(qty)
                        .multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP);

                ProposalLineItem item = ProposalLineItem.builder()
                        .proposal(proposal)
                        .productName(li.getProductName())
                        .description(li.getDescription())
                        .quantity(li.getQuantity() != null ? li.getQuantity() : 1)
                        .unitPrice(li.getUnitPrice())
                        .discount(discount)
                        .totalPrice(lineTotal)
                        .sortOrder(order++)
                        .build();
                proposal.getLineItems().add(item);
                total = total.add(lineTotal);
            }
            if (proposal.getAmount() == null) {
                proposal.setAmount(total);
            }
        }

        Proposal saved = proposalRepository.save(proposal);
        publishEvent("PROPOSAL_CREATED", saved.getId().toString());
        log.info("Proposal created: {} for opportunity: {}", saved.getId(), saved.getOpportunityId());
        return toResponse(saved);
    }

    /* ── Read ─────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public PagedResponse<ProposalResponse> getAllProposals(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<Proposal> p = proposalRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(
                tenantId, PageRequest.of(page, size));
        return buildPaged(p);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProposalResponse> getByOpportunity(UUID opportunityId, int page, int size) {
        Page<Proposal> p = proposalRepository.findByOpportunityIdAndDeletedFalseOrderByVersionDesc(
                opportunityId, PageRequest.of(page, size));
        return buildPaged(p);
    }

    @Transactional(readOnly = true)
    public ProposalResponse getById(UUID id) {
        String tenantId = TenantContext.getTenantId();
        Proposal p = proposalRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));
        return toResponse(p);
    }

    /* ── Status transitions ───────────────────────────────── */

    @Transactional
    public ProposalResponse sendProposal(UUID id) {
        Proposal p = getProposalEntity(id);
        p.setStatus("SENT");
        p.setSentAt(LocalDateTime.now());
        publishEvent("PROPOSAL_SENT", id.toString());
        return toResponse(proposalRepository.save(p));
    }

    @Transactional
    public ProposalResponse markViewed(UUID id) {
        Proposal p = getProposalEntity(id);
        if (p.getViewedAt() == null) {
            p.setViewedAt(LocalDateTime.now());
            p.setStatus("VIEWED");
        }
        return toResponse(proposalRepository.save(p));
    }

    @Transactional
    public ProposalResponse acceptProposal(UUID id) {
        Proposal p = getProposalEntity(id);
        p.setStatus("ACCEPTED");
        p.setRespondedAt(LocalDateTime.now());
        publishEvent("PROPOSAL_ACCEPTED", id.toString());
        return toResponse(proposalRepository.save(p));
    }

    @Transactional
    public ProposalResponse rejectProposal(UUID id) {
        Proposal p = getProposalEntity(id);
        p.setStatus("REJECTED");
        p.setRespondedAt(LocalDateTime.now());
        publishEvent("PROPOSAL_REJECTED", id.toString());
        return toResponse(proposalRepository.save(p));
    }

    @Transactional
    public void deleteProposal(UUID id) {
        Proposal p = getProposalEntity(id);
        p.setDeleted(true);
        proposalRepository.save(p);
    }

    /* ── Template Methods ─────────────────────────────────── */

    @Transactional(readOnly = true)
    public List<ProposalTemplate> getTemplates() {
        return templateRepository.findByTenantId(TenantContext.getTenantId(),
                PageRequest.of(0, 50)).getContent();
    }

    /* ── Helpers ──────────────────────────────────────────── */

    private Proposal getProposalEntity(UUID id) {
        String tenantId = TenantContext.getTenantId();
        return proposalRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));
    }

    private String generateContentFromTemplate(ProposalTemplate template, CreateProposalRequest req) {
        String content = template.getContentTemplate();
        content = content.replace("{{title}}", req.getTitle() != null ? req.getTitle() : "")
                .replace("{{recipientName}}", req.getRecipientName() != null ? req.getRecipientName() : "Valued Customer")
                .replace("{{amount}}", req.getAmount() != null ? req.getAmount().toPlainString() : "TBD")
                .replace("{{validUntil}}", req.getValidUntil() != null ? req.getValidUntil().toString() : "30 days from receipt");
        return content;
    }

    private String generateDefaultContent(CreateProposalRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(req.getTitle()).append("\n\n");
        sb.append("Dear ").append(req.getRecipientName() != null ? req.getRecipientName() : "Valued Customer").append(",\n\n");
        sb.append("Thank you for your interest. We are pleased to present the following proposal:\n\n");
        if (req.getAmount() != null) {
            sb.append("**Total Amount:** $").append(req.getAmount().toPlainString()).append("\n\n");
        }
        if (req.getValidUntil() != null) {
            sb.append("**Valid Until:** ").append(req.getValidUntil()).append("\n\n");
        }
        if (req.getLineItems() != null && !req.getLineItems().isEmpty()) {
            sb.append("## Line Items\n\n");
            sb.append("| Product | Qty | Unit Price | Discount | Total |\n");
            sb.append("|---------|-----|-----------|----------|-------|\n");
            for (CreateProposalRequest.LineItemRequest li : req.getLineItems()) {
                sb.append("| ").append(li.getProductName())
                        .append(" | ").append(li.getQuantity() != null ? li.getQuantity() : 1)
                        .append(" | $").append(li.getUnitPrice())
                        .append(" | ").append(li.getDiscount() != null ? li.getDiscount() + "%" : "0%")
                        .append(" | - |\n");
            }
            sb.append("\n");
        }
        sb.append("We look forward to working with you.\n\nBest regards,\nSales Team");
        return sb.toString();
    }

    private void publishEvent(String eventType, String entityId) {
        try {
            eventPublisher.publish("workflow-actions", TenantContext.getTenantId(),
                    "system", "PROPOSAL", entityId, eventType, null);
        } catch (Exception e) {
            log.warn("Failed to publish proposal event: {}", e.getMessage());
        }
    }

    private PagedResponse<ProposalResponse> buildPaged(Page<Proposal> p) {
        return PagedResponse.<ProposalResponse>builder()
                .content(p.getContent().stream().map(this::toResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .build();
    }

    private ProposalResponse toResponse(Proposal p) {
        return ProposalResponse.builder()
                .id(p.getId()).opportunityId(p.getOpportunityId())
                .templateId(p.getTemplateId()).title(p.getTitle())
                .content(p.getContent()).status(p.getStatus())
                .amount(p.getAmount()).validUntil(p.getValidUntil())
                .sentAt(p.getSentAt()).viewedAt(p.getViewedAt())
                .respondedAt(p.getRespondedAt())
                .recipientEmail(p.getRecipientEmail()).recipientName(p.getRecipientName())
                .notes(p.getNotes()).version(p.getVersion())
                .lineItems(p.getLineItems().stream().map(li ->
                        ProposalResponse.LineItemResponse.builder()
                                .id(li.getId()).productName(li.getProductName())
                                .description(li.getDescription()).quantity(li.getQuantity())
                                .unitPrice(li.getUnitPrice()).discount(li.getDiscount())
                                .totalPrice(li.getTotalPrice()).build()
                ).toList())
                .createdAt(p.getCreatedAt()).createdBy(p.getCreatedBy())
                .build();
    }
}
