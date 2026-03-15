package com.crm.supportcase.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_presence", indexes = {
        @Index(name = "idx_ap_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ap_user", columnList = "user_id"),
        @Index(name = "idx_ap_status", columnList = "status"),
        @Index(name = "idx_ap_queue", columnList = "queue_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentPresence extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "agent_name")
    private String agentName;

    @Column(name = "agent_email")
    private String agentEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PresenceStatus status = PresenceStatus.OFFLINE;

    @Column(name = "queue_id")
    private UUID queueId;

    @Column(name = "capacity", nullable = false)
    @Builder.Default
    private int capacity = 5;

    @Column(name = "active_work_count", nullable = false)
    @Builder.Default
    private int activeWorkCount = 0;

    @Column(name = "last_routed_at")
    private LocalDateTime lastRoutedAt;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "auto_accept", nullable = false)
    @Builder.Default
    private boolean autoAccept = false;

    public enum PresenceStatus {
        ONLINE, BUSY, AWAY, OFFLINE
    }

    public boolean isAvailable() {
        return status == PresenceStatus.ONLINE && activeWorkCount < capacity;
    }

    public double getUtilization() {
        return capacity > 0 ? (double) activeWorkCount / capacity : 1.0;
    }
}
