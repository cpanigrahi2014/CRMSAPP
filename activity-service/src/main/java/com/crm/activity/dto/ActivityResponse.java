package com.crm.activity.dto;

import com.crm.activity.entity.Activity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    private UUID id;
    private Activity.ActivityType type;
    private String subject;
    private String description;
    private Activity.ActivityStatus status;
    private Activity.ActivityPriority priority;
    private LocalDateTime dueDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private UUID assignedTo;
    private LocalDateTime completedAt;

    /* ---- Reminder ---- */
    private LocalDateTime reminderAt;
    private boolean reminderSent;

    /* ---- Recurrence ---- */
    private Activity.RecurrenceRule recurrenceRule;
    private LocalDate recurrenceEnd;
    private UUID parentActivityId;

    /* ---- Meeting/Call/Email ---- */
    private String location;
    private Integer callDurationMinutes;
    private String callOutcome;
    private String emailTo;
    private String emailCc;

    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
