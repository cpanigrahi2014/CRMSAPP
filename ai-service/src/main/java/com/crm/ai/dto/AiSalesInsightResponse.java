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
public class AiSalesInsightResponse {
    private UUID id;
    private String insightType;
    private String title;
    private String summary;
    private String details;
    private String impactArea;
    private String severity;
    private boolean actionable;
    private List<String> relatedEntities;
    private LocalDateTime generatedAt;
}
