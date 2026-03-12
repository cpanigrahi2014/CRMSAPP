package com.crm.auth.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private String ipAddress;
    private String status;
    private LocalDateTime createdAt;
}
