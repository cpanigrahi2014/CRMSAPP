package com.crm.opportunity.dto;

import com.crm.opportunity.entity.Opportunity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import com.crm.opportunity.entity.Opportunity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityResponse {
    private UUID id;
    private String name;
    private UUID accountId;
    private UUID contactId;
    private BigDecimal amount;
    private Opportunity.OpportunityStage stage;
    private Integer probability;
    private LocalDate closeDate;
    private String description;
    private UUID assignedTo;
    private Opportunity.ForecastCategory forecastCategory;
    private String lostReason;
    private LocalDateTime wonDate;
    private LocalDateTime lostDate;
    private String currency;
    private String nextStep;
    private String leadSource;
    private UUID campaignId;
    private LocalDate predictedCloseDate;
    private Integer confidenceScore;
    private UUID ownerId;
    private BigDecimal expectedRevenue;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
