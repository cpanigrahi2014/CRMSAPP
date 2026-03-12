package com.crm.notification.dto;

import com.crm.notification.entity.SmsMessage;
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
public class SmsMessageResponse {
    private UUID id;
    private String fromNumber;
    private String toNumber;
    private String body;
    private SmsMessage.Direction direction;
    private SmsMessage.SmsStatus status;
    private String externalId;
    private String errorMessage;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
