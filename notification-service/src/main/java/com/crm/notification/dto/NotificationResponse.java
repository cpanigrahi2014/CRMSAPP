package com.crm.notification.dto;

import com.crm.notification.entity.Notification;
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
public class NotificationResponse {
    private UUID id;
    private Notification.NotificationType type;
    private String channel;
    private String recipient;
    private String subject;
    private String body;
    private Notification.NotificationStatus status;
    private LocalDateTime sentAt;
    private String failureReason;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private Integer retryCount;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
