package com.crm.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Account name is required")
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
    private String segment;
}
