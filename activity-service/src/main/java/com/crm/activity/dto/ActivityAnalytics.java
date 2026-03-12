package com.crm.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAnalytics {
    private long totalActivities;
    private long completedActivities;
    private long overdueActivities;
    private double completionRate;
    private double avgCompletionDays;
    private Map<String, Long> countByType;
    private Map<String, Long> countByStatus;
    private Map<String, Long> countByPriority;
    private Map<String, Long> countByAssignee;
}
