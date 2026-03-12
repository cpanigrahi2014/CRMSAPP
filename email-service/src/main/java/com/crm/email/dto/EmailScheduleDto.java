package com.crm.email.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailScheduleDto {
    private UUID id;
    private UUID messageId;
    private UUID templateId;
    private String toAddresses;
    private String ccAddresses;
    private String subject;
    private String bodyHtml;
    private LocalDateTime scheduledAt;
    private String status;
    private LocalDateTime sentAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private String createdBy;
}
