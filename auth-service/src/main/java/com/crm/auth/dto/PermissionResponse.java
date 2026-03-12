package com.crm.auth.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PermissionResponse {
    private UUID id;
    private String name;
    private String description;
    private String resource;
    private String actions;
    private LocalDateTime createdAt;
}
