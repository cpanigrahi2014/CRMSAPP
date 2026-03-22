package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_sync_configs", indexes = {
        @Index(name = "idx_cal_sync_tenant", columnList = "tenant_id"),
        @Index(name = "idx_cal_sync_user", columnList = "tenant_id, user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarSyncConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider; // GOOGLE, OUTLOOK, APPLE

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DISCONNECTED"; // DISCONNECTED, CONNECTED, SYNCING, ERROR

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "calendar_id", length = 255)
    private String calendarId; // e.g., "primary" for Google

    @Column(name = "sync_direction", length = 20)
    @Builder.Default
    private String syncDirection = "BIDIRECTIONAL"; // TO_CALENDAR, FROM_CALENDAR, BIDIRECTIONAL

    @Column(name = "sync_interval_minutes")
    @Builder.Default
    private Integer syncIntervalMinutes = 15;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "last_sync_status", length = 50)
    private String lastSyncStatus;

    @Column(name = "events_synced")
    @Builder.Default
    private Integer eventsSynced = 0;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
