package com.crm.activity.dto;

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
public class ActivityStreamResponse {
    private UUID id;
    private String eventType;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String description;
    private String performedBy;
    private String performedByName;
    private String metadata;
    private LocalDateTime createdAt;
}
