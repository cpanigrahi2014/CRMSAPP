package com.crm.activity.dto;

import com.crm.activity.entity.Activity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateActivityRequest {

    @NotNull(message = "Activity type is required")
    private Activity.ActivityType type;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;
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
