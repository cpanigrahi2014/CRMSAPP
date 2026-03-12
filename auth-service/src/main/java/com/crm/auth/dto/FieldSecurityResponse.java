package com.crm.auth.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FieldSecurityResponse {
    private UUID id;
    private String entityType;
    private String fieldName;
    private String roleName;
    private String accessLevel;
    private LocalDateTime createdAt;
}
