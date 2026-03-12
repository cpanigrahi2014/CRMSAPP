package com.crm.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentAnalysisResponse {
    private UUID id;
    private String sourceType;
    private String sourceId;
    private String overallSentiment; // POSITIVE, NEGATIVE, NEUTRAL, MIXED
    private double sentimentScore; // -1.0 to 1.0
    private double confidence;
    private String summary;
    private List<EmotionBreakdown> emotions;
    private List<String> keyPhrases;
    private List<String> concerns;
    private List<String> positiveIndicators;
    private String recommendation;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionBreakdown {
        private String emotion;
        private double score;
    }
}
