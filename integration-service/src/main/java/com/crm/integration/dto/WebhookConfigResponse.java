package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConfigResponse {
    private UUID id;
    private String name;
    private String url;
    private List<String> events;
    private boolean active;
    private int retryCount;
    private int retryDelayMs;
    private long successCount;
    private long failureCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastTriggeredAt;
}
