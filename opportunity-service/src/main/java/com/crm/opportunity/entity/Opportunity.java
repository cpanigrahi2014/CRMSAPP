package com.crm.opportunity.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "opportunities", indexes = {
        @Index(name = "idx_opportunity_tenant", columnList = "tenant_id"),
        @Index(name = "idx_opportunity_stage", columnList = "stage"),
        @Index(name = "idx_opportunity_account", columnList = "account_id"),
        @Index(name = "idx_opportunity_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_opportunity_close_date", columnList = "close_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Opportunity extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    @Builder.Default
    private OpportunityStage stage = OpportunityStage.PROSPECTING;

    @Column(name = "probability")
    private Integer probability;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "forecast_category")
    @Builder.Default
    private ForecastCategory forecastCategory = ForecastCategory.PIPELINE;

    @Column(name = "lost_reason")
    private String lostReason;

    @Column(name = "won_date")
    private LocalDateTime wonDate;

    @Column(name = "lost_date")
    private LocalDateTime lostDate;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "next_step", length = 500)
    private String nextStep;

    @Column(name = "lead_source", length = 100)
    private String leadSource;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "predicted_close_date")
    private LocalDate predictedCloseDate;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "owner_id")
    private UUID ownerId;

    public enum OpportunityStage {
        PROSPECTING, QUALIFICATION, NEEDS_ANALYSIS, PROPOSAL, NEGOTIATION, CLOSED_WON, CLOSED_LOST
    }

    public enum ForecastCategory {
        PIPELINE, BEST_CASE, COMMIT, CLOSED
    }
}
