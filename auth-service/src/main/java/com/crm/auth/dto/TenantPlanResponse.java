package com.crm.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantPlanResponse {
    private String tenantId;
    private String planName;
    private int maxUsers;
    private int maxCustomObjects;
    private int maxWorkflows;
    private int maxDashboards;
    private int maxPipelines;
    private int maxRoles;
    private int maxRecordsPerObject;
    private boolean aiConfigEnabled;
    private boolean aiInsightsEnabled;
    private boolean emailTrackingEnabled;
    private boolean apiAccessEnabled;
    private boolean integrationsEnabled;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private boolean active;
}
