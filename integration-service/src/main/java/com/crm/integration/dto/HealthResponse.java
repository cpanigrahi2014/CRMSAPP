package com.crm.integration.dto;

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
public class HealthResponse {
    private UUID id;
    private UUID integrationId;
    private String integrationName;
    private String status;
    private BigDecimal uptime;
    private Integer avgResponseMs;
    private BigDecimal successRate;
    private long totalRequests;
    private LocalDateTime lastCheckedAt;
    private int alertsCount;
}
