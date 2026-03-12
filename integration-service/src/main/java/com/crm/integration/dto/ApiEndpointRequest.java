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
public class ApiEndpointRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Path is required")
    private String path;

    @NotBlank(message = "Method is required")
    private String method;

    private String description;
    private boolean authRequired;
    private int rateLimit;
    private boolean enabled;
    private String version;
}
