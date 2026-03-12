package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperApiKeyResponse {
    private UUID id;
    private String name;
    private String keyPrefix;
    private String rawKey; // Only populated on creation
    private List<String> scopes;
    private int rateLimit;
    private long callsToday;
    private long totalCalls;
    private boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private String createdBy;
    private LocalDateTime createdAt;
}
