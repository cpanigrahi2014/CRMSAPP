package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSyncResponse {
    private UUID id;
    private String name;
    private UUID integrationId;
    private String integrationName;
    private String entityType;
    private String direction;
    private String status;
    private String schedule;
    private LocalDateTime lastRunAt;
    private Long lastRunDuration;
    private long recordsSynced;
    private long recordsFailed;
    private Map<String, String> fieldMapping;
    private boolean enabled;
    private LocalDateTime createdAt;
}
