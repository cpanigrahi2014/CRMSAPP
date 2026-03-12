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
public class SalesForecastResponse {
    private UUID id;
    private String period;
    private String periodLabel;
    private BigDecimal predictedRevenue;
    private BigDecimal bestCase;
    private BigDecimal worstCase;
    private String confidence;
    private BigDecimal pipelineValue;
    private BigDecimal weightedPipeline;
    private BigDecimal closedToDate;
    private BigDecimal quota;
    private int attainmentPct;
    private List<String> factors;
    private LocalDateTime createdAt;
}
