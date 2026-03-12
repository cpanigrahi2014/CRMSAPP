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
public class CompetitorResponse {
    private UUID id;
    private UUID opportunityId;
    private String competitorName;
    private String strengths;
    private String weaknesses;
    private String strategy;
    private String threatLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
