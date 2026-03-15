package com.crm.campaign.service;

import com.crm.campaign.dto.*;
import com.crm.campaign.entity.Campaign;
import com.crm.campaign.entity.Campaign.CampaignStatus;
import com.crm.campaign.entity.Campaign.CampaignType;
import com.crm.campaign.entity.CampaignMember;
import com.crm.campaign.entity.CampaignMember.MemberStatus;
import com.crm.campaign.repository.CampaignMemberRepository;
import com.crm.campaign.repository.CampaignRepository;
import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository memberRepository;
    private final EventPublisher eventPublisher;

    // ==================== CAMPAIGN CRUD ====================

    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating campaign '{}' for tenant {}", request.getName(), tenantId);

        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .type(request.getType() != null ? CampaignType.valueOf(request.getType()) : CampaignType.EMAIL)
                .status(request.getStatus() != null ? CampaignStatus.valueOf(request.getStatus()) : CampaignStatus.DRAFT)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .budget(request.getBudget() != null ? request.getBudget() : BigDecimal.ZERO)
                .actualCost(request.getActualCost() != null ? request.getActualCost() : BigDecimal.ZERO)
                .expectedRevenue(request.getExpectedRevenue() != null ? request.getExpectedRevenue() : BigDecimal.ZERO)
                .description(request.getDescription())
                .build();

        campaign.setTenantId(tenantId);
        campaign.setCreatedBy(userId);
        campaign = campaignRepository.save(campaign);

        log.info("Campaign created with ID: {}", campaign.getId());
        publishEvent("campaign.created", campaign.getId().toString(), tenantId, userId);
        return toResponse(campaign);
    }

    @Transactional
    public CampaignResponse updateCampaign(UUID campaignId, UpdateCampaignRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        Campaign campaign = campaignRepository.findByIdAndTenantIdAndDeletedFalse(campaignId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));

        if (request.getName() != null) campaign.setName(request.getName());
        if (request.getType() != null) campaign.setType(CampaignType.valueOf(request.getType()));
        if (request.getStatus() != null) campaign.setStatus(CampaignStatus.valueOf(request.getStatus()));
        if (request.getStartDate() != null) campaign.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) campaign.setEndDate(request.getEndDate());
        if (request.getBudget() != null) campaign.setBudget(request.getBudget());
        if (request.getActualCost() != null) campaign.setActualCost(request.getActualCost());
        if (request.getExpectedRevenue() != null) campaign.setExpectedRevenue(request.getExpectedRevenue());
        if (request.getWonRevenue() != null) campaign.setWonRevenue(request.getWonRevenue());
        if (request.getDescription() != null) campaign.setDescription(request.getDescription());
        if (request.getNumberSent() != null) campaign.setNumberSent(request.getNumberSent());
        if (request.getLeadsGenerated() != null) campaign.setLeadsGenerated(request.getLeadsGenerated());
        if (request.getConversions() != null) campaign.setConversions(request.getConversions());

        campaign = campaignRepository.save(campaign);
        publishEvent("campaign.updated", campaign.getId().toString(), tenantId, userId);
        return toResponse(campaign);
    }

    public CampaignResponse getCampaignById(UUID campaignId) {
        String tenantId = TenantContext.getTenantId();
        Campaign campaign = campaignRepository.findByIdAndTenantIdAndDeletedFalse(campaignId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        return toResponse(campaign);
    }

    public PagedResponse<CampaignResponse> getAllCampaigns(int page, int size, String status, String type,
                                                            String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "desc"),
                sortBy != null ? sortBy : "createdAt");
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Campaign> campaignPage;
        if (status != null && !status.isEmpty()) {
            campaignPage = campaignRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageRequest);
        } else if (type != null && !type.isEmpty()) {
            campaignPage = campaignRepository.findByTenantIdAndTypeAndDeletedFalse(tenantId, type, pageRequest);
        } else {
            campaignPage = campaignRepository.findByTenantIdAndDeletedFalse(tenantId, pageRequest);
        }

        List<CampaignResponse> content = campaignPage.getContent().stream()
                .map(this::toResponse).collect(Collectors.toList());

        return PagedResponse.<CampaignResponse>builder()
                .content(content)
                .pageNumber(campaignPage.getNumber())
                .pageSize(campaignPage.getSize())
                .totalElements(campaignPage.getTotalElements())
                .totalPages(campaignPage.getTotalPages())
                .last(campaignPage.isLast())
                .first(campaignPage.isFirst())
                .build();
    }

    @Transactional
    public void deleteCampaign(UUID campaignId, String userId) {
        String tenantId = TenantContext.getTenantId();
        Campaign campaign = campaignRepository.findByIdAndTenantIdAndDeletedFalse(campaignId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        campaign.setDeleted(true);
        campaignRepository.save(campaign);
        publishEvent("campaign.deleted", campaign.getId().toString(), tenantId, userId);
    }

    // ==================== CAMPAIGN MEMBERS ====================

    @Transactional
    public List<CampaignMemberResponse> addMembers(UUID campaignId, AddMembersRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        Campaign campaign = campaignRepository.findByIdAndTenantIdAndDeletedFalse(campaignId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));

        List<CampaignMember> newMembers = new ArrayList<>();
        int added = 0;

        for (UUID leadId : request.getLeadIds()) {
            // Skip duplicates
            Optional<CampaignMember> existing = memberRepository
                    .findByCampaignIdAndLeadIdAndTenantIdAndDeletedFalse(campaignId, leadId, tenantId);
            if (existing.isPresent()) continue;

            CampaignMember member = CampaignMember.builder()
                    .campaignId(campaignId)
                    .leadId(leadId)
                    .status(MemberStatus.SENT)
                    .addedAt(LocalDateTime.now())
                    .build();
            member.setTenantId(tenantId);
            member.setCreatedBy(userId);
            newMembers.add(member);
            added++;
        }

        List<CampaignMember> saved = memberRepository.saveAll(newMembers);

        // Update campaign stats
        campaign.setNumberSent(campaign.getNumberSent() + added);
        campaign.setLeadsGenerated(campaign.getLeadsGenerated() + added);
        campaignRepository.save(campaign);

        log.info("Added {} members to campaign {}", added, campaignId);
        return saved.stream().map(this::toMemberResponse).collect(Collectors.toList());
    }

    @Transactional
    public CampaignMemberResponse updateMemberStatus(UUID campaignId, UUID memberId, String newStatus, String userId) {
        String tenantId = TenantContext.getTenantId();
        CampaignMember member = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign member not found: " + memberId));

        if (!member.getCampaignId().equals(campaignId)) {
            throw new ResourceNotFoundException("Member does not belong to campaign: " + campaignId);
        }

        MemberStatus status = MemberStatus.valueOf(newStatus);
        member.setStatus(status);

        if (status == MemberStatus.RESPONDED && member.getRespondedAt() == null) {
            member.setRespondedAt(LocalDateTime.now());
        }
        if (status == MemberStatus.CONVERTED && member.getConvertedAt() == null) {
            member.setConvertedAt(LocalDateTime.now());
            // Update campaign conversion count
            Campaign campaign = campaignRepository.findByIdAndTenantIdAndDeletedFalse(campaignId, tenantId)
                    .orElse(null);
            if (campaign != null) {
                campaign.setConversions(campaign.getConversions() + 1);
                campaignRepository.save(campaign);
            }
        }

        member = memberRepository.save(member);
        return toMemberResponse(member);
    }

    public PagedResponse<CampaignMemberResponse> getMembers(UUID campaignId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "addedAt"));
        Page<CampaignMember> memberPage = memberRepository
                .findByCampaignIdAndTenantIdAndDeletedFalse(campaignId, tenantId, pageRequest);

        List<CampaignMemberResponse> content = memberPage.getContent().stream()
                .map(this::toMemberResponse).collect(Collectors.toList());

        return PagedResponse.<CampaignMemberResponse>builder()
                .content(content)
                .pageNumber(memberPage.getNumber())
                .pageSize(memberPage.getSize())
                .totalElements(memberPage.getTotalElements())
                .totalPages(memberPage.getTotalPages())
                .last(memberPage.isLast())
                .first(memberPage.isFirst())
                .build();
    }

    // ==================== ROI CALCULATION ====================

    public CampaignROIResponse calculateROI(UUID campaignId) {
        String tenantId = TenantContext.getTenantId();
        Campaign campaign = campaignRepository.findByIdAndTenantIdAndDeletedFalse(campaignId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));

        long totalMembers = memberRepository.countByCampaignIdAndTenantIdAndDeletedFalse(campaignId, tenantId);
        long sentCount = memberRepository.countByCampaignIdAndStatusAndTenantId(campaignId, "SENT", tenantId);
        long respondedCount = memberRepository.countByCampaignIdAndStatusAndTenantId(campaignId, "RESPONDED", tenantId);
        long convertedCount = memberRepository.countByCampaignIdAndStatusAndTenantId(campaignId, "CONVERTED", tenantId);

        BigDecimal actualCost = campaign.getActualCost() != null ? campaign.getActualCost() : BigDecimal.ZERO;
        BigDecimal wonRevenue = campaign.getWonRevenue() != null ? campaign.getWonRevenue() : BigDecimal.ZERO;

        // ROI = (Revenue - Cost) / Cost * 100
        BigDecimal roi = BigDecimal.ZERO;
        if (actualCost.compareTo(BigDecimal.ZERO) > 0) {
            roi = wonRevenue.subtract(actualCost)
                    .divide(actualCost, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal costPerLead = BigDecimal.ZERO;
        if (totalMembers > 0) {
            costPerLead = actualCost.divide(BigDecimal.valueOf(totalMembers), 2, RoundingMode.HALF_UP);
        }

        BigDecimal costPerConversion = BigDecimal.ZERO;
        if (convertedCount > 0) {
            costPerConversion = actualCost.divide(BigDecimal.valueOf(convertedCount), 2, RoundingMode.HALF_UP);
        }

        return CampaignROIResponse.builder()
                .campaignId(campaignId)
                .campaignName(campaign.getName())
                .totalBudget(campaign.getBudget())
                .actualCost(actualCost)
                .wonRevenue(wonRevenue)
                .roi(roi)
                .totalMembers(totalMembers)
                .sentCount(sentCount)
                .respondedCount(respondedCount)
                .convertedCount(convertedCount)
                .costPerLead(costPerLead)
                .costPerConversion(costPerConversion)
                .build();
    }

    // ==================== HELPERS ====================

    private CampaignResponse toResponse(Campaign c) {
        return CampaignResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType().name())
                .status(c.getStatus().name())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .budget(c.getBudget())
                .actualCost(c.getActualCost())
                .expectedRevenue(c.getExpectedRevenue())
                .wonRevenue(c.getWonRevenue())
                .description(c.getDescription())
                .numberSent(c.getNumberSent())
                .leadsGenerated(c.getLeadsGenerated())
                .conversions(c.getConversions())
                .tenantId(c.getTenantId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .createdBy(c.getCreatedBy())
                .build();
    }

    private CampaignMemberResponse toMemberResponse(CampaignMember m) {
        return CampaignMemberResponse.builder()
                .id(m.getId())
                .campaignId(m.getCampaignId())
                .leadId(m.getLeadId())
                .status(m.getStatus().name())
                .addedAt(m.getAddedAt())
                .respondedAt(m.getRespondedAt())
                .convertedAt(m.getConvertedAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private void publishEvent(String type, String entityId, String tenantId, String userId) {
        try {
            eventPublisher.publish("crm-events", tenantId, userId, "campaign", entityId, type,
                    Map.of("entityType", "campaign", "entityId", entityId));
        } catch (Exception e) {
            log.warn("Failed to publish event {}: {}", type, e.getMessage());
        }
    }
}
