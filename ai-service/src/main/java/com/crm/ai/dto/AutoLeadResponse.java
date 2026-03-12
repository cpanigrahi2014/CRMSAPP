package com.crm.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoLeadResponse {

    private UUID id;
    private String sourceType;
    private String sourceReference;
    private String leadName;
    private String email;
    private String company;
    private String title;
    private String phone;
    private String notes;
    private BigDecimal confidence;
    private String status;
    private LocalDateTime createdAt;
}
