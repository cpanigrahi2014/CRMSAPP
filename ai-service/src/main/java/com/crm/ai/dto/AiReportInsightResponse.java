package com.crm.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReportInsightResponse {
    private UUID id;
    private String reportName;
    private String insightType;
    private String title;
    private String description;
    private String metric;
    private BigDecimal currentValue;
    private BigDecimal previousValue;
    private BigDecimal changePct;
    private String recommendation;
    private LocalDateTime generatedAt;
}
