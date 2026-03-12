package com.crm.lead.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeadTagResponse {
    private UUID id;
    private String name;
    private String color;
    private LocalDateTime createdAt;
}
