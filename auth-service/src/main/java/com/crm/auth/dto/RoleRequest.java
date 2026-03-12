package com.crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoleRequest {
    @NotBlank(message = "Role name is required")
    private String name;
    private String description;
    private List<String> permissions;
}
