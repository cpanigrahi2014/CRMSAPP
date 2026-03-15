package com.crm.campaign.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateCampaignRequest {
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
    private Integer numberSent;
    private Integer leadsGenerated;
    private Integer conversions;
}
