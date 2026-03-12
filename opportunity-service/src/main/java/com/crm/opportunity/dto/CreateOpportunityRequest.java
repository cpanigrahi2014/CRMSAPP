package com.crm.opportunity.dto;

import com.crm.opportunity.entity.Opportunity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOpportunityRequest {

    @NotBlank(message = "Opportunity name is required")
    private String name;

    private UUID accountId;

    private UUID contactId;

    @PositiveOrZero(message = "Amount must be zero or positive")
    private BigDecimal amount;

    private Opportunity.OpportunityStage stage;

    @Range(min = 0, max = 100, message = "Probability must be between 0 and 100")
    private Integer probability;

    private LocalDate closeDate;

    private String description;

    private UUID assignedTo;

    private Opportunity.ForecastCategory forecastCategory;

    private String currency;
    private String nextStep;
    private String leadSource;
    private UUID campaignId;
    private UUID ownerId;
}
