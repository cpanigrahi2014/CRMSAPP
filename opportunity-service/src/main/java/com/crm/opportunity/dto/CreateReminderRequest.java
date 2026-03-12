package com.crm.opportunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReminderRequest {

    @NotBlank(message = "Reminder type is required")
    private String reminderType;

    @NotBlank(message = "Reminder message is required")
    private String message;

    @NotNull(message = "Remind at date is required")
    private LocalDateTime remindAt;
}
