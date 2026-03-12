package com.crm.workflow.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.security.TenantContext;
import com.crm.workflow.dto.ContractResponse;
import com.crm.workflow.dto.CreateContractRequest;
import com.crm.workflow.dto.SignContractRequest;
import com.crm.workflow.entity.Contract;
import com.crm.workflow.entity.Proposal;
import com.crm.workflow.repository.ContractRepository;
import com.crm.workflow.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ProposalRepository proposalRepository;
    private final EventPublisher eventPublisher;

    /* ── Create ──────────────────────────────────────────── */

    @Transactional
    public ContractResponse createContract(CreateContractRequest req, String userId) {
        String tenantId = TenantContext.getTenantId();

        Contract contract = Contract.builder()
                .opportunityId(req.getOpportunityId())
                .proposalId(req.getProposalId())
                .title(req.getTitle())
                .amount(req.getAmount())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .signerName(req.getSignerName())
                .signerEmail(req.getSignerEmail())
                .notes(req.getNotes())
                .status("DRAFT")
                .tenantId(tenantId)
                .createdBy(userId)
                .build();

        // Auto-generate contract content
        contract.setContent(generateContractContent(req, tenantId));

        // If linked to proposal, copy amount if not set
        if (req.getProposalId() != null && contract.getAmount() == null) {
            proposalRepository.findById(req.getProposalId()).ifPresent(p ->
                    contract.setAmount(p.getAmount()));
        }

        Contract saved = contractRepository.save(contract);
        publishEvent("CONTRACT_CREATED", saved.getId().toString());
        log.info("Contract created: {} for opportunity: {}", saved.getId(), saved.getOpportunityId());
        return toResponse(saved);
    }

    /* ── Read ─────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public PagedResponse<ContractResponse> getAllContracts(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<Contract> p = contractRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(
                tenantId, PageRequest.of(page, size));
        return buildPaged(p);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContractResponse> getByOpportunity(UUID opportunityId, int page, int size) {
        Page<Contract> p = contractRepository.findByOpportunityIdAndDeletedFalseOrderByVersionDesc(
                opportunityId, PageRequest.of(page, size));
        return buildPaged(p);
    }

    @Transactional(readOnly = true)
    public ContractResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    /* ── Status transitions ───────────────────────────────── */

    @Transactional
    public ContractResponse sendContract(UUID id) {
        Contract c = getEntity(id);
        c.setStatus("SENT");
        c.setSentAt(LocalDateTime.now());
        publishEvent("CONTRACT_SENT", id.toString());
        return toResponse(contractRepository.save(c));
    }

    @Transactional
    public ContractResponse markViewed(UUID id) {
        Contract c = getEntity(id);
        if (c.getViewedAt() == null) {
            c.setViewedAt(LocalDateTime.now());
            if ("SENT".equals(c.getStatus())) c.setStatus("VIEWED");
        }
        return toResponse(contractRepository.save(c));
    }

    @Transactional
    public ContractResponse signContract(UUID id, SignContractRequest req, String signerIp) {
        Contract c = getEntity(id);
        c.setStatus("SIGNED");
        c.setSignedAt(LocalDateTime.now());
        c.setSignerName(req.getSignerName());
        c.setSignerEmail(req.getSignerEmail());
        c.setSignerIp(signerIp);
        c.setSignatureData(req.getSignatureData());
        publishEvent("CONTRACT_SIGNED", id.toString());
        return toResponse(contractRepository.save(c));
    }

    @Transactional
    public ContractResponse executeContract(UUID id) {
        Contract c = getEntity(id);
        c.setStatus("EXECUTED");
        c.setExecutedAt(LocalDateTime.now());
        publishEvent("CONTRACT_EXECUTED", id.toString());
        return toResponse(contractRepository.save(c));
    }

    @Transactional
    public ContractResponse cancelContract(UUID id) {
        Contract c = getEntity(id);
        c.setStatus("CANCELLED");
        publishEvent("CONTRACT_CANCELLED", id.toString());
        return toResponse(contractRepository.save(c));
    }

    @Transactional
    public void deleteContract(UUID id) {
        Contract c = getEntity(id);
        c.setDeleted(true);
        contractRepository.save(c);
    }

    /* ── Helpers ──────────────────────────────────────────── */

    private Contract getEntity(UUID id) {
        String tenantId = TenantContext.getTenantId();
        return contractRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
    }

    private String generateContractContent(CreateContractRequest req, String tenantId) {
        StringBuilder sb = new StringBuilder();
        sb.append("# SERVICE AGREEMENT\n\n");
        sb.append("**Contract Title:** ").append(req.getTitle()).append("\n\n");
        sb.append("## PARTIES\n\n");
        sb.append("**Service Provider:** [Company Name]\n\n");
        if (req.getSignerName() != null) {
            sb.append("**Client:** ").append(req.getSignerName()).append("\n\n");
        }
        sb.append("## TERMS\n\n");
        if (req.getStartDate() != null) {
            sb.append("**Effective Date:** ").append(req.getStartDate()).append("\n\n");
        }
        if (req.getEndDate() != null) {
            sb.append("**Expiration Date:** ").append(req.getEndDate()).append("\n\n");
        }
        if (req.getAmount() != null) {
            sb.append("**Contract Value:** $").append(req.getAmount().toPlainString()).append("\n\n");
        }
        sb.append("## SCOPE OF WORK\n\n");
        sb.append("The Service Provider agrees to deliver the products and services as outlined in the associated proposal.\n\n");
        sb.append("## PAYMENT TERMS\n\n");
        sb.append("Payment is due within 30 days of invoice date.\n\n");
        sb.append("## SIGNATURES\n\n");
        sb.append("By signing below, both parties agree to the terms outlined in this agreement.\n\n");
        sb.append("___________________________\n");
        sb.append("Client Signature & Date\n\n");
        sb.append("___________________________\n");
        sb.append("Service Provider Signature & Date\n");
        return sb.toString();
    }

    private void publishEvent(String eventType, String entityId) {
        try {
            eventPublisher.publish("workflow-actions", TenantContext.getTenantId(),
                    "system", "CONTRACT", entityId, eventType, null);
        } catch (Exception e) {
            log.warn("Failed to publish contract event: {}", e.getMessage());
        }
    }

    private PagedResponse<ContractResponse> buildPaged(Page<Contract> p) {
        return PagedResponse.<ContractResponse>builder()
                .content(p.getContent().stream().map(this::toResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .build();
    }

    private ContractResponse toResponse(Contract c) {
        return ContractResponse.builder()
                .id(c.getId()).opportunityId(c.getOpportunityId())
                .proposalId(c.getProposalId()).title(c.getTitle())
                .content(c.getContent()).status(c.getStatus())
                .amount(c.getAmount()).startDate(c.getStartDate()).endDate(c.getEndDate())
                .sentAt(c.getSentAt()).viewedAt(c.getViewedAt())
                .signedAt(c.getSignedAt()).executedAt(c.getExecutedAt())
                .signerName(c.getSignerName()).signerEmail(c.getSignerEmail())
                .notes(c.getNotes()).version(c.getVersion())
                .createdAt(c.getCreatedAt()).createdBy(c.getCreatedBy())
                .build();
    }
}
