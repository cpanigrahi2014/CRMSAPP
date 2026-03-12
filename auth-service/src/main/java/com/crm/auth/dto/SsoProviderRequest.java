package com.crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SsoProviderRequest {
    @NotBlank(message = "Provider name is required")
    private String name;
    @NotBlank(message = "Provider type is required")
    private String providerType;
    @NotBlank(message = "Client ID is required")
    private String clientId;
    private String issuerUrl;
    private String metadataUrl;
    private boolean enabled;
    private boolean autoProvision;
    private String defaultRole;
}
