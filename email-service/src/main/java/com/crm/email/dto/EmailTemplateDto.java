package com.crm.email.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailTemplateDto {
    private UUID id;
    private String name;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String category;
    private String variables;      // JSON array string
    private boolean isActive;
    private int usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
