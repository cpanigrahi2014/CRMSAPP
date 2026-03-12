package com.crm.email.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailTrackingEventDto {
    private UUID id;
    private UUID messageId;
    private String eventType;
    private String linkUrl;
    private String userAgent;
    private String ipAddress;
    private LocalDateTime createdAt;
}
