package com.crm.opportunity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WinLossAnalysis {
    private long totalClosedWon;
    private long totalClosedLost;
    private BigDecimal winRate;
    private BigDecimal avgWonDealSize;
    private BigDecimal avgLostDealSize;
    private BigDecimal totalWonRevenue;
    private BigDecimal totalLostRevenue;
    private Double avgDaysToClose;
    private Map<String, Long> lostReasonBreakdown;
    private Map<String, Long> wonByStageEntry;
}
