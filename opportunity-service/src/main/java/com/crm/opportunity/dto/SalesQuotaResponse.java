package com.crm.opportunity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesQuotaResponse {
    private String id;
    private String userId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal targetAmount;
    private Integer targetDeals;
    private BigDecimal actualAmount;
    private Integer actualDeals;
    private BigDecimal attainmentPct;
    private String createdAt;
    private String updatedAt;
}
