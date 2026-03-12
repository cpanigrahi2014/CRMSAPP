package com.crm.auth.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SsoProviderResponse {
    private UUID id;
    private String name;
    private String providerType;
    private String clientId;
    private String issuerUrl;
    private String metadataUrl;
    private boolean enabled;
    private boolean autoProvision;
    private String defaultRole;
    private LocalDateTime createdAt;
}
