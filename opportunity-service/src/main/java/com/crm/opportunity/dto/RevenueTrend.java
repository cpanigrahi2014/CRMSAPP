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
public class RevenueTrend {
    private List<MonthlyData> monthly;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyData {
        private String month;          // "2026-01"
        private BigDecimal wonRevenue;
        private BigDecimal pipeline;
        private long dealsWon;
        private long dealsClosed;
        private long dealsCreated;
    }
}
