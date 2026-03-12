package com.crm.lead.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeadActivityResponse {
    private UUID id;
    private UUID leadId;
    private String activityType;
    private String title;
    private String description;
    private String metadata;
    private String createdBy;
    private LocalDateTime createdAt;
}
