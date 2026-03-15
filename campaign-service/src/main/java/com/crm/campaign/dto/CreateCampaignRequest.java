package com.crm.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateCampaignRequest {
    @NotBlank(message = "Campaign name is required")
    private String name;

    private String type;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private BigDecimal actualCost;
    private BigDecimal expectedRevenue;
    private String description;
}
