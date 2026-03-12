package com.crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FieldSecurityRequest {
    @NotBlank(message = "Entity type is required")
    private String entityType;
    @NotBlank(message = "Field name is required")
    private String fieldName;
    @NotBlank(message = "Role name is required")
    private String roleName;
    private String accessLevel; // HIDDEN, READ_ONLY, READ_WRITE
}
