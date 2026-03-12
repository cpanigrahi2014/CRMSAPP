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
public class SalesQuotaRequest {
    private String userId;
    private String periodType;        // MONTHLY, QUARTERLY, ANNUAL
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal targetAmount;
    private Integer targetDeals;
}
