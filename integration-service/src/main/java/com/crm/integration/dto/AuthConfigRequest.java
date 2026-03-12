package com.crm.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthConfigRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Auth type is required")
    private String authType;

    private String clientId;
    private String tokenUrl;
    private List<String> scopes;
    private boolean active;
}
