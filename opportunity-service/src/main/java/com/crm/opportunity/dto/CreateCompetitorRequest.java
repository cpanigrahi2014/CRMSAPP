package com.crm.opportunity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompetitorRequest {

    @NotBlank(message = "Competitor name is required")
    private String competitorName;

    private String strengths;
    private String weaknesses;
    private String strategy;
    private String threatLevel;
}
