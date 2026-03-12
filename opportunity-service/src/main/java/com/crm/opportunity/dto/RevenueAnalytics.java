package com.crm.opportunity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalytics {
    private long totalOpportunities;
    private long openOpportunities;
    private long closedWon;
    private long closedLost;
    private BigDecimal totalRevenue;
    private BigDecimal totalPipeline;
    private BigDecimal avgDealSize;
    private BigDecimal winRate;
    private BigDecimal totalWeightedPipeline;
    private Map<String, BigDecimal> revenueByStage;
    private Map<String, Long> countByStage;
    private Map<String, BigDecimal> revenueByLeadSource;
    private List<ForecastSummary.StageSummary> stageBreakdown;
}
