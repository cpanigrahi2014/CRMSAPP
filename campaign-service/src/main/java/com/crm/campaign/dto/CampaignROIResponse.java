package com.crm.campaign.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder
public class CampaignROIResponse {
    private UUID campaignId;
    private String campaignName;
    private BigDecimal totalBudget;
    private BigDecimal actualCost;
    private BigDecimal wonRevenue;
    private BigDecimal roi;
    private long totalMembers;
    private long sentCount;
    private long respondedCount;
    private long convertedCount;
    private BigDecimal costPerLead;
    private BigDecimal costPerConversion;
}
