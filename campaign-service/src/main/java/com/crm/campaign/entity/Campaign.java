package com.crm.campaign.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "campaigns", indexes = {
        @Index(name = "idx_campaign_tenant", columnList = "tenant_id"),
        @Index(name = "idx_campaign_status", columnList = "status"),
        @Index(name = "idx_campaign_type", columnList = "type")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Campaign extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private CampaignType type = CampaignType.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "budget", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal budget = BigDecimal.ZERO;

    @Column(name = "actual_cost", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal actualCost = BigDecimal.ZERO;

    @Column(name = "expected_revenue", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal expectedRevenue = BigDecimal.ZERO;

    @Column(name = "won_revenue", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal wonRevenue = BigDecimal.ZERO;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "number_sent")
    @Builder.Default
    private int numberSent = 0;

    @Column(name = "leads_generated")
    @Builder.Default
    private int leadsGenerated = 0;

    @Column(name = "conversions")
    @Builder.Default
    private int conversions = 0;

    public enum CampaignStatus {
        DRAFT, PLANNED, ACTIVE, PAUSED, COMPLETED, ABORTED
    }

    public enum CampaignType {
        EMAIL, SOCIAL, EVENT, WEBINAR, CONTENT, PAID_ADS
    }
}
