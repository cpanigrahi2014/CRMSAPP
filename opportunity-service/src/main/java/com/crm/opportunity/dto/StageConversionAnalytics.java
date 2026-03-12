package com.crm.opportunity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageConversionAnalytics {
    private Map<String, StageConversionRate> conversionRates; // fromStage -> rate info
    private Map<String, Double> avgTimeInStage;               // stage -> avg seconds
    private List<StageTransition> transitions;
    private double overallConversionRate;                      // prospecting -> closed_won %

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageConversionRate {
        private String fromStage;
        private String toStage;
        private long transitioned;
        private long total;
        private double conversionPct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageTransition {
        private String fromStage;
        private String toStage;
        private long count;
    }
}
