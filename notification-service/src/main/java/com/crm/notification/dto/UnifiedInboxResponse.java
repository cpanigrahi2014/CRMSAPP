package com.crm.notification.dto;

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
public class UnifiedInboxResponse {
    private UUID id;
    private String channel;
    private String direction;
    private String sender;
    private String recipient;
    private String subject;
    private String body;
    private String status;
    private String sourceId;
    private String relatedEntityType;
    private String relatedEntityId;
    private String tenantId;
    private LocalDateTime createdAt;
}
