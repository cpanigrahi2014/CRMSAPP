package com.crm.lead.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeadAttachmentResponse {
    private UUID id;
    private UUID leadId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String createdBy;
    private LocalDateTime createdAt;
}
