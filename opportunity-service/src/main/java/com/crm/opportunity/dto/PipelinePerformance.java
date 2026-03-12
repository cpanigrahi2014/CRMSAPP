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
public class PipelinePerformance {
    // Velocity metrics
    private BigDecimal pipelineVelocity;     // (deals × avg_value × win_rate) / avg_cycle_days
    private BigDecimal avgDealSize;
    private BigDecimal winRate;
    private double avgCycleDays;
    private long totalDeals;

    // Rep performance
    private List<RepPerformance> repPerformances;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepPerformance {
        private String userId;
        private long totalDeals;
        private long wonDeals;
        private long lostDeals;
        private long openDeals;
        private BigDecimal totalRevenue;
        private BigDecimal avgDealSize;
        private BigDecimal winRate;
        private BigDecimal quotaAttainment;
    }
}
