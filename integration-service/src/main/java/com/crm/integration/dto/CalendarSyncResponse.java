package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarSyncResponse {
    private UUID id;
    private String provider;
    private String status;
    private String calendarId;
    private String syncDirection;
    private Integer syncIntervalMinutes;
    private LocalDateTime lastSyncAt;
    private String lastSyncStatus;
    private Integer eventsSynced;
    private boolean enabled;
    private LocalDateTime createdAt;
}
