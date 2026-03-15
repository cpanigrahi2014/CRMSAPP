package com.crm.supportcase.dto;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingAnalytics {
    private long totalWorkItems;
    private long queuedItems;
    private long assignedItems;
    private long completedItems;
    private long timedOutItems;
    private Double avgWaitTimeSeconds;
    private Double avgHandleTimeSeconds;
    private long onlineAgents;
    private long busyAgents;
    private long totalAgents;
    private double avgAgentUtilization;
    private Map<String, Long> itemsByQueue;
    private Map<String, Long> itemsByChannel;
    private Map<String, Long> itemsByStatus;
}
