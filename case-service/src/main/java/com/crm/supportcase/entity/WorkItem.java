package com.crm.supportcase.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_items", indexes = {
        @Index(name = "idx_wi_tenant", columnList = "tenant_id"),
        @Index(name = "idx_wi_queue", columnList = "queue_id"),
        @Index(name = "idx_wi_agent", columnList = "assigned_agent_id"),
        @Index(name = "idx_wi_status", columnList = "status"),
        @Index(name = "idx_wi_entity", columnList = "entity_type, entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkItem extends BaseEntity {

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "queue_id")
    private UUID queueId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WorkItemStatus status = WorkItemStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private SupportCase.CasePriority priority = SupportCase.CasePriority.MEDIUM;

    @Column(name = "channel", length = 20)
    private String channel;

    @Column(name = "subject")
    private String subject;

    @Column(name = "queued_at")
    private LocalDateTime queuedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "declined_count", nullable = false)
    @Builder.Default
    private int declinedCount = 0;

    @Column(name = "wait_time_seconds")
    private Long waitTimeSeconds;

    @Column(name = "handle_time_seconds")
    private Long handleTimeSeconds;

    public enum WorkItemStatus {
        QUEUED, ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, DECLINED, TIMED_OUT
    }
}
