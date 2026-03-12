package com.crm.opportunity.service;

import com.crm.opportunity.dto.SalesQuotaRequest;
import com.crm.opportunity.dto.SalesQuotaResponse;
import com.crm.opportunity.entity.SalesQuota;
import com.crm.opportunity.mapper.OpportunityMapper;
import com.crm.opportunity.repository.OpportunityRepository;
import com.crm.opportunity.repository.SalesQuotaRepository;
import com.crm.opportunity.entity.Opportunity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesQuotaService {

    private final SalesQuotaRepository quotaRepository;
    private final OpportunityRepository opportunityRepository;
    private final OpportunityMapper mapper;

    @Transactional
    public SalesQuotaResponse createQuota(SalesQuotaRequest req, String tenantId) {
        SalesQuota quota = SalesQuota.builder()
                .userId(req.getUserId())
                .periodType(SalesQuota.PeriodType.valueOf(req.getPeriodType()))
                .periodStart(req.getPeriodStart())
                .periodEnd(req.getPeriodEnd())
                .targetAmount(req.getTargetAmount() != null ? req.getTargetAmount() : BigDecimal.ZERO)
                .targetDeals(req.getTargetDeals() != null ? req.getTargetDeals() : 0)
                .actualAmount(BigDecimal.ZERO)
                .actualDeals(0)
                .attainmentPct(BigDecimal.ZERO)
                .build();
        quota.setTenantId(tenantId);
        quota.setDeleted(false);
        SalesQuota saved = quotaRepository.save(quota);
        return mapper.toQuotaResponse(saved);
    }

    @Transactional
    public SalesQuotaResponse updateQuota(UUID id, SalesQuotaRequest req, String tenantId) {
        SalesQuota quota = quotaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Quota not found: " + id));
        if (req.getTargetAmount() != null) quota.setTargetAmount(req.getTargetAmount());
        if (req.getTargetDeals() != null) quota.setTargetDeals(req.getTargetDeals());
        if (req.getPeriodType() != null) quota.setPeriodType(SalesQuota.PeriodType.valueOf(req.getPeriodType()));
        if (req.getPeriodStart() != null) quota.setPeriodStart(req.getPeriodStart());
        if (req.getPeriodEnd() != null) quota.setPeriodEnd(req.getPeriodEnd());
        if (req.getUserId() != null) quota.setUserId(req.getUserId());
        recalculateAttainment(quota, tenantId);
        return mapper.toQuotaResponse(quotaRepository.save(quota));
    }

    public SalesQuotaResponse getQuota(UUID id, String tenantId) {
        SalesQuota quota = quotaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Quota not found: " + id));
        return mapper.toQuotaResponse(quota);
    }

    public List<SalesQuotaResponse> getAllQuotas(String tenantId) {
        return quotaRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .map(mapper::toQuotaResponse)
                .collect(Collectors.toList());
    }

    public List<SalesQuotaResponse> getQuotasByUser(String userId, String tenantId) {
        return quotaRepository.findByTenantIdAndUserIdAndDeletedFalse(tenantId, userId).stream()
                .map(mapper::toQuotaResponse)
                .collect(Collectors.toList());
    }

    public List<SalesQuotaResponse> getActiveQuotas(String tenantId) {
        return quotaRepository.findActiveQuotas(tenantId, LocalDate.now()).stream()
                .map(mapper::toQuotaResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteQuota(UUID id, String tenantId) {
        SalesQuota quota = quotaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Quota not found: " + id));
        quota.setDeleted(true);
        quotaRepository.save(quota);
    }

    /**
     * Recalculate all active quotas by querying won opportunities in each quota's period.
     */
    @Transactional
    public void recalculateAllQuotas(String tenantId) {
        List<SalesQuota> activeQuotas = quotaRepository.findActiveQuotas(tenantId, LocalDate.now());
        for (SalesQuota quota : activeQuotas) {
            recalculateAttainment(quota, tenantId);
            quotaRepository.save(quota);
        }
    }

    /**
     * Refresh actuals for a given quota from the won-opportunity data.
     */
    private void recalculateAttainment(SalesQuota q, String tenantId) {
        List<Opportunity> won = opportunityRepository.findByTenantIdAndCloseDateBetweenAndDeletedFalse(
                tenantId, q.getPeriodStart(), q.getPeriodEnd());
        // Filter to CLOSED_WON assigned to the quota user
        List<Opportunity> userWon = won.stream()
                .filter(o -> o.getStage() == Opportunity.OpportunityStage.CLOSED_WON)
                .filter(o -> o.getAssignedTo() != null && o.getAssignedTo().toString().equals(q.getUserId()))
                .collect(Collectors.toList());

        BigDecimal actualAmount = userWon.stream()
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int actualDeals = userWon.size();

        q.setActualAmount(actualAmount);
        q.setActualDeals(actualDeals);
        if (q.getTargetAmount() != null && q.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            q.setAttainmentPct(actualAmount.multiply(BigDecimal.valueOf(100))
                    .divide(q.getTargetAmount(), 2, RoundingMode.HALF_UP));
        } else {
            q.setAttainmentPct(BigDecimal.ZERO);
        }
    }
}
