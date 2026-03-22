package com.crm.opportunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStageRequest {

    @NotBlank(message = "Stage name is required")
    @Size(max = 50)
    private String name;

    @NotBlank(message = "Display name is required")
    @Size(max = 100)
    private String displayName;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    @Size(max = 7)
    private String color;

    private Integer defaultProbability;

    private String forecastCategory;

    @Builder.Default
    private boolean closedWon = false;

    @Builder.Default
    private boolean closedLost = false;

    @Builder.Default
    private boolean active = true;
}
