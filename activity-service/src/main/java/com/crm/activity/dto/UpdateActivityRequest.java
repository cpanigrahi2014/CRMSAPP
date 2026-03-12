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
public class UpdateActivityRequest {

    private String subject;
    private String description;
    private Activity.ActivityType type;
    private Activity.ActivityStatus status;
    private Activity.ActivityPriority priority;
    private LocalDateTime dueDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private UUID assignedTo;

    /* ---- Reminder ---- */
    private LocalDateTime reminderAt;

    /* ---- Recurrence ---- */
    private Activity.RecurrenceRule recurrenceRule;
    private LocalDate recurrenceEnd;

    /* ---- Meeting/Call/Email ---- */
    private String location;
    private Integer callDurationMinutes;
    private String callOutcome;
    private String emailTo;
    private String emailCc;
}
