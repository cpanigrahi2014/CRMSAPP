package com.crm.ai.service;

import com.crm.ai.dto.*;
import com.crm.ai.entity.*;
import com.crm.ai.repository.*;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightsService {

    private final WinProbabilityRepository winProbabilityRepository;
    private final SalesForecastRepository salesForecastRepository;
    private final ChurnPredictionRepository churnPredictionRepository;
    private final AiReportInsightRepository aiReportInsightRepository;
    private final DataEntrySuggestionRepository dataEntrySuggestionRepository;
    private final AiSalesInsightRepository aiSalesInsightRepository;
    private final LeadScoreRepository leadScoreRepository;
    private final ObjectMapper objectMapper;

    // ---- Lead Scores ----

    @Transactional(readOnly = true)
    public List<PredictiveLeadScoreResponse> getLeadScores() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching lead scores for tenant: {}", tenantId);
        return leadScoreRepository.findByTenantIdOrderByPredictedScoreDesc(tenantId)
                .stream().map(this::toPredictiveLeadScoreResponse).toList();
    }

    // ---- Win Probability ----

    @Transactional(readOnly = true)
    public List<WinProbabilityResponse> getWinProbabilities() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching win probabilities for tenant: {}", tenantId);
        return winProbabilityRepository.findByTenantIdOrderByWinProbabilityDesc(tenantId)
                .stream().map(this::toWinProbabilityResponse).toList();
    }

    @Transactional(readOnly = true)
    public WinProbabilityResponse getWinProbabilityByOpportunity(String opportunityId) {
        String tenantId = TenantContext.getTenantId();
        return winProbabilityRepository.findByOpportunityIdAndTenantId(opportunityId, tenantId)
                .map(this::toWinProbabilityResponse)
                .orElse(null);
    }

    // ---- Sales Forecasts ----

    @Transactional(readOnly = true)
    public List<SalesForecastResponse> getSalesForecasts() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching sales forecasts for tenant: {}", tenantId);
        return salesForecastRepository.findByTenantIdOrderByPeriodAsc(tenantId)
                .stream().map(this::toSalesForecastResponse).toList();
    }

    // ---- Churn Predictions ----

    @Transactional(readOnly = true)
    public List<ChurnPredictionResponse> getChurnPredictions() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching churn predictions for tenant: {}", tenantId);
        return churnPredictionRepository.findByTenantIdOrderByChurnProbabilityDesc(tenantId)
                .stream().map(this::toChurnPredictionResponse).toList();
    }

    @Transactional(readOnly = true)
    public ChurnPredictionResponse getChurnPredictionByAccount(String accountId) {
        String tenantId = TenantContext.getTenantId();
        return churnPredictionRepository.findByAccountIdAndTenantId(accountId, tenantId)
                .map(this::toChurnPredictionResponse)
                .orElse(null);
    }

    // ---- AI Report Insights ----

    @Transactional(readOnly = true)
    public List<AiReportInsightResponse> getReportInsights() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching AI report insights for tenant: {}", tenantId);
        return aiReportInsightRepository.findByTenantIdOrderByGeneratedAtDesc(tenantId)
                .stream().map(this::toAiReportInsightResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AiReportInsightResponse> getReportInsightsByType(String insightType) {
        String tenantId = TenantContext.getTenantId();
        return aiReportInsightRepository.findByInsightTypeAndTenantId(insightType, tenantId)
                .stream().map(this::toAiReportInsightResponse).toList();
    }

    // ---- Data Entry Suggestions ----

    @Transactional(readOnly = true)
    public List<DataEntrySuggestionResponse> getPendingSuggestions() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching pending data suggestions for tenant: {}", tenantId);
        return dataEntrySuggestionRepository.findByTenantIdAndAcceptedIsNullOrderByConfidenceDesc(tenantId)
                .stream().map(this::toDataEntrySuggestionResponse).toList();
    }

    @Transactional
    public DataEntrySuggestionResponse actionSuggestion(SuggestionActionRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Actioning suggestion: {} accepted={}", request.getSuggestionId(), request.getAccepted());
        DataEntrySuggestionRecord record = dataEntrySuggestionRepository
                .findByIdAndTenantId(request.getSuggestionId(), tenantId)
                .orElseThrow(() -> new com.crm.common.exception.ResourceNotFoundException(
                        "Data suggestion not found: " + request.getSuggestionId()));
        record.setAccepted(request.getAccepted());
        return toDataEntrySuggestionResponse(dataEntrySuggestionRepository.save(record));
    }

    // ---- AI Sales Insights ----

    @Transactional(readOnly = true)
    public List<AiSalesInsightResponse> getSalesInsights() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching AI sales insights for tenant: {}", tenantId);
        return aiSalesInsightRepository.findByTenantIdOrderByGeneratedAtDesc(tenantId)
                .stream().map(this::toAiSalesInsightResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AiSalesInsightResponse> getSalesInsightsByType(String insightType) {
        String tenantId = TenantContext.getTenantId();
        return aiSalesInsightRepository.findByInsightTypeAndTenantId(insightType, tenantId)
                .stream().map(this::toAiSalesInsightResponse).toList();
    }

    // ---- Mappers ----

    private WinProbabilityResponse toWinProbabilityResponse(WinProbabilityRecord r) {
        return WinProbabilityResponse.builder()
                .id(r.getId())
                .opportunityId(r.getOpportunityId())
                .opportunityName(r.getOpportunityName())
                .accountName(r.getAccountName())
                .amount(r.getAmount())
                .stage(r.getStage())
                .winProbability(r.getWinProbability())
                .historicalWinRate(r.getHistoricalWinRate())
                .daysInStage(r.getDaysInStage())
                .riskFactors(parseJsonList(r.getRiskFactors()))
                .positiveSignals(parseJsonList(r.getPositiveSignals()))
                .recommendation(r.getRecommendation())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private SalesForecastResponse toSalesForecastResponse(SalesForecastRecord r) {
        return SalesForecastResponse.builder()
                .id(r.getId())
                .period(r.getPeriod())
                .periodLabel(r.getPeriodLabel())
                .predictedRevenue(r.getPredictedRevenue())
                .bestCase(r.getBestCase())
                .worstCase(r.getWorstCase())
                .confidence(r.getConfidence())
                .pipelineValue(r.getPipelineValue())
                .weightedPipeline(r.getWeightedPipeline())
                .closedToDate(r.getClosedToDate())
                .quota(r.getQuota())
                .attainmentPct(r.getAttainmentPct())
                .factors(parseJsonList(r.getFactors()))
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ChurnPredictionResponse toChurnPredictionResponse(ChurnPredictionRecord r) {
        return ChurnPredictionResponse.builder()
                .id(r.getId())
                .accountId(r.getAccountId())
                .accountName(r.getAccountName())
                .industry(r.getIndustry())
                .annualRevenue(r.getAnnualRevenue())
                .riskLevel(r.getRiskLevel())
                .churnProbability(r.getChurnProbability())
                .riskFactors(parseJsonList(r.getRiskFactors()))
                .lastActivityDays(r.getLastActivityDays())
                .healthScore(r.getHealthScore())
                .recommendedActions(parseJsonList(r.getRecommendedActions()))
                .predictedChurnDate(r.getPredictedChurnDate())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private AiReportInsightResponse toAiReportInsightResponse(AiReportInsightRecord r) {
        return AiReportInsightResponse.builder()
                .id(r.getId())
                .reportName(r.getReportName())
                .insightType(r.getInsightType())
                .title(r.getTitle())
                .description(r.getDescription())
                .metric(r.getMetric())
                .currentValue(r.getCurrentValue())
                .previousValue(r.getPreviousValue())
                .changePct(r.getChangePct())
                .recommendation(r.getRecommendation())
                .generatedAt(r.getGeneratedAt())
                .build();
    }

    private DataEntrySuggestionResponse toDataEntrySuggestionResponse(DataEntrySuggestionRecord r) {
        return DataEntrySuggestionResponse.builder()
                .id(r.getId())
                .entityType(r.getEntityType())
                .entityId(r.getEntityId())
                .entityName(r.getEntityName())
                .field(r.getField())
                .currentValue(r.getCurrentValue())
                .suggestedValue(r.getSuggestedValue())
                .confidence(r.getConfidence())
                .source(r.getSource())
                .accepted(r.getAccepted())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private AiSalesInsightResponse toAiSalesInsightResponse(AiSalesInsightRecord r) {
        return AiSalesInsightResponse.builder()
                .id(r.getId())
                .insightType(r.getInsightType())
                .title(r.getTitle())
                .summary(r.getSummary())
                .details(r.getDetails())
                .impactArea(r.getImpactArea())
                .severity(r.getSeverity())
                .actionable(r.isActionable())
                .relatedEntities(parseJsonList(r.getRelatedEntities()))
                .generatedAt(r.getGeneratedAt())
                .build();
    }

    private PredictiveLeadScoreResponse toPredictiveLeadScoreResponse(LeadScoreRecord r) {
        List<PredictiveLeadScoreResponse.ScoringFactor> factors = Collections.emptyList();
        if (r.getTopFactors() != null && !r.getTopFactors().isBlank()) {
            try {
                factors = objectMapper.readValue(r.getTopFactors(),
                        new TypeReference<List<PredictiveLeadScoreResponse.ScoringFactor>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse scoring factors: {}", e.getMessage());
            }
        }
        return PredictiveLeadScoreResponse.builder()
                .id(r.getId())
                .leadId(r.getLeadId())
                .leadName(r.getLeadName())
                .email(r.getEmail())
                .company(r.getCompany())
                .currentScore(r.getCurrentScore())
                .predictedScore(r.getPredictedScore())
                .trend(r.getTrend())
                .conversionProbability(r.getConversionProbability())
                .topFactors(factors)
                .lastUpdated(r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt())
                .build();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
