package com.crm.supportcase.dto;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAnalytics {
    private long totalCases;
    private long openCases;
    private long resolvedCases;
    private long escalatedCases;
    private double slaComplianceRate;
    private long slaMetCount;
    private long slaBreachedCount;
    private Double avgResolutionHours;
    private Double avgCsatScore;
    private Map<String, Long> countByStatus;
    private Map<String, Long> countByPriority;
}
