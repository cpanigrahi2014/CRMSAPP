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
public class PredictiveLeadScoreResponse {

    private UUID id;
    private String leadId;
    private String leadName;
    private String email;
    private String company;
    private int currentScore;
    private int predictedScore;
    private String trend;
    private BigDecimal conversionProbability;
    private List<ScoringFactor> topFactors;
    private LocalDateTime lastUpdated;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringFactor {
        private String factor;
        private int impact;
        private String direction;
    }
}
