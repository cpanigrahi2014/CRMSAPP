package com.crm.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChurnPredictionResponse {
    private UUID id;
    private String accountId;
    private String accountName;
    private String industry;
    private BigDecimal annualRevenue;
    private String riskLevel;
    private BigDecimal churnProbability;
    private List<String> riskFactors;
    private int lastActivityDays;
    private int healthScore;
    private List<String> recommendedActions;
    private LocalDateTime predictedChurnDate;
    private LocalDateTime createdAt;
}
