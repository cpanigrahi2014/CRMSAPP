package com.crm.opportunity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastSummary {
    private BigDecimal totalPipeline;
    private BigDecimal bestCase;
    private BigDecimal commit;
    private BigDecimal closed;
    private BigDecimal totalWeightedRevenue;
    private long totalOpenOpportunities;
    private long totalClosedWon;
    private long totalClosedLost;
    private List<StageSummary> stageBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageSummary {
        private String stage;
        private long count;
        private BigDecimal totalAmount;
    }
}
