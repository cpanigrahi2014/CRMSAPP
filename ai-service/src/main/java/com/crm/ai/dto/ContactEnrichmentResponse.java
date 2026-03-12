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
public class ContactEnrichmentResponse {

    private String contactId;
    private List<EnrichedField> enrichedFields;
    private double overallConfidence;
    private String enrichmentSource;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrichedField {
        private String field;
        private String currentValue;
        private String suggestedValue;
        private double confidence;
        private String source;
    }
}
