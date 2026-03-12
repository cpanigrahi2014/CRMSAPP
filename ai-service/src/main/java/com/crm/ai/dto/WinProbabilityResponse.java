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
public class WinProbabilityResponse {
    private UUID id;
    private String opportunityId;
    private String opportunityName;
    private String accountName;
    private BigDecimal amount;
    private String stage;
    private int winProbability;
    private int historicalWinRate;
    private int daysInStage;
    private List<String> riskFactors;
    private List<String> positiveSignals;
    private String recommendation;
    private LocalDateTime createdAt;
}
