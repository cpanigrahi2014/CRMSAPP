package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarSyncUpdateRequest {
    private String calendarId;
    private String syncDirection;
    private Integer syncIntervalMinutes;
    private Boolean enabled;
}
