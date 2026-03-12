package com.crm.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Type is required")
    private String type;

    private String description;
    private String authType;
    private boolean enabled;
}
