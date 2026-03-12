package com.crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PermissionRequest {
    @NotBlank(message = "Permission name is required")
    private String name;
    private String description;
    @NotBlank(message = "Resource is required")
    private String resource;
    @NotBlank(message = "Actions are required")
    private String actions;
}
