package com.crm.opportunity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStageResponse {
    private UUID id;
    private String name;
    private String displayName;
    private Integer displayOrder;
    private String color;
    private Integer defaultProbability;
    private String forecastCategory;
    private boolean closedWon;
    private boolean closedLost;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
