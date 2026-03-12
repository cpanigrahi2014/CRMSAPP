package com.crm.auth.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoleResponse {
    private UUID id;
    private String name;
    private String description;
    private List<String> permissions;
}
