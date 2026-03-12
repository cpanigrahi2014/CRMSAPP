package com.crm.notification.dto;

import com.crm.notification.entity.WhatsAppMessage;
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
public class WhatsAppMessageResponse {
    private UUID id;
    private String fromNumber;
    private String toNumber;
    private String body;
    private String mediaUrl;
    private String mediaType;
    private WhatsAppMessage.MessageType messageType;
    private WhatsAppMessage.Direction direction;
    private WhatsAppMessage.WaStatus status;
    private String externalId;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private LocalDateTime readAt;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
