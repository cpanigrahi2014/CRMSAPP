package com.crm.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStatusResponse {

    private int completedSteps;
    private int totalSteps;
    private int progressPercent;
    private List<OnboardingStep> steps;
    private String nextRecommendation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingStep {
        private String id;
        private String title;
        private String description;
        private String category;
        private boolean completed;
        private String actionUrl;
        private String aiHint;
    }
}
