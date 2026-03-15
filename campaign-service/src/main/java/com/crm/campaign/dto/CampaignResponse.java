package com.crm.campaign.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class CampaignResponse {
    private UUID id;
    private String name;
    private String type;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private BigDecimal actualCost;
    private BigDecimal expectedRevenue;
    private BigDecimal wonRevenue;
    private String description;
    private int numberSent;
    private int leadsGenerated;
    private int conversions;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
