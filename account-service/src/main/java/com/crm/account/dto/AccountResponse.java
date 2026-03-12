package com.crm.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private UUID id;
    private String name;
    private String industry;
    private String website;
    private String phone;
    private String billingAddress;
    private String shippingAddress;
    private BigDecimal annualRevenue;
    private Integer numberOfEmployees;
    private UUID parentAccountId;
    private String description;
    private String type;
    private String ownerId;
    private String territory;
    private String lifecycleStage;
    private Integer healthScore;
    private String segment;
    private Integer engagementScore;
    private List<AccountTagResponse> tags;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
