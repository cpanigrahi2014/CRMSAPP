package com.crm.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 100)
    private String tenantId;

    @Column(name = "plan_name", nullable = false, length = 30)
    @Builder.Default
    private String planName = "FREE";

    @Column(name = "max_users", nullable = false)
    @Builder.Default
    private int maxUsers = 3;

    @Column(name = "max_custom_objects", nullable = false)
    @Builder.Default
    private int maxCustomObjects = 2;

    @Column(name = "max_workflows", nullable = false)
    @Builder.Default
    private int maxWorkflows = 3;

    @Column(name = "max_dashboards", nullable = false)
    @Builder.Default
    private int maxDashboards = 1;

    @Column(name = "max_pipelines", nullable = false)
    @Builder.Default
    private int maxPipelines = 1;

    @Column(name = "max_roles", nullable = false)
    @Builder.Default
    private int maxRoles = 2;

    @Column(name = "max_records_per_object", nullable = false)
    @Builder.Default
    private int maxRecordsPerObject = 100;

    @Column(name = "ai_config_enabled", nullable = false)
    @Builder.Default
    private boolean aiConfigEnabled = true;

    @Column(name = "ai_insights_enabled", nullable = false)
    @Builder.Default
    private boolean aiInsightsEnabled = false;

    @Column(name = "email_tracking_enabled", nullable = false)
    @Builder.Default
    private boolean emailTrackingEnabled = false;

    @Column(name = "api_access_enabled", nullable = false)
    @Builder.Default
    private boolean apiAccessEnabled = false;

    @Column(name = "integrations_enabled", nullable = false)
    @Builder.Default
    private boolean integrationsEnabled = false;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
