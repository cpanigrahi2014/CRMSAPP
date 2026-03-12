package com.crm.activity.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activities", indexes = {
        @Index(name = "idx_activity_tenant", columnList = "tenant_id"),
        @Index(name = "idx_activity_type", columnList = "type"),
        @Index(name = "idx_activity_status", columnList = "status"),
        @Index(name = "idx_activity_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_activity_related_entity", columnList = "related_entity_id"),
        @Index(name = "idx_activity_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ActivityType type;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ActivityStatus status = ActivityStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private ActivityPriority priority = ActivityPriority.MEDIUM;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /* ---- Reminder fields ---- */
    @Column(name = "reminder_at")
    private LocalDateTime reminderAt;

    @Column(name = "reminder_sent")
    @Builder.Default
    private boolean reminderSent = false;

    /* ---- Recurring task fields ---- */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_rule")
    private RecurrenceRule recurrenceRule;

    @Column(name = "recurrence_end")
    private LocalDate recurrenceEnd;

    @Column(name = "parent_activity_id")
    private UUID parentActivityId;

    /* ---- Meeting/Call/Email fields ---- */
    @Column(name = "location")
    private String location;

    @Column(name = "call_duration_minutes")
    private Integer callDurationMinutes;

    @Column(name = "call_outcome")
    private String callOutcome;

    @Column(name = "email_to")
    private String emailTo;

    @Column(name = "email_cc")
    private String emailCc;

    public enum ActivityType {
        TASK, CALL, MEETING, EMAIL
    }

    public enum ActivityStatus {
        NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    public enum ActivityPriority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum RecurrenceRule {
        DAILY, WEEKLY, BIWEEKLY, MONTHLY
    }
}
