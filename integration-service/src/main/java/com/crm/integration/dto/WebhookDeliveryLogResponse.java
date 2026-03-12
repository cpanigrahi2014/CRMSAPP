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
public class WebhookDeliveryLogResponse {
    private UUID id;
    private UUID webhookId;
    private String webhookName;
    private String eventType;
    private String payload;
    private int responseStatus;
    private String responseBody;
    private int attempt;
    private String status;
    private String errorMessage;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
}
