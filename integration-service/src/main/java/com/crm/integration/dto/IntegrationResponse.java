package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationResponse {
    private UUID id;
    private String name;
    private String provider;
    private String type;
    private String status;
    private String description;
    private String authType;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastSyncAt;
}
