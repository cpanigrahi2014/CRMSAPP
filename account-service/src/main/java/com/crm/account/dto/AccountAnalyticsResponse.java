package com.crm.account.dto;

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
public class AccountAnalyticsResponse {
    private long totalAccounts;
    private long activeAccounts;
    private long newAccounts;
    private long churnedAccounts;
    private BigDecimal totalRevenue;
    private BigDecimal averageRevenue;
    private double averageHealthScore;
    private double averageEngagementScore;
    private Map<String, Long> byType;
    private Map<String, Long> byIndustry;
    private Map<String, Long> byLifecycleStage;
    private Map<String, Long> byTerritory;
    private Map<String, Long> bySegment;
}
