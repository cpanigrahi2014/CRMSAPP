package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "data_syncs")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DataSync {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_id", nullable = false)
    private ThirdPartyIntegration integration;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false, length = 20)
    private String direction; // INBOUND, OUTBOUND, BIDIRECTIONAL

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "IDLE";

    @Column(length = 100)
    private String schedule;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_run_duration")
    private Long lastRunDuration;

    @Column(name = "records_synced")
    @Builder.Default
    private long recordsSynced = 0;

    @Column(name = "records_failed")
    @Builder.Default
    private long recordsFailed = 0;

    @Column(name = "field_mapping", columnDefinition = "TEXT")
    private String fieldMapping; // JSON

    @Builder.Default
    private boolean enabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
