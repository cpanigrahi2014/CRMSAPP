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
public class PipelineDashboard {
    // KPI metrics
    private BigDecimal totalPipelineValue;
    private long totalOpenDeals;
    private long totalClosedWon;
    private long totalClosedLost;
    private BigDecimal totalRevenue;
    private BigDecimal avgDealSize;
    private BigDecimal winRate;
    private BigDecimal weightedPipeline;

    // Pipeline by stage
    private List<ForecastSummary.StageSummary> stageBreakdown;

    // Revenue by lead source
    private Map<String, BigDecimal> revenueByLeadSource;

    // Forecast categories
    private BigDecimal forecastPipeline;
    private BigDecimal forecastBestCase;
    private BigDecimal forecastCommit;
    private BigDecimal forecastClosed;

    // Alerts summary
    private long overdueDeals;
    private long closingSoonDeals;
    private long staleDeals;

    // Recent activity count
    private long activeReminders;
}
