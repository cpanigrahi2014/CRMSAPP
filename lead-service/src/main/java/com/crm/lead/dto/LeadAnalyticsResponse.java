package com.crm.lead.dto;

import lombok.*;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeadAnalyticsResponse {
    private long totalLeads;
    private long convertedLeads;
    private double conversionRate;
    private Double averageScore;
    private Map<String, Long> byStatus;
    private Map<String, Long> bySource;
    private long slaBreached;
}
