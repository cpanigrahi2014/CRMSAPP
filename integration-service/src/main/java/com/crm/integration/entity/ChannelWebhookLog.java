package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "channel_webhook_logs", indexes = {
        @Index(name = "idx_channel_log_tenant", columnList = "tenant_id"),
        @Index(name = "idx_channel_log_channel", columnList = "channel"),
        @Index(name = "idx_channel_log_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "channel", nullable = false, length = 50)
    private String channel;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "source_identifier", length = 500)
    private String sourceIdentifier;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "opportunity_id")
    private UUID opportunityId;

    @Column(name = "activity_id")
    private UUID activityId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
