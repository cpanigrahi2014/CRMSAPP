package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_configs")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String events; // JSON array

    @Builder.Default
    private boolean active = true;

    @Column(name = "secret_key", length = 500)
    private String secretKey;

    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 3;

    @Column(name = "retry_delay_ms")
    @Builder.Default
    private int retryDelayMs = 5000;

    @Column(name = "success_count")
    @Builder.Default
    private long successCount = 0;

    @Column(name = "failure_count")
    @Builder.Default
    private long failureCount = 0;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
